package polybot.cmds;

import net.dv8tion.jda.api.entities.Role;
import polybot.commands.CommandEvent;
import polybot.commands.CooldownType;
import polybot.commands.SlashCommand;
import polybot.commands.SlashCommandEvent;

import java.util.concurrent.TimeUnit;

public class EarliesCommand extends SlashCommand {

    public EarliesCommand() {
        super("earlies", "Display the total number of early gang members");

        this.isHidden = true;
        this.cooldownLength = 15;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.GUILD;
        this.requiredRole = 806648141046087722L;
        this.detailedHelp = """
            Show how many early gang members are still alive.
            When creating the help for this command, we're at 200 early gang members.""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        //if (true) return;

        Role role = event.getGuild().getRoleById(requiredRole);
        event.reply("There are " + event.getGuild().getMembersWithRoles(role).size() + " people with " + role.getName() + '.');
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        /*
        getMessageEvent.replyShown("t");
        if (true) return;
*/
        Role role = event.getGuild().getRoleById(requiredRole);
        event.replyShown("There are " + event.getGuild().getMembersWithRoles(role).size() + " people with " + role.getName() + '.');
    }
}
