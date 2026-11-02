package me.mafl1x.mxyoutubers.database.repository;

import me.mafl1x.mxyoutubers.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;

abstract class RepositorySupport {

    protected final DatabaseManager databaseManager;

    protected RepositorySupport(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    protected void writeInstant(PreparedStatement statement, int index, Instant instant) throws SQLException {
        if (instant == null) {
            statement.setLong(index, 0L);
        } else {
            statement.setLong(index, instant.toEpochMilli());
        }
    }

    protected Instant readInstant(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return value <= 0L ? null : Instant.ofEpochMilli(value);
    }

    protected void writeUuid(PreparedStatement statement, int index, java.util.UUID uuid) throws SQLException {
        if (uuid == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, uuid.toString());
        }
    }
}
