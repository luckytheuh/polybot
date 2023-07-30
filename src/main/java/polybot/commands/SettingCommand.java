package polybot.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import polybot.PermissionLimitedCommand;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.BotUtil;

import java.awt.Color;
import java.time.Instant;

public class SettingCommand extends PermissionLimitedCommand {

    private static final String GET_SETTING = "`%s`\n\n**Value:** `%s`";
    private static final String UPD_SETTING = "Changed `%s`\n\n*Old value:* `%s`\n**New value:** `%s`";

    public SettingCommand() {
        super(Permission.KICK_MEMBERS);
        this.name = "settings";
        this.aliases = new String[]{"setting", "config"};
    }

    @Override
    protected void executeCommand(CommandEvent event) {
        if (event.getArgs().isBlank() || event.getArgs().isEmpty()) {
            event.getMessage().replyEmbeds(new EmbedBuilder()
                    .setTitle("Settings command")
                    .setDescription("View or change bot settings\n\n" +
                            "Usage: `" + event.getPrefix() + name + " (help/setting), [value]`\n" +
                            "For a list of available settings, run `" + event.getPrefix() + name + " help`\n\n" +
                            "**Examples**\n" +
                            "`" + event.getPrefix() + name + " lvl up message, %s is now %s years old!`\n" +
                            "`" + event.getPrefix() + name + " no xp channels, 804492371575701516`")
                    .setFooter("() indicates a required value, [] indicates an optional value")
                    .setColor(new Color(39, 187, 40))
                    .build()
            ).queue();
            return;
        }

        Setting setting;
        String value = null;

        if (event.getArgs().contains(",")) {
            String[] args = event.getArgs().split(",");


            setting = Setting.searchForSetting(args[0]);
            value = event.getArgs().substring(args[0].length()+1).trim();
        } else setting = Setting.searchForSetting(event.getArgs());

        if (setting == null) {
            //show a help menu on "help"

            if (event.getArgs().startsWith("help")) {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("Available settings")
                        .setDescription("**Non Appendable Settings**\n")
                        .setFooter("To use a placeholder, add %s into the setting")
                        .setColor(BotUtil.IDLE);

                boolean endOfNonAppend = false;
                for (Setting s : Setting.values()) {
                    if (!endOfNonAppend && s.isAppendable()) {
                        endOfNonAppend = true;
                        builder.appendDescription("**Appendable settings**\n");
                    }
                    builder.appendDescription('`' + s.friendlyName(false) + '`' + (s.hasPlaceholders() ? ", " + s.getTotalPlaceholders() + " placeholder(s)" : "")+ "\n");
                }
                event.getMessage().replyEmbeds(builder.setTimestamp(Instant.now()).build()).queue();
            } else event.getMessage().reply("Unknown setting: `" + event.getArgs() + "`").queue();
            return;
        }

        EmbedBuilder builder = new EmbedBuilder().setColor(BotUtil.ONLINE);
        String oldValue = BotStorage.getSetting(setting, "null");

        if (value != null) {
            BotStorage.setSetting(setting, value);
            String newValue = BotStorage.getSetting(setting, "null");
            builder.setDescription(String.format(UPD_SETTING, setting.key(), oldValue.isBlank() || oldValue.isEmpty() ? " " : oldValue, newValue.isBlank() || newValue.isEmpty() ? " " : newValue));
        } else builder.setDescription(String.format(GET_SETTING, setting.key(), oldValue));

        event.getMessage().replyEmbeds(builder.setTimestamp(Instant.now()).build()).queue();
    }
}
