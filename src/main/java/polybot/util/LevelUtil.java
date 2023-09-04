package polybot.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import polybot.Constants;
import polybot.LevelEntry;
import polybot.storage.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class LevelUtil {
    private static final Ellipse2D.Float AVATAR_CIRCLE = new Ellipse2D.Float(Constants.CARD_BORDER + 15, Constants.CARD_BORDER + 20, 128, 128);
    private static final Cache<Long, BufferedImage> AVATAR_CACHE = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

    public static BufferedImage generateLeaderboard(long userId, int page) {
        if (page < 1) page = 0;
        int startIndex = 20 * page;
        List<LevelEntry> entries = BotStorage.getLevelEntries(startIndex, 20);
        List<UserSettingEntry> settingEntries = BotStorage.getUserSettings(entries, UserSetting.LEVEL_CARD_COLOR);

        BufferedImage leaderboardCard = new BufferedImage(entries.isEmpty() ? 800 : 4096, entries.isEmpty() ? 200 : 1024, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = leaderboardCard.createGraphics();
        LevelUtil.handleBackgroundImage(leaderboardCard, g2d);

        LevelUtil.adjustTransparency(g2d, BotStorage.getSettingAsLong(Setting.LEADERBOARD_TRANSPARENCY, 0));
        g2d.fillRoundRect(25, 25, leaderboardCard.getWidth() - 25*2, leaderboardCard.getHeight()-25*2, 50, 50);


        if (entries.isEmpty()) {
            g2d.setColor(Color.white);
            g2d.setFont(Fonts.ROBOTO.getFont().deriveFont(40f));
            BotUtil.drawTextCentered(g2d, "This page is empty.", leaderboardCard.getWidth(), (leaderboardCard.getHeight()/2) + (g2d.getFontMetrics().getHeight()/2));
        } else {
            int xpos = 60;
            int ypos = 50;
            for (int i = 0; i < entries.size(); i++) {
                if (i != 0 && i % 5 == 0) {
                    xpos += (leaderboardCard.getWidth()/4) - 35;
                    ypos = 50;
                }

                LevelEntry entry = entries.get(i);
                User user = UserUtil.getUser(entry.getUserId());
                Color savedColor = ColorUtil.getColorFromString(settingEntries.get(i).getValue());
                BufferedImage image = getCachedAvatar(user);

                if (entry.getUserId() == userId) {
                    g2d.setColor(new Color(255, 255, 255 , 94));
                    g2d.fillRoundRect(xpos-25, ypos-15, (leaderboardCard.getWidth()/4)-40, (leaderboardCard.getHeight()/5) - 25, 50, 50);
                }

                int currXp = entry.getXp() - entry.getXpForLevel(entry.getLevel());
                int xpRequired = entry.getXpForNextLevel() - entry.getXpForLevel(entry.getLevel());

                Ellipse2D.Float circle = new Ellipse2D.Float(xpos, ypos, 128, 128);
                g2d.setClip(circle);
                g2d.drawImage(image, xpos, ypos, 128, 128, null);
                g2d.setClip(null);

                g2d.setColor(ColorUtil.PROGRESS_BAR_BACKGROUND);
                g2d.setStroke(new BasicStroke(4));
                g2d.draw(circle);

                g2d.setColor(Color.white);
                g2d.setFont(Fonts.ROBOTO.getFont().deriveFont(40f));
                g2d.drawString(user.getName(), xpos+150, ypos+35);

                g2d.setFont(g2d.getFont().deriveFont(42f));
                FontMetrics metrics = g2d.getFontMetrics();
                String rank = "#" + (i+1 + (20 * page));
                g2d.drawString(rank, (xpos + 925) - metrics.stringWidth(rank), ypos+35); //make bigger?

                g2d.setStroke(Constants.LEVEL_CARD_LINE);
                g2d.drawLine(xpos + 128 + 25, ypos + 50, xpos + 925, ypos + 50);

                g2d.setFont(Constants.ROBOTO_LIGHT.deriveFont(30f));
                metrics = g2d.getFontMetrics();

                String xpFirst = "XP: " + currXp + "/" + xpRequired;
                String xpLast = " (Total XP: " + entry.getXp() + ")";

                g2d.setColor(Color.white);
                g2d.drawString(xpFirst, xpos+150, ypos+90);

                g2d.setColor(ColorUtil.LEVEL_CARD_GRAY);
                g2d.drawString("Level " + entry.getLevel(), (xpos + 925) - metrics.stringWidth("Level " + entry.getLevel()), ypos+90);

                g2d.setFont(g2d.getFont().deriveFont(26f));
                g2d.drawString(xpLast, (xpos+150) + metrics.stringWidth(xpFirst), ypos+90); //make smaller slightly w//diff color?
                //g2d.drawString("XP: " + entry.getXp(), xpos, ypos);

                // Draw progress bar background
                g2d.setColor(ColorUtil.PROGRESS_BAR_BACKGROUND);
                g2d.setStroke(Constants.LEVEL_CARD_STROKE);
                g2d.drawLine(xpos + 155, ypos + 113, xpos + 925, ypos + 113);

                // Draw progress bar if they have progress
                if (entry.getLevelProgress() != 0) {
                    g2d.setColor(savedColor);
                    g2d.drawLine(xpos + 155, ypos + 113, (xpos + 155) + (int) (entry.getLevelProgress() * 770), ypos + 113);
                }

                ypos+=200;
            }
        }

        g2d.dispose();
        return leaderboardCard;
    }

    public static BufferedImage getLevelCard(User user, OnlineStatus status, LevelEntry entry) {
        BufferedImage levelCard = new BufferedImage(810, 245, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = levelCard.createGraphics(); // set these just because the level card may have bg disabled
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        UserSettingEntry settingEntry = BotStorage.getUserSetting(user.getIdLong(), UserSetting.LEVEL_CARD_FONT);
        FontMetrics bigFontMetrics = g2d.getFontMetrics(Fonts.getFontFromString(settingEntry.getValue()).getFont());

        settingEntry = BotStorage.getUserSetting(user.getIdLong(), UserSetting.LEVEL_CARD_BACKGROUND);
        if (settingEntry.getValue().equalsIgnoreCase("true")) LevelUtil.handleBackgroundImage(levelCard, g2d);
        else {
            g2d.setColor(Color.darkGray);
            g2d.fillRect(0, 0, levelCard.getWidth(), levelCard.getHeight());
        }

        // Background graphic
        LevelUtil.adjustTransparency(g2d, BotStorage.getSettingAsLong(Setting.LEVEL_CARD_TRANSPARENCY, 0));
        g2d.fillRoundRect(Constants.CARD_BORDER, Constants.CARD_BORDER, levelCard.getWidth()- Constants.CARD_BORDER*2, levelCard.getHeight()-Constants.CARD_BORDER*2, 50, 50);

        // Profile picture
        g2d.setClip(AVATAR_CIRCLE);
        g2d.drawImage(getCachedAvatar(user), (int) AVATAR_CIRCLE.x, (int) AVATAR_CIRCLE.y, 128, 128, null);
        g2d.setClip(null);

        // Profile picture outline
        g2d.setColor(Color.black);
        g2d.setStroke(new BasicStroke(4));
        g2d.draw(AVATAR_CIRCLE);

        // Set color based off status (offline color if unknown/actually offline)
        switch (status != null ? status : OnlineStatus.UNKNOWN) {
            case ONLINE -> g2d.setColor(ColorUtil.ONLINE);
            case IDLE -> g2d.setColor(ColorUtil.IDLE);
            case DO_NOT_DISTURB -> g2d.setColor(ColorUtil.DND);
            default -> g2d.setColor(ColorUtil.OFFLINE);
        }

        // Status indicator
        g2d.fillOval((int) AVATAR_CIRCLE.x + 96, (int) AVATAR_CIRCLE.y + 96, 32, 32);

        // Status indicator outline
        g2d.setColor(Color.black);
        g2d.drawOval((int) AVATAR_CIRCLE.x + 96, (int) AVATAR_CIRCLE.y + 96, 32, 32);

        // Username
        g2d.setColor(Color.white);
        g2d.setFont(bigFontMetrics.getFont());
        g2d.drawString(UserUtil.getUserAsName(user), 128 + Constants.CARD_BORDER*2, 65);

        // Dividing line
        g2d.setStroke(Constants.LEVEL_CARD_LINE);
        g2d.drawLine(128 + Constants.CARD_BORDER*2, Constants.CARD_BORDER*3, levelCard.getWidth()- Constants.CARD_BORDER*2, Constants.CARD_BORDER*3);

        settingEntry = BotStorage.getUserSetting(user.getIdLong(), UserSetting.LEVEL_CARD_COLOR);
        Color settingColor = ColorUtil.getColorFromString(settingEntry.getValue());

        // User's current level/rank position
        g2d.setColor(settingColor);
        String lvlStr = String.valueOf(entry.getLevel()), rankStr = "#" + entry.getRank() + ",";
        g2d.drawString(lvlStr, levelCard.getWidth() - bigFontMetrics.stringWidth(lvlStr) - Constants.CARD_BORDER*2, Constants.CARD_BORDER*2 + 15);

        g2d.setColor(Color.white);
        g2d.drawString(rankStr, 128 + Constants.CARD_BORDER*4, levelCard.getHeight() - Constants.CARD_BORDER*3);

        g2d.setColor(settingColor);
        g2d.setFont(bigFontMetrics.getFont().deriveFont(bigFontMetrics.getFont().getSize() - 10f));
        int levelXPos = levelCard.getWidth() - g2d.getFontMetrics().stringWidth("Level ") - bigFontMetrics.stringWidth(lvlStr) - Constants.CARD_BORDER*2;
        g2d.drawString("Level", levelXPos, Constants.CARD_BORDER*2 + 15);

        String lbPage = " Page " + LevelUtil.getPageNumber(entry);

        // Rank info
        g2d.setColor(Color.white);
        g2d.drawString("Rank", 128 + Constants.CARD_BORDER*2, levelCard.getHeight() - Constants.CARD_BORDER*3);
        g2d.drawString(lbPage, 128 + Constants.CARD_BORDER*2 + g2d.getFontMetrics().stringWidth("Rank ") + bigFontMetrics.stringWidth(rankStr), levelCard.getHeight() - Constants.CARD_BORDER*3);
        g2d.drawString("Levels may be inaccurate!", 128 + Constants.CARD_BORDER*2, levelCard.getHeight() - Constants.CARD_BORDER*6);

        // Draw their xp progress to next level
        int currXp = entry.getXp() - entry.getXpForLevel(entry.getLevel());
        int xpRequired = entry.getXpForNextLevel() - entry.getXpForLevel(entry.getLevel());

        // Neatly format their xp
        String xpString = currXp + "/" + xpRequired + " XP";
        g2d.setColor(ColorUtil.LEVEL_CARD_GRAY);
        g2d.drawString(xpString, levelCard.getWidth() - g2d.getFontMetrics().stringWidth(xpString) - Constants.CARD_BORDER*2, levelCard.getHeight()- Constants.CARD_BORDER*3);

        // Draw progress bar background
        g2d.setColor(ColorUtil.PROGRESS_BAR_BACKGROUND);
        g2d.setStroke(Constants.LEVEL_CARD_STROKE);
        g2d.drawLine(Constants.CARD_BORDER*2, levelCard.getHeight()- Constants.CARD_BORDER*2, levelCard.getWidth() - Constants.CARD_BORDER*2, levelCard.getHeight()- Constants.CARD_BORDER*2);

        // Draw progress bar if they have progress
        if (entry.getLevelProgress() != 0) {
            g2d.setColor(settingColor);
            g2d.drawLine(Constants.CARD_BORDER*2, levelCard.getHeight()- Constants.CARD_BORDER*2, Constants.CARD_BORDER*2 + (int) (entry.getLevelProgress() * (levelCard.getWidth() - Constants.CARD_BORDER*4)), levelCard.getHeight()- Constants.CARD_BORDER*2);
        }

        return levelCard;
    }

    public static String getLevelMessage(User user, LevelEntry entry) {
        int currXp = entry.getXp() - entry.getXpForLevel(entry.getLevel());
        int xpRequired = entry.getXpForNextLevel() - entry.getXpForLevel(entry.getLevel());

        return "**__" + UserUtil.getUserAsName(user) + "__**\n" +
                "**Rank:** " + entry.getRank() +  "\n" +
                "**Level:** " + entry.getLevel() + "\n" +
                "**XP:** " + currXp + "/" + xpRequired + " (tot. " + entry.getXp() + ")\n" +
                "**Leaderboard Page:** " + Math.round(entry.getRank() / 20f);
    }

    public static void handleBackgroundImage(BufferedImage image, Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (BotStorage.getCardBackground() == null) return;

        AffineTransform transform = g2d.getTransform();

        float scaleW = (float) image.getWidth() / BotStorage.getCardBackground().getWidth();
        float scaleH = (float) image.getHeight() / BotStorage.getCardBackground().getHeight();
        float scale = Math.max(scaleW, scaleH);

        g2d.scale(scale, scale);
        g2d.drawImage(BotStorage.getCardBackground(),
                (scale == scaleW ? 0 : image.getWidth()/2 - BotStorage.getCardBackground().getWidth()/2),
                (scale == scaleH ? 0 : image.getHeight()/2 - BotStorage.getCardBackground().getHeight()/2),
                null);
        g2d.setTransform(transform);
    }

    public static BufferedImage getCachedAvatar(User user) {
        BufferedImage image = AVATAR_CACHE.get(user.getIdLong(), id -> {
            if (user.getAvatarId() != null) {
                Path avatarPath = Paths.get("./avatars/" + user.getId() + "-" + user.getAvatarId() + ".png");

                if (Files.notExists(avatarPath)) {
                    try {
                        user.getAvatar().downloadToPath(avatarPath).get();
                    } catch (ExecutionException | InterruptedException e) {
                        return BotUtil.DEFAULT_AVATAR;
                    }
                }

                try (InputStream stream = Files.newInputStream(avatarPath)) {
                    return ImageIO.read(stream);
                } catch (IOException ignored) {}
            }

            return BotUtil.DEFAULT_AVATAR;
        });
        if (AVATAR_CACHE.getIfPresent(user.getIdLong()) == null) AVATAR_CACHE.put(user.getIdLong(), image);

        return image;
    }

    public static long getCachedAvatars() {
        return AVATAR_CACHE.estimatedSize();
    }

    public static void adjustTransparency(Graphics2D g2d, long value) {
        g2d.setColor(new Color(0f, 0f, 0f, value / 100f));
    }

    public static int getPageNumber(@NotNull LevelEntry entry) {
        if (entry.getRank() % 20 == 0) return (entry.getRank() / 20)-1;
        return entry.getRank() / 20;
    }

    public static void distributeRewards(@NotNull Member member, @Nullable LevelEntry levelEntry) {
        if (levelEntry == null) return;

        List<Role> roles = new ArrayList<>(member.getRoles());

        // Give them all their roles
        for (Map.Entry<Integer, Long> entry: BotUtil.mapToIntLongMap(BotStorage.getSetting(Setting.ROLE_REWARDS)).entrySet()) {
            if (entry.getKey() > levelEntry.getLevel()) continue;

            Role role = member.getGuild().getRoleById(entry.getValue());
            if (role == null) continue;

            member.getGuild().addRoleToMember(member, role).queue();
        }

        GuildUtil.setRoles(member, roles);
    }

}
