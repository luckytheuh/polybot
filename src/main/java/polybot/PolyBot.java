package polybot;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polybot.commands.*;
import polybot.levels.RankCommand;
import polybot.levels.LevelListener;
import polybot.listeners.AutoReportListener;
import polybot.listeners.FilterListener;
import polybot.listeners.ReactionListener;

public class PolyBot {

    //TODO:
    // leaderboard command
    // per user settings

    //TODO: optional things to do:
    // music playback

    private static JDA jda;

    public static void main(String[] args) throws InterruptedException {
        CommandClientBuilder builder = new CommandClientBuilder()
                .addCommands(new SettingCommand(), new RankCommand(), new MergeXPCommand(), new ColorCommand(), new Randowo(), new UserSettingCommand(), new EarliesCommand())
                .useHelpBuilder(false)
                .setActivity(null)
                .setPrefix("!!")
                .setOwnerId(778318296666996796L);

        jda = JDABuilder.create("INSERT TOKEN HERE", GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.SCHEDULED_EVENTS)
                .enableCache(CacheFlag.ONLINE_STATUS)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.listening(" the voices"))
                .setEnableShutdownHook(true)
                .addEventListeners(builder.build(), new LevelListener(), new AutoReportListener(), new FilterListener(), new ReactionListener())
                .build().awaitReady();
    }

    public static Logger getLogger() {
        return LoggerFactory.getLogger(PolyBot.class);
    }

    public static JDA getJDA() {
        return jda;
    }
}
