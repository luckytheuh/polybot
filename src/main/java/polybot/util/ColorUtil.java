package polybot.util;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

public class ColorUtil {

    public static final int RGB_COLOR_MAX = 0xFFFFFF;

    public static Color getColorFromString(String str) {
        try {
            int rgb = Integer.parseInt(str.replaceAll("#|0x", ""), 16);
            return rgb > RGB_COLOR_MAX ? null : new Color(rgb);
        } catch (NumberFormatException ignored) {}
        return null;
    }

    public static Color getRandomColor() {
        return new Color(ThreadLocalRandom.current().nextInt(0, RGB_COLOR_MAX+1));
    }

}
