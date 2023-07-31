package polybot.storage;

public enum UserSetting {
    LEVEL_CARD_BACKGROUND("true", "Toggle level card background"),
    LEVEL_CARD_THEME("62d3f5", "Set the level card color theme");

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
            if (setting.friendlyName().equalsIgnoreCase(str) ||
                    setting.key().equalsIgnoreCase(str) ||
                    setting.name().equalsIgnoreCase(str) ||
                    setting.friendlyName()
                            .replace("message", "msg")
                            .replace("image", "img")
                            .replace("level", "lvl")
                            .replace("background", "bg")
                            .equalsIgnoreCase(str)) {
                return setting;
            }
        }

        return null;
    }


}
