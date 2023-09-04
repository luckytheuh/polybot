package polybot.listeners;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import polybot.Constants;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.ColorUtil;
import polybot.util.GuildUtil;
import polybot.util.UserUtil;
import polybot.util.WordUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoReportListener extends ListenerAdapter {

    public static final String DESCRIPTION = "Message %s in %s\n**Content:** %s";
    public static final String RESPONSE = "Marked %s by %s with %s";

    private final Cache<Long, Object> DO_NOT_REPORT = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (DO_NOT_REPORT.getIfPresent(event.getMessageIdLong()) != null) return; // Check early on to try and prevent multiple reports

        if (!event.isFromGuild()) return; // No guild no report
        if (TimeUtil.getTimeCreated(event.getMessageIdLong()).toLocalDateTime().isBefore(LocalDateTime.now().minusDays(1))) return; // If message older than 1 day
        if (event.getEmoji().getType() != Emoji.Type.UNICODE || !event.getEmoji().getName().equals(Constants.WARNING.getName())) return; // If not warning emoji

        final long max = BotStorage.getSettingAsLong(Setting.AUTO_REPORT_COUNT, 0);
        if (event.getReaction().hasCount() && event.getReaction().getCount() < max) return;

        event.retrieveMessage().queue(message -> {
            MessageReaction reaction = message.getReaction(Constants.WARNING);
            final int totalWarn = reaction != null ? reaction.getCount() : -1;
            if (totalWarn < max) return;
            DO_NOT_REPORT.put(message.getIdLong(), new Object());

            TextChannel channel = GuildUtil.getChannelFromSetting(event.getGuild(), Setting.BOT_REPORT_CHANNEL);
            if (channel != null) {
                EmbedBuilder builder = getReportEmbed(message);

                channel.sendMessage(totalWarn + " :warning: reactions").addEmbeds(builder.build()).addActionRow(
                        Button.success("y", "Mark valid"),
                        Button.danger("n", "Mark invalid"),
                        Button.danger(message.getChannel().getId() + '-' + message.getId(), "Delete original")
                ).queue();
            }
        }, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild()) return;
        if (event.isAcknowledged()) return;

        String[] args;
        if (event.getComponentId().startsWith("y") || event.getComponentId().startsWith("n")) {
            event.editMessage(String.format(RESPONSE, (event.getComponentId().startsWith("n") ? "invalid" : "valid"), UserUtil.getUserAsName(event.getUser()), event.getMessage().getContentRaw()))
                    .setComponents(Collections.emptyList()).queue();
            return;
        } else if ((args = event.getComponentId().split("-")).length > 1) {
            TextChannel channel = event.getGuild().getTextChannelById(args[0]);
            if (channel != null) {
                if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
                    // We can't delete it, so fail and tell them we can't.
                    event.reply("I do not have permission to delete messages in " + channel.getAsMention() + "!").setEphemeral(true).queue();
                    return;
                }

                event.editMessage(String.format(RESPONSE, "as deleted", UserUtil.getUserAsName(event.getUser()), event.getMessage().getContentRaw())).setComponents(Collections.emptyList()).queue();
                channel.retrieveMessageById(args[1]).queue(message -> {
                    message.delete().reason("Delete requested by " + UserUtil.getUserAsName(event.getUser())).queue();
                }, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE).handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                    event.reply("I do not have permission to delete messages in " + channel.getAsMention() + "!").setEphemeral(true).queue();
                }));
                return;
            }
        }

        // Once we reach here, it's probably a legacy report.
        event.reply(":question: Unknown interaction.").setEphemeral(true).queue();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        checkMessageForReport(event.getMessage());
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        checkMessageForReport(event.getMessage());
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        DO_NOT_REPORT.invalidate(event.getMessageIdLong());
    }

    private void checkMessageForReport(Message message) {
        if (!message.isFromGuild()) return;
        if (message.getAuthor().isBot()) return;
        if (DO_NOT_REPORT.getIfPresent(message.getIdLong()) != null) return;
        if (UserUtil.getMemberFromUser(message.getGuild(), message.getAuthor()).hasPermission(Permission.MESSAGE_MANAGE)) return;

        List<String> blackListed = BotStorage.getSettingAsList(Setting.BLACKLISTED_KEYWORDS);
        if (blackListed.isEmpty()) return;

        String keyword = WordUtil.checkAgainstFilter(message.getContentRaw());

        if (keyword != null) {
            TextChannel channel = GuildUtil.getChannelFromSetting(message.getGuild(), Setting.BOT_REPORT_CHANNEL);
            if (channel != null) {
                EmbedBuilder builder = getReportEmbed(message);

                channel.sendMessageEmbeds(builder.build()).addActionRow(
                        Button.success("y", "Mark valid"),
                        Button.danger("n", "Mark invalid"),
                        Button.danger(message.getChannel().getId() + '-' + message.getId(), "Delete original")
                ).addContent("blacklisted keyword `" + keyword + "`").queue();
            }
        }
        /*
        for (String word : blackListed) {
            if (!message.getContentRaw().toLowerCase().contains(word) && !message.getContentRaw().toUpperCase().contains(word)) continue;
            DO_NOT_REPORT.put(message.getIdLong(), new Object());


            return;
        }*/
    }


    private EmbedBuilder getReportEmbed(Message message) {
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(UserUtil.getUserAsName(message.getAuthor()) + " (" + message.getAuthor().getIdLong() + ")", null, message.getAuthor().getAvatarUrl())
                .setDescription(String.format(DESCRIPTION, message.getId(), message.getJumpUrl(), message.getContentRaw()))
                .setFooter(message.isEdited() ? "This message was edited" : null)
                .setTimestamp(Instant.now())
                .setColor(ColorUtil.REPORT_YELLOW);

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
