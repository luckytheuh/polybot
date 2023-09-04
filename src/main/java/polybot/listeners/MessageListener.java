package polybot.listeners;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import polybot.PolyBot;
import polybot.storage.Setting;
import polybot.util.ColorUtil;
import polybot.util.GuildUtil;
import polybot.util.UserUtil;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MessageListener {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final String EDIT_DESC = "Message %s by %s edited in %s\n**Before:** %s\n**After:** %s";
    private static final String DEL_DESC = "Message %s by %s deleted from %s\n**Content:** %s";

    private final Cache<Long, Message> messageCache = Caffeine.newBuilder().expireAfterWrite(6, TimeUnit.HOURS).build();
    private boolean isEnabled;

    private Message getCachedMessage(long messageId) {
        return messageCache.getIfPresent(messageId);
    }

    private Message removeCachedMessage(long messageId) {
        Message message = getCachedMessage(messageId);
        messageCache.invalidate(messageId);
        return message;
    }

    private Message updateCachedMessage(Message newMessage) {
        Message oldMessage = getCachedMessage(newMessage.getIdLong());

        // If this message was already cached, make it invalid
        if (oldMessage != null) messageCache.invalidate(oldMessage.getIdLong());

        // Add new message
        messageCache.put(newMessage.getIdLong(), newMessage);
        return oldMessage;
    }

    public void toggle() {
        isEnabled = !isEnabled;
    }

    public boolean isToggled() {
        return isEnabled;
    }

    public void fireEvent(GenericEvent event) {
        if (event instanceof MessageReceivedEvent messageEvent) onNew(messageEvent);
        else if (event instanceof MessageUpdateEvent updateEvent) onUpdate(updateEvent);
        else if (event instanceof MessageDeleteEvent deleteEvent) onDelete(deleteEvent);
        else if (event instanceof MessageBulkDeleteEvent bulkDeleteEvent) onBulkDelete(bulkDeleteEvent);
    }

    public void onNew(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) {
            logToConsole(" sent DM: ", event.getMessage());
            return;
        }

        updateCachedMessage(event.getMessage());
    }

    public void onUpdate(@NotNull MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) {
            logToConsole(" updated DM: ", event.getMessage());
            return;
        }

        Message old = updateCachedMessage(event.getMessage());

        if (!isEnabled) return;

        logToMessageChat(event.getGuild(), getEditedEmbed(old, event.getMessage()));
    }

    public void onDelete(@NotNull MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;

        Message message = removeCachedMessage(event.getMessageIdLong());

        if (!isEnabled) return;
        if (message != null && message.getAuthor().isBot()) return;

        logToMessageChat(event.getGuild(), getDeletedEmbed(message));
    }

    public void onBulkDelete(@NotNull MessageBulkDeleteEvent event) {
        if (!isEnabled) return;

        //text file
        MessageCreateAction action = getBulkDeleteEmbed(event.getChannel(), event.getMessageIds());
        if (action != null) action.queue();
    }

    private void logToMessageChat(Guild guild, EmbedBuilder embed) {
        if (guild == null || embed == null) return;

        TextChannel logChannel = GuildUtil.getChannelFromSetting(guild, Setting.MESSAGE_LOG_CHANNEL);

        if (logChannel != null) logChannel.sendMessageEmbeds(embed.build()).queue();
    }

    private void logToConsole(String key, Message message) {
        PolyBot.getLogger().info(UserUtil.getUserAsName(message.getAuthor()) + key + message.getContentRaw());

        if (key.contains("updated")) return;
        for (Message.Attachment attachment : message.getAttachments()) {
            PolyBot.getLogger().info(attachment.getUrl());
        }
    }

    private MessageCreateAction getBulkDeleteEmbed(GuildMessageChannel channel, List<String> ids) {
        Message[] messages = new Message[ids.size()];
        boolean allNull = true;

        for (int i = 0; i < ids.size(); i++) {
            messages[i] = removeCachedMessage(Long.parseLong(ids.get(i)));
            if (messages[i] != null) allNull = false;
        }

        StringBuilder builder = new StringBuilder();

        if (!allNull) {
            for (Message message : messages) {
                if (message == null) continue;

                builder.append('[').append(TIME_FORMATTER.format(message.getTimeCreated())).append("] (").append(UserUtil.getUserAsName(message.getAuthor()))
                        .append(" - ").append(message.getAuthor().getId()).append(") [").append(message.getId()).append("]: ")
                        .append(message.getContentRaw());

                for (Message.Attachment attachment : message.getAttachments()) {
                    builder.append(" | ").append(attachment.getUrl());
                }

                builder.append("\n\n");
            }
        }

        TextChannel logChannel = GuildUtil.getChannelFromSetting(channel.getGuild(), Setting.MESSAGE_LOG_CHANNEL);

        if (logChannel != null) {
            MessageCreateAction action = logChannel.sendMessageEmbeds(new EmbedBuilder()
                            .setAuthor("PolyBot Message Logging", null, PolyBot.getJDA().getSelfUser().getAvatarUrl())
                            .setDescription(messages.length + " message(s) deleted from " + channel.getAsMention())
                            .setColor(ColorUtil.IDLE)
                            .setTimestamp(Instant.now())
                            .setFooter((allNull ? "None of the messages could be found" : null))
                            .build());
            if (!allNull) action.addFiles(FileUpload.fromData(builder.toString().getBytes(StandardCharsets.UTF_8), "bulk_delete_" + System.currentTimeMillis() + ".txt"));
            return action;
        } else return null;
    }

    public static EmbedBuilder getEditedEmbed(Message oldMessage, Message newMessage) {
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(UserUtil.getUserAsName(newMessage.getAuthor()) + " (" + newMessage.getAuthor().getIdLong() + ")", null, newMessage.getAuthor().getAvatarUrl())
                .setDescription(String.format(EDIT_DESC, newMessage.getId(), newMessage.getAuthor().getAsMention(), newMessage.getChannel(), oldMessage == null ? "*Unknown*" : oldMessage.getContentRaw(), newMessage.getContentRaw()))
                .setTimestamp(Instant.now())
                .setColor(new Color(241, 196, 15));

        return builder;
    }

    public static EmbedBuilder getDeletedEmbed(Message message) {
        if (message == null) return null;

        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(UserUtil.getUserAsName(message.getAuthor()) + " (" + message.getAuthor().getIdLong() + ")", null, message.getAuthor().getAvatarUrl())
                .setDescription(String.format(DEL_DESC, message.getId(), message.getAuthor().getAsMention(), message.getChannel(), message.getContentRaw()))
                .setTimestamp(Instant.now())
                .setColor(Color.red);

        if (!message.getAttachments().isEmpty()) {
            for (int i = 0; i < message.getAttachments().size(); i++) {
                if (i == 0) {
                    builder.setImage(message.getAttachments().get(i).getUrl());
                }

                builder.addField("Attachment " + (i+1), "[View](" + message.getAttachments().get(i).getUrl() + ")", true);
            }
        }

        if (!message.getStickers().isEmpty()) {
            for (int i = 0; i < message.getStickers().size(); i++) {
                StickerItem sticker = message.getStickers().get(i);

                builder.addField("Sticker " + (i+1), sticker.getName(), true);
            }
        }

        return builder;
    }
}
