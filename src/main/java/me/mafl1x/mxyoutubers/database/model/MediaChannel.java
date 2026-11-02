package me.mafl1x.mxyoutubers.database.model;

import java.time.Instant;
import java.util.UUID;

public final class MediaChannel {

    private UUID playerUuid;
    private String playerName;
    private String channelId;
    private String channelTitle;
    private String channelUrl;
    private String promoCode;
    private MediaStatus status;
    private String levelId;
    private long subscribers;
    private long totalViews;
    private long videoCount;
    private String lastLinkedVideoId;
    private Instant linkedAt;
    private Instant lastRefreshAt;
    private Instant lastQualifyingVideoAt;
    private Instant lastPromoChangeAt;
    private Instant lastManualRefreshAt;
    private int suspensionStrikes;
    private Instant lastSuspendedAt;
    private String lastSuspensionReason;
    private boolean rewardsEnabled;

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public void setStatus(MediaStatus status) {
        this.status = status;
    }

    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }

    public long getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(long subscribers) {
        this.subscribers = subscribers;
    }

    public long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(long totalViews) {
        this.totalViews = totalViews;
    }

    public long getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(long videoCount) {
        this.videoCount = videoCount;
    }

    public String getLastLinkedVideoId() {
        return lastLinkedVideoId;
    }

    public void setLastLinkedVideoId(String lastLinkedVideoId) {
        this.lastLinkedVideoId = lastLinkedVideoId;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }

    public Instant getLastRefreshAt() {
        return lastRefreshAt;
    }

    public void setLastRefreshAt(Instant lastRefreshAt) {
        this.lastRefreshAt = lastRefreshAt;
    }

    public Instant getLastQualifyingVideoAt() {
        return lastQualifyingVideoAt;
    }

    public void setLastQualifyingVideoAt(Instant lastQualifyingVideoAt) {
        this.lastQualifyingVideoAt = lastQualifyingVideoAt;
    }

    public Instant getLastPromoChangeAt() {
        return lastPromoChangeAt;
    }

    public void setLastPromoChangeAt(Instant lastPromoChangeAt) {
        this.lastPromoChangeAt = lastPromoChangeAt;
    }

    public Instant getLastManualRefreshAt() {
        return lastManualRefreshAt;
    }

    public void setLastManualRefreshAt(Instant lastManualRefreshAt) {
        this.lastManualRefreshAt = lastManualRefreshAt;
    }

    public int getSuspensionStrikes() {
        return suspensionStrikes;
    }

    public void setSuspensionStrikes(int suspensionStrikes) {
        this.suspensionStrikes = suspensionStrikes;
    }

    public Instant getLastSuspendedAt() {
        return lastSuspendedAt;
    }

    public void setLastSuspendedAt(Instant lastSuspendedAt) {
        this.lastSuspendedAt = lastSuspendedAt;
    }

    public String getLastSuspensionReason() {
        return lastSuspensionReason;
    }

    public void setLastSuspensionReason(String lastSuspensionReason) {
        this.lastSuspensionReason = lastSuspensionReason;
    }

    public boolean isRewardsEnabled() {
        return rewardsEnabled;
    }

    public void setRewardsEnabled(boolean rewardsEnabled) {
        this.rewardsEnabled = rewardsEnabled;
    }
}
