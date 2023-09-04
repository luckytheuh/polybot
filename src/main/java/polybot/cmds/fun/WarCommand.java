package polybot.cmds.fun;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.PolyBot;
import polybot.commands.*;
import polybot.util.ColorUtil;

import java.awt.Color;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class WarCommand extends SlashCommand {

    private static final EmbedBuilder WAR_EMBED = new EmbedBuilder()
            .setTitle("Declaration of War")
            .setColor(ColorUtil.DND);

    public WarCommand() {
        super("war", Category.FUN, "Declare war on something!");

        this.arguments = "(on)";
        this.argsRequired = true;
        this.cooldownLength = 15;
        this.cooldownUnit = TimeUnit.MINUTES;
        this.cooldownType = CooldownType.GLOBAL;
        this.deleteOption = DeleteOption.DELETE_BOT;
        this.options = Collections.singletonList(new OptionData(OptionType.STRING, "on", "What to declare war on", true));
        this.detailedHelp = """
            *Disclaimer, this is purely visual, nothing is actually declared.*
                    
            **Examples**
            `&war pineapple on pizza`
            `&war skibidi toilet`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.replyEmbeds(getWarEmbed(event.getUser(), event.reqString("on"))).queue();
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        event.getMessage().replyEmbeds(getWarEmbed(event.getAuthor(), event.stringArgs().startsWith("on ") ? event.stringArgs().replaceFirst("on ", "") : event.stringArgs())).queue();
    }

    @Override
    public String getCooldownMessage(String formatted) {
        return "A war can be declared again " + formatted + '!';
    }

    @Override
    public String getNoArgMessage() {
        return "Please specify something to declare war on!";
    }

    private MessageEmbed getWarEmbed(User user, String str) {
        if (str.contains(user.getId())) str = "**themselves <:lololo:861020251788148757>**";

        if (str.contains(PolyBot.getJDA().getSelfUser().getAsMention()) || str.toLowerCase().contains("polybot")) {
            return new EmbedBuilder().setColor(Color.black).setImage("https://media.discordapp.net/attachments/1134243036797358171/1139759353692430376/1127853132991447070.png").build();
        } else return new EmbedBuilder(WAR_EMBED).setDescription(user.getAsMention() + " has declared war on *" + str + "*!").setTimestamp(Instant.now()).build();
    }
}
