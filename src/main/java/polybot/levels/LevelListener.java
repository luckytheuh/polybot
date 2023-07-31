package polybot.levels;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;
import polybot.PolyBot;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.BotUtil;
import polybot.util.GuildUtil;
import polybot.util.UserUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LevelListener extends ListenerAdapter {
    private static final Ellipse2D.Float WELCOME_AVATAR_CIRCLE = new Ellipse2D.Float(422, BotUtil.CARD_BORDER*2, 256, 256);
    private static final Font ROBOTO = new Font("Roboto", Font.PLAIN, 40);

    //user id, timestamp when they can get xp again
    private final Cache<Long, Long> messageCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
/*
    List<LevelEntry> entries = new ArrayList<>();

    public void sortEntries() {
        entries.sort(Comparator.comparingInt(LevelEntry::getXp));
    }
*/
//TODO: to get the rank of a user, we can store their rank position in the db,
// and whenever they go up a rank, adjust all values accordingly (probably inefficient)
// or store a cache of every entry into memory, then sort the list on write
    // maybe write to db after each change?

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // if this is dm or user is a bot, don't do anythin
        if (!event.isFromGuild() || event.getAuthor().isBot()) return;
        if (event.getGuild().getIdLong() != 804491292405923841L) return;

        if (BotStorage.getSetting(Setting.NO_XP_CHANNELS).contains(event.getChannel().getId())) return;

        // Check if xp muted
        if (GuildUtil.memberHasRole(event.getMember(), BotUtil.getAsLong(BotStorage.getSetting(Setting.XP_MUTE_ROLE)))) return;

        if (event.getMessage().getContentRaw().startsWith("!!")) return;

        // check if it's not time to let you gain xp again
        Long time = messageCache.getIfPresent(event.getAuthor().getIdLong()) ;
        if (time != null && time >= Instant.now().toEpochMilli()) return;

        // take current time, add 60 seconds in miliseconds to it and store it to the map
        messageCache.put(event.getAuthor().getIdLong(), Instant.now().toEpochMilli() + (1000 * 60));

        int gainedXp = 15 + ThreadLocalRandom.current().nextInt(11);

        LevelEntry entry = BotStorage.getLevelEntry(event.getAuthor().getIdLong());
        if (entry == null) entry = new LevelEntry(0L, 0, 0, BotStorage.getTotalRankedUsers(), 0, false);

        entry.setXp(entry.getXp() + gainedXp);
        entry.setMessages(entry.getMessages() + 1);
        if (entry.calculateLevel()) {
            PolyBot.getLogger().info(String.format(BotStorage.getSetting(Setting.LEVEL_UP_MESSAGE), event.getAuthor().getAsMention(), Integer.toUnsignedString(entry.getLevel())));

            if (entry.getLevel() == 30) {
                event.getGuild().getTextChannelById(834437126203768852L).sendMessage("Welcome " + event.getAuthor().getAsMention() + " to Level 30 General.").queue();
            }

            /*
            TextChannel channel = GuildUtil.getChannelFromSetting(event.getGuild(), Setting.LEVEL_UP_CHANNEL);
            if (channel != null) channel.sendMessage(String.format(BotStorage.getSetting(Setting.LVL_UP_MESSAGE), event.getAuthor().getAsMention(), Integer.toUnsignedString(entry.getLevel()))).queue();

             */
        }

        BotStorage.saveLevelEntry(event.getAuthor().getIdLong(), entry);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        //CompletableFuture.runAsync(() -> handleLevelRewards(event.getMember()));

        if (true) return;

        CompletableFuture.runAsync(() -> {
            TextChannel channel = GuildUtil.getChannelFromSetting(event.getGuild(), Setting.WELCOME_LEAVE_CHANNEL);
            if (channel != null) {
                UserUtil.downloadProfilePicture(event.getUser(), image -> {
                    MessageCreateAction action = BotUtil.uploadImage(channel, getWelcomeImage(image, event.getMember()), event.getUser().getId() + ".png", true);

                    if (action != null) {
                        // Add the join message from settings if exists
                        String s = BotStorage.getSetting(Setting.WELCOME_MESSAGE);
                        if (!s.isEmpty() && !s.isBlank()) action.addContent(String.format(s, event.getUser().getAsMention()));

                        action.queue();
                    }
                });
            }

            System.out.println(UserUtil.getUserAsName(event.getMember().getUser()) + " has joined");
        });
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        if (true) return;

        TextChannel channel = event.getGuild().getTextChannelById(BotStorage.getSettingAsLong(Setting.WELCOME_LEAVE_CHANNEL, 0));

        if (channel != null) {
            String leaveMsg = BotStorage.getSetting(Setting.LEAVE_MESSAGE);

            if (!leaveMsg.isBlank() && !leaveMsg.isEmpty()) channel.sendMessage(String.format(leaveMsg, UserUtil.getUserAsName(event.getUser()))).queue();
        }
    }

    public void handleLevelRewards(Member member) {
        LevelEntry levelEntry = BotStorage.getLevelEntry(member.getIdLong());
        if (levelEntry == null) return;

        RestAction<Void> action = null;

        // Give them all their roles
        for (Map.Entry<Integer, Long> entry: BotUtil.mapToIntLongMap(BotStorage.getSetting(Setting.ROLE_REWARDS)).entrySet()) {
            if (entry.getKey() > levelEntry.getLevel()) continue;

            AuditableRestAction<Void> a = BotUtil.addRoleToMember(member, entry.getValue());
            if (a != null) {
                action = action == null ? a : action.and(a);
            }
        }

        if (action != null) action.queue();
    }

    public static BufferedImage getWelcomeImage(BufferedImage avatar, Member member) {
        BufferedImage welcomeCard = new BufferedImage(1100, 500, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = welcomeCard.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (BotStorage.getCardBackground() != null) {
            AffineTransform transform = g2d.getTransform();

            float scaleW = (float) welcomeCard.getWidth() / BotStorage.getCardBackground().getWidth();
            float scaleH = (float) welcomeCard.getHeight() / BotStorage.getCardBackground().getHeight();
            float scale = Math.max(scaleW, scaleH);

            g2d.scale(scale, scale);
            g2d.drawImage(BotStorage.getCardBackground(),
                     (scale == scaleW ? 0 : welcomeCard.getWidth()/2 - BotStorage.getCardBackground().getWidth()/2),
                     (scale == scaleH ? 0 : welcomeCard.getHeight()/2 - BotStorage.getCardBackground().getHeight()/2),
                    null);
            g2d.setTransform(transform);

            //g2d.drawImage(BotStorage.getCardBackground(), 0, 0, null);
        }


        // rect
        g2d.setColor(new Color(0f, 0f, 0f, BotStorage.getSettingAsLong(Setting.LEVEL_CARD_TRANSPARENCY, 0) / 100f));
        g2d.fillRoundRect(BotUtil.CARD_BORDER, BotUtil.CARD_BORDER, welcomeCard.getWidth() - BotUtil.CARD_BORDER*2, welcomeCard.getHeight() - BotUtil.CARD_BORDER*2, 25, 25);

        g2d.setFont(ROBOTO);
        g2d.setColor(Color.white);
        BotUtil.drawTextCentered(g2d, UserUtil.getMemberAsName(member) + " just joined the server", welcomeCard.getWidth(), welcomeCard.getHeight() - BotUtil.CARD_BORDER*5);

        g2d.setFont(ROBOTO.deriveFont(32f));
        g2d.setColor(Color.lightGray);
        BotUtil.drawTextCentered(g2d, "Member #" + member.getGuild().getMemberCount(), welcomeCard.getWidth(), welcomeCard.getHeight() - BotUtil.CARD_BORDER*3);

        g2d.setClip(WELCOME_AVATAR_CIRCLE);
        g2d.drawImage(avatar, (int) WELCOME_AVATAR_CIRCLE.x, (int) WELCOME_AVATAR_CIRCLE.y, (int) WELCOME_AVATAR_CIRCLE.getWidth(), (int) WELCOME_AVATAR_CIRCLE.getHeight(), null);
        g2d.setClip(null);

        // Profile picture outline
        g2d.setColor(Color.white);
        g2d.setStroke(new BasicStroke(6));
        g2d.draw(WELCOME_AVATAR_CIRCLE);

        g2d.dispose();
        return welcomeCard;
    }

}
