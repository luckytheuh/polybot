package polybot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polybot.commands.*;
import polybot.storage.BotStorage;
import polybot.storage.Configuration;
import polybot.storage.Fonts;
import polybot.storage.Setting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.*;

public class PolyBot {

    //TODO:
    // custom level images (LevelImage enum like Fonts with the command to display)
    // audio file to a video (display something for a single frame, maybe the name of the original file, and the duration?)
    // all media commands share the same limitations (emojis, frame cap 1.5k, 2.5k)
    // mediafetch command

    public static final long startTime = System.currentTimeMillis();

    private static final Logger LOGGER = LoggerFactory.getLogger(PolyBot.class);
    private static final List<EventListener> LISTENERS = new ArrayList<>();
    private static JDA jda;

    // Load in all console and bot commands automatically
    static {
        Reflections reflections = new Reflections("polybot.cmds");
        Set<Class<? extends Command>> commands = reflections.getSubTypesOf(Command.class);

        reflections = new Reflections("polybot.listeners");
        Set<Class<? extends EventListener>> listeners = reflections.getSubTypesOf(EventListener.class);
        commands.removeIf(c -> c.isAnnotationPresent(ExcludeCommand.class));

        CommandManagerBuilder builder = new CommandManagerBuilder("&")
                .setAltPrefixes("!!")
                .setOwnerId(778318296666996796L);

        for (Class<? extends Command> clazz : commands) {
            try {
                if (Modifier.isAbstract(clazz.getModifiers())) continue;
                Command command = clazz.getDeclaredConstructor().newInstance();

                builder.addCommand(command);
                if (command instanceof SlashCommand slashCommand) builder.addSlashCommand(slashCommand);
                if (command instanceof ContextMenu contextMenu) builder.addContextMenu(contextMenu);
                if (clazz.isAnnotationPresent(ListenerCommandCombo.class)) LISTENERS.add((EventListener) command);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        for (Class<? extends EventListener> clazz : listeners) {
            try {
                if (Modifier.isAbstract(clazz.getModifiers())) continue;

                LISTENERS.add(clazz.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        LISTENERS.add(builder.build());
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        for (Fonts f : Fonts.values()) {
            getLogger().debug(f.friendlyName());
        }

        for (ConsoleCommand command : ConsoleCommand.values()) {
            getLogger().debug(command.name());
        }

        jda = JDABuilder.create(Configuration.getSetting("discord-bot-token", ""), GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES)
                .setActivity(Activity.listening(BotStorage.getSetting(Setting.ACTIVITY_STATUS, "the voices")))
                .disableCache(List.of(CacheFlag.values()))
                .setChunkingFilter(ChunkingFilter.ALL)
                .setBulkDeleteSplittingEnabled(false)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.ONLINE_STATUS)
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(LISTENERS.toArray())
                .setEnableShutdownHook(System.getProperty("os.name").contains("win")) //dev reason
                .build().awaitReady();

        long currentMili = System.currentTimeMillis();
        LOGGER.info("Bot started in {}ms", (currentMili - startTime));

        Scanner scanner = new Scanner(System.in);
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                for (ConsoleCommand command : ConsoleCommand.values()) {
                    if (!line.startsWith(command.name().toLowerCase().replaceAll("[-_]", ""))) continue;

                    try {
                        command.run(line.substring(command.name().length()).trim());
                    } catch (Exception e) {
                        getLogger().error("Error thrown from console command", e);
                    }
                }

            }
        } catch (NoSuchElementException ignored) {}
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static JDA getJDA() {
        return jda;
    }

    public static boolean isSelfUser(User user) {
        return user.getIdLong() == jda.getSelfUser().getIdLong();
    }
}
