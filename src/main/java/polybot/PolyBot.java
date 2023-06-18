package polybot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polybot.levels.LevelListener;

public class PolyBot {

    //TODO:
    // leveling system (implemented, untested)
    // rank command (graphics2d, 64x64 image, progress bar, and stuff using the api)
    // basic auto mod
    // nyan commands

    //TODO: optional things to do:
    // music playback

    private static JDA jda;

    public static void main(String[] args) throws InterruptedException {
        jda = JDABuilder.create("token goes here", GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.listening("to your thoughts"))
                .setEnableShutdownHook(true)
                .addEventListeners(new LevelListener())
                .build().awaitReady();
    }

    public static Logger getLogger() {
        return LoggerFactory.getLogger(PolyBot.class);
    }

    public static JDA getJDA() {
        return jda;
    }
}
