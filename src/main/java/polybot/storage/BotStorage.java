package polybot.storage;

import polybot.PolyBot;
import polybot.levels.LevelEntry;
import polybot.util.BotUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BotStorage {

    private static final ExecutorService DB_WORKERS = Executors.newCachedThreadPool();
    private static BufferedImage CARD_BACKGROUND;
    private static final Map<Setting, String> SETTING_CACHE = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Long, UserSettingEntry[]> USER_SETTING_CACHE = Collections.synchronizedMap(new HashMap<>());
    private static final Connection connection;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:polybot.db");
            Statement initDB = connection.createStatement();
            initDB.executeUpdate("CREATE TABLE IF NOT EXISTS `levels` (`id` INTEGER NOT NULL, `currLvl` INTEGER NOT NULL, `xp` INTEGER NOT NULL, `messages` INTEGER NOT NULL, PRIMARY KEY(`id`));");
            initDB.executeUpdate("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`));");
            initDB.executeUpdate("CREATE TABLE IF NOT EXISTS `userSettings` (`id` INTEGER NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL);");
            initDB.close();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }

        changeCardImage(getSetting(Setting.CARD_IMAGE));
    }

    private static boolean changeCardImage(String url) { //this assumes the new url was added to the db and simply fetches
        if (url.isEmpty() || url.isBlank()) return false;

        try {
            CARD_BACKGROUND = ImageIO.read(new URL(url));
            return true;
        } catch (IOException e) {
            CARD_BACKGROUND = null;
            return false;
        }
    }


    public static String getSetting(Setting setting) {
        return getSetting(setting, "");
    }

    public static long getSettingAsLong(Setting setting, long def) {
        return BotUtil.getAsLong(getSetting(setting), def);
    }

    public static List<String> getSettingAsList(Setting setting) {
        String value = getSetting(setting, "");
        if (value.isBlank() || value.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(List.of(value.split(",")));
    }

    public static String getSetting(Setting setting, String def) {
        if (SETTING_CACHE.containsKey(setting)) return SETTING_CACHE.getOrDefault(setting, def);

        synchronized (connection) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT `value` FROM `settings` WHERE `key`='" + setting.key() + "';")) {
                    if (resultSet.next()) {
                        SETTING_CACHE.put(setting, resultSet.getString("value"));
                        return resultSet.getString("value");
                    }
                }
            } catch (SQLException e) { logDatabaseError(OperationType.READING, e); }
        }

        return def;
    }

    public static void setSetting(final Setting setting, final String value) {
        String existingValue = SETTING_CACHE.getOrDefault(setting, "");
        boolean isClear = value.toLowerCase().startsWith("clear");

        if (isClear) {
            SETTING_CACHE.put(setting, existingValue = "");
        } else if (setting.isAppendable()) {
            existingValue = (existingValue.isBlank() || existingValue.isEmpty() ? "" : existingValue + ',') + value;
            SETTING_CACHE.put(setting, existingValue);
        } else {
            SETTING_CACHE.put(setting, existingValue = value);
        }

        final String newValue = existingValue;
        DB_WORKERS.submit(() -> {
            synchronized (connection) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO `settings` (`key`, `value`) VALUES ('" + setting.key() + "', ?);")) {
                    statement.setString(1, newValue);
                    statement.executeUpdate();

                    if (setting == Setting.CARD_IMAGE) changeCardImage(isClear ? "" : value);
                } catch (SQLException e) { logDatabaseError(OperationType.WRITING, e); }
            }
        });
    }


    public static UserSettingEntry getUserSetting(long userId, UserSetting setting) { //TODO: find a system to allow caching the user settings while also keeping the key value method
        if (USER_SETTING_CACHE.containsKey(userId)) {
            //return if the user setting in the cache is not null, otherwise fetch from db

            UserSettingEntry e = USER_SETTING_CACHE.get(userId)[setting.ordinal()];
            if (e != null) return e;
        }

        synchronized (connection) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM `userSettings` WHERE `id`=" + userId + " AND `key`='" + setting.key() + "';")) {
                    if (resultSet.next()) {
                        UserSettingEntry entry = new UserSettingEntry(setting, resultSet.getString("value"));
                        USER_SETTING_CACHE.put(userId, new UserSettingEntry[UserSetting.values().length]);
                        USER_SETTING_CACHE.get(userId)[setting.ordinal()] = entry;
                        return entry;
                    }
                }
            } catch (SQLException e) { logDatabaseError(OperationType.READING, e); }
        }

        return new UserSettingEntry(setting);
    }

    public static void setUserSetting(long userId, UserSettingEntry settingEntry) {
        if (USER_SETTING_CACHE.get(userId) == null) {
            USER_SETTING_CACHE.put(userId, new UserSettingEntry[UserSetting.values().length]);
            USER_SETTING_CACHE.get(userId)[settingEntry.getUserSetting().ordinal()] = settingEntry;
        }

        DB_WORKERS.submit(() -> {
            synchronized (connection) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `userSettings` WHERE `id`=? AND `key`=?;")) {
                    statement.setLong(1, userId);
                    statement.setString(2, settingEntry.getUserSetting().key());
                    statement.executeUpdate();
                } catch (SQLException e) { logDatabaseError(OperationType.WRITING, e); }

                try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO `userSettings` (`id`, `key`, `value`) VALUES (" + userId + ",?,?);")) {
                    statement.setString(1, settingEntry.getUserSetting().key());
                    statement.setString(2, settingEntry.getValue());
                    statement.executeUpdate();
                } catch (SQLException e) { logDatabaseError(OperationType.WRITING, e); }
            }
        });
    }











    public static int getTotalRankedUsers() {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(`id`) FROM `levels`;")) {

                return resultSet.next() ? resultSet.getInt("count(`id`)") : 0;
            }
        } catch (SQLException e) {
            logDatabaseError(OperationType.READING, e);
            return 0;
        }
    }//INDECIDES for id?

    public static synchronized LevelEntry getLevelEntry(long userId) {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM `levels` WHERE `id`=" + userId + ";")) {
                // If we have an entry in the database, return it
                if (resultSet.next()) return new LevelEntry(userId, resultSet.getInt("currLvl"), resultSet.getInt("xp"), 0, resultSet.getInt("messages"), false);
            }
        } catch (SQLException e) {
            logDatabaseError(OperationType.READING, e);
            return new LevelEntry(0, 0, 0, 0, 0, true);
        }

        return null; // Returning null means there was no entry stored for them
    }

    public static void saveLevelEntry(long userId, LevelEntry entry) {
        DB_WORKERS.submit(() -> {
            synchronized (connection) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO `levels` (`id`, `currLvl`, `xp`, `messages`) VALUES (?,?,?,?);")) {
                    statement.setLong(1, userId);
                    statement.setInt(2, entry.getLevel());
                    statement.setInt(3, entry.getXp());
                    statement.setInt(4, entry.getMessages());
                    statement.executeUpdate();
                } catch (SQLException e) { logDatabaseError(OperationType.WRITING, e); }
            }
        });
    }

    public static void removeLevelEntry(long userId) {
        DB_WORKERS.submit(() -> {
            synchronized (connection) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM `levels` WHERE `id`=" + userId + ";");
                } catch (SQLException e) { logDatabaseError(OperationType.WRITING, e); }
            }
        });
    }

    private static void logDatabaseError(OperationType type, Throwable t) {
        PolyBot.getLogger().error("An error occurred in the database while " + type, t);
    }

    public static BufferedImage getCardBackground() {
        return CARD_BACKGROUND;
    }

    public static float getCardScale(int width, int height) {
        float scaleW = (float) width / getCardBackground().getWidth();
        float scaleH = (float) height / getCardBackground().getHeight();
        return Math.max(scaleW, scaleH);
    }

    private enum OperationType {
        SETTING_UP,
        READING,
        WRITING;

        @Override
        public String toString() {
            //Keep first character uppercase, rest lowercase and replace _ with spaces
            return /*name().charAt(0) +*/ (name().toLowerCase()/*.substring(1)*/.replaceAll("_", " "));
        }
    }
}
