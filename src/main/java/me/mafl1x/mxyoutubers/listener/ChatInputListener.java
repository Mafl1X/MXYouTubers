package me.mafl1x.mxyoutubers.listener;

import me.mafl1x.mxyoutubers.core.config.MessagesConfig;
import me.mafl1x.mxyoutubers.database.model.ApplicationType;
import me.mafl1x.mxyoutubers.service.ApplicationService;
import me.mafl1x.mxyoutubers.service.InputSession;
import me.mafl1x.mxyoutubers.service.InputSessionService;
import me.mafl1x.mxyoutubers.service.InputType;
import me.mafl1x.mxyoutubers.service.MediaService;
import me.mafl1x.mxyoutubers.service.MessagingService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatInputListener implements Listener {

    private final MessagesConfig messages;
    private final MessagingService messagingService;
    private final InputSessionService inputSessionService;
    private final ApplicationService applicationService;
    private final MediaService mediaService;

    public ChatInputListener(
            MessagesConfig messages,
            MessagingService messagingService,
            InputSessionService inputSessionService,
            ApplicationService applicationService,
            MediaService mediaService
    ) {
        this.messages = messages;
        this.messagingService = messagingService;
        this.inputSessionService = inputSessionService;
        this.applicationService = applicationService;
        this.mediaService = mediaService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        InputSession session = inputSessionService.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        if ("отмена".equalsIgnoreCase(message)) {
            inputSessionService.remove(event.getPlayer().getUniqueId());
            messagingService.send(event.getPlayer(), messages.inputCancelled);
            return;
        }
        inputSessionService.remove(event.getPlayer().getUniqueId());
        if (session.getType() == InputType.SUBMIT_VIDEO) {
            applicationService.submit(event.getPlayer(), message, ApplicationType.INITIAL);
            return;
        }
        if (session.getType() == InputType.RESTORE_VIDEO) {
            applicationService.submit(event.getPlayer(), message, ApplicationType.RESTORATION);
            return;
        }
        if (session.getType() == InputType.PROMO_CODE) {
            mediaService.changePromoCode(event.getPlayer(), message);
            return;
        }
        if (session.getType() == InputType.ADMIN_REJECTION_REASON && session.getApplicationId() != null) {
            applicationService.reject(event.getPlayer(), session.getApplicationId().longValue(), message);
            return;
        }
        if (session.getType() == InputType.ADMIN_SUSPEND_REASON && session.getTargetPlayerUuid() != null) {
            mediaService.findByPlayer(session.getTargetPlayerUuid()).thenCompose(optionalMedia -> {
                if (!optionalMedia.isPresent()) {
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }
                return mediaService.suspendMedia(optionalMedia.get(), message, event.getPlayer(), true);
            });
        }
    }
}
