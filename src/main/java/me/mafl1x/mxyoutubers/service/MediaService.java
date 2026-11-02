package me.mafl1x.mxyoutubers.service;

import me.mafl1x.mxyoutubers.api.YouTubeApiClient;
import me.mafl1x.mxyoutubers.api.YouTubeChannel;
import me.mafl1x.mxyoutubers.api.YouTubeVideo;
import me.mafl1x.mxyoutubers.core.config.MainConfig;
import me.mafl1x.mxyoutubers.core.config.MessagesConfig;
import me.mafl1x.mxyoutubers.core.util.PlaceholderUtil;
import me.mafl1x.mxyoutubers.core.util.TimeUtil;
import me.mafl1x.mxyoutubers.database.model.MediaApplication;
import me.mafl1x.mxyoutubers.database.model.MediaChannel;
import me.mafl1x.mxyoutubers.database.model.MediaStatus;
import me.mafl1x.mxyoutubers.database.repository.MediaRepository;
import me.mafl1x.mxyoutubers.database.repository.ReferralRepository;
import me.mafl1x.mxyoutubers.media.VideoRequirementsChecker;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MediaService {

    private final JavaPlugin plugin;
    private final MainConfig mainConfig;
    private final MessagesConfig messages;
    private final MediaRepository mediaRepository;
    private final ReferralRepository referralRepository;
    private final YouTubeApiClient youTubeApiClient;
    private final VideoRequirementsChecker requirementsChecker;
    private final LevelService levelService;
    private final RewardService rewardService;
    private final AuditLogService auditLogService;
    private final CommandService commandService;
    private final MessagingService messagingService;

    public MediaService(
            JavaPlugin plugin,
            MainConfig mainConfig,
            MessagesConfig messages,
            MediaRepository mediaRepository,
            ReferralRepository referralRepository,
            YouTubeApiClient youTubeApiClient,
            VideoRequirementsChecker requirementsChecker,
            LevelService levelService,
            RewardService rewardService,
            AuditLogService auditLogService,
            CommandService commandService,
            MessagingService messagingService
    ) {
        this.plugin = plugin;
        this.mainConfig = mainConfig;
        this.messages = messages;
        this.mediaRepository = mediaRepository;
        this.referralRepository = referralRepository;
        this.youTubeApiClient = youTubeApiClient;
        this.requirementsChecker = requirementsChecker;
        this.levelService = levelService;
        this.rewardService = rewardService;
        this.auditLogService = auditLogService;
        this.commandService = commandService;
        this.messagingService = messagingService;
    }

    public CompletableFuture<Optional<MediaChannel>> findByPlayer(UUID uuid) {
        return mediaRepository.findByPlayer(uuid);
    }

    public CompletableFuture<List<MediaChannel>> findAll() {
        return mediaRepository.findAll();
    }

    public CompletableFuture<Void> manualRefresh(Player player) {
        return mediaRepository.findByPlayer(player.getUniqueId()).thenCompose(optionalMedia -> {
            if (!optionalMedia.isPresent()) {
                messagingService.send(player, messages.channelNotLinked);
                return CompletableFuture.completedFuture(null);
            }
            MediaChannel media = optionalMedia.get();
            if (media.getLastManualRefreshAt() != null) {
                Duration cooldown = Duration.ofMinutes(mainConfig.cooldowns.manualRefreshMinutes);
                Instant nextRefresh = media.getLastManualRefreshAt().plus(cooldown);
                if (nextRefresh.isAfter(Instant.now())) {
                    messagingService.send(player, PlaceholderUtil.apply(
                            messages.manualRefreshCooldown,
                            PlaceholderUtil.map("time", TimeUtil.formatDuration(Duration.between(Instant.now(), nextRefresh)))
                    ));
                    return CompletableFuture.completedFuture(null);
                }
            }
            return refreshMedia(media, true).thenAccept(unused -> messagingService.send(player, messages.statsUpdated));
        });
    }

    public CompletableFuture<Void> refreshAllMedia() {
        return mediaRepository.findAll().thenCompose(mediaList -> {
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (MediaChannel media : mediaList) {
                chain = chain.thenCompose(unused -> refreshMedia(media, false).exceptionally(throwable -> {
                    plugin.getLogger().warning("Could not refresh media " + media.getPlayerName() + ": " + throwable.getMessage());
                    return null;
                }));
            }
            return chain;
        });
    }

    public CompletableFuture<Void> scanForNewQualifyingVideos() {
        return mediaRepository.findAllByStatus(MediaStatus.ACTIVE).thenCompose(mediaList -> {
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (MediaChannel media : mediaList) {
                chain = chain.thenCompose(unused -> processRecentVideos(media).exceptionally(throwable -> {
                    plugin.getLogger().warning("Could not scan videos for " + media.getPlayerName() + ": " + throwable.getMessage());
                    return null;
                }));
            }
            return chain;
        });
    }

    public CompletableFuture<Void> checkActivity() {
        return mediaRepository.findAllByStatus(MediaStatus.ACTIVE).thenCompose(mediaList -> {
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            final Instant threshold = Instant.now().minus(Duration.ofDays(mainConfig.schedules.activityWindowDays));
            for (MediaChannel media : mediaList) {
                if (media.getLastQualifyingVideoAt() != null && !media.getLastQualifyingVideoAt().isBefore(threshold)) {
                    continue;
                }
                chain = chain.thenCompose(unused -> suspendMedia(media, "не выполнены условия активности", null, true));
            }
            return chain;
        });
    }

    public CompletableFuture<MediaChannel> activateFromApplication(MediaApplication application, CommandSender actor) {
        return mediaRepository.findByPlayer(application.getPlayerUuid()).thenCompose(existingMedia -> {
            CompletableFuture<YouTubeChannel> channelFuture = youTubeApiClient.fetchChannel(application.getChannelId());
            CompletableFuture<YouTubeVideo> videoFuture = youTubeApiClient.fetchVideo(application.getVideoId());
            return channelFuture.thenCombine(videoFuture, Pair::new).thenCompose(pair -> {
                MediaChannel media = existingMedia.orElseGet(MediaChannel::new);
                boolean restoration = existingMedia.isPresent();
                Instant now = Instant.now();
                media.setPlayerUuid(application.getPlayerUuid());
                media.setPlayerName(application.getPlayerName());
                media.setChannelId(pair.channel.getId());
                media.setChannelTitle(pair.channel.getTitle());
                media.setChannelUrl(pair.channel.getUrl());
                media.setStatus(MediaStatus.ACTIVE);
                media.setSubscribers(pair.channel.getSubscribers());
                media.setTotalViews(pair.channel.getTotalViews());
                media.setVideoCount(pair.channel.getVideoCount());
                media.setLastLinkedVideoId(pair.video.getId());
                media.setLinkedAt(restoration && media.getLinkedAt() != null ? media.getLinkedAt() : now);
                media.setLastRefreshAt(now);
                media.setLastQualifyingVideoAt(pair.video.getPublishedAt());
                if (media.getPromoCode() == null) {
                    media.setPromoCode("");
                }
                media.setRewardsEnabled(true);
                Map<String, String> placeholders = placeholders(media);
                levelService.applyLevel(media, placeholders);
                CompletableFuture<Void> saveFuture = mediaRepository.save(media)
                        .thenCompose(unused -> mediaRepository.saveStatSnapshot(media, pair.video.getId(), pair.video.getPublishedAt()))
                        .thenCompose(unused -> rewardService.handleViewRewards(media));
                return saveFuture.thenApply(unused -> {
                    if (restoration) {
                        commandService.dispatchConsoleCommands(mainConfig.commands.onRestore, placeholders(media));
                        messagingService.sendIfOnline(media.getPlayerUuid(), messages.mediaRestored);
                        auditLogService.log(actor, media.getPlayerUuid(), "media_restore", "status restored for " + media.getPlayerName());
                    } else {
                        commandService.dispatchConsoleCommands(mainConfig.commands.onApprove, placeholders(media));
                        messagingService.sendIfOnline(media.getPlayerUuid(), messages.applicationApproved);
                        auditLogService.log(actor, media.getPlayerUuid(), "media_approve", "channel linked for " + media.getPlayerName());
                    }
                    return media;
                });
            });
        });
    }

    public CompletableFuture<Void> suspendMedia(MediaChannel media, String reason, CommandSender actor, boolean incrementStrike) {
        media.setStatus(MediaStatus.SUSPENDED);
        media.setRewardsEnabled(false);
        media.setLastSuspensionReason(reason);
        media.setLastSuspendedAt(Instant.now());
        if (incrementStrike) {
            media.setSuspensionStrikes(media.getSuspensionStrikes() + 1);
        }
        return mediaRepository.save(media).thenAccept(unused -> {
            commandService.dispatchConsoleCommands(mainConfig.commands.onSuspend, placeholders(media));
            messagingService.sendIfOnline(media.getPlayerUuid(), PlaceholderUtil.apply(
                    messages.mediaSuspended,
                    PlaceholderUtil.map("reason", reason)
            ));
            messagingService.sendIfOnline(media.getPlayerUuid(), messages.mediaRestoreHint);
            auditLogService.log(actor, media.getPlayerUuid(), "media_suspend", reason);
        });
    }

    public CompletableFuture<Void> restoreMedia(MediaChannel media, CommandSender actor, String reason) {
        media.setStatus(MediaStatus.ACTIVE);
        media.setRewardsEnabled(true);
        media.setLastSuspensionReason(reason);
        return mediaRepository.save(media).thenAccept(unused -> {
            commandService.dispatchConsoleCommands(mainConfig.commands.onRestore, placeholders(media));
            messagingService.sendIfOnline(media.getPlayerUuid(), messages.mediaRestored);
            auditLogService.log(actor, media.getPlayerUuid(), "media_restore_manual", reason);
        });
    }

    public CompletableFuture<Void> unlinkMedia(MediaChannel media, CommandSender actor, String reason) {
        return mediaRepository.archive(media, reason)
                .thenCompose(unused -> referralRepository.deleteByMedia(media.getPlayerUuid()))
                .thenCompose(unused -> mediaRepository.deleteByPlayer(media.getPlayerUuid()))
                .thenAccept(unused -> {
                    commandService.dispatchConsoleCommands(mainConfig.commands.onUnlink, placeholders(media));
                    messagingService.sendIfOnline(media.getPlayerUuid(), "&cКанал отвязан: &f" + reason);
                    auditLogService.log(actor, media.getPlayerUuid(), "media_unlink", reason);
                });
    }

    public CompletableFuture<Void> updatePromoCode(MediaChannel media, String code) {
        media.setPromoCode(code);
        media.setLastPromoChangeAt(Instant.now());
        return mediaRepository.save(media);
    }

    public CompletableFuture<Void> changePromoCode(Player player, String code) {
        if (code == null || !code.matches("[A-Za-z0-9_-]{3,16}")) {
            messagingService.send(player, messages.invalidPromoCode);
            return CompletableFuture.completedFuture(null);
        }
        return mediaRepository.findByPlayer(player.getUniqueId()).thenCompose(optionalMedia -> {
            if (!optionalMedia.isPresent()) {
                messagingService.send(player, messages.channelNotLinked);
                return CompletableFuture.completedFuture(null);
            }
            MediaChannel media = optionalMedia.get();
            if (media.getStatus() != MediaStatus.ACTIVE) {
                messagingService.send(player, messages.channelInactive);
                return CompletableFuture.completedFuture(null);
            }
            if (media.getLastPromoChangeAt() != null) {
                Instant nextChange = media.getLastPromoChangeAt().plus(Duration.ofHours(mainConfig.cooldowns.promoChangeHours));
                if (nextChange.isAfter(Instant.now())) {
                    messagingService.send(player, PlaceholderUtil.apply(
                            messages.promoCooldown,
                            PlaceholderUtil.map("time", TimeUtil.formatDuration(Duration.between(Instant.now(), nextChange)))
                    ));
                    return CompletableFuture.completedFuture(null);
                }
            }
            return mediaRepository.findByPromoCode(code).thenCompose(existingByCode -> {
                if (existingByCode.isPresent() && !existingByCode.get().getPlayerUuid().equals(player.getUniqueId())) {
                    messagingService.send(player, "&cЭтот код уже занят.");
                    return CompletableFuture.completedFuture(null);
                }
                return updatePromoCode(media, code).thenAccept(unused -> messagingService.send(player, PlaceholderUtil.apply(
                        messages.promoChanged,
                        PlaceholderUtil.map("code", code)
                )));
            });
        });
    }

    private CompletableFuture<Void> refreshMedia(MediaChannel media, boolean manual) {
        return youTubeApiClient.fetchChannel(media.getChannelId()).thenCompose(channel -> {
            media.setChannelTitle(channel.getTitle());
            media.setChannelUrl(channel.getUrl());
            media.setSubscribers(channel.getSubscribers());
            media.setTotalViews(channel.getTotalViews());
            media.setVideoCount(channel.getVideoCount());
            media.setLastRefreshAt(Instant.now());
            if (manual) {
                media.setLastManualRefreshAt(Instant.now());
            }
            levelService.applyLevel(media, placeholders(media));
            return mediaRepository.save(media)
                    .thenCompose(unused -> mediaRepository.saveStatSnapshot(media, null, Instant.now()))
                    .thenCompose(unused -> rewardService.handleViewRewards(media));
        });
    }

    private CompletableFuture<Void> processRecentVideos(MediaChannel media) {
        final Instant publishedAfter = media.getLastQualifyingVideoAt() != null
                ? media.getLastQualifyingVideoAt().minusSeconds(3600L)
                : (media.getLinkedAt() != null ? media.getLinkedAt().minusSeconds(3600L) : Instant.now().minus(Duration.ofDays(7)));
        CompletableFuture<YouTubeChannel> channelFuture = youTubeApiClient.fetchChannel(media.getChannelId());
        CompletableFuture<List<YouTubeVideo>> videosFuture = youTubeApiClient.fetchRecentVideos(media.getChannelId(), publishedAfter);
        return channelFuture.thenCombine(videosFuture, PairVideos::new).thenCompose(result -> {
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            Instant latestQualifying = media.getLastQualifyingVideoAt();
            String latestVideoId = media.getLastLinkedVideoId();
            for (YouTubeVideo video : result.videos) {
                if (!requirementsChecker.isQualifyingVideo(result.channel, video)) {
                    continue;
                }
                if (latestQualifying == null || video.getPublishedAt().isAfter(latestQualifying)) {
                    latestQualifying = video.getPublishedAt();
                    latestVideoId = video.getId();
                }
                final YouTubeVideo currentVideo = video;
                chain = chain.thenCompose(unused -> rewardService.rewardPublication(media, currentVideo));
            }
            if (latestQualifying != null) {
                media.setLastQualifyingVideoAt(latestQualifying);
                media.setLastLinkedVideoId(latestVideoId);
                final String qualifyingId = latestVideoId;
                final Instant qualifyingTime = latestQualifying;
                return chain.thenCompose(unused -> mediaRepository.save(media))
                        .thenCompose(unused -> mediaRepository.saveStatSnapshot(media, qualifyingId, qualifyingTime));
            }
            return chain;
        });
    }

    private Map<String, String> placeholders(MediaChannel media) {
        return PlaceholderUtil.map(
                "player", media.getPlayerName(),
                "channel", media.getChannelTitle(),
                "status", media.getStatus() == MediaStatus.ACTIVE ? messages.activeTag : messages.inactiveTag,
                "subscribers", String.valueOf(media.getSubscribers()),
                "views", String.valueOf(media.getTotalViews()),
                "level", levelService.getDisplayName(media.getLevelId()),
                "promo", media.getPromoCode() == null || media.getPromoCode().isEmpty() ? "-" : media.getPromoCode()
        );
    }

    private static final class Pair {
        private final YouTubeChannel channel;
        private final YouTubeVideo video;

        private Pair(YouTubeChannel channel, YouTubeVideo video) {
            this.channel = channel;
            this.video = video;
        }
    }

    private static final class PairVideos {
        private final YouTubeChannel channel;
        private final List<YouTubeVideo> videos;

        private PairVideos(YouTubeChannel channel, List<YouTubeVideo> videos) {
            this.channel = channel;
            this.videos = videos;
        }
    }
}
