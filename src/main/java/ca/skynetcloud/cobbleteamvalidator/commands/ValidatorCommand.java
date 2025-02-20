package ca.skynetcloud.cobbleteamvalidator.commands;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static ca.skynetcloud.cobbleteamvalidator.CobbleTeamValidator.miniMessage;
import static ca.skynetcloud.cobbleteamvalidator.config.FormatConfig.formatsData;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ValidatorCommand {


    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("validate")
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                .then(argument("format", StringArgumentType.string())
                        .suggests(FORMAT_SUGGESTIONS) // Add tab completion
                        .executes(context -> {
                            String format = StringArgumentType.getString(context, "format");
                            return handleValidateCommand(context.getSource(), format);
                        })
                )
        );
    }

    private static int handleValidateCommand(ServerCommandSource source, String format) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(Objects.requireNonNull(source.getPlayer()));


        long count = party.toGappyList().stream().filter(Objects::nonNull).count();

        if (count < 1 || count > 6) {
            sendMiniMessage(source, "<red>You must have between 1 and 6 Pokémon in your party to validate.</red>");
            return Command.SINGLE_SUCCESS;
        }

        String validationMessage = validateTeamForFormat(format, party);
        sendMiniMessage(source, validationMessage);

        return Command.SINGLE_SUCCESS;
    }

    private static void sendMiniMessage(ServerCommandSource source, String message) {
        ServerPlayerEntity player = source.getPlayer();
        if (player != null) {
            FabricServerAudiences audiences = FabricServerAudiences.of(source.getServer());
            Component component = miniMessage.deserialize(message);
            audiences.players().sendMessage(component);
        }
    }

    private static @NotNull String validateTeamForFormat(String format, PlayerPartyStore party) {
        if (formatsData == null || !formatsData.has(format)) {
            return "<red>Unknown format: <gold>" + format + "</gold>. Please check the format name.</red>";
        }

        JsonObject formatRules = formatsData.getAsJsonObject(format);
        String originalName = formatRules.has("original") ? formatRules.get("original").getAsString() : format;

        Set<String> bannedSet = new HashSet<>();

        if (formatRules.has("banned_pokemon")) {
            for (JsonElement bannedPokemonElement : formatRules.getAsJsonArray("banned_pokemon")) {
                String bannedPokemon = bannedPokemonElement.getAsString().toLowerCase();

                for (Pokemon pokemon : party.toGappyList()) {
                    if (pokemon != null && pokemon.getSpecies().getName().equalsIgnoreCase(bannedPokemon)) {
                        bannedSet.add("<gold>" + bannedPokemon + "</gold>");
                    }
                }
            }
        }

        if (!bannedSet.isEmpty()) {
            return "<red>Your team contains banned Pokémon: <newline>" + String.join(", ", bannedSet) +
                    " (Not allowed in <blue>" + originalName + "</blue>).</red>";
        }

        return "<green>Your team is valid for the <blue>" + originalName + "</blue> format!</green>";
    }

    private static final SuggestionProvider<ServerCommandSource> FORMAT_SUGGESTIONS = (context, builder) -> {
        if (formatsData != null) {
            for (String format : formatsData.keySet()) {
                builder.suggest(format);
            }
        }
        return CompletableFuture.supplyAsync(builder::build);
    };
}
