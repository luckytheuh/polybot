package polybot.cmds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.Category;
import polybot.commands.CommandEvent;
import polybot.commands.SlashCommand;
import polybot.commands.SlashCommandEvent;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.ColorUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingCommand extends SlashCommand {

    private static final String GET_SETTING = "`%s`\n\n**Value:** `%s`";
    private static final String UPD_SETTING = "Changed `%s`\n\n*Old value:* `%s`\n**New value:** `%s`";

    public SettingCommand() {
        super("settings", Category.UTILITY, "View or change bot settings");

        List<Command.Choice> choices = new ArrayList<>(25);
        for (int i = 0; i < Setting.values().length; i++) {
            if (choices.size() >= 25) break;
            Setting setting = Setting.values()[i];
            if (setting.excludeFromList()) continue;

            choices.add(new Command.Choice(setting.friendlyName(), setting.ordinal()));
        }

        this.isHidden = true;
        this.argsRequired = true;
        this.aliases = new String[]{"setting", "config"};
        this.requiredPermission = Permission.KICK_MEMBERS;
        this.arguments = "(list/setting), [[delete] value]";
        this.options = List.of(
                new OptionData(OptionType.STRING, "setting", "Setting to retrieve", true).addChoices(choices),
                new OptionData(OptionType.STRING, "value", "Value to set/remove"),
                new OptionData(OptionType.BOOLEAN, "delete", "Treat value as what to remove from the setting")
        );
        this.detailedHelp = """
            For a list of available settings, run `&settings list`
                    
            **Examples**
            `&settings lvl up msg, %s is now %s years old!`
            `&settings no xp channels, remove 834437126203768852`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.replyShown(handle(
                Setting.values()[Integer.parseInt(event.reqString("setting"))],
                event.optString("value", null),
                event.optBoolean("delete", false)
        ));
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        Setting setting;
        String value = null;

        if (event.stringArgs().contains(",")) {
            String[] args = event.stringArgs().split(",");

            setting = Setting.searchForSetting(args[0]);
            value = String.join(",", Arrays.copyOfRange(args, 1, args.length));
        } else setting = Setting.searchForSetting(event.stringArgs());

        if (setting == null) {
            //show a help menu on "list"

            if (event.args()[0].startsWith("list")) {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("Available settings")
                        .setDescription("**Non Appendable Settings**\n")
                        .setFooter("To use a placeholder, add %s into the setting")
                        .setColor(ColorUtil.IDLE);

                boolean endOfNonAppend = false;
                for (Setting s : Setting.values()) {
                    if (!endOfNonAppend && s.isAppendable()) {
                        endOfNonAppend = true;
                        builder.appendDescription("**Appendable settings**\n");
                    }
                    builder.appendDescription('`' + s.friendlyName(false) + '`' + (s.hasPlaceholders() ? ", " + s.getTotalPlaceholders() + " placeholder(s)" : "")+ "\n");
                }
                event.reply(builder.setTimestamp(Instant.now()).build());
            } else event.reply("Unknown setting: `" + String.join(" ", event.args()) + "`");
            return;
        }

        event.reply(handle(setting, value, null));
    }

    private MessageEmbed handle(Setting setting, String value, Boolean delete) {
        EmbedBuilder builder = new EmbedBuilder().setColor(ColorUtil.ONLINE);
        String oldValue = BotStorage.getSetting(setting, "null");

        if (value != null) {
            if (value.startsWith(" ")) value = value.substring(1);

            BotStorage.setSetting(setting, value, delete);
            String newValue = BotStorage.getSetting(setting, "null");
            builder.setDescription(String.format(UPD_SETTING, setting.key(), oldValue.isBlank() || oldValue.isEmpty() ? "null" : oldValue, newValue.isBlank() || newValue.isEmpty() ? "null" : newValue));
        } else builder.setDescription(String.format(GET_SETTING, setting.key(), oldValue));

        return builder.setTimestamp(Instant.now()).build();
    }
}
