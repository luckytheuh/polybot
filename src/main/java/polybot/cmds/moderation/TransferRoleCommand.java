package polybot.cmds.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.Category;
import polybot.commands.CommandEvent;
import polybot.commands.SlashCommand;
import polybot.commands.SlashCommandEvent;
import polybot.util.ColorUtil;
import polybot.util.GuildUtil;
import polybot.util.UserUtil;

import java.util.ArrayList;
import java.util.List;

public class TransferRoleCommand extends SlashCommand {

    public TransferRoleCommand() {
        super("transferrole", Category.MODERATOR, "Transfer roles from one member to another");

        this.isHidden = true;
        this.argsRequired = true;
        this.totalArgsRequired = 2;
        this.arguments = "(from) (to)";
        this.requiredPermission = Permission.MANAGE_ROLES;
        this.aliases = new String[]{"addroles", "giveroles"};
        this.options = List.of(new OptionData(OptionType.USER, "from", "User to copy from", true),
                new OptionData(OptionType.USER, "to", "User to give roles", true));
        this.detailedHelp = """
            This command will give all the roles from the first member, to the second member provided.
                    
            **Examples**
            `&transferrole Kit steve.07`
            `&giveroles 974314293464018984 565065032472199169`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        Member[] members = new Member[]{event.reqMember("from"), event.reqMember("to")};

        for (Member member : members) {
            if (member == null) {
                event.replyShown("Please provide two valid server members!");
                return;
            }

            if (member.getUser().isBot() || member.getUser().isSystem()) {
                event.replyShown("Please provide two non bot server members!");
                return;
            }
        }

        handle(members[0], members[1]);
        event.replyShown("Transferred all roles. (Some roles may be excluded if I do not have permission to grant them.)");
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        Member[] members = new Member[2];
        for (int i = 0; i < members.length; i++) {
            members[i] = UserUtil.searchForMember(event.getGuild(), event.args()[i]);
            if (members[i] == null) {
                event.reply(failCommand("A member provided was not found. Please provide two valid members."));
                return;
            }

            if (members[i].getUser().isBot() || members[i].getUser().isSystem()) {
                event.reply(failCommand("A member provided is either a bot or a system account."));
                return;
            }
        }

        handle(members[0], members[1]);
        event.reply("Transferred all roles. (Some roles may be excluded if I do not have permission to grant them.)");
    }

    private void handle(Member from, Member to) {
        List<Role> roles = new ArrayList<>(from.getRoles());
        roles.removeIf(r -> !GuildUtil.canHandOutRole(r));

        to.getGuild().modifyMemberRoles(to, roles).queue(null,
                new ErrorHandler().ignore(InsufficientPermissionException.class, HierarchyException.class));
    }

    private MessageEmbed failCommand(String message) {
        return new EmbedBuilder()
                .setTitle("Transfer failed")
                .setColor(ColorUtil.DND)
                .setDescription(message + "\n\nMembers can be provided via mention, their id, or using their username. (Case Sensitive!)")
                .setFooter("If the user still has a discriminator, include their username without it.")
                .build();
    }
}
