package polybot.storage;

public enum Setting {
    WELCOME_MESSAGE(false, true, 1),
    LVL_UP_MESSAGE(false, true, 2),
    LEAVE_MESSAGE(false, true, 1),
    WELCOME_LEAVE_CHANNEL(false),
    PARK_RANGER_CHANNEL(false),
    BOT_REPORT_CHANNEL(false),
    AUTO_REPORT_COUNT(false),
    LEVEL_UP_CHANNEL(false),
    XP_MUTE_ROLE(false),
    CARD_IMAGE(false),
    EMOJI_CAP(false),
    LINK_WHITELIST,
    NO_XP_CHANNELS,
    ROLE_REWARDS;

    final boolean hasPlaceholders;
    final int totalPlaceholders;
    final boolean canAppend;
    Setting() {
        canAppend = true;
        totalPlaceholders = -1;
        hasPlaceholders = false;
    }

    Setting(boolean canAppend) {
        this.canAppend = canAppend;
        hasPlaceholders = false;
        totalPlaceholders = -1;
    }

    Setting(boolean canAppend, boolean hasPlaceholders, int totalPlaceholders) {
        this.totalPlaceholders = totalPlaceholders;
        this.hasPlaceholders = hasPlaceholders;
        this.canAppend = canAppend;
    }

    public boolean isAppendable() {
        return canAppend;
    }

    public boolean hasPlaceholders() {
        return hasPlaceholders;
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
            if (setting.friendlyName().equalsIgnoreCase(str) ||
                    setting.key().equalsIgnoreCase(str) ||
                    setting.name().equalsIgnoreCase(str) ||
                    setting.friendlyName().replace("message", "msg").equalsIgnoreCase(str) ||
                    setting.friendlyName().replace("image", "img").equalsIgnoreCase(str)) {
                return setting;
            }
        }

        return null;
    }
}
