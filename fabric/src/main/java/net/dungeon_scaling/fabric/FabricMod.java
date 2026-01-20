package net.dungeon_scaling.fabric;

import net.dungeon_scaling.DungeonScaling;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.dungeon_scaling.config.Commands;

public final class FabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        DungeonScaling.init();
        DungeonScaling.registerLootFunctions();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            Commands.register(dispatcher); // Call the common logic
        });
    }
}
