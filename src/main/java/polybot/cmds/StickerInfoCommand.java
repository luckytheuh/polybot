package polybot.cmds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.sticker.Sticker;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import polybot.commands.Category;
import polybot.commands.Command;
import polybot.commands.CommandEvent;
import polybot.util.ColorUtil;

import java.util.List;

public class StickerInfoCommand extends Command {

    public StickerInfoCommand() {
        super("stickerinfo", Category.MEDIA, "Show information about a sticker");

        this.detailedHelp = "Reply to or attach a sticker to obtain information about it.";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (event.getMessage().getStickers().isEmpty() && event.getMessage().getMessageReference() == null) {
            event.replyMention("Please reply to a sticker or include one in your message!");
            return;
        }

        List<StickerItem> stickers;
        if (event.getMessage().getMessageReference() != null) {
            Message message = event.getMessage().getReferencedMessage();
            if (message == null) message = event.getMessage().getMessageReference().resolve().complete();

            stickers = message.getStickers();
        } else stickers = event.getMessage().getStickers();

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Sticker Info")
                .setColor(ColorUtil.ONLINE);

        for (StickerItem sticker : stickers) {
            boolean isCustom = sticker.getFormatType() != Sticker.StickerFormat.LOTTIE;
            String desc = "ID: `%s`\nCustom Sticker: `%b`\nCreated on <t:%d>\n[Sticker Link](%s)";


            builder.addField(sticker.getName(), String.format(desc, sticker.getId(), isCustom, sticker.getTimeCreated().toEpochSecond(), sticker.getIconUrl()), true);
        }

        event.reply(builder.build());
    }
}
