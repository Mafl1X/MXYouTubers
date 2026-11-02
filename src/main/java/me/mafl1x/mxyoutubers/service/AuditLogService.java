package me.mafl1x.mxyoutubers.service;

import me.mafl1x.mxyoutubers.database.model.AuditLogEntry;
import me.mafl1x.mxyoutubers.database.repository.AuditLogRepository;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;

public final class AuditLogService {

    private final JavaPlugin plugin;
    private final AuditLogRepository repository;

    public AuditLogService(JavaPlugin plugin, AuditLogRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void log(CommandSender actor, UUID targetUuid, String action, String details) {
        AuditLogEntry entry = new AuditLogEntry();
        if (actor instanceof Player) {
            entry.setActorUuid(((Player) actor).getUniqueId());
            entry.setActorName(actor.getName());
        } else {
            entry.setActorName(actor == null ? "SYSTEM" : actor.getName());
        }
        entry.setTargetUuid(targetUuid);
        entry.setAction(action);
        entry.setDetails(details);
        entry.setCreatedAt(Instant.now());
        repository.save(entry);
        plugin.getLogger().info("[Audit] " + action + " | target=" + targetUuid + " | " + details);
    }

    public void logSystem(UUID targetUuid, String action, String details) {
        log(null, targetUuid, action, details);
    }
}
