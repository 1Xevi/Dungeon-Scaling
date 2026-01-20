package net.dungeon_scaling.logic;

import java.util.List;

public interface DifficultyHandler {
    List<Difficulty.Announcement> getLastDifficultyAnnouncements();
}
