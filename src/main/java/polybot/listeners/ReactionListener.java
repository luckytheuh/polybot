package polybot.listeners;

import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import polybot.PolyBot;
import polybot.util.UserUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class ReactionListener extends ListenerAdapter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
    private static final String LOG_FORMAT = "[%s | %s | %s (%d)] [%s]: %s\n";
    private static final Path DIRECTORY = Paths.get("./reactions");

    private BufferedWriter fileWriter;
    private int totalLines;

    public ReactionListener() {
        initLogFile();
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (!event.isFromGuild()) return;
        if (TimeUtil.getTimeCreated(event.getMessageIdLong()).isBefore(OffsetDateTime.now().minusDays(7))) return;
        //if (!event.getGuild().getPublicRole().hasPermission(event.getGuildChannel(), Permission.MESSAGE_ADD_REACTION)) return;

        log(event.getChannel(), event.getUser(), "REACT", event.getReaction());
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if (!event.isFromGuild()) return;
        if (TimeUtil.getTimeCreated(event.getMessageIdLong()).isBefore(OffsetDateTime.now().minusDays(7))) return;
        //if (!event.getGuild().getPublicRole().hasPermission(event.getGuildChannel(), Permission.MESSAGE_ADD_REACTION)) return;

        log(event.getChannel(), event.getUser(), "UNREACT", event.getReaction());
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        closeLogFile();
    }

    private void log(MessageChannel channel, User user, String type, MessageReaction reaction) {
        String userName = user != null ? UserUtil.getUserAsName(user) : "Unknown";
        long userId = user != null ? user.getIdLong() : -1;

        if (type.equalsIgnoreCase("UNREACT")) {
            PolyBot.getLogger().info("[{} | {} ({})] unreacted {}", channel.getName(), userName, userId, reaction.getEmoji().getName());
        }

        if (fileWriter == null) return;

        try {
            fileWriter.write(String.format(LOG_FORMAT, TIME_FORMATTER.format(LocalDateTime.now()), channel.getName(), userName, userId, type, reaction.getEmoji().getName()));
            fileWriter.flush();
            totalLines++;
        } catch (IOException e) {
            PolyBot.getLogger().warn("Failed to log reaction!");
        }

        if (totalLines >= 15000) initLogFile();
    }

    private void initLogFile() {
        try {
            closeLogFile();

            if (Files.notExists(DIRECTORY)) Files.createDirectory(DIRECTORY);

            fileWriter = Files.newBufferedWriter(DIRECTORY.resolve("reaction." + TIME_FORMATTER.format(LocalDateTime.now()).replace(' ', '_') + ".log"),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            totalLines = 0;
        } catch (IOException e) {
            PolyBot.getLogger().warn("Failed to init reaction log", e);
            fileWriter = null;
        }
    }

    private void closeLogFile() {
        if (fileWriter == null) return;

        try {
            fileWriter.flush();
            fileWriter.close();
            fileWriter = null;
        } catch (IOException e) {
            PolyBot.getLogger().warn("Failed to close reaction log", e);
        }
    }
}
