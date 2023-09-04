package polybot.cmds.fun;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.EmbedBuilder;
import polybot.commands.*;
import polybot.storage.Configuration;
import polybot.util.BotUtil;
import polybot.util.ColorUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DogCommand extends SlashCommand {

    public DogCommand() {
        super("dog", Category.FUN, "Get a random image of a dog");

        this.cooldownLength = 10;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.CHANNEL;
        this.aliases = new String[]{"canine"};
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.deferReply().queue(hook -> {
            String link = getRandomDog();
            if (link == null) {
                hook.editOriginal("Failed to find a dog image..").queue();
                return;
            }

            hook.editOriginalEmbeds(new EmbedBuilder().setColor(ColorUtil.COOLDOWN_ADD).setImage(link).build()).queue();
        });
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        CompletableFuture.runAsync(() -> {
            String link = getRandomDog();
            if (link == null) {
                replyAndDelete(event, "Failed to find a dog image..");
                return;
            }

            event.reply(new EmbedBuilder().setColor(ColorUtil.COOLDOWN_ADD).setImage(link).build());
        });
    }

    private String getRandomDog() {
        JsonElement element = BotUtil.fetchWebsiteJson("https://api.thedogapi.com/v1/images/search" + "?api_key=" + Configuration.getSetting("dog-api-key", ""));

        if (element != null && element.isJsonArray())
            return element.getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
        return null;
    }
}
