package polybot.levels;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.PolyBot;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.storage.UserSetting;
import polybot.storage.UserSettingEntry;
import polybot.util.BotUtil;
import polybot.util.ColorUtil;
import polybot.util.UserUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Collections;

public class RankCommand extends SlashCommand {
    private static final Font FONT_BIG = new Font("Tahoma", Font.PLAIN, 28), FONT_SMALL = FONT_BIG.deriveFont(20f);
    private static final int LEVEL_CARD_BORDER = 25;
    private static final Ellipse2D.Float AVATAR_CIRCLE = new Ellipse2D.Float(LEVEL_CARD_BORDER + 15, LEVEL_CARD_BORDER + 20, 128, 128);


    public RankCommand() {
        this.name = "rank";
        this.guildOnly = false;
        this.aliases = new String[]{"level"};
        this.options = Collections.singletonList(new OptionData(OptionType.USER, "user", "User to rank check", false));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!event.getChannel().getType().isGuild()) return;

        User user = event.optUser("user", event.getUser());
        Member member = UserUtil.getMemberFromUser(event.getGuild(), user);
event.reply("🐗").setEphemeral(true).queue();
        handleCommand(user, member != null ? member.getOnlineStatus() : null, event.getGuildChannel());
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getChannel().getType().isGuild()) return;

        String arg = event.getArgs().split(" ")[0];
        User user;

        if (!arg.isEmpty() && !arg.isBlank()) {
            user = UserUtil.searchForUser(arg);

            if (user == null) {
                event.getChannel().sendMessage("I am not able to find the user you provided!").queue();
                return;
            }
        } else user = event.getAuthor();

        // Try to get their status, null if not found
        Member member = UserUtil.getMemberFromUser(event.getGuild(), user);

        handleCommand(user, member != null ? member.getOnlineStatus() : null, event.getGuildChannel());
    }

    private void handleCommand(User user, OnlineStatus status, GuildMessageChannel channel) {
        if (user.isBot()) {
            if (user.getIdLong() == PolyBot.getJDA().getSelfUser().getIdLong()) channel.sendMessage("That's me, I'm **un-rankable!** 🤓").queue(); //TODO: allow customizing messages
            else channel.sendMessage("🚫 " + user.getAsMention() + " is a **bot**! Bots aren't invited to the **super fancy `!rank` party**.").queue();

            return;
        }

        // Check if the user does not have a rank
        LevelEntry entry = BotStorage.getLevelEntry(user.getIdLong());
        if (entry == null) {
            channel.sendMessage("🚫 **" + UserUtil.getUserAsName(user) + "** isn't ranked yet.").queue();
            return;
        }

        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ATTACH_FILES)) {
            int currXp = entry.getXp() - entry.getXpForLevel(entry.getLevel());
            int xpRequired = entry.getXpForNextLevel() - entry.getXpForLevel(entry.getLevel());

            channel.sendMessage("**__" + UserUtil.getUserAsName(user) + "__**\n" +
                    //"**Rank:** " + entry.getRank() + "\n" +
                    "**Level:** " + entry.getLevel() + "\n" +
                    "**XP:** " + currXp + "/" + xpRequired + " (tot. " + entry.getXp() + ")").queue();
        } else {
            UserUtil.downloadProfilePicture(user, avatar -> {
                BufferedImage levelCard = new BufferedImage(810, 245, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = levelCard.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                FontMetrics bigFontMetrics = g2d.getFontMetrics(FONT_BIG);
                FontMetrics smallFontMetrics = g2d.getFontMetrics(FONT_SMALL);

                UserSettingEntry settingEntry = BotStorage.getUserSetting(user.getIdLong(), UserSetting.LEVEL_CARD_BACKGROUND);

                System.out.println(settingEntry.getValue());

                if (settingEntry.getValue().equalsIgnoreCase("true")) {
                    // Background image
                    AffineTransform transform = g2d.getTransform();

                    float scaleW = (float) levelCard.getWidth() / BotStorage.getCardBackground().getWidth();
                    float scaleH = (float) levelCard.getHeight() / BotStorage.getCardBackground().getHeight();
                    float scale = Math.max(scaleW, scaleH);

                    g2d.scale(scale, scale);

                    g2d.drawImage(BotStorage.getCardBackground(),
                            (scale == scaleW ? 0 : levelCard.getWidth()/2 - BotStorage.getCardBackground().getWidth()/2),
                            (scale == scaleH ? 0 : levelCard.getHeight()/2 - BotStorage.getCardBackground().getHeight()/2),
                            null);
                    g2d.setTransform(transform);
                } else {
                    g2d.setColor(Color.black);
                    g2d.fillRect(0, 0, levelCard.getWidth(), levelCard.getHeight());
                }

                // Background graphic
                g2d.setColor(new Color(0f, 0f, 0f, BotStorage.getSettingAsLong(Setting.LEVEL_CARD_TRANSPARENCY, 0) / 100f));
                g2d.fillRoundRect(LEVEL_CARD_BORDER, LEVEL_CARD_BORDER, levelCard.getWidth()- LEVEL_CARD_BORDER*2, levelCard.getHeight()-LEVEL_CARD_BORDER*2, 50, 50);

                // Profile picture
                g2d.setClip(AVATAR_CIRCLE);
                g2d.drawImage(avatar, (int) AVATAR_CIRCLE.x, (int) AVATAR_CIRCLE.y, 128, 128, null);
                g2d.setClip(null);

                // Profile picture outline
                g2d.setColor(Color.black);
                g2d.setStroke(new BasicStroke(4));
                g2d.draw(AVATAR_CIRCLE);

                // Set color based off status (offline color if unknown/actually offline)
                switch (status != null ? status : OnlineStatus.UNKNOWN) {
                    case ONLINE -> g2d.setColor(BotUtil.ONLINE);
                    case IDLE -> g2d.setColor(BotUtil.IDLE);
                    case DO_NOT_DISTURB -> g2d.setColor(BotUtil.DND);
                    default -> g2d.setColor(BotUtil.OFFLINE);
                }

                // Status indicator
                g2d.fillOval((int) AVATAR_CIRCLE.x + 96, (int) AVATAR_CIRCLE.y + 96, 32, 32);

                // Status indicator outline
                g2d.setColor(Color.black);
                g2d.drawOval((int) AVATAR_CIRCLE.x + 96, (int) AVATAR_CIRCLE.y + 96, 32, 32);

                // Username
                g2d.setColor(Color.white);
                g2d.setFont(FONT_BIG);
                g2d.drawString(UserUtil.getUserAsName(user), 128 + LEVEL_CARD_BORDER*2, 65);

                // User's rank in the leaderboard
                //g2d.setFont(FONT_SMALL);
                //g2d.drawString("Rank", 128 + LEVEL_CARD_BORDER*2, levelCard.getHeight() - LEVEL_CARD_BORDER*3);

                //g2d.setFont(FONT_BIG);
                //g2d.drawString("#" + entry.getRank(), 128 + LEVEL_CARD_BORDER*4, levelCard.getHeight() - LEVEL_CARD_BORDER*3);

                // Dividing line
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(128 + LEVEL_CARD_BORDER*2, LEVEL_CARD_BORDER*3, levelCard.getWidth()- LEVEL_CARD_BORDER*2, LEVEL_CARD_BORDER*3);

                settingEntry = BotStorage.getUserSetting(user.getIdLong(), UserSetting.LEVEL_CARD_THEME);
                Color settingColor = ColorUtil.getColorFromString(settingEntry.getValue());

                // User's current level
                g2d.setColor(settingColor);
                String lvlStr = String.valueOf(entry.getLevel());
                g2d.drawString(lvlStr, levelCard.getWidth() - bigFontMetrics.stringWidth(lvlStr) - LEVEL_CARD_BORDER*2, LEVEL_CARD_BORDER*2 + 15);

                g2d.setFont(FONT_SMALL);
                int levelXPos = levelCard.getWidth() - smallFontMetrics.stringWidth("Level ") - bigFontMetrics.stringWidth(lvlStr) - LEVEL_CARD_BORDER*2;
                g2d.drawString("Level", levelXPos, LEVEL_CARD_BORDER*2 + 15);

                // Draw their xp progress to next level
                int currXp = entry.getXp() - entry.getXpForLevel(entry.getLevel());
                int xpRequired = entry.getXpForNextLevel() - entry.getXpForLevel(entry.getLevel());

                // Neatly format their xp
                String xpString = coolFormat(currXp) + "/" + coolFormat(xpRequired) + " XP";
                g2d.setColor(new Color(182, 182, 182));
                g2d.drawString(xpString, levelCard.getWidth() - smallFontMetrics.stringWidth(xpString) - LEVEL_CARD_BORDER*2, levelCard.getHeight()- LEVEL_CARD_BORDER*3);

                // Draw progress bar background
                g2d.setColor(new Color(72, 75, 78));
                g2d.setStroke(new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(LEVEL_CARD_BORDER*2, levelCard.getHeight()- LEVEL_CARD_BORDER*2, levelCard.getWidth() - LEVEL_CARD_BORDER*2, levelCard.getHeight()- LEVEL_CARD_BORDER*2);

                // Draw progress bar if they have progress
                if (entry.getLevelProgress() != 0) {
                    g2d.setColor(settingColor);
                    g2d.drawLine(LEVEL_CARD_BORDER*2, levelCard.getHeight()- LEVEL_CARD_BORDER*2, LEVEL_CARD_BORDER*2 + (int) (entry.getLevelProgress() * (levelCard.getWidth() - LEVEL_CARD_BORDER*4)), levelCard.getHeight()- LEVEL_CARD_BORDER*2);
                }

                BotUtil.uploadImage(channel, levelCard, user.getId() + ".png", false);
            });
        }
    }

    private static final char[] FORMAT_CHARS = new char[]{'k', 'm', 'b', 't'};

    public static String coolFormat(int n) {
        return n < 1000 ? String.valueOf(n) : coolFormat(n, 0);
    }

    private static String coolFormat(double n, int iteration) {
        double d = ((long) n / 100L) / 10.0;
        boolean isRound = (d * 10) % 10 == 0;//true if the decimal part is equal to 0 (then it's trimmed anyway)
        return (d < 1000? //this determines the class, i.e. 'k', 'm' etc
                ((d > 99.9 || isRound || (!isRound && d > 9.99)? //this decides whether to trim the decimals
                        (int) d * 10 / 10 : d + "" // (int) d * 10 / 10 drops the decimal
                ) + "" + FORMAT_CHARS[iteration])
                : coolFormat(d, iteration+1));
    }
}
