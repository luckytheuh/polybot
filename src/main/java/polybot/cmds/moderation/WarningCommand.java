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

public class WarningCommand extends Command {

    public WarningCommand() {
        super("warning", Category.MODERATOR, "Give a user a warning role");

        this.isHidden = true;
        this.argsRequired = true;
        this.arguments = "(user)";
        this.requiredPermission = Permission.KICK_MEMBERS;
        this.aliases = new String[]{"w1", "w2", "warning1", "warning2"};
        this.detailedHelp = """
            This command will give the provided user either the WARNING or WARNING TWO role.
            Providing additional parameters will DM the affected user and log it into the log channel using the additional parameters as a reason.
            If the user already had the role, it will be removed and :x: will be the response, :white_check_mark: otherwise.
            
            WARNING - `warning` | `warning1` | `w1`
            WARNING TWO - `warning2` | `w2`
                    
            **Examples**
            `&w1 805687770818936845`
            `&warning2 1139739940524130314 fart smella`""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        Member member = UserUtil.getMemberFromUser(event.getGuild(), UserUtil.searchForUser(event.args()[0]));
        if (member == null) {
            event.reply("Please provide a valid server member!");
            return;
        }

        if (event.commandUsed().startsWith("w2") || event.commandUsed().startsWith("warning2")) {
            if (GuildUtil.memberHasRole(member, Constants.WARNING_TWO_ROLE)) {
                GuildUtil.removeRole(member, Constants.WARNING_TWO_ROLE);
                event.getMessage().addReaction(Constants.CROSS_EMOJI).queue();
                return;
            } else {
                GuildUtil.addRole(member, Constants.WARNING_TWO_ROLE);
                event.getMessage().addReaction(Constants.CHECK_EMOJI).queue();
            }
        } else {
            if (GuildUtil.memberHasRole(member, Constants.WARNING_ROLE)) {
                GuildUtil.removeRole(member, Constants.WARNING_ROLE);
                event.getMessage().addReaction(Constants.CROSS_EMOJI).queue();
                return;
            } else {
                GuildUtil.addRole(member, Constants.WARNING_ROLE);
                event.getMessage().addReaction(Constants.CHECK_EMOJI).queue();
            }
        }

        if (event.args().length > 1) {
            event.offsetArgs(1);
            member.getUser().openPrivateChannel().queue(privateChannel -> {
                privateChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(String.format("You were warned in %s for %s", event.getGuild().getName(), event.stringArgs()))
                        .setColor(ColorUtil.DND)
                        .build()).queue();

                event.send(new EmbedBuilder()
                        .setDescription(String.format("%s ***%s has been warned.***", Constants.CHECK_EMOJI.getFormatted(), UserUtil.getMemberAsName(member)))
                        .setColor(ColorUtil.ONLINE)
                        .build());
            }, throwable -> {
                event.send(new EmbedBuilder()
                        .setDescription(String.format("%s ***%s has been warned, I was unable to DM them.***", Constants.CHECK_EMOJI.getFormatted(), UserUtil.getMemberAsName(member)))
                        .setColor(ColorUtil.ONLINE)
                        .build());
            });

            TextChannel logChannel = GuildUtil.getChannelFromSetting(event.getGuild(), Setting.DYNO_LOG_CHANNEL);
            if (logChannel != null) {
                logChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setAuthor("Warn | " + UserUtil.getMemberAsName(member), null, member.getUser().getAvatarUrl())
                        .addField("User", member.getAsMention(), true)
                        .addField("Moderator", event.getAuthor().getAsMention(), true)
                        .addField("Reason", event.stringArgs(), true)
                        .setFooter("ID: " + member.getId())
                        .setColor(ColorUtil.IDLE)
                        .setTimestamp(Instant.now())
                        .build()).queue();
            }
        }
    }

    @Override
    public String getNoArgMessage() {
        return "Please provide a member!";
    }
}
