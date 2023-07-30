package polybot.commands;

import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import polybot.RoleLimitedCommand;
import polybot.util.BotUtil;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

public class ColorCommand extends RoleLimitedCommand {

    public ColorCommand() {
        super(804551691847729172L);
        this.name = "color";
        this.aliases = new String[]{"colour"};
    }

    @Override
    protected void executeCommand(CommandEvent event) {
        Color color = new Color(ThreadLocalRandom.current().nextInt(0, 16777216)); // Generate a random color
        EmbedBuilder builder = new EmbedBuilder().setTitle("Random Color");

        String arg = event.getArgs().split(" ")[0];
        if (!arg.isBlank() && !arg.isEmpty()) {
            try {
                color = new Color(Integer.parseInt(arg.replaceAll("#|0x", ""), 16));
                builder.setTitle("Specific Color");
            } catch (NumberFormatException ignored) {}
        }

        String hex = Integer.toHexString(color.getRGB()).substring(2);
        JsonObject object = BotUtil.fetchWebsiteJson("https://www.thecolorapi.com/id?hex=" + hex.toUpperCase());

        event.reply(builder.setColor(color)
                .setImage("https://singlecolorimage.com/get/" + hex + "/150x150")
                .setDescription("Hex Code: #" + hex)
                .setFooter("Name: " + (object == null ? "unknown" : object.get("name").getAsJsonObject().get("value").getAsString()))
                .build());
    }


/*
    public BufferedImage getColorAsImage(Color color) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, 256, 256);
        g2d.dispose();
        return image;
    }
*/
}
