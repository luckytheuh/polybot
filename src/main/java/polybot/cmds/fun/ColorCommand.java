package polybot.cmds.fun;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.*;
import polybot.util.BotUtil;
import polybot.util.ColorUtil;
import polybot.util.GuildUtil;
import polybot.util.WordUtil;

import java.awt.Color;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ColorCommand extends SlashCommand {

    public ColorCommand() {
        super("color", Category.FUN, "Display a user provided or randomly generated color");

        this.cooldownLength = 30;
        this.arguments = "[hex color code]";
        this.cooldownUnit = TimeUnit.SECONDS;
        this.aliases = new String[]{"colour"};
        this.cooldownType = CooldownType.CHANNEL;
        this.deleteOption = DeleteOption.DELETE_BOTH; //TODO: display the built in list?
        this.options = Collections.singletonList(new OptionData(OptionType.STRING, "color", "Hex Color Code"));
        this.detailedHelp = """
            Java has a predefined list of built in colors.
            You can access these colors by typing in the name of the color instead of a hex code.
            
            **Examples**
            `&color FFFA00`
            `&color blue`
            `&color lightgray`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        EmbedBuilder builder = new EmbedBuilder().setTitle("Random Color");
        Color color = ColorUtil.getColorFromString(event.optString("color", null));

        if (color == null) color = ColorUtil.getRandomColor();
        else builder.setTitle("Specific Color");

        String hex = Integer.toHexString(color.getRGB()).substring(2);
        JsonElement element = BotUtil.fetchWebsiteJson("https://www.thecolorapi.com/id?hex=" + hex.toUpperCase());

        event.replyEmbeds(builder.setColor(color)
                .setImage("https://singlecolorimage.com/get/" + hex + "/150x150")
                .setDescription("Hex Code: #" + hex)
                .setFooter("Name: " + getName(element))
                .build()
        ).queue();
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        EmbedBuilder builder = new EmbedBuilder().setTitle("Random " + WordUtil.uppercaseFirst(event.commandUsed()));
        Color color = null;

        if (event.args() != null) color = ColorUtil.getColorFromString(event.args()[0]);

        if (color == null) color = ColorUtil.getRandomColor();
        else builder.setTitle("Specific " + WordUtil.uppercaseFirst(event.commandUsed()));

        String hex = Integer.toHexString(color.getRGB()).substring(2);
        JsonElement element = BotUtil.fetchWebsiteJson("https://www.thecolorapi.com/id?hex=" + hex.toUpperCase());

        if (!event.isFromGuild() || GuildUtil.hasPermissions(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_EMBED_LINKS)) {
            event.reply(builder.setColor(color)
                    .setImage("https://singlecolorimage.com/get/" + hex + "/150x150")
                    .setDescription("Hex Code: #" + hex)
                    .setFooter("Name: " + getName(element))
                    .build());
        } else event.reply("Hex Code: #" + hex + ", Name: " + getName(element));
    }

    private String getName(JsonElement element) {
        return element == null || !element.isJsonObject() ? "unknown" : element.getAsJsonObject().get("name").getAsJsonObject().get("value").getAsString();
    }
}
