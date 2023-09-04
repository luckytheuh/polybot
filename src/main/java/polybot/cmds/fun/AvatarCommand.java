package polybot.cmds.fun;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import polybot.commands.*;
import polybot.util.ColorUtil;
import polybot.util.UserUtil;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class AvatarCommand extends SlashCommand {

    public AvatarCommand() {
        super("avatar", Category.MEDIA, "Fetch the avatar of a user.");

        this.cooldownLength = 5;
        this.arguments = "[user]";
        this.aliases = new String[]{"av", "pfp"};
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.USER;
        this.options = Collections.singletonList(new OptionData(OptionType.USER, "user", "User to get the avatar of"));
        this.detailedHelp = """
            **Examples**
            `&avatar 411636091683340290`
            `&av vsntt`""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        User user;

        if (event.args() != null) {
            user = UserUtil.searchForUser(event.args()[0]);

            if (user == null) {
                event.replyMention("Please provide a valid user!");
                return;
            }
        } else user = event.getAuthor();

        event.reply(getAvatarEmbed(user));
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.replyShown(getAvatarEmbed(event.optUser("user", event.getUser())));
    }

    private MessageEmbed getAvatarEmbed(User user) {
        return new EmbedBuilder().setColor(ColorUtil.ONLINE).setTitle(MarkdownSanitizer.escape(UserUtil.getUserAsName(user)) + "'s avatar").setImage(user.getAvatarUrl() + "?size=1024").build();
    }
}
