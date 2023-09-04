package polybot.listeners;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;
import polybot.Constants;
import polybot.PolyBot;
import polybot.LevelEntry;
import polybot.storage.BotStorage;
import polybot.storage.Fonts;
import polybot.storage.Setting;
import polybot.util.BotUtil;
import polybot.util.GuildUtil;
import polybot.util.LevelUtil;
import polybot.util.UserUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LevelListener extends ListenerAdapter {
    private static final Ellipse2D.Float WELCOME_AVATAR_CIRCLE = new Ellipse2D.Float(422, Constants.CARD_BORDER*2, 256, 256);
    private final Cache<Long, Long> messageCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // if this is dm or user is a bot, don't do anythin
        if (!event.isFromGuild() || (event.getAuthor().isBot() && !PolyBot.isSelfUser(event.getAuthor()))) return;

        if (BotStorage.getSetting(Setting.NO_XP_CHANNELS).contains(event.getChannel().getId())) return;

        // Check if xp muted
        if (GuildUtil.memberHasRole(event.getMember(), BotUtil.getAsLong(BotStorage.getSetting(Setting.XP_MUTE_ROLE)))) return;

        if (event.getMessage().getContentRaw().startsWith("&") || event.getMessage().getContentRaw().startsWith("!!")) return;

        if (!PolyBot.isSelfUser(event.getAuthor())) {
            // check if it's not time to let you gain xp again
            Long time = messageCache.getIfPresent(event.getAuthor().getIdLong()) ;
            if (time != null && time >= Instant.now().toEpochMilli()) return;

            // take current time, add 60 seconds in miliseconds to it and store it to the map
            messageCache.put(event.getAuthor().getIdLong(), Instant.now().toEpochMilli() + (1000 * 60));
        }

        int gainedXp = 15 + ThreadLocalRandom.current().nextInt(11);

        LevelEntry entry = BotStorage.getLevelEntry(event.getAuthor().getIdLong());
        if (entry == null) entry = new LevelEntry(0L, 0, 0, BotStorage.getTotalRankedUsers(), 0, false);

        entry.setXp(entry.getXp() + gainedXp);
        entry.setMessages(entry.getMessages() + 1);
        if (entry.calculateLevel()) {
            PolyBot.getLogger().info(String.format(BotStorage.getSetting(Setting.LEVEL_UP_MESSAGE), event.getAuthor().getAsMention(), Integer.toUnsignedString(entry.getLevel())));

            if (entry.getLevel() == 30) {
                TextChannel lvl30Channel = event.getGuild().getTextChannelById(834437126203768852L);
                if (lvl30Channel != null) lvl30Channel.sendMessage("Welcome " + event.getAuthor().getAsMention() + " to level 30 general.").queue();
            }

            if (PolyBot.isSelfUser(event.getAuthor())) LevelUtil.distributeRewards(event.getMember(), entry);

/*
            handleLevelRewards(getMessageEvent.getMember(), entry);

            TextChannel channel = GuildUtil.getChannelFromSetting(getMessageEvent.getGuild(), Setting.LEVEL_UP_CHANNEL);
            if (channel != null) channel.sendMessage(String.format(BotStorage.getSetting(Setting.LEVEL_UP_MESSAGE), getMessageEvent.getAuthor().getAsMention(), Integer.toUnsignedString(entry.getLevel()))).queue();

*/
        }

        BotStorage.saveLevelEntry(event.getAuthor().getIdLong(), entry);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        //CompletableFuture.runAsync(() -> handleLevelRewards(getMessageEvent.getMember(), BotStorage.getLevelEntry(getMessageEvent.getUser().getIdLong())));

        CompletableFuture.runAsync(() -> {
            TextChannel channel = GuildUtil.getChannelFromSetting(event.getGuild(), Setting.WELCOME_LEAVE_CHANNEL);
            if (channel != null) {
                MessageCreateAction action = BotUtil.uploadImage(channel, getWelcomeImage(event.getMember()), event.getUser().getId() + ".png", true);

                if (action != null) {
                    // Add the join message from settings if exists
                    String s = BotStorage.getSetting(Setting.WELCOME_MESSAGE);
                    if (!s.isEmpty() && !s.isBlank()) action.addContent(String.format(s, event.getUser().getAsMention()));

                    action.queue();
                }

                LevelUtil.distributeRewards(event.getMember(), BotStorage.getLevelEntry(event.getMember().getIdLong()));
            }

            PolyBot.getLogger().info(UserUtil.getUserAsName(event.getMember().getUser()) + " has joined");
        });
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        //if (true) return;

        TextChannel channel = event.getGuild().getTextChannelById(BotStorage.getSettingAsLong(Setting.WELCOME_LEAVE_CHANNEL, 0));

        if (channel != null) {
            String leaveMsg = BotStorage.getSetting(Setting.LEAVE_MESSAGE);

            if (!leaveMsg.isBlank() && !leaveMsg.isEmpty()) channel.sendMessage(String.format(leaveMsg, UserUtil.getUserAsName(event.getUser()))).queue();
        }

        PolyBot.getLogger().info(UserUtil.getUserAsName(event.getUser()) + " has left");
    }

    public static BufferedImage getWelcomeImage(Member member) {
        BufferedImage welcomeCard = new BufferedImage(1100, 500, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = welcomeCard.createGraphics();
        LevelUtil.handleBackgroundImage(welcomeCard, g2d);

        // rect
        LevelUtil.adjustTransparency(g2d, BotStorage.getSettingAsLong(Setting.WELCOME_CARD_TRANSPARENCY, 0));
        g2d.fillRoundRect(Constants.CARD_BORDER, Constants.CARD_BORDER, welcomeCard.getWidth() - Constants.CARD_BORDER*2, welcomeCard.getHeight() - Constants.CARD_BORDER*2, 25, 25);

        g2d.setFont(Constants.ROBOTO_MEDIUM);
        g2d.setColor(Color.white);
        BotUtil.drawTextCentered(g2d, UserUtil.getMemberAsName(member) + " just joined the server", welcomeCard.getWidth(), welcomeCard.getHeight() - Constants.CARD_BORDER*5);

        g2d.setFont(Fonts.ROBOTO.getFont().deriveFont(32f));
        g2d.setColor(Color.lightGray);
        BotUtil.drawTextCentered(g2d, "Member #" + member.getGuild().getMemberCount(), welcomeCard.getWidth(), welcomeCard.getHeight() - Constants.CARD_BORDER*3);

        g2d.setClip(WELCOME_AVATAR_CIRCLE);
        g2d.drawImage(LevelUtil.getCachedAvatar(member.getUser()), (int) WELCOME_AVATAR_CIRCLE.x, (int) WELCOME_AVATAR_CIRCLE.y, (int) WELCOME_AVATAR_CIRCLE.getWidth(), (int) WELCOME_AVATAR_CIRCLE.getHeight(), null);
        g2d.setClip(null);

        // Profile picture outline
        g2d.setColor(Color.white);
        g2d.setStroke(new BasicStroke(6));
        g2d.draw(WELCOME_AVATAR_CIRCLE);

        g2d.dispose();
        return welcomeCard;
    }

}
