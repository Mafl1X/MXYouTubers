package me.mafl1x.mxyoutubers.database.repository;

import me.mafl1x.mxyoutubers.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RewardRepository extends RepositorySupport {

    public RewardRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    public CompletableFuture<Boolean> exists(UUID playerUuid, String rewardKey, String sourceKey) {
        return databaseManager.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT 1 FROM reward_history WHERE player_uuid = ? AND reward_key = ? AND source_key = ? LIMIT 1")) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, rewardKey);
                statement.setString(3, sourceKey);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        });
    }

    public CompletableFuture<Void> save(UUID playerUuid, String rewardKey, String rewardType, String sourceKey) {
        return databaseManager.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "REPLACE INTO reward_history (player_uuid, reward_key, reward_type, source_key, created_at) VALUES (?, ?, ?, ?, ?)")) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, rewardKey);
                statement.setString(3, rewardType);
                statement.setString(4, sourceKey);
                writeInstant(statement, 5, Instant.now());
                statement.executeUpdate();
            }
        });
    }
}
