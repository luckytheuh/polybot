package polybot.cmds.media;

import polybot.commands.*;

import java.util.concurrent.TimeUnit;

@ExcludeCommand
public class FetchMediaCommand extends Command {

    public FetchMediaCommand() {
        super("fetch", Category.MEDIA, "Retrieve media from renderers based on an id.");

        this.cooldownLength = 15;
        this.argsRequired = true;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.CHANNEL;
        this.deleteOption = DeleteOption.DELETE_BOT;
        this.detailedHelp = """
                If an exporting error occurred, please ping the bot owner with the render/task id.
                
                Providing `-i` or `-invert` will invert the colors to check for a caption.""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        int id;
        try {
            id = Integer.parseInt(event.args()[0]);
        } catch (NumberFormatException e) {
            replyAndDelete(event, "Invalid id provided!");
            return;
        }

/*
        CompletableFuture.runAsync(() -> {
            byte[] ret = ImageUtil.fetchMedia(id);

        });*/
    }
}
