package polybot.cmds.media;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.*;
import polybot.util.ImageUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class WojakCommand extends SlashCommand {

    public WojakCommand() {
        super("wojak", Category.MEDIA, "Add the wojaks pointing image over an image.");

        this.cooldownLength = 15;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.aliases = new String[]{"wojack"};
        this.cooldownType = CooldownType.CHANNEL;
        this.deleteOption = DeleteOption.DELETE_BOT;
        this.options = List.of(new OptionData(OptionType.ATTACHMENT, "media", "Media to edit"),
                new OptionData(OptionType.STRING, "link", "Link to media to edit")
        );
        this.detailedHelp = """
                This command will stretch the provided image to size, then overlay the wojaks pointing.
                Images can be provided by replying to one, attaching one, or including the link to one.
                Only the first frame of a GIF will have the overlay on it.""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        ImageUtil.searchAndFire(event, name);
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        Message.Attachment attachment = event.optAttachment("media", null);
        String url = attachment == null ? event.optString("link", null) : attachment.getUrl();

        ImageUtil.searchAndFireSlash(event, name, url, null);
    }
}
