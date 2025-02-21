package ca.skynetcloud.cobbleteamvalidator;

import ca.skynetcloud.cobbleteamvalidator.commands.FormatCommand;
import ca.skynetcloud.cobbleteamvalidator.commands.ValidatorCommand;
import ca.skynetcloud.cobbleteamvalidator.config.FormatConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobbleTeamValidator implements ModInitializer {

    public static final String NAME = "CobbleTeamValidator";
    public static final Logger LOGGER = LoggerFactory.getLogger("cobbleteamvalidator");
    public static final String MODID = "cobbleteamvalidator";
    public static final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onInitialize() {


        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> {
            LOGGER.info(
                    "Starting up %n by %authors %v".replace("%n", NAME).replace("%authors", "SkyNetCloud").replace("%v",
                            "0.0.1"
                    )
            );
        });


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ValidatorCommand.registerCommand(dispatcher);
            FormatCommand.register(dispatcher);
        });

        FormatConfig.loadFormats();


    }



}
