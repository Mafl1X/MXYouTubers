package me.mafl1x.mxyoutubers.database.model;

import java.time.Instant;
import java.util.UUID;

public final class RewardRecord {

    private long id;
    private UUID playerUuid;
    private String rewardKey;
    private String rewardType;
    private String sourceKey;
    private Instant createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getRewardKey() {
        return rewardKey;
    }

    public void setRewardKey(String rewardKey) {
        this.rewardKey = rewardKey;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
