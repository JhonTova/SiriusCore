package me.sirius.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.sirius.core.config.Config;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Storage implements AutoCloseable {

    private final HikariDataSource source;
    private final boolean sqlite;

    private Storage(HikariDataSource source, boolean sqlite) {
        this.source = source;
        this.sqlite = sqlite;
    }

    public static Storage from(Config config, File dataFolder, String fileName) {
        String type = config.string("storage.type", "sqlite").toLowerCase();

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("Sirius-" + fileName);

        boolean sqlite = !type.equals("mysql") && !type.equals("mariadb");
        if (sqlite) {
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new IllegalStateException("No se pudo crear " + dataFolder);
            }
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, fileName + ".db"));
            // SQLite es un fichero: más de una conexión de escritura solo produce bloqueos.
            hikari.setMaximumPoolSize(1);
        } else {
            hikari.setJdbcUrl("jdbc:mysql://%s:%d/%s?useSSL=%s&characterEncoding=utf8".formatted(
                    config.string("storage.host", "localhost"),
                    config.integer("storage.port", 3306),
                    config.string("storage.database", "minecraft"),
                    config.bool("storage.ssl", false)));
            hikari.setUsername(config.string("storage.username", "root"));
            hikari.setPassword(config.string("storage.password", ""));
            hikari.setMaximumPoolSize(config.integer("storage.pool-size", 10));
            hikari.addDataSourceProperty("cachePrepStmts", "true");
            hikari.addDataSourceProperty("prepStmtCacheSize", "250");
            hikari.addDataSourceProperty("rewriteBatchedStatements", "true");
        }

        hikari.setConnectionTimeout(10_000);
        hikari.setLeakDetectionThreshold(30_000);

        return new Storage(new HikariDataSource(hikari), sqlite);
    }

    public static Storage sqlite(File file) {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("No se pudo crear " + parent);
        }

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("Sirius-" + file.getName());
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setJdbcUrl("jdbc:sqlite:" + file);
        hikari.setMaximumPoolSize(1);
        hikari.setConnectionTimeout(10_000);

        return new Storage(new HikariDataSource(hikari), true);
    }

    public boolean isSqlite() {
        return sqlite;
    }

    public void execute(String sql, Object... params) {
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTo(statement, params);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Falló la sentencia: " + sql, e);
        }
    }

    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTo(statement, params);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? mapper.map(rows) : null;
            }
        } catch (SQLException e) {
            throw new StorageException("Falló la consulta: " + sql, e);
        }
    }

    public <T> List<T> queryAll(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTo(statement, params);
            try (ResultSet rows = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rows.next()) {
                    results.add(mapper.map(rows));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new StorageException("Falló la consulta: " + sql, e);
        }
    }

    public void batch(String sql, List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Object[] params : rows) {
                    bindTo(statement, params);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new StorageException("Falló el lote: " + sql, e);
        }
    }

    public <T> T transaction(Function<Tx, T> work) {
        SQLException last = null;
        for (int attempt = 1; attempt <= TRANSACTION_ATTEMPTS; attempt++) {
            try (Connection connection = source.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    T result = work.apply(new Tx(connection));
                    connection.commit();
                    return result;
                } catch (Rollback cancelled) {
                    connection.rollback();
                    return null;
                } catch (Retry retry) {
                    connection.rollback();
                    throw retry.cause;
                } catch (RuntimeException | Error e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                last = e;
                if (!isRetryable(e) || attempt == TRANSACTION_ATTEMPTS) {
                    break;
                }
                backoff(attempt);
            }
        }
        throw new StorageException("Falló la transacción", last);
    }

    public void atomically(Consumer<Tx> work) {
        transaction(tx -> {
            work.accept(tx);
            return null;
        });
    }

    private static final int TRANSACTION_ATTEMPTS = 3;

    private static boolean isRetryable(SQLException e) {
        String state = e.getSQLState();
        if ("40001".equals(state) || "40P01".equals(state)) {
            return true;
        }
        return e.getErrorCode() == 5 || e.getErrorCode() == 6;
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(20L * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new StorageException("Transacción interrumpida", interrupted);
        }
    }

    public static final class Tx {

        private final Connection connection;

        private Tx(Connection connection) {
            this.connection = connection;
        }

        public int execute(String sql, Object... params) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindTo(statement, params);
                return statement.executeUpdate();
            } catch (SQLException e) {
                throw wrap(e, sql);
            }
        }

        public long insert(String sql, Object... params) {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                bindTo(statement, params);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : -1L;
                }
            } catch (SQLException e) {
                throw wrap(e, sql);
            }
        }

        public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindTo(statement, params);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? mapper.map(rows) : null;
                }
            } catch (SQLException e) {
                throw wrap(e, sql);
            }
        }

        public <T> List<T> queryAll(String sql, RowMapper<T> mapper, Object... params) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindTo(statement, params);
                try (ResultSet rows = statement.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rows.next()) {
                        results.add(mapper.map(rows));
                    }
                    return results;
                }
            } catch (SQLException e) {
                throw wrap(e, sql);
            }
        }

        private static RuntimeException wrap(SQLException e, String sql) {
            if (isRetryable(e)) {
                return new Retry(e);
            }
            return new StorageException("Falló dentro de la transacción: " + sql, e);
        }
    }

    public static final class Rollback extends RuntimeException {
        public Rollback() {
            super(null, null, false, false);
        }
    }

    private static final class Retry extends RuntimeException {
        private final SQLException cause;

        Retry(SQLException cause) {
            super(null, cause, false, false);
            this.cause = cause;
        }
    }

    private static void bindTo(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    @Override
    public void close() {
        if (source != null && !source.isClosed()) {
            source.close();
        }
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rows) throws SQLException;
    }

    public static final class StorageException extends RuntimeException {
        StorageException(String message, Throwable cause) {
            super(message, Objects.requireNonNull(cause));
        }
    }
}
