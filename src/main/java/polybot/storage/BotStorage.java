package polybot.storage;

import polybot.PolyBot;
import polybot.levels.LevelEntry;

import java.sql.*;

public class BotStorage {

    private static final Connection connection;

    static {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:polybot.db");
            Statement initDB = c.createStatement();
            //initDB.executeUpdate("CREATE TABLE IF NOT EXISTS `mod_log` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `server` INTEGER NOT NULL, `time` INTEGER NOT NULL, `user` INTEGER NOT NULL, `action` VARCHAR(32) NOT NULL, `target` INTEGER NOT NULL, `reason` TEXT NOT NULL);");
            initDB.executeUpdate("CREATE TABLE IF NOT EXISTS `levels` (`id` INTEGER NOT NULL, `currLvl` INTEGER NOT NULL, `xp` INTEGER NOT NULL, PRIMARY KEY(`id`));");
            initDB.close();
        } catch (ClassNotFoundException | SQLException e) {
            PolyBot.getLogger().error("Cannot load database, exiting", e);
            System.exit(1);
        }

        connection = c;
    }

    public static LevelEntry getLevelEntry(long userId) {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SELECT `id`, `currLvl`, `xp` FROM `levels` WHERE `id`=" + userId + ";")) {
                // If we have an entry in the database, return it
                if (resultSet.next()) return new LevelEntry(userId, resultSet.getInt("currLvl"), resultSet.getInt("xp"));
            }

        } catch (SQLException e) { logDatabaseError(OperationType.READING, e); }
        return new LevelEntry(0, 0, 0); //Return blank values (low rank)
    }

    public static void saveLevelEntry(long userId, LevelEntry entry) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT OR REPLACE INTO `levels` (`id`, `currLvl`, `xp`) VALUES (" + userId + ", " + entry.getLevel() + ", " + entry.getXp() + ");");
        } catch (SQLException e) { logDatabaseError(OperationType.WRITING, e); }
    }

    private static void logDatabaseError(OperationType type, Throwable t) {
        PolyBot.getLogger().error("An error occurred in the database while " + type, t);
    }

    private enum OperationType {
        SETTING_UP,
        READING,
        WRITING;

        @Override
        public String toString() {
            //Keep first character uppercase, rest lowercase and replace _ with spaces
            return /*name().charAt(0) +*/ (name().toLowerCase().substring(1).replaceAll("_", " "));
        }
    }
}
