package polybot.cmds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import polybot.Constants;
import polybot.commands.*;
import polybot.storage.Fonts;
import polybot.util.BotUtil;
import polybot.util.ColorUtil;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class FontCommand extends SlashCommand {

    private static final String ALPHABET_SAMPLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ    abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBER_SYMBOL_SAMPLE = "`1234567890-=[]\\;',./    ~!@#$%^&*()_+{}|:\"<>?";

    public FontCommand() {
        super("fonts", Category.UTILITY, "List off all or view bot fonts.");

        this.cooldownLength = 5;
        this.arguments = "[font]";
        this.aliases = new String[]{"font"};
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.USER;
        this.deleteOption = DeleteOption.DELETE_BOTH;
        this.addToAutoCompleteList("font", Arrays.stream(Fonts.values()).map(Fonts::friendlyName).toList());
        this.options = Collections.singletonList(new OptionData(OptionType.STRING, "font", "Font to preview").setAutoComplete(true));
        this.detailedHelp = """
            If a font name is not provided, a list of all fonts will be provided.
            To view a font, include the font name when running the command, this font has to exist for it to display.
                    
            **Examples**
            `&font minecraft`
            `&fonts`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        if (event.optString("font", null) != null) {
            Fonts fonts = Fonts.getFontFromString(event.reqString("font"));

            if (fonts == null) {
                event.replyShown(new EmbedBuilder()
                        .setTitle("Invalid font")
                        .setDescription('`' + event.reqString("font") + "` is not a valid font.")
                        .setColor(ColorUtil.DND)
                        .build()
                );
                return;
            }

            try {
                BufferedImage img = generateFontTester(fonts);

                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ImageIO.write(img, "png", byteStream);

                event.replyFiles(FileUpload.fromData(byteStream.toByteArray(), fonts.friendlyName() + ".png")).queue();
                return;
            } catch (IOException ignored) {}
        }

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("List Of Fonts")
                .setDescription("To preview a font, include the font name when running this command.\n\n")
                .setFooter("These fonts can be used with the 'level card font' usersetting")
                .setColor(ColorUtil.ONLINE);


        for (int i = 0; i < Fonts.values().length; i++) {
            if (Fonts.values()[i].getFont() == null) continue;

            builder.appendDescription(String.format("`%s`%s%s",
                    Fonts.values()[i].friendlyName(),
                    Fonts.values()[i].friendlyName().equals("roboto") ? " (Default) " : "",
                    i == Fonts.values().length - 1 ? "" : " | ")
            );
        }

        event.replyShown(builder.build());

    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (event.args() != null) {
            Fonts font = Fonts.getFontFromString(event.stringArgs());

            if (font != null) {
                MessageCreateAction action = BotUtil.uploadImage(event.getChannel(), generateFontTester(font), font.friendlyName() + ".png", true);
                if (action != null) {
                    action.setMessageReference(event.getMessage()).mentionRepliedUser(false).queue();
                }
            } else {
                event.replyMention(new EmbedBuilder()
                        .setTitle("Invalid font")
                        .setDescription('`' + event.stringArgs() + "` is not a valid font.")
                        .setColor(ColorUtil.DND)
                        .build()
                );
            }
        } else {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle("List Of Fonts")
                    .setDescription("To preview a font, include the font name when running this command.\n\n")
                    .setFooter("These fonts can be used with the 'level card font' usersetting")
                    .setColor(ColorUtil.ONLINE);


            for (int i = 0; i < Fonts.values().length; i++) {
                if (Fonts.values()[i].getFont() == null) continue;

                builder.appendDescription(String.format("`%s`%s%s",
                        Fonts.values()[i].friendlyName(),
                        Fonts.values()[i].friendlyName().equals("roboto") ? " (Default) " : "",
                        i == Fonts.values().length - 1 ? "" : " | ")
                );
            }

            event.reply(builder.build());
        }
    }

    private BufferedImage generateFontTester(Fonts f) {
        FontMetrics metrics = BotUtil.getFontMetrics(f.getFont());

        int width = Math.max(metrics.stringWidth(ALPHABET_SAMPLE), metrics.stringWidth(NUMBER_SYMBOL_SAMPLE));
        int height = metrics.getHeight() * 2 + Constants.CARD_BORDER;

        BufferedImage fontImage = new BufferedImage(width + Constants.CARD_BORDER, height - (Constants.CARD_BORDER/2), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = fontImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, fontImage.getWidth(), fontImage.getHeight());

        g2d.setFont(f.getFont());
        g2d.setColor(Color.black);
        BotUtil.drawTextCentered(g2d, ALPHABET_SAMPLE, fontImage.getWidth(), g2d.getFontMetrics().getHeight());
        BotUtil.drawTextCentered(g2d, NUMBER_SYMBOL_SAMPLE, fontImage.getWidth(), height - Constants.CARD_BORDER);
        g2d.dispose();

        return fontImage;
    }
}
