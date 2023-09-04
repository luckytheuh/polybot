package polybot.cmds.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.FileUpload;
import polybot.commands.Category;
import polybot.commands.Command;
import polybot.commands.CommandEvent;
import polybot.util.ColorUtil;
import polybot.util.UserUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UsersWithRolesCommand extends Command {

    public UsersWithRolesCommand() {
        super("userswithroles", Category.MODERATOR, "Get a list of members with the roles given");

        this.isHidden = true;
        this.argsRequired = true;
        this.aliases = new String[]{"uwr"};
        this.requiredPermission = Permission.KICK_MEMBERS;
        this.detailedHelp = """
            If the list of members exceeds 50, a text file will be returned with every member's username and id.
                    
            **Examples**
            `&uwr 804507156250099762` - All Mod Donuts
            `&uwr 804550712926928896 804519709868949574` - All level 1's with the warning role.""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        List<Member> members;

        {
            List<Role> roles = new ArrayList<>();

            for (String s : event.args()) {
                Role r = event.getGuild().getRoleById(s);
                if (r != null) roles.add(r);
            }

            members = event.getGuild().getMembersWithRoles(roles);
        }

        if (members.size() == 0) {
            event.replyMention("Zero members could be found with the roles given!");
            return;
        }

        StringBuilder builder = new StringBuilder("Number of members with the roles provided: " + members.size() + '\n');
        for (Member member : members) {
            builder.append((members.size() > 50 ? UserUtil.getMemberAsName(member) : member.getAsMention())).append(" (").append(member.getId()).append(")\n");
        }

        if (builder.length() > 4096) {
            event.reply(FileUpload.fromData(builder.toString().getBytes(StandardCharsets.UTF_8), "members.txt"));
        } else {
            event.reply(new EmbedBuilder().setColor(ColorUtil.DND).setTitle("List of members with the roles").setDescription(builder.toString()).build());
        }
    }
}
