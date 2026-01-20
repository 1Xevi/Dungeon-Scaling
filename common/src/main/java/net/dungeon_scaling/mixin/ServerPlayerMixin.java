package net.dungeon_scaling.mixin;

import net.dungeon_scaling.logic.Difficulty;
import net.dungeon_scaling.logic.DifficultyHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerMixin implements DifficultyHandler {
    private List<Difficulty.Announcement> lastDifficultyAnnouncement = new ArrayList<>();

    @Override
    public List<Difficulty.Announcement> getLastDifficultyAnnouncements() {
        return lastDifficultyAnnouncement;
    }
}
