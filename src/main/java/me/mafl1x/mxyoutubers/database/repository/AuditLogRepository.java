package me.mafl1x.mxyoutubers.database.repository;

import me.mafl1x.mxyoutubers.database.DatabaseManager;
import me.mafl1x.mxyoutubers.database.model.AuditLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.CompletableFuture;

public final class AuditLogRepository extends RepositorySupport {

    public AuditLogRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    public CompletableFuture<Void> save(AuditLogEntry entry) {
        return databaseManager.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO audit_logs (actor_uuid, actor_name, action, target_uuid, details, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                writeUuid(statement, 1, entry.getActorUuid());
                statement.setString(2, entry.getActorName());
                statement.setString(3, entry.getAction());
                writeUuid(statement, 4, entry.getTargetUuid());
                statement.setString(5, entry.getDetails());
                writeInstant(statement, 6, entry.getCreatedAt());
                statement.executeUpdate();
            }
        });
    }
}
