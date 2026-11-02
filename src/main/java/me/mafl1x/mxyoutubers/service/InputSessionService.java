package me.mafl1x.mxyoutubers.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InputSessionService {

    private final Map<UUID, InputSession> sessions = new ConcurrentHashMap<UUID, InputSession>();

    public void set(UUID uuid, InputSession session) {
        sessions.put(uuid, session);
    }

    public InputSession get(UUID uuid) {
        return sessions.get(uuid);
    }

    public InputSession remove(UUID uuid) {
        return sessions.remove(uuid);
    }
}
