package me.mafl1x.mxyoutubers.service;

import java.time.Instant;
import java.util.UUID;

public final class InputSession {

    private final InputType type;
    private final Long applicationId;
    private final UUID targetPlayerUuid;
    private final Instant createdAt;

    public InputSession(InputType type, Long applicationId, UUID targetPlayerUuid) {
        this.type = type;
        this.applicationId = applicationId;
        this.targetPlayerUuid = targetPlayerUuid;
        this.createdAt = Instant.now();
    }

    public InputType getType() {
        return type;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public UUID getTargetPlayerUuid() {
        return targetPlayerUuid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
