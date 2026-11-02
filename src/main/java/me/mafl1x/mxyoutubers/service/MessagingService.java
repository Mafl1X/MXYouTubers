package me.mafl1x.mxyoutubers.service;

import me.mafl1x.mxyoutubers.core.config.MainConfig;
import me.mafl1x.mxyoutubers.core.config.MessagesConfig;
import me.mafl1x.mxyoutubers.core.util.ColorUtil;
import me.mafl1x.mxyoutubers.core.util.PlaceholderUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class MessagingService {

    private final JavaPlugin plugin;
    private final MessagesConfig messages;

    public MessagingService(JavaPlugin plugin, MessagesConfig messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void send(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        runSync(new Runnable() {
            @Override
            public void run() {
                sender.sendMessage(ColorUtil.colorize(messages.prefix + message));
            }
        });
    }

    public void sendRaw(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        runSync(new Runnable() {
            @Override
            public void run() {
                sender.sendMessage(ColorUtil.colorize(message));
            }
        });
    }

    public void sendInstruction(Player player, MainConfig config) {
        Map<String, String> placeholders = PlaceholderUtil.map(
                "min_subscribers", String.valueOf(config.requirements.minSubscribers),
                "min_views", String.valueOf(config.requirements.minVideoViews),
                "server_ip", config.requirements.serverIp,
                "hashtag", config.requirements.requiredHashtag
        );
        for (String line : messages.instructionLines) {
            sendRaw(player, PlaceholderUtil.apply(line, placeholders));
        }
        TextComponent ipButton = new TextComponent(ColorUtil.colorize(messages.instructionCopyIp));
        ipButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, config.requirements.serverIp));
        TextComponent separator = new TextComponent(ColorUtil.colorize(" &8| "));
        TextComponent hashtagButton = new TextComponent(ColorUtil.colorize(messages.instructionCopyHashtag));
        hashtagButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, config.requirements.requiredHashtag));
        TextComponent separator2 = new TextComponent(ColorUtil.colorize(" &8| "));
        TextComponent mediaButton = new TextComponent(ColorUtil.colorize(messages.instructionRunMedia));
        mediaButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/media"));
        player.spigot().sendMessage(new ComponentBuilder().append(ipButton).append(separator).append(hashtagButton).append(separator2).append(mediaButton).create());
    }

    public void sendConfirmationPrompt(Player player, String code) {
        send(player, PlaceholderUtil.apply(messages.confirmationInstruction, PlaceholderUtil.map("code", code)));
        TextComponent codeComponent = new TextComponent(ColorUtil.colorize("&d&l[" + code + "]"));
        codeComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, code));
        codeComponent.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ColorUtil.colorize("&7Скопировать")).create()
        ));
        TextComponent confirm = new TextComponent(ColorUtil.colorize(messages.confirmButton));
        confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/media confirm"));
        TextComponent spacer = new TextComponent(" ");
        TextComponent cancel = new TextComponent(ColorUtil.colorize(messages.cancelButton));
        cancel.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/media cancel"));
        runSync(new Runnable() {
            @Override
            public void run() {
                player.spigot().sendMessage(codeComponent);
                player.spigot().sendMessage(new ComponentBuilder().append(confirm).append(spacer).append(cancel).create());
            }
        });
    }

    public void notifyAdmins(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("mxyoutubers.admin")) {
                send(player, message);
            }
        }
    }

    public void sendIfOnline(java.util.UUID playerUuid, String message) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            send(player, message);
        }
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }
}
