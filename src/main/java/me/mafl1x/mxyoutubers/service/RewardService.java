package me.mafl1x.mxyoutubers.service;

import me.mafl1x.mxyoutubers.api.YouTubeVideo;
import me.mafl1x.mxyoutubers.core.config.MainConfig;
import me.mafl1x.mxyoutubers.database.model.MediaChannel;
import me.mafl1x.mxyoutubers.database.repository.RewardRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RewardService {

    private final MainConfig.Rewards rewards;
    private final RewardRepository rewardRepository;
    private final CommandService commandService;

    public RewardService(MainConfig.Rewards rewards, RewardRepository rewardRepository, CommandService commandService) {
        this.rewards = rewards;
        this.rewardRepository = rewardRepository;
        this.commandService = commandService;
    }

    public CompletableFuture<Void> handleViewRewards(MediaChannel media) {
        if (!media.isRewardsEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (final MainConfig.ViewReward reward : rewards.views) {
            if (!reward.enabled || media.getTotalViews() < reward.requiredViews) {
                continue;
            }
            chain = chain.thenCompose(unused -> rewardRepository.exists(media.getPlayerUuid(), reward.id, media.getChannelId())
                    .thenCompose(exists -> {
                        if (exists) {
                            return CompletableFuture.completedFuture(null);
                        }
                        commandService.dispatchConsoleCommands(reward.commands, placeholders(media, null));
                        return rewardRepository.save(media.getPlayerUuid(), reward.id, "views", media.getChannelId());
                    }));
        }
        return chain;
    }

    public CompletableFuture<Void> rewardPublication(MediaChannel media, YouTubeVideo video) {
        if (!media.isRewardsEnabled() || !rewards.publication.enabled) {
            return CompletableFuture.completedFuture(null);
        }
        return rewardRepository.exists(media.getPlayerUuid(), "publication", video.getId()).thenCompose(exists -> {
            if (exists) {
                return CompletableFuture.completedFuture(null);
            }
            commandService.dispatchConsoleCommands(rewards.publication.mediaCommands, placeholders(media, null));
            return rewardRepository.save(media.getPlayerUuid(), "publication", "publication", video.getId());
        });
    }

    public CompletableFuture<Void> rewardReferral(MediaChannel media, OfflinePlayer referral) {
        if (!media.isRewardsEnabled() || !rewards.referral.enabled) {
            return CompletableFuture.completedFuture(null);
        }
        return rewardRepository.exists(media.getPlayerUuid(), "referral", referral.getUniqueId().toString()).thenCompose(exists -> {
            if (exists) {
                return CompletableFuture.completedFuture(null);
            }
            commandService.dispatchConsoleCommands(rewards.referral.mediaCommands, placeholders(media, referral));
            commandService.dispatchConsoleCommands(rewards.referral.referralCommands, placeholders(media, referral));
            return rewardRepository.save(media.getPlayerUuid(), "referral", "referral", referral.getUniqueId().toString());
        });
    }

    private Map<String, String> placeholders(MediaChannel media, OfflinePlayer referral) {
        String referralName = referral == null ? "" : (referral.getName() == null ? referral.getUniqueId().toString() : referral.getName());
        return me.mafl1x.mxyoutubers.core.util.PlaceholderUtil.map(
                "player", media.getPlayerName(),
                "channel", media.getChannelTitle(),
                "promo", media.getPromoCode() == null ? "-" : media.getPromoCode(),
                "referral", referralName
        );
    }
}
