package me.mafl1x.mxyoutubers.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.mafl1x.mxyoutubers.core.config.MainConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class DatabaseManager {

    private final MainConfig.Database config;
    private final ExecutorService executor;
    private HikariDataSource dataSource;
    private boolean mysql;

    public DatabaseManager(MainConfig.Database config, ExecutorService executor) {
        this.config = config;
        this.executor = executor;
    }

    public void connect() {
        HikariConfig hikariConfig = new HikariConfig();
        this.mysql = "MYSQL".equalsIgnoreCase(config.type);
        if (mysql) {
            hikariConfig.setJdbcUrl("jdbc:mysql://" + config.host + ":" + config.port + "/" + config.database
                    + "?useSSL=" + config.useSsl + "&characterEncoding=utf8");
            hikariConfig.setUsername(config.username);
            hikariConfig.setPassword(config.password);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + config.sqliteFile);
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
        }
        hikariConfig.setMaximumPoolSize(Math.max(2, config.maximumPoolSize));
        hikariConfig.setPoolName("MXYouTubers-Hikari");
        hikariConfig.setAutoCommit(true);
        this.dataSource = new HikariDataSource(hikariConfig);
        initializeSchema();
    }

    private void initializeSchema() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(createMediaChannelsTable());
            statement.executeUpdate(createApplicationsTable());
            statement.executeUpdate(createRefusalHistoryTable());
            statement.executeUpdate(createReferralsTable());
            statement.executeUpdate(createRewardHistoryTable());
            statement.executeUpdate(createMediaStatsTable());
            statement.executeUpdate(createMediaArchivesTable());
            statement.executeUpdate(createAuditLogsTable());
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_media_status ON media_channels(status)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_applications_status ON applications(status)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_referrals_media ON referrals(media_player_uuid)");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize database schema", exception);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean isMysql() {
        return mysql;
    }

    public <T> CompletableFuture<T> supplyAsync(SqlSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }, executor);
    }

    public CompletableFuture<Void> runAsync(SqlRunnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }, executor);
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private String createMediaChannelsTable() {
        return "CREATE TABLE IF NOT EXISTS media_channels ("
                + "player_uuid VARCHAR(36) PRIMARY KEY,"
                + "player_name VARCHAR(16) NOT NULL,"
                + "channel_id VARCHAR(64) NOT NULL,"
                + "channel_title VARCHAR(255) NOT NULL,"
                + "channel_url VARCHAR(255) NOT NULL,"
                + "promo_code VARCHAR(64),"
                + "status VARCHAR(32) NOT NULL,"
                + "level_id VARCHAR(64) NOT NULL,"
                + "subscribers BIGINT NOT NULL,"
                + "total_views BIGINT NOT NULL,"
                + "video_count BIGINT NOT NULL,"
                + "last_linked_video_id VARCHAR(32),"
                + "linked_at BIGINT NOT NULL,"
                + "last_refresh_at BIGINT NOT NULL,"
                + "last_qualifying_video_at BIGINT NOT NULL,"
                + "last_promo_change_at BIGINT NOT NULL,"
                + "last_manual_refresh_at BIGINT NOT NULL,"
                + "suspension_strikes INT NOT NULL,"
                + "last_suspended_at BIGINT NOT NULL,"
                + "last_suspension_reason TEXT,"
                + "rewards_enabled BOOLEAN NOT NULL"
                + ")";
    }

    private String createApplicationsTable() {
        return "CREATE TABLE IF NOT EXISTS applications ("
                + idDefinition()
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "player_name VARCHAR(16) NOT NULL,"
                + "channel_id VARCHAR(64) NOT NULL,"
                + "channel_title VARCHAR(255) NOT NULL,"
                + "channel_url VARCHAR(255) NOT NULL,"
                + "video_id VARCHAR(32) NOT NULL,"
                + "video_url VARCHAR(255) NOT NULL,"
                + "video_title VARCHAR(255) NOT NULL,"
                + "video_description TEXT,"
                + "channel_subscribers BIGINT NOT NULL,"
                + "video_views BIGINT NOT NULL,"
                + "type VARCHAR(32) NOT NULL,"
                + "status VARCHAR(32) NOT NULL,"
                + "confirmation_code VARCHAR(64),"
                + "reason TEXT,"
                + "created_at BIGINT NOT NULL,"
                + "updated_at BIGINT NOT NULL"
                + ")";
    }

    private String createRefusalHistoryTable() {
        return "CREATE TABLE IF NOT EXISTS refusal_history ("
                + idDefinition()
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "reason TEXT NOT NULL,"
                + "source VARCHAR(32) NOT NULL,"
                + "created_at BIGINT NOT NULL"
                + ")";
    }

    private String createReferralsTable() {
        return "CREATE TABLE IF NOT EXISTS referrals ("
                + idDefinition()
                + "media_player_uuid VARCHAR(36) NOT NULL,"
                + "referral_player_uuid VARCHAR(36) NOT NULL UNIQUE,"
                + "code_snapshot VARCHAR(64) NOT NULL,"
                + "start_play_ticks BIGINT NOT NULL,"
                + "active BOOLEAN NOT NULL,"
                + "rewarded BOOLEAN NOT NULL,"
                + "linked_at BIGINT NOT NULL,"
                + "activated_at BIGINT NOT NULL"
                + ")";
    }

    private String createRewardHistoryTable() {
        return "CREATE TABLE IF NOT EXISTS reward_history ("
                + idDefinition()
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "reward_key VARCHAR(128) NOT NULL,"
                + "reward_type VARCHAR(64) NOT NULL,"
                + "source_key VARCHAR(128) NOT NULL,"
                + "created_at BIGINT NOT NULL,"
                + "UNIQUE(player_uuid, reward_key, source_key)"
                + ")";
    }

    private String createMediaStatsTable() {
        return "CREATE TABLE IF NOT EXISTS media_stats ("
                + idDefinition()
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "subscribers BIGINT NOT NULL,"
                + "total_views BIGINT NOT NULL,"
                + "video_count BIGINT NOT NULL,"
                + "qualifying_video_id VARCHAR(32),"
                + "published_at BIGINT NOT NULL,"
                + "created_at BIGINT NOT NULL"
                + ")";
    }

    private String createMediaArchivesTable() {
        return "CREATE TABLE IF NOT EXISTS media_archives ("
                + idDefinition()
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "channel_id VARCHAR(64) NOT NULL,"
                + "payload TEXT NOT NULL,"
                + "reason TEXT NOT NULL,"
                + "created_at BIGINT NOT NULL"
                + ")";
    }

    private String createAuditLogsTable() {
        return "CREATE TABLE IF NOT EXISTS audit_logs ("
                + idDefinition()
                + "actor_uuid VARCHAR(36),"
                + "actor_name VARCHAR(32) NOT NULL,"
                + "action VARCHAR(64) NOT NULL,"
                + "target_uuid VARCHAR(36),"
                + "details TEXT,"
                + "created_at BIGINT NOT NULL"
                + ")";
    }

    private String idDefinition() {
        if (mysql) {
            return "id BIGINT PRIMARY KEY AUTO_INCREMENT,";
        }
        return "id INTEGER PRIMARY KEY AUTOINCREMENT,";
    }

    public interface SqlSupplier<T> {
        T get() throws Exception;
    }

    public interface SqlRunnable {
        void run() throws Exception;
    }
}
