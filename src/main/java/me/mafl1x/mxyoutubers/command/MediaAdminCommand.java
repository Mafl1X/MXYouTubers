package me.mafl1x.mxyoutubers.command;

import me.mafl1x.mxyoutubers.core.config.MessagesConfig;
import me.mafl1x.mxyoutubers.gui.GuiManager;
import me.mafl1x.mxyoutubers.service.MessagingService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MediaAdminCommand implements CommandExecutor {

    private final MessagesConfig messages;
    private final MessagingService messagingService;
    private final GuiManager guiManager;

    public MediaAdminCommand(MessagesConfig messages, MessagingService messagingService, GuiManager guiManager) {
        this.messages = messages;
        this.messagingService = messagingService;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messagingService.send(sender, messages.playerOnly);
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("mxyoutubers.admin")) {
            messagingService.send(player, messages.noPermission);
            return true;
        }
        guiManager.openAdminRoot(player);
        return true;
    }
}
