package polybot.storage;

import polybot.util.BotUtil;

public enum UserSetting {
    LEVEL_CARD_BACKGROUND("true", "Toggle level card background"),
    LEVEL_CARD_FONT("Roboto", "Change the font used for the level card text (Use `&fonts` for a list of available fonts)"),
    LEVEL_CARD_COLOR("62d3f5", "Changes the color of the level card progress bar + level text");

    final String description, defaultValue;

    UserSetting(String defaultValue, String description) {
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
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

    public static UserSetting searchForSetting(String str) {
        for (UserSetting setting : UserSetting.values()) {
            if (BotUtil.settingNameMatch(str, setting.friendlyName(false), setting.key(), setting.name())) return setting;
        }

        return null;
    }


}
