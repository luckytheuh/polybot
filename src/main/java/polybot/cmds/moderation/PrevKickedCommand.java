package polybot.cmds.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import polybot.Constants;
import polybot.commands.Category;
import polybot.commands.Command;
import polybot.commands.CommandEvent;
import polybot.storage.Setting;
import polybot.util.ColorUtil;
import polybot.util.GuildUtil;
import polybot.util.UserUtil;

import java.time.Instant;

public class PrevKickedCommand extends Command {

    public PrevKickedCommand() {
        super("kick", Category.MODERATOR, "Give a user the prev kicked role");

        this.isHidden = true;
        this.argsRequired = true;
        this.arguments = "(user)";
        this.aliases = new String[]{"k", "kill"};
        this.requiredPermission = Permission.KICK_MEMBERS;
        this.detailedHelp = """
            This command will give the provided user the PREVIOUSLY KICKED role.
            Providing additional parameters will DM the user, kick them, and then log to the log channel, using the additional parameters as the reason.
                    
            **Examples**
            `&kick 805687770818936845`
            `&k 1139739940524130314`
            `&kill 783265748533379092 firing squad (rule 3 or sum)`""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        Member member = UserUtil.getMemberFromUser(event.getGuild(), UserUtil.searchForUser(event.args()[0]));
        if (member == null) {
            event.reply("Please provide a valid server member!");
            return;
        }

        if (!event.getGuild().getSelfMember().canInteract(member)) {
            if (event.args().length > 1) {
                event.send(new EmbedBuilder()
                        .setDescription(":x: I cannot perform moderation actions on that user, I likely do not have permission to do so.")
                        .setColor(ColorUtil.DND)
                        .build());
            } else {
                event.reply("That user is a moderator.");
            }

            return;
        }

        if (GuildUtil.memberHasRole(member, Constants.KICKED_ROLE)) {
            GuildUtil.removeRole(member, Constants.KICKED_ROLE);
            event.getMessage().addReaction(Constants.CROSS_EMOJI).queue();
            return;
        } else {
            GuildUtil.addRole(member, Constants.KICKED_ROLE);
            event.getMessage().addReaction(Constants.CHECK_EMOJI).queue();
        }

        if (event.args().length > 1) {
            event.offsetArgs(1);
            member.getUser().openPrivateChannel().queue(privateChannel -> {
                privateChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(String.format("You were kicked from %s for %s", event.getGuild().getName(), event.stringArgs()))
                        .setColor(ColorUtil.DND)
                        .build()).queue();

                event.send(new EmbedBuilder()
                        .setDescription(String.format("%s ***%s has been kicked.***", Constants.CHECK_EMOJI.getFormatted(), UserUtil.getMemberAsName(member)))
                        .setColor(ColorUtil.ONLINE)
                        .build());
            }, throwable -> {
                event.send(new EmbedBuilder()
                        .setDescription(String.format("%s ***%s has been kicked, I was unable to DM them.***", Constants.CHECK_EMOJI.getFormatted(), UserUtil.getMemberAsName(member)))
                        .setColor(ColorUtil.ONLINE)
                        .build());
            });

            member.kick().queue();
            event.getMessage().delete().queue();

            TextChannel logChannel = GuildUtil.getChannelFromSetting(event.getGuild(), Setting.DYNO_LOG_CHANNEL);
            if (logChannel != null) {
                logChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setAuthor("Kick | " + UserUtil.getMemberAsName(member), null, member.getUser().getAvatarUrl())
                        .addField("User", member.getAsMention(), true)
                        .addField("Moderator", event.getAuthor().getAsMention(), true)
                        .addField("Reason", event.stringArgs(), true)
                        .setFooter("ID: " + member.getId())
                        .setColor(ColorUtil.DND)
                        .setTimestamp(Instant.now())
                        .build()).queue();
            }


            return;
        }
    }

    @Override
    public String getNoArgMessage() {
        return "Please provide a member!";
    }
}
