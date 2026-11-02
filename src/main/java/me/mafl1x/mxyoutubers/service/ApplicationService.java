package me.mafl1x.mxyoutubers.service;

import me.mafl1x.mxyoutubers.api.YouTubeApiClient;
import me.mafl1x.mxyoutubers.api.YouTubeChannel;
import me.mafl1x.mxyoutubers.api.YouTubeVideo;
import me.mafl1x.mxyoutubers.core.config.MainConfig;
import me.mafl1x.mxyoutubers.core.config.MessagesConfig;
import me.mafl1x.mxyoutubers.core.util.PlaceholderUtil;
import me.mafl1x.mxyoutubers.core.util.TimeUtil;
import me.mafl1x.mxyoutubers.core.util.YouTubeUrlUtil;
import me.mafl1x.mxyoutubers.database.model.ApplicationStatus;
import me.mafl1x.mxyoutubers.database.model.ApplicationType;
import me.mafl1x.mxyoutubers.database.model.MediaApplication;
import me.mafl1x.mxyoutubers.database.model.MediaChannel;
import me.mafl1x.mxyoutubers.database.model.MediaStatus;
import me.mafl1x.mxyoutubers.database.repository.ApplicationRepository;
import me.mafl1x.mxyoutubers.database.repository.MediaRepository;
import me.mafl1x.mxyoutubers.media.VideoRequirementsChecker;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ApplicationService {

    private final JavaPlugin plugin;
    private final MainConfig mainConfig;
    private final MessagesConfig messages;
    private final ApplicationRepository applicationRepository;
    private final MediaRepository mediaRepository;
    private final YouTubeApiClient youTubeApiClient;
    private final VideoRequirementsChecker requirementsChecker;
    private final MediaService mediaService;
    private final MessagingService messagingService;
    private final DiscordWebhookService discordWebhookService;
    private final AuditLogService auditLogService;
    private final SecureRandom random = new SecureRandom();

    public ApplicationService(
            JavaPlugin plugin,
            MainConfig mainConfig,
            MessagesConfig messages,
            ApplicationRepository applicationRepository,
            MediaRepository mediaRepository,
            YouTubeApiClient youTubeApiClient,
            VideoRequirementsChecker requirementsChecker,
            MediaService mediaService,
            MessagingService messagingService,
            DiscordWebhookService discordWebhookService,
            AuditLogService auditLogService
    ) {
        this.plugin = plugin;
        this.mainConfig = mainConfig;
        this.messages = messages;
        this.applicationRepository = applicationRepository;
        this.mediaRepository = mediaRepository;
        this.youTubeApiClient = youTubeApiClient;
        this.requirementsChecker = requirementsChecker;
        this.mediaService = mediaService;
        this.messagingService = messagingService;
        this.discordWebhookService = discordWebhookService;
        this.auditLogService = auditLogService;
    }

    public CompletableFuture<Void> submit(Player player, String rawLink, ApplicationType type) {
        final String videoId = YouTubeUrlUtil.extractVideoId(rawLink);
        if (videoId == null) {
            messagingService.send(player, messages.invalidVideoLink);
            return CompletableFuture.completedFuture(null);
        }
        return mediaRepository.findByPlayer(player.getUniqueId()).thenCompose(existingMedia -> {
            String accessError = validateState(existingMedia, type);
            if (accessError != null) {
                messagingService.send(player, accessError);
                return CompletableFuture.completedFuture(null);
            }
            return applicationRepository.findPendingByPlayer(player.getUniqueId()).thenCompose(pending -> {
                if (pending.isPresent()) {
                    messagingService.send(player, messages.pendingApplicationExists);
                    return CompletableFuture.completedFuture(null);
                }
                return checkCooldown(player, type).thenCompose(cooldownBlocked -> {
                    if (cooldownBlocked) {
                        return CompletableFuture.completedFuture(null);
                    }
                    CompletableFuture<YouTubeVideo> videoFuture = youTubeApiClient.fetchVideo(videoId);
                    return videoFuture.thenCompose(video -> youTubeApiClient.fetchChannel(video.getChannelId()).thenCompose(channel -> {
                        if (type == ApplicationType.RESTORATION && existingMedia.isPresent()
                                && !existingMedia.get().getChannelId().equalsIgnoreCase(channel.getId())) {
                            return autoReject(player, "ролик должен быть опубликован на уже привязанном канале", type);
                        }
                        String validationFailure = requirementsChecker.validate(channel, video);
                        if (validationFailure != null) {
                            return autoReject(player, validationFailure, type);
                        }
                        return createApplication(player, type, channel, video);
                    }));
                });
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().warning("Could not submit application: " + throwable.getMessage());
            messagingService.send(player, "&cВо время проверки произошла ошибка. Проверь API ключ и попробуй позже.");
            return null;
        });
    }

    public CompletableFuture<Void> confirmPending(Player player) {
        return applicationRepository.findPendingByPlayer(player.getUniqueId()).thenCompose(optionalApplication -> {
            if (!optionalApplication.isPresent() || optionalApplication.get().getStatus() != ApplicationStatus.WAITING_CODE_CONFIRMATION) {
                messagingService.send(player, messages.noPendingReview);
                return CompletableFuture.completedFuture(null);
            }
            MediaApplication application = optionalApplication.get();
            return youTubeApiClient.fetchFreshVideo(application.getVideoId()).thenCompose(video -> {
                if (!requirementsChecker.containsConfirmationCode(video, application.getConfirmationCode())) {
                    messagingService.send(player, messages.codeNotFound);
                    return CompletableFuture.completedFuture(null);
                }
                application.setStatus(ApplicationStatus.WAITING_ADMIN_REVIEW);
                application.setUpdatedAt(Instant.now());
                application.setVideoDescription(video.getDescription());
                return applicationRepository.update(application).thenCompose(unused -> {
                    messagingService.send(player, messages.sentToReview);
                    messagingService.notifyAdmins(PlaceholderUtil.apply(
                            messages.adminNotification,
                            PlaceholderUtil.map("type", formatType(application.getType()), "player", application.getPlayerName())
                    ));
                    auditLogService.logSystem(application.getPlayerUuid(), "application_review", "application " + application.getId() + " sent for review");
                    return discordWebhookService.sendApplication(application);
                });
            });
        });
    }

    public CompletableFuture<Void> cancelPending(Player player) {
        return applicationRepository.findPendingByPlayer(player.getUniqueId()).thenCompose(optionalApplication -> {
            if (!optionalApplication.isPresent()) {
                messagingService.send(player, messages.noPendingReview);
                return CompletableFuture.completedFuture(null);
            }
            MediaApplication application = optionalApplication.get();
            application.setStatus(ApplicationStatus.CANCELLED);
            application.setUpdatedAt(Instant.now());
            application.setReason("cancelled_by_player");
            return applicationRepository.update(application).thenAccept(unused -> messagingService.send(player, messages.inputCancelled));
        });
    }

    public CompletableFuture<Void> approve(CommandSender actor, long applicationId) {
        return applicationRepository.findById(applicationId).thenCompose(optionalApplication -> {
            if (!optionalApplication.isPresent()) {
                messagingService.send(actor, messages.noPendingReview);
                return CompletableFuture.completedFuture(null);
            }
            MediaApplication application = optionalApplication.get();
            if (application.getStatus() != ApplicationStatus.WAITING_ADMIN_REVIEW) {
                messagingService.send(actor, messages.noPendingReview);
                return CompletableFuture.completedFuture(null);
            }
            application.setStatus(ApplicationStatus.APPROVED);
            application.setUpdatedAt(Instant.now());
            return applicationRepository.update(application)
                    .thenCompose(unused -> mediaService.activateFromApplication(application, actor))
                    .thenAccept(unused -> messagingService.send(actor, messages.actionCompleted));
        });
    }

    public CompletableFuture<Void> reject(CommandSender actor, long applicationId, String reason) {
        return applicationRepository.findById(applicationId).thenCompose(optionalApplication -> {
            if (!optionalApplication.isPresent()) {
                messagingService.send(actor, messages.noPendingReview);
                return CompletableFuture.completedFuture(null);
            }
            MediaApplication application = optionalApplication.get();
            if (application.getStatus() != ApplicationStatus.WAITING_ADMIN_REVIEW) {
                messagingService.send(actor, messages.noPendingReview);
                return CompletableFuture.completedFuture(null);
            }
            application.setStatus(ApplicationStatus.REJECTED);
            application.setReason(reason);
            application.setUpdatedAt(Instant.now());
            return applicationRepository.update(application)
                    .thenCompose(unused -> applicationRepository.addRefusal(application.getPlayerUuid(), reason, "admin"))
                    .thenAccept(unused -> {
                        messagingService.sendIfOnline(application.getPlayerUuid(), PlaceholderUtil.apply(
                                messages.applicationRejected,
                                PlaceholderUtil.map("reason", reason)
                        ));
                        messagingService.send(actor, messages.actionCompleted);
                        auditLogService.log(actor, application.getPlayerUuid(), "application_reject", reason);
                    });
        });
    }

    private String validateState(Optional<MediaChannel> existingMedia, ApplicationType type) {
        if (type == ApplicationType.INITIAL) {
            if (existingMedia.isPresent()) {
                if (existingMedia.get().getStatus() == MediaStatus.ACTIVE) {
                    return messages.activeMediaExists;
                }
                return messages.channelInactive;
            }
            return null;
        }
        if (!existingMedia.isPresent()) {
            return messages.channelNotLinked;
        }
        if (existingMedia.get().getStatus() != MediaStatus.SUSPENDED) {
            return messages.activeMediaExists;
        }
        return null;
    }

    private CompletableFuture<Boolean> checkCooldown(Player player, ApplicationType type) {
        return applicationRepository.findLastApplicationAt(player.getUniqueId(), type).thenApply(optionalInstant -> {
            if (!optionalInstant.isPresent()) {
                return Boolean.FALSE;
            }
            int hours = type == ApplicationType.INITIAL ? mainConfig.cooldowns.applicationHours : mainConfig.cooldowns.restorationHours;
            Instant nextTime = optionalInstant.get().plus(Duration.ofHours(hours));
            if (!nextTime.isAfter(Instant.now())) {
                return Boolean.FALSE;
            }
            String formatted = TimeUtil.formatDuration(Duration.between(Instant.now(), nextTime));
            String message = type == ApplicationType.INITIAL ? messages.applicationCooldown : messages.restorationCooldown;
            messagingService.send(player, PlaceholderUtil.apply(message, PlaceholderUtil.map("time", formatted)));
            return Boolean.TRUE;
        });
    }

    private CompletableFuture<Void> autoReject(Player player, String reason, ApplicationType type) {
        return applicationRepository.addRefusal(player.getUniqueId(), reason, type.name().toLowerCase(Locale.ROOT))
                .thenAccept(unused -> messagingService.send(player, PlaceholderUtil.apply(
                        messages.autoRejectReason,
                        PlaceholderUtil.map("reason", reason)
                )));
    }

    private CompletableFuture<Void> createApplication(Player player, ApplicationType type, YouTubeChannel channel, YouTubeVideo video) {
        MediaApplication application = new MediaApplication();
        application.setPlayerUuid(player.getUniqueId());
        application.setPlayerName(player.getName());
        application.setChannelId(channel.getId());
        application.setChannelTitle(channel.getTitle());
        application.setChannelUrl(channel.getUrl());
        application.setVideoId(video.getId());
        application.setVideoUrl(video.getUrl());
        application.setVideoTitle(video.getTitle());
        application.setVideoDescription(video.getDescription());
        application.setChannelSubscribers(channel.getSubscribers());
        application.setVideoViews(video.getViewCount());
        application.setType(type);
        application.setStatus(ApplicationStatus.WAITING_CODE_CONFIRMATION);
        application.setConfirmationCode(generateCode());
        application.setCreatedAt(Instant.now());
        application.setUpdatedAt(Instant.now());
        return applicationRepository.create(application).thenAccept(id -> {
            application.setId(id.longValue());
            messagingService.sendConfirmationPrompt(player, application.getConfirmationCode());
            auditLogService.logSystem(application.getPlayerUuid(), "application_create", "application " + id + " created");
        });
    }

    private String generateCode() {
        int number = 1000 + random.nextInt(9000);
        return "LoneGrief-" + number;
    }

    private String formatType(ApplicationType type) {
        return type == ApplicationType.RESTORATION ? "восстановление" : "первичная";
    }
}
