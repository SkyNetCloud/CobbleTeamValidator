package ca.skynetcloud.cobbleteamvalidator.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ca.skynetcloud.cobbleteamvalidator.CobbleTeamValidator.LOGGER;
import static ca.skynetcloud.cobbleteamvalidator.CobbleTeamValidator.miniMessage;
import static ca.skynetcloud.cobbleteamvalidator.config.FormatConfig.formatsData;

public class FormatCommand {
    private static final int FORMATS_PER_PAGE = 10;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("listformats")
                .executes(context -> listFormats(context, 1)) // Default to page 1
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1)) // Ensure the page is 1 or greater
                        .executes(context -> {
                            int page = IntegerArgumentType.getInteger(context, "page");
                            return listFormats(context, page);
                        })));
    }

    private static int listFormats(CommandContext<ServerCommandSource> context, int page) {
        try {
            Set<String> formatNamesSet = formatsData.keySet();
            List<String> formatNames = new ArrayList<>(formatNamesSet); // Convert to a list for indexing
            int totalFormats = formatNames.size();
            int totalPages = (int) Math.ceil((double) totalFormats / FORMATS_PER_PAGE);

            if (page > totalPages || page < 1) {
                sendMiniMessage(context.getSource(), "<red>Invalid page number. There are " + totalPages + " pages.</red>");
                return 0;
            }

            int start = (page - 1) * FORMATS_PER_PAGE;
            int end = Math.min(start + FORMATS_PER_PAGE, totalFormats);



            StringBuilder formatsList = new StringBuilder("<gold>Available formats (Page " + page + "/" + totalPages + "):</gold>\n");
            for (int i = start; i < end; i++) {
                String formatKey = formatNames.get(i);
                String originalName = getOriginalName(formatKey);
                String rulesText = getRulesText(formatKey); // Get formatted rules

                // Build hover text
                String hoverText = "<#03c6fc>" + formatKey;
                if (!rulesText.isEmpty()) {
                    hoverText += "\n<yellow>Rules:</yellow>\n" + rulesText;
                }

                // Apply hover effect only to the format name
                formatsList.append("<hover:show_text:'").append(hoverText).append("'>")
                        .append("<green>- ").append(originalName).append("</hover>\n");
            }

            if (page < totalPages) {
                formatsList.append("\n<yellow>Type /listformats ").append(page + 1).append(" to see the next page.</yellow>");
                formatsList.append("\n<#ADD8E6> Note that the formats names are there Showdown default names /validate [formats] \n uses converted name which are tab completable</#ADD8E6>");
            }

            sendMiniMessage(context.getSource(), formatsList.toString());

            return 1;

        } catch (Exception e) {
            LOGGER.error("Error while listing formats: " + e.getMessage(), e);
            sendMiniMessage(context.getSource(), "<red>An error occurred while fetching formats.</red>");
            return 0;
        }
    }


    private static void sendMiniMessage(ServerCommandSource source, String message) {
        ServerPlayerEntity player = source.getPlayer();
        if (player != null) {
            FabricServerAudiences audiences = FabricServerAudiences.of(source.getServer());
            Component component = miniMessage.deserialize(message);
            audiences.players().sendMessage(component);
        }
    }


    private static String getOriginalName(String formatKey) {
        JsonElement element = formatsData.get(formatKey);
        if (element != null && element.isJsonObject()) {
            JsonObject formatObject = element.getAsJsonObject();
            if (formatObject.has("original")) {
                return formatObject.get("original").getAsString();
            }
        }
        return formatKey; // Fallback if "originalName" is missing
    }

    private static String getRulesText(String formatKey) {
        JsonElement element = formatsData.get(formatKey);
        if (element != null && element.isJsonObject()) {
            JsonObject formatObject = element.getAsJsonObject();
            if (formatObject.has("rules") && formatObject.get("rules").isJsonArray()) {
                StringBuilder rulesList = new StringBuilder();
                formatObject.get("rules").getAsJsonArray().forEach(rule ->
                        rulesList.append("<gray>- ").append(rule.getAsString()).append("</gray>\n")
                );
                return rulesList.toString().trim();
            }
        }
        return "";
    }
}
