package polybot.cmds.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.*;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.ColorUtil;
import polybot.util.GuildUtil;
import polybot.util.UserUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CooldownCommand extends SlashCommand {

    public CooldownCommand() {
        super("cooldown", Category.MODERATOR, "Put a user in cooldown");

        this.isHidden = true;
        this.argsRequired = true;
        this.ignoreSendCheck = true;
        this.arguments = "(member)";
        this.deleteOption = DeleteOption.DELETE_USER;
        this.requiredPermission = Permission.KICK_MEMBERS;
        this.options = Collections.singletonList(new OptionData(OptionType.USER, "user", "User to cool down", true));
        this.detailedHelp = """
            This command will remove all user roles, and grant them the COOLING DOWN role.
            Roles removed will be saved to the database for later use.
                    
            **Examples**
            `&cooldown lucky_the_uh`
            `&cooldown 485552876689817600`""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        Member member = UserUtil.searchForMember(event.getGuild(), event.args()[0]);

        if (member == null) {
            replyAndDelete(event, getNoArgMessage());
            return;
        }

        if (member.getUser().isBot()) {
            replyAndDelete(event, DeleteOption.DELETE_BOTH, "The member specified is a bot!");
            return;
        }

        if (GuildUtil.memberHasRole(member, BotStorage.getSettingAsLong(Setting.COOLDOWN_ROLE, 0))) {
            replyAndDelete(event, DeleteOption.DELETE_BOTH, "The member specified already is cooled down!");
            return;
        }

        handle(event.getMember(), member);
        event.getMessage().delete().queue();
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        Member member = event.optMember("user", null);

        if (member == null) {
            event.replyHidden(getNoArgMessage());
            return;
        }

        if (member.getUser().isBot()) {
            event.replyHidden("The member specified is a bot!");
            return;
        }

        if (GuildUtil.memberHasRole(member, BotStorage.getSettingAsLong(Setting.COOLDOWN_ROLE, 0))) {
            event.replyHidden("The member specified already is cooled down!");
            return;
        }

        handle(event.getMember(), member);
        event.replyHidden("Cooled down " + UserUtil.getMemberAsName(member));
    }

    private void handle(Member moderator, Member member) {
        List<Role> memberRoles = new ArrayList<>(member.getRoles());

        Role cooldownRole = GuildUtil.getRoleFromSetting(member.getGuild(), Setting.COOLDOWN_ROLE);
        member.getGuild().modifyMemberRoles(member, (cooldownRole == null ? Collections.emptyList() : Collections.singletonList(cooldownRole))).queue();

        StringBuilder roleBuilder = new StringBuilder();
        for (int i = 0; i < memberRoles.size(); i++) {
            roleBuilder.append(memberRoles.get(i).getId());

            if (i != member.getRoles().size()-1) roleBuilder.append(',');
        }
        BotStorage.addMemberToCooldown(member, roleBuilder.toString());

        TextChannel channel = GuildUtil.getChannelFromSetting(member.getGuild(), Setting.COOLDOWN_LOG_CHANNEL);
        if (channel != null) {
            channel.sendMessageEmbeds(new EmbedBuilder()
                    .setAuthor(UserUtil.getMemberAsName(member) + " has been cooled down.", null, member.getUser().getAvatarUrl())
                    .addField("User", member.getAsMention(), true)
                    .addField("Staff Member", moderator.getAsMention(), true)
                    .setFooter("User ID: " + member.getId())
                    .setTimestamp(Instant.now())
                    .setColor(ColorUtil.COOLDOWN_ADD)
                    .build()
            ).queue();
        }
    }

    @Override
    public String getNoArgMessage() {
        return "Please specify a user to cool down!";
    }

/*
    List<Role> removedRoles = new ArrayList<>();

    // Remove all level roles, as those are the only ones that give perms
    for (int i = 0; i < memberRoles.size(); i++) {
        if (memberRoles.get(i).getName().contains("(Lvl")) {
            removedRoles.add(memberRoles.get(i));
            memberRoles.remove(i--);
        }
    }
*/
}
