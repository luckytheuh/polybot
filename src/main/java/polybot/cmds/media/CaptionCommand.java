package polybot.cmds.media;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.*;
import polybot.util.ImageUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CaptionCommand extends SlashCommand {

    public CaptionCommand() {
        super("caption", Category.MEDIA, "Add an esmBot style caption to your media.");

        this.cooldownLength = 15;
        this.argsRequired = true;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.CHANNEL;
        this.deleteOption = DeleteOption.DELETE_BOT;
        this.options = List.of(new OptionData(OptionType.STRING, "caption", "Caption to add to the media", true),
                new OptionData(OptionType.ATTACHMENT, "image", "Media to edit"),
                new OptionData(OptionType.STRING, "link", "Link to media to edit"),
                new OptionData(OptionType.BOOLEAN, "invert", "Invert the colors of the caption")
        );
        this.detailedHelp = """
                This command supports most image, gif, and video files to caption.
                If an exporting error occurred, please ping the bot owner with the render/task id.
                
                Providing `-i` or `-invert` will invert the caption colors.
                
                **Examples**
                `&caption when you jumbo but the garten of banban is josh`
                `&caption -i ip grabber`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        Message.Attachment attachment = event.optAttachment("image", null);
        String url = attachment == null ? event.optString("link", null) : attachment.getUrl();
        if (event.optBoolean("invert", false)) event.setParams("-i");

        ImageUtil.searchAndFireSlash(event, name, url, event.reqString("caption"));
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (ImageUtil.invalidCaptionArgs(event.args())) {
            replyAndDelete(event, "Invalid caption provided, please avoid including emojis.");
            return;
        }

        String param = event.args()[0];
        if (param.equalsIgnoreCase("-i") || param.equalsIgnoreCase("-invert")) {
            event.offsetArgs(1);
            event.setParams("-i");
        }

        ImageUtil.searchAndFire(event, name);
    }
}
