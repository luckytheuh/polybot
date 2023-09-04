package polybot.cmds.fun;

import polybot.commands.*;
import polybot.util.BotUtil;

import java.util.concurrent.TimeUnit;

public class TacoCommand extends SlashCommand {

    private static final String TACO = "here is your taco :taco:";

    public TacoCommand() {
        super("taco", Category.FUN, "order a taco");

        this.cooldownLength = 1;
        this.cooldownUnit = TimeUnit.DAYS;
        this.cooldownType = CooldownType.USER;
        this.detailedHelp = """
            Order a taco with custom ingredients on it.
                    
            **Examples**
            `&taco with cheese`
            `&taco broken glass`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.reply(TACO).queue();
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (BotUtil.hasIllegalKeywords(event.getMessage())) {
            event.reply(TACO);
            return;
        }

        String args = event.stringArgs();
        if (args.startsWith("with ")) args = args.substring(5);

        event.reply(TACO + (event.args() == null ? "" : " with " + args));
    }
}
