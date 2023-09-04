package polybot.cmds.fun;

import polybot.commands.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Randowo extends SlashCommand {

    public Randowo() {
        super("random", Category.FUN, "Generate a random number from 0-500");

        this.cooldownLength = 15;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.aliases = new String[]{"randowo"};
        this.cooldownType = CooldownType.CHANNEL;
        this.deleteOption = DeleteOption.DELETE_BOTH;
        this.help = "Generate a random number from 0-500";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.replyShown(String.valueOf(ThreadLocalRandom.current().nextInt(0, 501)));
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        event.reply(String.valueOf(ThreadLocalRandom.current().nextInt(0, 501)));
    }
}
