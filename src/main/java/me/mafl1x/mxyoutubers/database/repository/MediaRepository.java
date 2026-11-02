package me.mafl1x.mxyoutubers.database.repository;

import me.mafl1x.mxyoutubers.database.DatabaseManager;
import me.mafl1x.mxyoutubers.database.model.MediaChannel;
import me.mafl1x.mxyoutubers.database.model.MediaStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MediaRepository extends RepositorySupport {

    public MediaRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    public CompletableFuture<Optional<MediaChannel>> findByPlayer(UUID uuid) {
        return databaseManager.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM media_channels WHERE player_uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(map(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    public CompletableFuture<Optional<MediaChannel>> findByPromoCode(String promoCode) {
        return databaseManager.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM media_channels WHERE promo_code = ?")) {
                statement.setString(1, promoCode);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(map(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    public CompletableFuture<List<MediaChannel>> findAll() {
        return databaseManager.supplyAsync(() -> {
            List<MediaChannel> result = new ArrayList<MediaChannel>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM media_channels ORDER BY player_name ASC");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(map(resultSet));
                }
            }
            return result;
        });
    }

    public CompletableFuture<List<MediaChannel>> findAllByStatus(MediaStatus status) {
        return databaseManager.supplyAsync(() -> {
            List<MediaChannel> result = new ArrayList<MediaChannel>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM media_channels WHERE status = ? ORDER BY player_name ASC")) {
                statement.setString(1, status.name());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(map(resultSet));
                    }
                }
            }
            return result;
        });
    }

    public CompletableFuture<Void> save(MediaChannel channel) {
        return databaseManager.runAsync(() -> {
            String sql = "REPLACE INTO media_channels ("
                    + "player_uuid, player_name, channel_id, channel_title, channel_url, promo_code, status, level_id, "
                    + "subscribers, total_views, video_count, last_linked_video_id, linked_at, last_refresh_at, "
                    + "last_qualifying_video_at, last_promo_change_at, last_manual_refresh_at, suspension_strikes, "
                    + "last_suspended_at, last_suspension_reason, rewards_enabled"
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, channel.getPlayerUuid().toString());
                statement.setString(2, channel.getPlayerName());
                statement.setString(3, channel.getChannelId());
                statement.setString(4, channel.getChannelTitle());
                statement.setString(5, channel.getChannelUrl());
                statement.setString(6, channel.getPromoCode());
                statement.setString(7, channel.getStatus().name());
                statement.setString(8, channel.getLevelId());
                statement.setLong(9, channel.getSubscribers());
                statement.setLong(10, channel.getTotalViews());
                statement.setLong(11, channel.getVideoCount());
                statement.setString(12, channel.getLastLinkedVideoId());
                writeInstant(statement, 13, channel.getLinkedAt());
                writeInstant(statement, 14, channel.getLastRefreshAt());
                writeInstant(statement, 15, channel.getLastQualifyingVideoAt());
                writeInstant(statement, 16, channel.getLastPromoChangeAt());
                writeInstant(statement, 17, channel.getLastManualRefreshAt());
                statement.setInt(18, channel.getSuspensionStrikes());
                writeInstant(statement, 19, channel.getLastSuspendedAt());
                statement.setString(20, channel.getLastSuspensionReason());
                statement.setBoolean(21, channel.isRewardsEnabled());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> saveStatSnapshot(MediaChannel channel, String qualifyingVideoId, Instant publishedAt) {
        return databaseManager.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO media_stats (player_uuid, subscribers, total_views, video_count, qualifying_video_id, published_at, created_at) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, channel.getPlayerUuid().toString());
                statement.setLong(2, channel.getSubscribers());
                statement.setLong(3, channel.getTotalViews());
                statement.setLong(4, channel.getVideoCount());
                statement.setString(5, qualifyingVideoId);
                writeInstant(statement, 6, publishedAt);
                writeInstant(statement, 7, Instant.now());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> deleteByPlayer(UUID uuid) {
        return databaseManager.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM media_channels WHERE player_uuid = ?")) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> archive(MediaChannel channel, String reason) {
        return databaseManager.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO media_archives (player_uuid, channel_id, payload, reason, created_at) VALUES (?, ?, ?, ?, ?)")) {
                statement.setString(1, channel.getPlayerUuid().toString());
                statement.setString(2, channel.getChannelId());
                statement.setString(3, serialize(channel));
                statement.setString(4, reason);
                writeInstant(statement, 5, Instant.now());
                statement.executeUpdate();
            }
        });
    }

    private String serialize(MediaChannel channel) {
        return "player=" + channel.getPlayerName()
                + ";channel=" + channel.getChannelTitle()
                + ";status=" + channel.getStatus()
                + ";subscribers=" + channel.getSubscribers()
                + ";views=" + channel.getTotalViews()
                + ";promo=" + channel.getPromoCode();
    }

    private MediaChannel map(ResultSet resultSet) throws SQLException {
        MediaChannel channel = new MediaChannel();
        channel.setPlayerUuid(UUID.fromString(resultSet.getString("player_uuid")));
        channel.setPlayerName(resultSet.getString("player_name"));
        channel.setChannelId(resultSet.getString("channel_id"));
        channel.setChannelTitle(resultSet.getString("channel_title"));
        channel.setChannelUrl(resultSet.getString("channel_url"));
        channel.setPromoCode(resultSet.getString("promo_code"));
        channel.setStatus(MediaStatus.valueOf(resultSet.getString("status")));
        channel.setLevelId(resultSet.getString("level_id"));
        channel.setSubscribers(resultSet.getLong("subscribers"));
        channel.setTotalViews(resultSet.getLong("total_views"));
        channel.setVideoCount(resultSet.getLong("video_count"));
        channel.setLastLinkedVideoId(resultSet.getString("last_linked_video_id"));
        channel.setLinkedAt(readInstant(resultSet, "linked_at"));
        channel.setLastRefreshAt(readInstant(resultSet, "last_refresh_at"));
        channel.setLastQualifyingVideoAt(readInstant(resultSet, "last_qualifying_video_at"));
        channel.setLastPromoChangeAt(readInstant(resultSet, "last_promo_change_at"));
        channel.setLastManualRefreshAt(readInstant(resultSet, "last_manual_refresh_at"));
        channel.setSuspensionStrikes(resultSet.getInt("suspension_strikes"));
        channel.setLastSuspendedAt(readInstant(resultSet, "last_suspended_at"));
        channel.setLastSuspensionReason(resultSet.getString("last_suspension_reason"));
        channel.setRewardsEnabled(resultSet.getBoolean("rewards_enabled"));
        return channel;
    }
}
