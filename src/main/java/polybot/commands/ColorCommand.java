package polybot.commands;

import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ThreadLocalRandom;

public class ColorCommand extends SlashCommand {

    public ColorCommand() {
        this.name = "color";
        this.aliases = new String[]{"colour"};
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        Color color = getRandomColor();
        String hex = Integer.toHexString(color.getRGB()).substring(2);

        event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Random Color")
                        .setDescription("Hex Code: #" + hex)
                        .setFooter("Name: " + getColorName(hex))
                        .setColor(color)
                .build()).queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split(" ");

        //Check if they specified a hex value, otherwise just gen random
        Color color = null;
        if (!args[0].isBlank() && !args[0].isEmpty()) {
            try {
                color = new Color(Integer.parseInt(args[0].replaceAll("#|0x", ""), 16));
            } catch (NumberFormatException ignored) {}
        }

        EmbedBuilder builder = new EmbedBuilder();

        //TODO: check for specified hex
        if (color == null) {
            color = getRandomColor();
            builder.setTitle("Random Color");
        } else builder.setTitle("Specific Color");

        String hex = Integer.toHexString(color.getRGB()).substring(2);

        builder.setColor(color)
                .setImage("https://singlecolorimage.com/get/" + hex + "/150x150")
                .setDescription("Hex Code: #" + hex)
                .setFooter("Name: " + getColorName(hex));

        event.reply(builder.build());
    }

    public String getColorName(String hex) {
        try {
            URLConnection connection = new URL("https://www.thecolorapi.com/id?hex=" + hex.toUpperCase()).openConnection();
            connection.addRequestProperty("User-Agent", "NPC Bot v0.0.0");
            connection.connect();

            String line = "";

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String c;
                while ((c = reader.readLine()) != null) {
                    line = line.concat(c + "\n");
                }
            }

            return JsonParser.parseString(line).getAsJsonObject().get("name").getAsJsonObject().get("value").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    public Color getRandomColor() {
        return new Color(ThreadLocalRandom.current().nextInt(0, 16777216));
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
