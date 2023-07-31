package polybot.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BotUtil {

    public static final Color ONLINE = new Color(35, 165, 90), IDLE = new Color(250, 165, 27), DND = new Color(240, 72, 72), OFFLINE = new Color(128, 132, 142);
    public static final int CARD_BORDER = 25;
    public static final BufferedImage NULL_IMAGE_LOL = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);

    static {
        Graphics2D g = NULL_IMAGE_LOL.createGraphics();

        g.setColor(Color.magenta);
        g.fillRect(0, 0, 32, 32);
        g.fillRect(32, 32, 32, 32);

        g.setColor(Color.black);
        g.fillRect(32, 0, 32, 32);
        g.fillRect(0, 32, 32, 32);
        g.dispose();
    }

    public static JsonObject fetchWebsiteJson(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.addRequestProperty("User-Agent", "NPC Bot v0.0.0");
            connection.connect();

            String line = "";

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String c;
                while ((c = reader.readLine()) != null) {
                    line = line.concat(c + "\n");
                }
            }

            return JsonParser.parseString(line).getAsJsonObject();
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

            MessageCreateAction action = channel.sendFiles(FileUpload.fromData(byteStream.toByteArray(), filename));

            byteStream.close();
            if (returnAction) return action;
            else {
                action.queue();
                return null;
            }
        } catch (IOException ignored) {}

        return null;
    }

    public static AuditableRestAction<Void> addRoleToMember(Member member, long roleId) {
        Role role = getRoleFromGuild(member.getGuild(), roleId);
        if (role == null) return null;

        return member.getGuild().addRoleToMember(member, role);
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

        for (String reward : str.split(",")) {
            try {
                String[] rewardValues = reward.split("\\|");
                integerLongMap.put(Integer.parseInt(rewardValues[0]), Long.parseLong(rewardValues[1]));
            } catch (NumberFormatException ignored) {}
        }

        return integerLongMap;
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
        return id >= 0;
    }

}
