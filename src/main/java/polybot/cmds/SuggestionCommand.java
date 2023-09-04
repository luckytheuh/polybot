package polybot.cmds;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.PolyBot;
import polybot.commands.*;
import polybot.util.UserUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SuggestionCommand extends SlashCommand {

    private static final String SUGGESTION = "%s (%s): %s\n";
    private static final Path SUGGESTION_FILE_PATH = Paths.get("./suggestions.diff");

    private final BufferedWriter writer;

    public SuggestionCommand() {
        super("suggest", Category.GENERAL, "Suggest something for the bot with a cool down of 60 minutes");

        this.argsRequired = true;
        this.cooldownLength = 30;
        this.ignoreSendCheck = true;
        this.arguments = "(suggestion)";
        this.cooldownUnit = TimeUnit.MINUTES;
        this.cooldownType = CooldownType.USER;
        this.deleteOption = DeleteOption.DELETE_BOTH;
        this.options = Collections.singletonList(new OptionData(OptionType.STRING, "suggestion", "The suggestion to provide.", true));
        this.detailedHelp = """
            The suggestion will be sent to a file that the bot owner can then access.
            Abuse of this system will result in being blacklisted from suggesting things!

            **Examples**
            `&suggest the gmod font so i can act like an npc`""";

        try {
            writer = Files.newBufferedWriter(SUGGESTION_FILE_PATH, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { writer.close(); } catch (IOException e) { PolyBot.getLogger().error("Failed closing suggestion writer", e); }
        }));
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.replyHidden("Sent!");
        tryWrite(event.getUser(), event.reqString("suggestion"));
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (event.isFromGuild()) event.getMessage().delete().queue();
        tryWrite(event.getAuthor(), event.stringArgs());
    }

    private void tryWrite(User user, String str) {
        try {
            writer.append(String.format(SUGGESTION, UserUtil.getUserAsName(user), user.getId(), str));
            writer.flush();
        } catch (IOException ignored) {}
    }

    @Override
    public String getNoArgMessage() {
        return "Please provide a suggestion!";
    }
}
