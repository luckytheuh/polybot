package polybot.cmds.fun;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.*;
import polybot.util.BotUtil;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class BurgerCommand extends SlashCommand {
    private static final String BURGER = "heres your burger :hamburger:";

    public BurgerCommand() {
        super("burger", Category.FUN, "mmm.. burger");

        this.cooldownLength = 6;
        this.cooldownUnit = TimeUnit.HOURS;
        this.cooldownType = CooldownType.USER;
        this.aliases = new String[]{"cheeseburger", "hamburger"};
        this.options = Collections.singletonList(new OptionData(OptionType.STRING, "toppings", "additional toppings for burgr"));
        this.detailedHelp = """
            Order a burger with custom toppings.
                    
            **Examples**
            `&burger with cheese`
            `&burger with no hopes and dreams`""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (BotUtil.hasIllegalKeywords(event.getMessage())) {
            event.reply(BURGER);
            return;
        }

        event.reply(getBurger(event.stringArgs()));
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        if (BotUtil.hasIllegalKeywords(event.optString("toppings", ""))) {
            event.replyShown(BURGER);
            return;
        }

        event.replyShown(getBurger(event.optString("toppings", "")));
    }

    private String getBurger(String additional) {
        if (additional.isEmpty() || additional.isBlank()) return BURGER;

        if (additional.startsWith("with ")) additional = additional.replaceFirst("with ", "");
        return BURGER + " with " + additional;
    }
}
