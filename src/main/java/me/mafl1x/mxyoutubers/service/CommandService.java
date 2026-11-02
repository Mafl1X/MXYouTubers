package me.mafl1x.mxyoutubers.service;

import me.mafl1x.mxyoutubers.core.util.ColorUtil;
import me.mafl1x.mxyoutubers.core.util.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public final class CommandService {

    private final JavaPlugin plugin;

    public CommandService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void dispatchConsoleCommands(List<String> commands, Map<String, String> placeholders) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                for (String raw : commands) {
                    String command = ColorUtil.colorize(PlaceholderUtil.apply(raw, placeholders));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.startsWith("/") ? command.substring(1) : command);
                }
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
