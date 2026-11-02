package me.mafl1x.mxyoutubers.database.model;

import java.time.Instant;
import java.util.UUID;

public final class ReferralRecord {

    private long id;
    private UUID mediaPlayerUuid;
    private UUID referralPlayerUuid;
    private String codeSnapshot;
    private long startPlayTicks;
    private boolean active;
    private boolean rewarded;
    private Instant linkedAt;
    private Instant activatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getMediaPlayerUuid() {
        return mediaPlayerUuid;
    }

    public void setMediaPlayerUuid(UUID mediaPlayerUuid) {
        this.mediaPlayerUuid = mediaPlayerUuid;
    }

    public UUID getReferralPlayerUuid() {
        return referralPlayerUuid;
    }

    public void setReferralPlayerUuid(UUID referralPlayerUuid) {
        this.referralPlayerUuid = referralPlayerUuid;
    }

    public String getCodeSnapshot() {
        return codeSnapshot;
    }

    public void setCodeSnapshot(String codeSnapshot) {
        this.codeSnapshot = codeSnapshot;
    }

    public long getStartPlayTicks() {
        return startPlayTicks;
    }

    public void setStartPlayTicks(long startPlayTicks) {
        this.startPlayTicks = startPlayTicks;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isRewarded() {
        return rewarded;
    }

    public void setRewarded(boolean rewarded) {
        this.rewarded = rewarded;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
    }
}
