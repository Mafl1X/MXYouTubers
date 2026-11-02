package me.mafl1x.mxyoutubers.service;

import me.mafl1x.mxyoutubers.core.config.MainConfig;
import me.mafl1x.mxyoutubers.database.model.MediaChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class LevelService {

    private final List<MainConfig.LevelDefinition> levels;
    private final CommandService commandService;

    public LevelService(List<MainConfig.LevelDefinition> levels, CommandService commandService) {
        this.levels = new ArrayList<MainConfig.LevelDefinition>(levels);
        this.commandService = commandService;
        Collections.sort(this.levels, new Comparator<MainConfig.LevelDefinition>() {
            @Override
            public int compare(MainConfig.LevelDefinition left, MainConfig.LevelDefinition right) {
                return Long.compare(left.minSubscribers, right.minSubscribers);
            }
        });
    }

    public MainConfig.LevelDefinition resolve(long subscribers) {
        MainConfig.LevelDefinition current = levels.isEmpty() ? new MainConfig.LevelDefinition() : levels.get(0);
        for (MainConfig.LevelDefinition level : levels) {
            if (subscribers >= level.minSubscribers) {
                current = level;
            }
        }
        return current;
    }

    public void applyLevel(MediaChannel channel, Map<String, String> placeholders) {
        MainConfig.LevelDefinition resolved = resolve(channel.getSubscribers());
        String previousId = channel.getLevelId();
        if (previousId == null || previousId.trim().isEmpty()) {
            channel.setLevelId(resolved.id);
            commandService.dispatchConsoleCommands(resolved.commands, withLevel(placeholders, resolved.displayName));
            return;
        }
        MainConfig.LevelDefinition current = find(previousId);
        if (current == null) {
            channel.setLevelId(resolved.id);
            commandService.dispatchConsoleCommands(resolved.commands, withLevel(placeholders, resolved.displayName));
            return;
        }
        if (resolved.minSubscribers > current.minSubscribers) {
            channel.setLevelId(resolved.id);
            commandService.dispatchConsoleCommands(resolved.commands, withLevel(placeholders, resolved.displayName));
        } else {
            channel.setLevelId(current.id);
        }
    }

    public String getDisplayName(String levelId) {
        for (MainConfig.LevelDefinition level : levels) {
            if (level.id.equalsIgnoreCase(levelId)) {
                return level.displayName;
            }
        }
        return levelId == null ? "-" : levelId;
    }

    private MainConfig.LevelDefinition find(String levelId) {
        for (MainConfig.LevelDefinition level : levels) {
            if (level.id.equalsIgnoreCase(levelId)) {
                return level;
            }
        }
        return null;
    }

    private Map<String, String> withLevel(Map<String, String> placeholders, String levelName) {
        java.util.HashMap<String, String> copy = new java.util.HashMap<String, String>();
        if (placeholders != null) {
            copy.putAll(placeholders);
        }
        copy.put("level", levelName);
        return copy;
    }
}
