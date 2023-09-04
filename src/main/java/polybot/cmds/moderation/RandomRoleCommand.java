package polybot.cmds.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.*;
import polybot.util.GuildUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomRoleCommand extends SlashCommand {

    public RandomRoleCommand() {
        super("randomrole", Category.MODERATOR, "Filter by people with a specific role, then randomly give one of them a role");

        this.isHidden = true;
        this.argsRequired = true;
        this.arguments = "(filter by role),(role to hand out)";
        this.deleteOption = DeleteOption.DELETE_BOTH;
        this.requiredPermission = Permission.MANAGE_ROLES;
        this.options = List.of(new OptionData(OptionType.ROLE, "filterby", "Role to filter by", true),
                new OptionData(OptionType.ROLE, "role", "Role to randomly give out", true));
        this.detailedHelp = """
            Filtering by all members who have the first role provided, this command will randomly select a member and grant them the second role provided.
            Members who already have the second role provided will be skipped.
            If no member could be found without the second role provided, the command will fail.
                    
            **Examples**
            `&randomrole he/him,REACTION MUTE`
            `&randomrole 804550712926928896,804552387859709972`""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (!event.stringArgs().contains(",")) {
            replyAndDelete(event, "Please include a `,` to separate the two roles!");
            return;
        }

        String[] args = event.stringArgs().split(",");
        if (args.length < 2) {
            replyAndDelete(event, "Please include two valid roles!");
            return;
        }

        Role[] roles = new Role[2];
        for (int i = 0; i < roles.length; i++) {
            roles[i] = GuildUtil.searchForRole(event.getGuild(), args[i]);
            if (roles[i] == null) {
                replyAndDelete(event, "Failed to find a valid role for the given string: `" + args[i] + '`');
                return;
            }
        }

        event.getMessage().delete().queue();
        Member result = handleRoles(roles[0], roles[1]);
        if (result == null) event.replyInDm("The command failed! This could mean nobody can eligible or my role isn't high enough!");
        else event.replyInDm("Role given to " + result.getAsMention());
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        Member member = handleRoles(event.reqRole("filterby"), event.reqRole("role"));
        if (member == null) event.replyHidden("The command failed! This could mean nobody is eligible or my role isn't high enough!");
        else event.replyHidden("Role given to " + member.getAsMention());
    }

    private Member handleRoles(Role filterByRole, Role roleToGive) {
        if (!GuildUtil.canHandOutRole(roleToGive)) return null;

        List<Member> membersWithRole = new ArrayList<>(filterByRole.getGuild().getMembersWithRoles(filterByRole));
        membersWithRole.removeIf(member -> member.getUser().isBot() || member.getUser().isSystem() || GuildUtil.memberHasRole(member, roleToGive.getIdLong()));

        if (membersWithRole.isEmpty()) return null;

        Member member = membersWithRole.get(ThreadLocalRandom.current().nextInt(0, membersWithRole.size()));
        member.getGuild().addRoleToMember(member, roleToGive).queue();
        return member;
    }

}
