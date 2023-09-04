package polybot.cmds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import polybot.commands.*;
import polybot.storage.BotStorage;
import polybot.storage.UserSetting;
import polybot.storage.UserSettingEntry;
import polybot.util.ColorUtil;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UserSettingCommand extends SlashCommand {

    private static final String INV_VALUE = "**Invalid value for** `%s`**!**\n\n`%s` is not valid, setting will remain unchanged.";
    private static final String UPD_SETTING = "Changed `%s`\n\n*Old value:* `%s`\n**New value:** `%s`";
    private static final String GET_SETTING = "`%s`\n\n**Value:** `%s`";

    public UserSettingCommand() {
        super("usersettings", Category.UTILITY, "View or change user settings");

        this.cooldownLength = 5;
        this.argsRequired = true;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.USER;
        this.arguments = "(list/setting), [value]";
        this.deleteOption = DeleteOption.DELETE_BOT;
        this.aliases = new String[]{"user-settings", "usersetting", "user-setting"};
        this.options = List.of(
                new OptionData(OptionType.STRING, "setting", "Setting to retrieve", true)
                        .addChoices(Arrays.stream(UserSetting.values())
                                .map(setting -> new Command.Choice(setting.friendlyName(), setting.ordinal()))
                                .sorted(Comparator.comparingInt(o -> o.getName().charAt(0)))
                                .toList()
                        ), new OptionData(OptionType.STRING, "value", "Value to set")
        );
        this.detailedHelp = """
            To reset a value to default, run `&usersettings (setting), default`
            For a list of available settings, run `&usersettings list`
            For a list of available fonts, run `&fonts`

            **Examples**
            `&usersettings level card color, 00FF00`
            `&usersettings level card font, comic sans`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.replyShown(getSettingEmbed(event.getUser(), UserSetting.values()[Integer.parseInt(event.reqString("setting"))], event.optString("value", null)));
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        UserSetting setting;
        String value = null;

        if (event.stringArgs().contains(",")) {
            String[] args = event.stringArgs().split(",");

            setting = UserSetting.searchForSetting(args[0]);
            value = event.stringArgs().substring(args[0].length()+1).trim();
        } else setting = UserSetting.searchForSetting(event.stringArgs());

        if (setting == null) {
            //show a help menu on "list"

            if (event.stringArgs().startsWith("list")) {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("Available settings")
                        .setFooter("Use hex codes for colors")
                        .setColor(ColorUtil.IDLE);

                for (UserSetting s : UserSetting.values()) {
                    builder.appendDescription('`' + s.friendlyName(false) + "`, " + s.getDescription() + "\n");
                }
                event.reply(builder.setTimestamp(Instant.now()).build());
            } else {
                event.reply("Unknown setting provided, a list of valid settings can be found via `&usersettings list`.");
            }
            return;
        }

        event.reply(getSettingEmbed(event.getAuthor(), setting, value));
    }

    private MessageEmbed getSettingEmbed(User user, UserSetting setting, String value) {
        EmbedBuilder builder = new EmbedBuilder().setColor(ColorUtil.ONLINE).setTimestamp(Instant.now());
        UserSettingEntry entry = BotStorage.getUserSetting(user.getIdLong(), setting);

        if (value != null) {
            String oldValue = entry.getValue();

            if (!entry.setValue(value)) return builder.setColor(ColorUtil.DND).setDescription(String.format(INV_VALUE, setting.key(), value)).build();

            //get old value, then store the new into the entry
            //save to db and then reply to the embed
            BotStorage.setUserSetting(user.getIdLong(), entry);

            builder.setDescription(String.format(UPD_SETTING, setting.key(), oldValue, entry.getValue()));
        } else builder.setDescription(String.format(GET_SETTING, setting.key(), entry.getValue()));

        return builder.build();
    }
}
