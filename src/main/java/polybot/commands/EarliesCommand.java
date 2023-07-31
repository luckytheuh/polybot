package polybot.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CooldownScope;
import polybot.RoleLimitedCommand;

public class EarliesCommand extends RoleLimitedCommand {

    public EarliesCommand() {
        super(806648141046087722L);
        this.name = "earlies";
        this.cooldown = 15;
        this.cooldownScope = CooldownScope.GUILD;
    }

    @Override
    protected void executeCommand(CommandEvent event) {
        event.reply("There are " + event.getGuild().getMembersWithRoles(event.getGuild().getRoleById(restrictedToRoleId)) + " people with early gang.");
    }
}
