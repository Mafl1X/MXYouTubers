package me.mafl1x.mxyoutubers.database.repository;

import me.mafl1x.mxyoutubers.database.DatabaseManager;
import me.mafl1x.mxyoutubers.database.model.ApplicationStatus;
import me.mafl1x.mxyoutubers.database.model.ApplicationType;
import me.mafl1x.mxyoutubers.database.model.MediaApplication;
import me.mafl1x.mxyoutubers.database.model.RefusalEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ApplicationRepository extends RepositorySupport {

    public ApplicationRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    public CompletableFuture<Long> create(MediaApplication application) {
        return databaseManager.supplyAsync(() -> {
            String sql = "INSERT INTO applications (player_uuid, player_name, channel_id, channel_title, channel_url, video_id, video_url, "
                    + "video_title, video_description, channel_subscribers, video_views, type, status, confirmation_code, reason, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                fill(statement, application);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
            }
            return 0L;
        });
    }

    public CompletableFuture<Void> update(MediaApplication application) {
        return databaseManager.runAsync(() -> {
            String sql = "UPDATE applications SET player_uuid = ?, player_name = ?, channel_id = ?, channel_title = ?, channel_url = ?, "
                    + "video_id = ?, video_url = ?, video_title = ?, video_description = ?, channel_subscribers = ?, video_views = ?, "
                    + "type = ?, status = ?, confirmation_code = ?, reason = ?, created_at = ?, updated_at = ? WHERE id = ?";
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                fill(statement, application);
                statement.setLong(18, application.getId());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Optional<MediaApplication>> findPendingByPlayer(UUID playerUuid) {
        return databaseManager.supplyAsync(() -> {
            String sql = "SELECT * FROM applications WHERE player_uuid = ? AND status IN (?, ?) ORDER BY created_at DESC LIMIT 1";
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, ApplicationStatus.WAITING_CODE_CONFIRMATION.name());
                statement.setString(3, ApplicationStatus.WAITING_ADMIN_REVIEW.name());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(map(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    public CompletableFuture<Optional<MediaApplication>> findById(long id) {
        return databaseManager.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM applications WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(map(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    public CompletableFuture<List<MediaApplication>> findPendingReview() {
        return databaseManager.supplyAsync(() -> {
            List<MediaApplication> result = new ArrayList<MediaApplication>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM applications WHERE status = ? ORDER BY created_at ASC")) {
                statement.setString(1, ApplicationStatus.WAITING_ADMIN_REVIEW.name());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(map(resultSet));
                    }
                }
            }
            return result;
        });
    }

    public CompletableFuture<Optional<Instant>> findLastApplicationAt(UUID playerUuid, ApplicationType type) {
        return databaseManager.supplyAsync(() -> {
            String sql = "SELECT created_at FROM applications WHERE player_uuid = ? AND type = ? ORDER BY created_at DESC LIMIT 1";
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, type.name());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        long createdAt = resultSet.getLong("created_at");
                        return createdAt <= 0L ? Optional.<Instant>empty() : Optional.of(Instant.ofEpochMilli(createdAt));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    public CompletableFuture<Void> addRefusal(UUID playerUuid, String reason, String source) {
        return databaseManager.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO refusal_history (player_uuid, reason, source, created_at) VALUES (?, ?, ?, ?)")) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, reason);
                statement.setString(3, source);
                writeInstant(statement, 4, Instant.now());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<List<RefusalEntry>> listRefusals(UUID playerUuid, int limit) {
        return databaseManager.supplyAsync(() -> {
            List<RefusalEntry> result = new ArrayList<RefusalEntry>();
            String sql = "SELECT * FROM refusal_history WHERE player_uuid = ? ORDER BY created_at DESC LIMIT " + Math.max(1, limit);
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        RefusalEntry entry = new RefusalEntry();
                        entry.setId(resultSet.getLong("id"));
                        entry.setPlayerUuid(UUID.fromString(resultSet.getString("player_uuid")));
                        entry.setReason(resultSet.getString("reason"));
                        entry.setSource(resultSet.getString("source"));
                        entry.setCreatedAt(readInstant(resultSet, "created_at"));
                        result.add(entry);
                    }
                }
            }
            return result;
        });
    }

    private void fill(PreparedStatement statement, MediaApplication application) throws SQLException {
        statement.setString(1, application.getPlayerUuid().toString());
        statement.setString(2, application.getPlayerName());
        statement.setString(3, application.getChannelId());
        statement.setString(4, application.getChannelTitle());
        statement.setString(5, application.getChannelUrl());
        statement.setString(6, application.getVideoId());
        statement.setString(7, application.getVideoUrl());
        statement.setString(8, application.getVideoTitle());
        statement.setString(9, application.getVideoDescription());
        statement.setLong(10, application.getChannelSubscribers());
        statement.setLong(11, application.getVideoViews());
        statement.setString(12, application.getType().name());
        statement.setString(13, application.getStatus().name());
        statement.setString(14, application.getConfirmationCode());
        statement.setString(15, application.getReason());
        writeInstant(statement, 16, application.getCreatedAt());
        writeInstant(statement, 17, application.getUpdatedAt());
    }

    private MediaApplication map(ResultSet resultSet) throws SQLException {
        MediaApplication application = new MediaApplication();
        application.setId(resultSet.getLong("id"));
        application.setPlayerUuid(UUID.fromString(resultSet.getString("player_uuid")));
        application.setPlayerName(resultSet.getString("player_name"));
        application.setChannelId(resultSet.getString("channel_id"));
        application.setChannelTitle(resultSet.getString("channel_title"));
        application.setChannelUrl(resultSet.getString("channel_url"));
        application.setVideoId(resultSet.getString("video_id"));
        application.setVideoUrl(resultSet.getString("video_url"));
        application.setVideoTitle(resultSet.getString("video_title"));
        application.setVideoDescription(resultSet.getString("video_description"));
        application.setChannelSubscribers(resultSet.getLong("channel_subscribers"));
        application.setVideoViews(resultSet.getLong("video_views"));
        application.setType(ApplicationType.valueOf(resultSet.getString("type")));
        application.setStatus(ApplicationStatus.valueOf(resultSet.getString("status")));
        application.setConfirmationCode(resultSet.getString("confirmation_code"));
        application.setReason(resultSet.getString("reason"));
        application.setCreatedAt(readInstant(resultSet, "created_at"));
        application.setUpdatedAt(readInstant(resultSet, "updated_at"));
        return application;
    }
}
