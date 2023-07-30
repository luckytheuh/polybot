package polybot.listeners;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.BotUtil;
import polybot.util.GuildUtil;
import polybot.util.UserUtil;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AutoReportListener extends ListenerAdapter {

    private static final Cache<Long, Message> REPORTS = Caffeine.newBuilder().expireAfterWrite((int) (60*3.5), TimeUnit.MINUTES).build(); // 3:30 hours
    private static final String DESCRIPTION = "Message %s in %s\n**Content:** %s";
    private static final UnicodeEmoji WARNING = Emoji.fromUnicode("⚠️");
    //private static final int WARNING_CAP = 5;


    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getEmoji().getType() != Emoji.Type.UNICODE || !event.getEmoji().getName().equals(WARNING.getName())) return;

        event.retrieveMessage().queue(message -> {
            Message botMsgReport = REPORTS.getIfPresent(message.getIdLong());
            final int totalWarn = message.getReaction(WARNING).getCount();
            if (totalWarn < BotStorage.getSetting(Setting.AUTO_REPORT_COUNT, 0)) return;

            TextChannel channel = GuildUtil.getChannelFromSetting(event.getGuild(), Setting.BOT_REPORT_CHANNEL);
            if (channel != null) {
                if (botMsgReport != null) {
                    // If not null, we've already reported, and we should update the reaction total

                    channel.retrieveMessageById(botMsgReport.getId()).queue(bMsg -> bMsg.editMessage(totalWarn + " :warning: reactions").queue());
                } else {
                    // New report

                    EmbedBuilder builder = new EmbedBuilder()
                            .setAuthor(UserUtil.getUserAsName(message.getAuthor()) + " (" + message.getAuthor().getIdLong() + ")", null, message.getAuthor().getAvatarUrl())
                            .setDescription(String.format(DESCRIPTION, message.getId(), message.getChannel().getAsMention(), message.getContentRaw()))
                            .setTimestamp(Instant.now())
                            .setColor(BotUtil.IDLE);

                    for (int i = 0; i < message.getAttachments().size(); i++) {
                        builder.addField("Attachment " + (i+1), "[View](" + message.getAttachments().get(i).getUrl() + ")", true);
                    }

                    channel.sendMessage(totalWarn + " :warning: reactions").addEmbeds(builder.build()).setActionRow(Button.danger(message.getId(), "Validate Report")).queue(botMessage -> {
                        REPORTS.put(message.getIdLong(), botMessage);

                        // If the message wasn't checked in the 3-hour period, it will be marked as expired
                        botMessage.editMessage("This report has expired.")
                                .setComponents(Collections.emptyList())
                                .setCheck(() -> REPORTS.getIfPresent(message.getIdLong()) != null)
                                .queueAfter(3, TimeUnit.HOURS);
                    });
                }
            }
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild()) return;
        if (event.isAcknowledged()) return;

        // Get all reports, and search for the one this button wants
        for (Map.Entry<Long, Message> message : REPORTS.asMap().entrySet()) {
            if (String.valueOf(message.getKey()).equals(event.getComponentId())) {
                // Disable the button
                event.editComponents(Collections.emptyList()).setContent(event.getMessage().getContentRaw() + ", marked valid by " + UserUtil.getUserAsName(event.getUser())).queue();

                // Resend into PR channel
                TextChannel channel = GuildUtil.getChannelFromSetting(Objects.requireNonNull(event.getGuild()), Setting.PARK_RANGER_CHANNEL);
                if (channel != null) {
                    channel.sendMessage(event.getUser().getAsMention() + " marked this as a valid report").addEmbeds(message.getValue().getEmbeds()).queue();
                    REPORTS.invalidate(message.getKey());
                }
                return;
            }
        }

        // Once we reach here, it means it was an older button or it already expired
        event.reply(":warning: Unknown or expired report.").setEphemeral(true).queue();
    }
}
