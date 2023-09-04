package polybot.cmds;

import polybot.commands.CommandEvent;
import polybot.commands.CooldownType;
import polybot.commands.SlashCommand;
import polybot.commands.SlashCommandEvent;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class PingCommand extends SlashCommand {

    public PingCommand() {
        super("ping", "pong");

        this.cooldownLength = 15;
        this.aliases = new String[]{"pong"};
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.GLOBAL;
        this.detailedHelp = """
            Show how laggy the bot is in ms
            Slash commands have no accurate way of displaying ping, so it will always default to 0ms.""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.replyShown("Pong!", hook -> {
            long ms = event.getInteraction().getTimeCreated().until(hook.getInteraction().getTimeCreated(), ChronoUnit.MILLIS);
            hook.editOriginal("Pong! `" + ms + "ms` | Websocket: `" + event.getJDA().getGatewayPing() + "ms`").queue();
        });
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        event.reply("Pong!", message -> {
            long ms = event.getMessage().getTimeCreated().until(message.getTimeCreated(), ChronoUnit.MILLIS);
            message.editMessage("Pong! `" + ms + "ms` | Websocket: `" + event.getJDA().getGatewayPing() + "ms`").queue();
        });
    }
}
