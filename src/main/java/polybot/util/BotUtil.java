package polybot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import polybot.Constants;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class BotUtil {

    private static final Pattern HTTP_PATTERN = Pattern.compile("([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])", Pattern.CASE_INSENSITIVE);
    public static final BufferedImage NULL_IMAGE = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);

    public static final BufferedImage DEFAULT_AVATAR;
    private static final Graphics2D g;

    static {
        g = NULL_IMAGE.createGraphics();

        g.setColor(Color.magenta);
        g.fillRect(0, 0, 32, 32);
        g.fillRect(32, 32, 32, 32);

        g.setColor(Color.black);
        g.fillRect(32, 0, 32, 32);
        g.fillRect(0, 32, 32, 32);

        BufferedImage da = null;
        try {
            da = ImageIO.read(new URL(String.format(User.DEFAULT_AVATAR_URL, 0)));
        } catch (IOException ignored) {}

        DEFAULT_AVATAR = da == null ? NULL_IMAGE : da;
    }

    public static FontMetrics getFontMetrics(Font font) {
        return g.getFontMetrics(font);
    }

    public static JsonElement fetchWebsiteJson(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.addRequestProperty("User-Agent", "PolyBot v" + Constants.VERSION);
            connection.connect();

            String line = "";

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String c;
                while ((c = reader.readLine()) != null) {
                    line = line.concat(c + "\n");
                }
            }

            return JsonParser.parseString(line);
        } catch (IOException ignored) {}

        return null;
    }

    public static void drawTextCentered(Graphics2D g, String text, int width, int ypos) {
        g.drawString(text, (width - g.getFontMetrics().stringWidth(text)) / 2, ypos);
    }

    public static MessageCreateAction uploadImage(MessageChannel channel, BufferedImage image, String filename, boolean returnAction) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", byteStream);
            MessageCreateAction action = channel.sendFiles(FileUpload.fromStreamSupplier(filename, () -> new ByteArrayInputStream(byteStream.toByteArray())));
            if (returnAction) return action;
            else {
                action.queue();
                return null;
            }
        } catch (IOException ignored) {}

        return null;
    }

    public static byte[] getBytesFromImage(BufferedImage image) {
        if (image != null) {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ImageIO.write(image, "png", byteStream);
                return byteStream.toByteArray();
            } catch (IOException ignored) {}
        }

        return new byte[0];
    }

    public static Role getRoleFromGuild(Guild guild, long roleId) {
        // Can't do ANYTHIN
        if (guild == null || !isValidId(roleId)) return null;

        return guild.getRoleById(roleId);
    }

    public static long[] mapToLongArray(String str) {
        return Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray();
    }

    public static Map<Integer, Long> mapToIntLongMap(String str) {
        Map<Integer, Long> integerLongMap = new HashMap<>();

        if (str == null || str.isEmpty() || str.isBlank()) return integerLongMap;

        for (String reward : str.split("\\|")) {
            try {
                String[] rewardValues = reward.split(",");
                integerLongMap.put(Integer.parseInt(rewardValues[0]), Long.parseLong(rewardValues[1]));
            } catch (NumberFormatException ignored) {}
        }

        return integerLongMap;
    }

    public static boolean settingNameMatch(String strToCheck, String... alts) {
        if (alts == null) return false;

        for (String alt : alts) {
            if (alt.equalsIgnoreCase(strToCheck)) return true;
            alt = replace(alt.toLowerCase());

            if (alt.equalsIgnoreCase(replace(strToCheck))) return true;
        }

        return false;
    }

    public static long getAsLong(String value) {
        return getAsLong(value, 0L);
    }

    public static long getAsLong(String value, long def) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {}
        return def;
    }

    public static boolean isValidId(long id) {
        return id > 0;
    }

    public static String sanitise(String input) {
        input = HTTP_PATTERN.matcher(input).replaceAll("<redacted>");
        return input;
    }

    public static boolean hasIllegalKeywords(Message message) {
        String contentRaw = message.getContentRaw();

        return message.getMentions().mentionsEveryone() || hasIllegalKeywords(contentRaw);
    }

    public static boolean hasIllegalKeywords(String message) {
        return message.contains("@everyone") ||
                message.contains("@here") ||
                HTTP_PATTERN.matcher(message).find();
    }

    private static String replace(String str) {
        return str.replace("image", "img")
                .replace("level", "lvl")
                .replace("message", "msg")
                .replace("background", "bg")
                .replace("alternative", "alt")
                .replace("leaderboard", "lb")
                .replace("calculator", "calc")
                .replaceAll("[-_]", " ");
    }

}
