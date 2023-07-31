package polybot.commands;

import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CooldownScope;
import net.dv8tion.jda.api.EmbedBuilder;
import polybot.RoleLimitedCommand;
import polybot.util.BotUtil;
import polybot.util.ColorUtil;

import java.awt.Color;

public class ColorCommand extends RoleLimitedCommand {

    public ColorCommand() {
        super(804551691847729172L);
        this.name = "color";
        this.aliases = new String[]{"colour"};
        this.cooldown = 5;
        this.cooldownScope = CooldownScope.GUILD;
    }

    @Override
    protected void executeCommand(CommandEvent event) {
        EmbedBuilder builder = new EmbedBuilder().setTitle("Random Color");
        Color color = null;

        String arg = event.getArgs().split(" ")[0];
        if (!arg.isBlank() && !arg.isEmpty()) color = ColorUtil.getColorFromString(arg);

        if (color == null) color = ColorUtil.getRandomColor();
        else builder.setTitle("Specific Color");

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
