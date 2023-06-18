package polybot.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.ImageProxy;
import polybot.levels.LevelEntry;
import polybot.storage.BotStorage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RankCommand extends SlashCommand {

    private static final BufferedImage BACKGROUND;

    static {
        //load the backgrond image
        BufferedImage cache = null;

        try {
            InputStream stream = ClassLoader.getSystemResourceAsStream("mat.jpg");
            if (stream != null) cache = ImageIO.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BACKGROUND = cache;
    }

    public RankCommand() {
        this.name = "rank";
        this.guildOnly = false;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE) return;

        handleCommand(event.getUser(), event.getChannel());
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE) return;

        handleCommand(event.getAuthor(), event.getChannel());
    }

    private void handleCommand(User user, MessageChannel channel) { //TODO: check if we can send images, send text if we cant
        LevelEntry entry = BotStorage.getLevelEntry(user.getIdLong());

        final ImageProxy proxy = user.getAvatar() == null ? user.getDefaultAvatar() : user.getAvatar();
        proxy.download().thenAcceptAsync(stream -> {
            BufferedImage avatar;
            try {
                avatar = ImageIO.read(stream);
                stream.close();

                BufferedImage levelImage = new BufferedImage(800, 200, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = levelImage.createGraphics();

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(BACKGROUND, -50, -50, 1200, 600, null);

                //Draw background
                g2d.setColor(new Color(0, 0, 0, 192));
                g2d.fillRect(25, 25, 750, 150);
                g2d.drawImage(avatar, 35, 35, 128, 128, null);

                //Draw text
                g2d.setColor(Color.white);
                g2d.setFont(new Font("FuturaBT-ExtraBlackCondensed", Font.PLAIN, 32));
                g2d.drawString(user.getName(), 175, 65);
                g2d.setFont(new Font("FuturaBT-ExtraBlackCondensed", Font.PLAIN, 24));
                g2d.drawString("Level " + entry.getLevel(), 175, 100);
                g2d.drawString("Xp: " + entry.getXpToNextLevel() + "/" + entry.getXpForNextLevel(), 175, 125);

                //Draw progress bar
                g2d.setColor(Color.white);
                g2d.setStroke(new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(190, 150, 750, 150);

                g2d.setColor(Color.blue);
                g2d.drawLine(190, 150, 190 + (int) (entry.getLevelProgress() * 560), 150);

                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                ImageIO.write(levelImage, "png", bStream);
                channel.sendFiles(FileUpload.fromData(bStream.toByteArray(), "level.png")).queue();
            } catch (IOException e) {}
        });





    }
}
