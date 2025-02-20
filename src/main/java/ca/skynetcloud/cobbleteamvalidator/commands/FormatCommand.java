package ca.skynetcloud.cobbleteamvalidator.commands;

import ca.skynetcloud.cobbleteamvalidator.config.FormatConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static ca.skynetcloud.cobbleteamvalidator.CobbleTeamValidator.*;
import static ca.skynetcloud.cobbleteamvalidator.config.FormatConfig.formatsData;

public class FormatCommand {
    private static final int FORMATS_PER_PAGE = 5; // Change this number to adjust how many formats per page

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

            // Ensure the requested page is within range
            if (page > totalPages || page < 1) {
                context.getSource().sendFeedback(() -> Text.literal("Invalid page number. There are " + totalPages + " pages."), false);
                return 0;
            }

            // Calculate the start and end indices for the current page
            int start = (page - 1) * FORMATS_PER_PAGE;
            int end = Math.min(start + FORMATS_PER_PAGE, totalFormats);

            // Build the paginated list
            StringBuilder formatsList = new StringBuilder("Available formats (Page " + page + "/" + totalPages + "):\n");
            for (int i = start; i < end; i++) {
                formatsList.append("- ").append(formatNames.get(i)).append("\n");
            }

            // Add page navigation instructions
            if (page < totalPages) {
                formatsList.append("\nType /listformats ").append(page + 1).append(" to see the next page.");
            }

            context.getSource().sendFeedback(() -> Text.literal(formatsList.toString()), false);
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error while listing formats: " + e.getMessage(), e);
            context.getSource().sendFeedback(() -> Text.literal("An error occurred while fetching formats."), false);
            return 0;
        }
    }
}

