package me.mafl1x.mxyoutubers.core.config;

import de.exlll.configlib.YamlConfigurations;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private MainConfig mainConfig;
    private MessagesConfig messagesConfig;
    private GuiConfig guiConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            Path dataFolder = plugin.getDataFolder().toPath();
            Files.createDirectories(dataFolder);
            this.mainConfig = YamlConfigurations.update(dataFolder.resolve("config.yml"), MainConfig.class);
            this.messagesConfig = YamlConfigurations.update(dataFolder.resolve("messages.yml"), MessagesConfig.class);
            this.guiConfig = YamlConfigurations.update(dataFolder.resolve("gui.yml"), GuiConfig.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create plugin data folder", exception);
        }
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }
}
