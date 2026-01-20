package net.dungeon_scaling;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.dungeon_scaling.config.ConfigServer;
import net.dungeon_scaling.logic.ItemScaling;
import net.dungeon_scaling.logic.LocalScalingLootFunction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class DungeonScaling {
    public static final String MODID = "dungeon_scaling";

    public static void init() {
        AutoConfig.register(ConfigServer.class, GsonConfigSerializer::new);
        ItemScaling.initialize();
    }

    public static void registerLootFunctions() {
        Registry.register(Registries.LOOT_FUNCTION_TYPE, LocalScalingLootFunction.ID, LocalScalingLootFunction.TYPE);
    }
}
