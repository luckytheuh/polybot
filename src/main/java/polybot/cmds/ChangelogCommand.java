package polybot.cmds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import polybot.Constants;
import polybot.PolyBot;
import polybot.commands.CommandEvent;
import polybot.commands.CooldownType;
import polybot.commands.SlashCommand;
import polybot.commands.SlashCommandEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class ChangelogCommand extends SlashCommand {
    private static final Path CHANGE_LOG_PATH = Paths.get("./changelog.txt");

    private Instant CHANGELOG_UPD_TIME;
    private String CHANGELOG;

    public ChangelogCommand() {
        super("changelog", "Display the most recent changes done to the bot.");

        this.loadChangeLog();
        this.cooldownLength = 60;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.GLOBAL;
        this.aliases = new String[]{"changelogs"};
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.replyShown(getChangeLogEmbed());
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (event.isFromBotOwner() && event.args() != null && event.args()[0].equalsIgnoreCase("reload")) {
            event.getMessage().delete().queue();
            loadChangeLog();
            return;
        }

        event.reply(getChangeLogEmbed());
    }

    private void loadChangeLog() {
        try {
            CHANGELOG = String.format(String.join("\n", Files.readAllLines(CHANGE_LOG_PATH)), Constants.VERSION);
            BasicFileAttributes attributes = Files.readAttributes(CHANGE_LOG_PATH, BasicFileAttributes.class);
            CHANGELOG_UPD_TIME = attributes.lastModifiedTime().toInstant();
        } catch (IOException e) {
            PolyBot.getLogger().warn("Failed to load changelogs", e);
            CHANGELOG = "Failed to load changelog for v" + Constants.VERSION;
            CHANGELOG_UPD_TIME = Instant.now();
        }
    }

    private MessageEmbed getChangeLogEmbed() {
        return new EmbedBuilder()
                .setTitle("Changelog")
                .setDescription(CHANGELOG)
                .setColor(0x48baac)
                .setTimestamp(CHANGELOG_UPD_TIME)
                .build();
    }
}
