package polybot.cmds.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import polybot.commands.*;
import polybot.listeners.MessageListener;

@ListenerCommandCombo
public class MessageLoggerCommand extends SlashCommand implements EventListener {

    private final MessageListener listener;

    public MessageLoggerCommand() {
        super("toggle", Category.MODERATOR, "Toggle the message logger");

        this.isHidden = true;
        this.listener = new MessageListener();
        this.requiredPermission = Permission.KICK_MEMBERS;
        this.detailedHelp = "When enabled, messages will be logged into the channel id provided in the `message-log-channel` setting.";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        listener.toggle();
        event.replyShown("Message logger enabled: `" + listener.isToggled() + '`');
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        listener.toggle();
        event.reply("Message logger enabled: `" + listener.isToggled() + '`');
    }

    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        listener.fireEvent(genericEvent);
    }
}
