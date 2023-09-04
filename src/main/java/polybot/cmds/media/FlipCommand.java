package polybot.cmds.media;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.*;
import polybot.util.ImageUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FlipCommand extends SlashCommand {

    public FlipCommand() {
        super("flip", Category.MEDIA, "Flip a media file horizontally or vertically");

        this.cooldownLength = 15;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.CHANNEL;
        this.deleteOption = DeleteOption.DELETE_BOT;
        this.options = List.of(new OptionData(OptionType.ATTACHMENT, "image", "Media to edit"),
                new OptionData(OptionType.STRING, "link", "Link to media to edit"),
                new OptionData(OptionType.BOOLEAN, "vertical", "Whether to flip the image vertically")
        );
        this.detailedHelp = """
                If an exporting error occurred, please ping the bot owner with the render/task id.
                
                Providing `-v` or `-vertical` will flip the media vertically.""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        Message.Attachment attachment = event.optAttachment("image", null);
        String url = attachment == null ? event.optString("link", null) : attachment.getUrl();
        if (event.optBoolean("invert", false)) event.setParams("-i");

        ImageUtil.searchAndFireSlash(event, name, url, null);
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (event.args() != null) {
            for (String param : event.args()) {
                if (param.equalsIgnoreCase("-v") || param.equalsIgnoreCase("-vertical")) {
                    event.setParams("-v");
                    break;
                }
            }
        }

        ImageUtil.searchAndFire(event, name);
    }

}
