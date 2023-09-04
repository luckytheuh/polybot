package polybot.storage;

import polybot.util.BotUtil;

public enum Setting {
    LEVEL_UP_MESSAGE(false, false, true, 2),
    WELCOME_MESSAGE(false, false, true, 1),
    LEAVE_MESSAGE(false, false, true, 1),
    WELCOME_CARD_TRANSPARENCY(false),
    LEADERBOARD_TRANSPARENCY(false),
    LEVEL_CARD_TRANSPARENCY(false),
    WELCOME_LEAVE_CHANNEL(false),
    COOLDOWN_LOG_CHANNEL(false),
    PARK_RANGER_CHANNEL(false),
    MESSAGE_LOG_CHANNEL(false),
    BOT_REPORT_CHANNEL(false),
    AUTO_REPORT_COUNT(false),
    LEVEL_UP_CHANNEL(false),
    DYNO_LOG_CHANNEL(false),
    ACTIVITY_STATUS(false),
    COOLDOWN_ROLE(false),
    XP_MUTE_ROLE(false),
    ROBOT_ROLE(false),
    CARD_IMAGE(false),
    EMOJI_CAP(false),
    EXCLUDED_FROM_MOD_CHECK,
    BLACKLISTED_KEYWORDS,
    WHITELISTED_KEYWORDS,
    STICKER_BLACKLIST,
    CHANNEL_BLACKLIST,
    REPORT_BLACKLIST,
    USER_BLACKLIST,
    LINK_WHITELIST,
    NO_XP_CHANNELS,
    ROLE_REWARDS(true, false);

    final boolean hasPlaceholders, canAppend, excludeFromList;
    final int totalPlaceholders;
    Setting() {
        this(true);
    }

    Setting(boolean canAppend) {
        this(canAppend, false);
    }

    Setting(boolean canAppend, boolean excludeFromList) {
        this(canAppend, excludeFromList, false, -1);
    }

    Setting(boolean canAppend, boolean excludeFromList, boolean hasPlaceholders, int totalPlaceholders) {
        this.totalPlaceholders = totalPlaceholders;
        this.excludeFromList = excludeFromList;
        this.hasPlaceholders = hasPlaceholders;
        this.canAppend = canAppend;
    }

    public boolean isAppendable() {
        return canAppend;
    }

    public boolean hasPlaceholders() {
        return hasPlaceholders;
    }

    public boolean excludeFromList() {
        return excludeFromList;
    }

    public int getTotalPlaceholders() {
        return totalPlaceholders;
    }

    public String key() {
        return name().toLowerCase().replace("_", "-");
    }

    public String friendlyName() {
        return friendlyName(true);
    }

    public String friendlyName(boolean upper) {
        if (upper) return name().charAt(0) + name().substring(1).toLowerCase().replace("_", " ");
        else return name().toLowerCase().replace("_", " ");
    }

    public static Setting searchForSetting(String str) {
        for (Setting setting : Setting.values()) {
            if (BotUtil.settingNameMatch(str, setting.friendlyName(false), setting.key(), setting.name())) return setting;
        }

        return null;
    }
}
