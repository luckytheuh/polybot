package polybot.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ColorUtil {
    private static final Map<String, Color> DEFAULT_COLORS;

    public static final Color ONLINE = new Color(35, 165, 90),
            IDLE = new Color(250, 165, 27),
            DND = new Color(240, 72, 72),
            OFFLINE = new Color(128, 132, 142),
            LEVEL_CARD_GRAY = new Color(182, 182, 182),
            PROGRESS_BAR_BACKGROUND = new Color(72, 75, 78, 128),
            REPORT_YELLOW = new Color(249, 194, 107),
            COOLDOWN_ADD = new Color(113, 174, 215),
            COOLDOWN_REMOVE = new Color(187, 35, 86);
            //DEFAULT_LEVEL_COLOR = new Color(Integer.parseInt(UserSetting.LEVEL_CARD_COLOR.getDefaultValue(), 16));
    public static final int RGB_COLOR_MAX = 0xFFFFFF;

    static {
        DEFAULT_COLORS = new HashMap<>();

        try {
            for (Field field : Class.forName("java.awt.Color").getFields()) {
                if (DEFAULT_COLORS.containsKey(field.getName().replace("_", "").toLowerCase())) continue;

                Object obj = field.get(null);
                if (obj instanceof Color color) DEFAULT_COLORS.put(field.getName().toLowerCase(), color);
            }
        } catch (IllegalAccessException | ClassNotFoundException ignored) {}
    }

    public static Color getColorFromString(String str) {
        if (str == null || str.isEmpty() || str.isBlank()) return null;

        try {
            int rgb = Integer.parseInt(str.replaceAll("#|0x", ""), 16);
            return rgb > RGB_COLOR_MAX ? null : new Color(rgb);
        } catch (NumberFormatException ignored) {}
        if (DEFAULT_COLORS.containsKey(str.toLowerCase())) return DEFAULT_COLORS.get(str.toLowerCase());

        return null;
    }

    public static Map<String, Color> getColorClassDefaults() {
        return DEFAULT_COLORS;
    }

    public static BufferedImage getColorAsImage(Color color) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, 256, 256);
        g2d.dispose();
        return image;
    }

    public static Color getRandomColor() {
        return new Color(ThreadLocalRandom.current().nextInt(0, RGB_COLOR_MAX+1));
    }

}
