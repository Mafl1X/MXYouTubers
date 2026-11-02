package me.mafl1x.mxyoutubers.database.repository;

import me.mafl1x.mxyoutubers.database.DatabaseManager;
import me.mafl1x.mxyoutubers.database.model.ReferralRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ReferralRepository extends RepositorySupport {

    public ReferralRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    public CompletableFuture<Optional<ReferralRecord>> findByReferral(UUID referralUuid) {
        return databaseManager.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM referrals WHERE referral_player_uuid = ?")) {
                statement.setString(1, referralUuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(map(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    public CompletableFuture<List<ReferralRecord>> findByMedia(UUID mediaUuid) {
        return databaseManager.supplyAsync(() -> {
            List<ReferralRecord> result = new ArrayList<ReferralRecord>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM referrals WHERE media_player_uuid = ?")) {
                statement.setString(1, mediaUuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(map(resultSet));
                    }
                }
            }
            return result;
        });
    }

    public CompletableFuture<List<ReferralRecord>> listPendingActivation() {
        return databaseManager.supplyAsync(() -> {
            List<ReferralRecord> result = new ArrayList<ReferralRecord>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM referrals WHERE active = ?")) {
                statement.setBoolean(1, false);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(map(resultSet));
                    }
                }
            }
            return result;
        });
    }

    public CompletableFuture<Void> save(ReferralRecord record) {
        return databaseManager.runAsync(() -> {
            String sql = "REPLACE INTO referrals (id, media_player_uuid, referral_player_uuid, code_snapshot, start_play_ticks, active, rewarded, linked_at, activated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                if (record.getId() <= 0L) {
                    statement.setNull(1, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(1, record.getId());
                }
                statement.setString(2, record.getMediaPlayerUuid().toString());
                statement.setString(3, record.getReferralPlayerUuid().toString());
                statement.setString(4, record.getCodeSnapshot());
                statement.setLong(5, record.getStartPlayTicks());
                statement.setBoolean(6, record.isActive());
                statement.setBoolean(7, record.isRewarded());
                writeInstant(statement, 8, record.getLinkedAt());
                writeInstant(statement, 9, record.getActivatedAt());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> deleteByMedia(UUID mediaUuid) {
        return databaseManager.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM referrals WHERE media_player_uuid = ?")) {
                statement.setString(1, mediaUuid.toString());
                statement.executeUpdate();
            }
        });
    }

    private ReferralRecord map(ResultSet resultSet) throws SQLException {
        ReferralRecord record = new ReferralRecord();
        record.setId(resultSet.getLong("id"));
        record.setMediaPlayerUuid(UUID.fromString(resultSet.getString("media_player_uuid")));
        record.setReferralPlayerUuid(UUID.fromString(resultSet.getString("referral_player_uuid")));
        record.setCodeSnapshot(resultSet.getString("code_snapshot"));
        record.setStartPlayTicks(resultSet.getLong("start_play_ticks"));
        record.setActive(resultSet.getBoolean("active"));
        record.setRewarded(resultSet.getBoolean("rewarded"));
        record.setLinkedAt(readInstant(resultSet, "linked_at"));
        record.setActivatedAt(readInstant(resultSet, "activated_at"));
        return record;
    }
}
