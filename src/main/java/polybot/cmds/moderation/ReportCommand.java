package polybot.cmds.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import polybot.commands.*;
import polybot.listeners.AutoReportListener;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.ColorUtil;
import polybot.util.GuildUtil;
import polybot.util.UserUtil;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ReportCommand extends ContextMenu {

    public ReportCommand() {
        super("report", "Report To Staff", "Report a message to the staff by replying or providing the message.");

        this.cooldownLength = 30;
        this.argsRequired = true;
        this.ignoreSendCheck = true;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.USER;
        this.deleteOption = DeleteOption.DELETE_BOT;
        this.arguments = "((message link/id)/reason) [reason if using msg link]";
        this.detailedHelp = """
            A message can be reported by using the message link, "Copy Message ID" button, or by replying to the message in question.
            A reason for reporting is required to use this command.
            
            **Examples**
            `&report https://discord.com/channels/804491292405923841/804492371575701516/1140828829011677246 it is her favorite activity.`
            `&report 804491292897574944-1140830612870156358 this will be tf2 in 2020??`""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (event.getMessage().getMessageReference() != null) {
            event.getMessage().getMessageReference().resolve().queue(message -> {
                if (handle(message, event.getAuthor(), event.stringArgs()))
                    event.getMessage().delete().queue();
            }, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        } else {
            String[] ids = getChannelMessageIdFromArg(event.args()[0]);
            if (ids == null) return;

            event.offsetArgs(1);
            event.getGuild().getTextChannelById(ids[0]).retrieveMessageById(ids[1]).queue(message -> {
                if (handle(message, event.getAuthor(), event.args().length == 1 ? null : event.stringArgs()))
                    event.getMessage().delete().queue();
            }, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        }
    }

    @Override
    protected void fireContextMenu(ContextMenuEvent event) {
        if (handle(event.getTarget(), event.getUser(), null)) event.replyHidden("Reported!");
        else event.replyHidden("Unable to report!");
    }

    private String[] getChannelMessageIdFromArg(String arg) {
        if (arg.length() < 5) return null;

        if (arg.contains("/")) {
            String[] args = arg.split("/");

            return Arrays.copyOfRange(args, args.length-2, args.length);
        } else if (arg.contains("-")) return arg.split("-");
        else return null;
    }

    private boolean handle(Message message, User user, String reason) {
        if (BotStorage.getSettingAsList(Setting.REPORT_BLACKLIST).contains(user.getId())) return false;

        if (reason == null) reason = "No reason given.";

        TextChannel channel = GuildUtil.getChannelFromSetting(message.getGuild(), Setting.BOT_REPORT_CHANNEL);
        if (channel != null) {
            channel.sendMessageEmbeds(new EmbedBuilder()
                            .setAuthor(UserUtil.getUserAsName(message.getAuthor()) + " (" + message.getAuthor().getIdLong() + ")", null, message.getAuthor().getAvatarUrl())
                            .setDescription(String.format(AutoReportListener.DESCRIPTION, message.getId(), message.getJumpUrl(), message.getContentRaw()))
                            .setTimestamp(Instant.now())
                            .setColor(ColorUtil.OFFLINE)
                            .build())
                    .setContent("report from " + user.getAsMention() + " with reason: `" + reason + '`')
                    .setActionRow(
                            Button.success("y", "Mark valid"),
                            Button.danger("n", "Mark invalid"),
                            Button.danger(message.getChannel().getId() + '-' + message.getId(), "Delete original")
                    ).queue();
        } else return false;

        return true;
    }
}
