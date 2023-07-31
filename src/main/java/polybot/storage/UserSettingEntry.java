package polybot.storage;

import polybot.util.ColorUtil;

public class UserSettingEntry {
    private final UserSetting userSetting;
    private String value;

    public UserSettingEntry(UserSetting userSetting) {
        this(userSetting, null);
    }

    public UserSettingEntry(UserSetting userSetting, String value) {
        this.userSetting = userSetting;
        this.value = value;
    }

    public UserSetting getUserSetting() {
        return userSetting;
    }

    public String getValue() {
        return value == null ? userSetting.getDefaultValue() : value;
    }

    public boolean setValue(String value) {
        //TODO: do checks here to see if this value is appropriate
        switch (userSetting) {
            case LEVEL_CARD_THEME -> {
                if (ColorUtil.getColorFromString(value) == null) return false;
            } case LEVEL_CARD_BACKGROUND -> {
                if (!(value.equalsIgnoreCase("false") || value.equalsIgnoreCase("true"))) return false;
            }
        }

        this.value = value;
        return true;
    }
}
