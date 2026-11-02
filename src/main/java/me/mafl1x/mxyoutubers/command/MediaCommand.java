package me.mafl1x.mxyoutubers.command;

import me.mafl1x.mxyoutubers.core.config.MessagesConfig;
import me.mafl1x.mxyoutubers.gui.GuiManager;
import me.mafl1x.mxyoutubers.service.ApplicationService;
import me.mafl1x.mxyoutubers.service.MessagingService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MediaCommand implements CommandExecutor {

    private final MessagesConfig messages;
    private final MessagingService messagingService;
    private final GuiManager guiManager;
    private final ApplicationService applicationService;

    public MediaCommand(
            MessagesConfig messages,
            MessagingService messagingService,
            GuiManager guiManager,
            ApplicationService applicationService
    ) {
        this.messages = messages;
        this.messagingService = messagingService;
        this.guiManager = guiManager;
        this.applicationService = applicationService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messagingService.send(sender, messages.playerOnly);
            return true;
        }
        Player player = (Player) sender;
        if (args.length > 0) {
            if ("confirm".equalsIgnoreCase(args[0])) {
                applicationService.confirmPending(player);
                return true;
            }
            if ("cancel".equalsIgnoreCase(args[0])) {
                applicationService.cancelPending(player);
                return true;
            }
        }
        guiManager.openPlayerMenu(player);
        return true;
    }
}
