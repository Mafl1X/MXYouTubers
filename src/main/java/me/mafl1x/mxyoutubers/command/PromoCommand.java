package me.mafl1x.mxyoutubers.command;

import me.mafl1x.mxyoutubers.core.config.MessagesConfig;
import me.mafl1x.mxyoutubers.service.MessagingService;
import me.mafl1x.mxyoutubers.service.ReferralService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PromoCommand implements CommandExecutor {

    private final MessagesConfig messages;
    private final MessagingService messagingService;
    private final ReferralService referralService;

    public PromoCommand(MessagesConfig messages, MessagingService messagingService, ReferralService referralService) {
        this.messages = messages;
        this.messagingService = messagingService;
        this.referralService = referralService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messagingService.send(sender, messages.playerOnly);
            return true;
        }
        if (args.length < 1) {
            messagingService.send(sender, "&cИспользование: /promo <код>");
            return true;
        }
        referralService.applyPromo((Player) sender, args[0]);
        return true;
    }
}
