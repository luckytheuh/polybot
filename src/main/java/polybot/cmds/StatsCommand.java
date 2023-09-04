package polybot.cmds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import polybot.Constants;
import polybot.PolyBot;
import polybot.commands.*;
import polybot.storage.Setting;
import polybot.storage.UserSetting;
import polybot.util.ColorUtil;
import polybot.util.LevelUtil;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class StatsCommand extends Command {

    public StatsCommand() {
        super("stats", Category.UTILITY, "show cool bot stats");

        this.cooldownLength = 10;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.GLOBAL;
        this.deleteOption = DeleteOption.DELETE_BOTH;
        this.detailedHelp = "Display statistical information about the bot.";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        File[] files = Paths.get("./avatars/").toFile().listFiles(File::isFile);
        String cached = LevelUtil.getCachedAvatars() + "/" + (files != null ? files.length : 0);
        JDA jda = PolyBot.getJDA();

        long uptime = System.currentTimeMillis() - PolyBot.startTime;

        //long millis = uptime % 1000;
        long second = (uptime / 1000) % 60;
        long minute = (uptime / (1000 * 60)) % 60;
        long hour = (uptime / (1000 * 60 * 60)) % 24;
        long days = (uptime / (1000 * 60 * 60 * 24));

        long memUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;

        String description = String.format("Bot Owner: <@%d>\nPrefixes: `%s` | `%s`\nJava Version: `%s`\nMemory: `%dmb`\nPing: `%dms`",
                event.manager().getOwnerId(),
                event.manager().getPrefix(),
                String.join("` | `", event.manager().getAltPrefixes()),
                System.getProperty("java.vm.version", "unknown"),
                memUsage,
                jda.getGatewayPing()
        );

        event.reply(new EmbedBuilder()
                .setTitle(jda.getSelfUser().getName() + " v" + Constants.VERSION)
                .setDescription(description)
                .addField("User Cache", asString(jda.getUsers().size()), true)
                .addField("Avatar Cache", '`' + cached + '`', true)
                .addField("Slash/Commands", "`" + event.manager().getSlashCommands().size() + "/" + event.manager().getCommands().size() + "`", true)
                .addField("User/Settings", "`" + UserSetting.values().length + "|" + Setting.values().length + "`", true)
                .setFooter(String.format("Uptime: %dd %dh %dm %ds", days, hour, minute, second))
                .setThumbnail(jda.getSelfUser().getAvatarUrl())
                .setTimestamp(Instant.now())
                .setColor(ColorUtil.COOLDOWN_REMOVE)
                .build()
        );
    }

    private String asString(int i) {
        return "`" + i + "`";
    }
}
