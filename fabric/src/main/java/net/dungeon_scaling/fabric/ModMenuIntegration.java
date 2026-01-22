package net.dungeon_scaling.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.dungeon_scaling.fabric.client.*;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return configScreen -> {
            GuiUtils.clearCache();
            return GuiBuilder.create(configScreen);
        };
    }
}