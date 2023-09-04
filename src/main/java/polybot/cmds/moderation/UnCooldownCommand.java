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
import java.util.Collections;
import java.util.List;

public class UnCooldownCommand extends SlashCommand {

    public UnCooldownCommand() {
        super("uncooldown", Category.MODERATOR, "Remove a user from cooldown");

        this.isHidden = true;
        this.argsRequired = true;
        this.ignoreSendCheck = true;
        this.arguments = "(member)";
        this.deleteOption = DeleteOption.DELETE_USER;
        this.requiredPermission = Permission.KICK_MEMBERS;
        this.options = Collections.singletonList(new OptionData(OptionType.USER, "user", "User to cool down", true));
        this.detailedHelp = """
            This command will remove the COOLING DOWN role from the user provided, and regive all of their roles stored in the database.
            COOLING DOWN is required for the target user to be uncool-downed.
                    
            **Examples**
            `&uncooldown lucky_the_uh`
            `&uncooldown 485552876689817600`""";
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

        if (!GuildUtil.memberHasRole(member, BotStorage.getSettingAsLong(Setting.COOLDOWN_ROLE, 0))) {
            replyAndDelete(event, DeleteOption.DELETE_BOTH, "The member specified is not cooled down!");
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

        if (!GuildUtil.memberHasRole(member, BotStorage.getSettingAsLong(Setting.COOLDOWN_ROLE, 0))) {
            event.replyHidden("The member specified is not cooled down!");
            return;
        }

        handle(event.getMember(), member);
        event.replyHidden("Uncooled down " + UserUtil.getMemberAsName(member));
    }

    private void handle(Member moderator, Member member) {
        List<Role> memberRoles = BotStorage.removeMemberFromCooldown(member);
        //memberRoles.addAll(;

        member.getGuild().modifyMemberRoles(member, memberRoles).queue();

        TextChannel channel = GuildUtil.getChannelFromSetting(member.getGuild(), Setting.COOLDOWN_LOG_CHANNEL);
        if (channel != null) {
            channel.sendMessageEmbeds(new EmbedBuilder()
                    .setAuthor(UserUtil.getMemberAsName(member) + "'s cooldown was removed.", null, member.getUser().getAvatarUrl())
                    .addField("User", member.getAsMention(), true)
                    .addField("Staff Member", moderator.getAsMention(), true)
                    .setFooter("User ID: " + member.getId())
                    .setTimestamp(Instant.now())
                    .setColor(ColorUtil.COOLDOWN_REMOVE)
                    .build()
            ).queue();
        }
    }

    @Override
    public String getNoArgMessage() {
        return "Please specify a user to uncool down!";
    }
}
