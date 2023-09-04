package polybot.cmds.media;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.*;
import polybot.util.ImageUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class UncaptionCommand extends SlashCommand {

    public UncaptionCommand() {
        super("uncaption", Category.MEDIA, "Remove an esmBot style caption from your media.");

        this.cooldownLength = 15;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.CHANNEL;
        this.deleteOption = DeleteOption.DELETE_BOT;
        this.options = List.of(new OptionData(OptionType.ATTACHMENT, "image", "Media to edit"),
                new OptionData(OptionType.STRING, "link", "Link to media to edit"),
                new OptionData(OptionType.BOOLEAN, "invert", "Invert the uncaption color check (white -> blue)")
        );
        this.detailedHelp = """
                If an exporting error occurred, please ping the bot owner with the render/task id.
                
                Providing `-i` or `-invert` will invert the colors to check for a caption.""";
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
                if (param.equalsIgnoreCase("-i") || param.equalsIgnoreCase("-invert")) {
                    event.setParams("-i");
                    break;
                }
            }
        }

        ImageUtil.searchAndFire(event, name);
    }
}
