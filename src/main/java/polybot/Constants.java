package polybot;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import polybot.storage.Configuration;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.Manifest;

public class Constants {

    public static final long GUILD_ID = Configuration.getSetting("guild-id", 0L), STAFF_CATEGORY = Configuration.getSetting("staff-category", 0L),
            KICKED_ROLE = Configuration.getSetting("kicked-role", 0L), WARNING_ROLE =  Configuration.getSetting("warning-role", 0L),
            WARNING_TWO_ROLE =  Configuration.getSetting("waring-two-role", 0L);
    public static final BasicStroke LEVEL_CARD_STROKE = new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
            LEVEL_CARD_LINE = new BasicStroke(2);
    public static final Emoji CHECK_EMOJI = Emoji.fromUnicode("✅"), WARNING = Emoji.fromUnicode("⚠️"), CROSS_EMOJI = Emoji.fromUnicode("❌");
    public static final Font ROBOTO_LIGHT, ROBOTO_MEDIUM;
    public static final int CARD_BORDER = 25;
    public static final String VERSION;

    static {
        Font rL = null, rM = null;
        try {
            rL = Font.createFont(Font.TRUETYPE_FONT, Files.newInputStream(Paths.get("./fonts/Roboto-Light.ttf"))).deriveFont(28f);
            rM = Font.createFont(Font.TRUETYPE_FONT, Files.newInputStream(Paths.get("./fonts/Roboto-Medium.ttf"))).deriveFont(40f);
        } catch (IOException | FontFormatException e) {
            PolyBot.getLogger().error("Failed to load roboto fonts!", e);
        }

        ROBOTO_LIGHT = rL;
        ROBOTO_MEDIUM = rM;

        String tempVer = null;
        try {
            InputStream stream = ClassLoader.getSystemResourceAsStream("META-INF/MANIFEST.MF");
            if (stream != null) {
                Manifest manifest = new Manifest(stream);

                tempVer = manifest.getMainAttributes().getValue("Version");
            }
        } catch (IOException e) {
            PolyBot.getLogger().warn("Failed to find the version number from the manifest", e);
        }
        if (tempVer == null || tempVer.equalsIgnoreCase("null")) tempVer = "1.4.3";

        VERSION = tempVer;
    }
}
