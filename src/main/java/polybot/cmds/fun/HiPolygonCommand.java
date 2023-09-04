package polybot.cmds.fun;

import polybot.commands.Category;
import polybot.commands.Command;
import polybot.commands.CommandEvent;
import polybot.commands.CooldownType;

import java.util.concurrent.TimeUnit;

public class HiPolygonCommand extends Command {

    public HiPolygonCommand() {
        super("hipolugondonute", Category.FUN, "hi polugon donute");

        this.cooldownLength = 1;
        this.cooldownUnit = TimeUnit.HOURS;
        this.cooldownType = CooldownType.GLOBAL;
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        event.reply("hi polugo n donute :) you now own the polugo n donute role!!! (disclaimer: role will not be given)");
        //TODO: give the role
    }
}
