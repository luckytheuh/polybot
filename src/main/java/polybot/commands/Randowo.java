package polybot.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CooldownScope;
import polybot.RoleLimitedCommand;

import java.util.concurrent.ThreadLocalRandom;

public class Randowo extends RoleLimitedCommand {

    public Randowo() {
        super(804551691847729172L);
        this.name = "randowo";
        this.cooldown = 5;
        this.cooldownScope = CooldownScope.USER;
    }

    @Override
    protected void executeCommand(CommandEvent event) {
        event.reply(String.valueOf(ThreadLocalRandom.current().nextInt(0, 501)));
    }
}
