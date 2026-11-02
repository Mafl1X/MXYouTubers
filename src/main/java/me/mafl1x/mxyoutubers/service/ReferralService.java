package me.mafl1x.mxyoutubers.service;

import me.mafl1x.mxyoutubers.core.config.MainConfig;
import me.mafl1x.mxyoutubers.core.config.MessagesConfig;
import me.mafl1x.mxyoutubers.core.util.PlaceholderUtil;
import me.mafl1x.mxyoutubers.database.model.MediaChannel;
import me.mafl1x.mxyoutubers.database.model.MediaStatus;
import me.mafl1x.mxyoutubers.database.model.ReferralRecord;
import me.mafl1x.mxyoutubers.database.repository.MediaRepository;
import me.mafl1x.mxyoutubers.database.repository.ReferralRepository;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ReferralService {

    private final MainConfig mainConfig;
    private final MessagesConfig messages;
    private final MediaRepository mediaRepository;
    private final ReferralRepository referralRepository;
    private final RewardService rewardService;
    private final MessagingService messagingService;
    private final AuditLogService auditLogService;

    public ReferralService(
            MainConfig mainConfig,
            MessagesConfig messages,
            MediaRepository mediaRepository,
            ReferralRepository referralRepository,
            RewardService rewardService,
            MessagingService messagingService,
            AuditLogService auditLogService
    ) {
        this.mainConfig = mainConfig;
        this.messages = messages;
        this.mediaRepository = mediaRepository;
        this.referralRepository = referralRepository;
        this.rewardService = rewardService;
        this.messagingService = messagingService;
        this.auditLogService = auditLogService;
    }

    public CompletableFuture<Void> applyPromo(Player referral, String code) {
        return referralRepository.findByReferral(referral.getUniqueId()).thenCompose(existingReferral -> {
            if (existingReferral.isPresent()) {
                messagingService.send(referral, messages.promoAlreadyUsed);
                return CompletableFuture.completedFuture(null);
            }
            return mediaRepository.findByPromoCode(code).thenCompose(media -> {
                if (!media.isPresent() || media.get().getStatus() != MediaStatus.ACTIVE
                        || media.get().getPromoCode() == null || media.get().getPromoCode().trim().isEmpty()) {
                    messagingService.send(referral, messages.promoUnavailable);
                    return CompletableFuture.completedFuture(null);
                }
                MediaChannel mediaChannel = media.get();
                if (mediaChannel.getPlayerUuid().equals(referral.getUniqueId())) {
                    messagingService.send(referral, messages.promoOwnCode);
                    return CompletableFuture.completedFuture(null);
                }
                ReferralRecord record = new ReferralRecord();
                record.setMediaPlayerUuid(mediaChannel.getPlayerUuid());
                record.setReferralPlayerUuid(referral.getUniqueId());
                record.setCodeSnapshot(code);
                record.setStartPlayTicks(referral.getStatistic(Statistic.PLAY_ONE_MINUTE));
                record.setActive(false);
                record.setRewarded(false);
                record.setLinkedAt(Instant.now());
                record.setActivatedAt(null);
                return referralRepository.save(record).thenAccept(unused -> {
                    messagingService.send(referral, PlaceholderUtil.apply(
                            messages.promoApplied,
                            PlaceholderUtil.map("minutes", String.valueOf(mainConfig.rewards.referral.requiredPlayMinutes))
                    ));
                    auditLogService.logSystem(referral.getUniqueId(), "promo_apply", "promo " + code + " -> " + mediaChannel.getPlayerName());
                });
            });
        });
    }

    public CompletableFuture<Integer> countActiveReferrals(UUID mediaUuid) {
        return referralRepository.findByMedia(mediaUuid).thenApply(records -> {
            int count = 0;
            for (ReferralRecord record : records) {
                if (record.isActive()) {
                    count++;
                }
            }
            return Integer.valueOf(count);
        });
    }

    public CompletableFuture<Void> processPendingActivations() {
        return referralRepository.listPendingActivation().thenCompose(records -> {
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (ReferralRecord record : records) {
                chain = chain.thenCompose(unused -> processSingle(record));
            }
            return chain;
        });
    }

    private CompletableFuture<Void> processSingle(ReferralRecord record) {
        Player player = Bukkit.getPlayer(record.getReferralPlayerUuid());
        if (player == null) {
            return CompletableFuture.completedFuture(null);
        }
        long playedTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE) - record.getStartPlayTicks();
        long requiredTicks = mainConfig.rewards.referral.requiredPlayMinutes * 60L * 20L;
        if (playedTicks < requiredTicks) {
            return CompletableFuture.completedFuture(null);
        }
        return mediaRepository.findByPlayer(record.getMediaPlayerUuid()).thenCompose(media -> {
            if (!media.isPresent() || media.get().getStatus() != MediaStatus.ACTIVE) {
                return CompletableFuture.completedFuture(null);
            }
            record.setActive(true);
            record.setRewarded(true);
            record.setActivatedAt(Instant.now());
            return referralRepository.save(record)
                    .thenCompose(unused -> rewardService.rewardReferral(media.get(), player))
                    .thenAccept(unused -> {
                        messagingService.sendIfOnline(media.get().getPlayerUuid(), PlaceholderUtil.apply(
                                messages.referralActivated,
                                PlaceholderUtil.map("player", player.getName())
                        ));
                        messagingService.send(player, "&aТы стал активным рефералом.");
                        auditLogService.logSystem(player.getUniqueId(), "promo_activate", "for media " + media.get().getPlayerName());
                    });
        });
    }
}
