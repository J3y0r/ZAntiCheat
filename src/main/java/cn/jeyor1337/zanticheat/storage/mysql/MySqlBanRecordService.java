package cn.jeyor1337.zanticheat.storage.mysql;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.logger.LogType;
import cn.jeyor1337.zanticheat.util.logger.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

public class MySqlBanRecordService {

    private static final String TABLE_NAME = "zac_banned_players";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + "uuid VARCHAR(36) NOT NULL, "
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "PRIMARY KEY (uuid)"
            + ")";
    private static final String UPSERT_SQL = "INSERT INTO " + TABLE_NAME + " (uuid) VALUES (?) "
            + "ON DUPLICATE KEY UPDATE uuid = VALUES(uuid)";
    private static final String[] BAN_COMMAND_PREFIXES = new String[]{"ban", "tempban", "ban-ip", "ipban"};

    private volatile boolean initialized;
    private volatile boolean closed = true;

    public boolean initialize() {
        if (!ConfigManager.Config.Database.enabled)
            return false;
        close();
        closed = false;
        try (Connection connection = openConnection()) {
            if (ConfigManager.Config.Database.Mysql.Init.createTables)
                createTables(connection);
            initialized = true;
            Logger.logConsole(LogType.INFO, "(" + Main.getInstance().getName() + ") Connected to MySQL ban storage");
            return true;
        } catch (SQLException exception) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Failed to initialize MySQL ban storage: " + exception.getMessage());
            close();
            return false;
        }
    }

    public boolean saveBannedPlayer(UUID uuid) {
        if (uuid == null || !isReady())
            return false;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Failed to save banned player UUID " + uuid + ": " + exception.getMessage());
            return false;
        }
    }

    public static boolean shouldStorePunishment(Iterable<String> commands) {
        if (commands == null)
            return false;
        for (String command : commands) {
            if (isBanCommand(command))
                return true;
        }
        return false;
    }

    public static boolean isBanCommand(String command) {
        if (command == null)
            return false;
        String normalized = command.trim();
        if (normalized.startsWith("/"))
            normalized = normalized.substring(1);
        int separator = normalized.indexOf(' ');
        String root = separator == -1 ? normalized : normalized.substring(0, separator);
        root = root.toLowerCase(Locale.ROOT);
        for (String prefix : BAN_COMMAND_PREFIXES) {
            if (root.equals(prefix))
                return true;
        }
        return false;
    }

    public void reload() {
        initialize();
    }

    public void close() {
        initialized = false;
        closed = true;
    }

    private boolean isReady() {
        return initialized && !closed;
    }

    private Connection openConnection() throws SQLException {
        if (closed)
            throw new SQLException("MySQL ban storage is closed");
        return DriverManager.getConnection(buildJdbcUrl(),
                ConfigManager.Config.Database.Mysql.username,
                ConfigManager.Config.Database.Mysql.password);
    }

    private void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE_SQL);
        }
    }

    private String buildJdbcUrl() {
        return "jdbc:mysql://"
                + ConfigManager.Config.Database.Mysql.host
                + ":"
                + ConfigManager.Config.Database.Mysql.port
                + "/"
                + ConfigManager.Config.Database.Mysql.database
                + "?ssl="
                + ConfigManager.Config.Database.Mysql.Advanced.ssl
                + "&useUnicode="
                + ConfigManager.Config.Database.Mysql.Advanced.useUnicode
                + "&verifyServerCertificate="
                + ConfigManager.Config.Database.Mysql.Advanced.verifyServerCertificate
                + "&characterEncoding="
                + ConfigManager.Config.Database.Mysql.Advanced.characterEncoding;
    }
}
