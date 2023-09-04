package polybot.cmds.fun;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.EmbedBuilder;
import polybot.Constants;
import polybot.commands.*;
import polybot.storage.Configuration;
import polybot.util.BotUtil;
import polybot.util.ColorUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CatCommand extends SlashCommand {

    public CatCommand() {
        super("cat", Category.FUN, "Get a random image of a cat");

        this.cooldownLength = 10;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.CHANNEL;
        this.aliases = new String[]{"kitty", "feline"};
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.deferReply().queue(hook -> {
            String link = getRandomCat();
            if (link == null) {
                hook.editOriginal("Failed to find a cat image..").queue();
                return;
            }

            hook.editOriginalEmbeds(new EmbedBuilder().setColor(ColorUtil.COOLDOWN_ADD).setImage(link).build()).queue();
        });
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        CompletableFuture.runAsync(() -> {
            String link = getRandomCat();
            if (link == null) {
                replyAndDelete(event, "Failed to find a cat image..");
                return;
            }

            event.reply(new EmbedBuilder().setColor(ColorUtil.COOLDOWN_ADD).setImage(link).build());
        });
    }

    private String getRandomCat() {
        if (ThreadLocalRandom.current().nextDouble(0, 1000) < 1)
            return CatApis.RORY.run();

        return CatApis.values()[ThreadLocalRandom.current().nextInt(1, CatApis.values().length)].run();
    }

    private enum CatApis {
        RORY(true, "https://rory.cat/purr", url -> {
            JsonElement element = BotUtil.fetchWebsiteJson(url);

            if (element != null && element.isJsonObject() && element.getAsJsonObject().has("url"))
                return element.getAsJsonObject().get("url").getAsString();
            return null;
        }), CATAAS(false, "https://cataas.com/cat?json=true", url -> {
            JsonElement element = BotUtil.fetchWebsiteJson(url);

            if (element != null && element.isJsonObject() && element.getAsJsonObject().has("url"))
                return "https://cataas.com" + element.getAsJsonObject().get("url").getAsString();
            return null;
        }), ESM(false, "https://files.projectlounge.pw/cta/", url -> {
            try {
                HttpURLConnection connection = connect(url);

                String link = connection.getHeaderField("Location");
                connection.getInputStream().close();
                return link == null ? null : "https://files.projectlounge.pw" + link;
            } catch (IOException ignored) {}
            return null;
        }), THE_CAT_API(false, "https://api.thecatapi.com/v1/images/search", url -> {
            JsonElement element = BotUtil.fetchWebsiteJson(url + "?api_key=" + Configuration.getSetting("cat-api-key", ""));

            if (element != null && element.isJsonArray())
                return element.getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
            return null;
        });

        final Function<String, String> catFunc;
        final boolean rare;
        final String url;

        CatApis(boolean rare, String url, Function<String, String> catFunc) {
            this.catFunc = catFunc;
            this.rare = rare;
            this.url = url;
        }

        String run() {
            return catFunc.apply(url);
        }

        static HttpURLConnection connect(String url) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.addRequestProperty("User-Agent", "PolyBot v" + Constants.VERSION);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.connect();
            return connection;
        }
    }
}
