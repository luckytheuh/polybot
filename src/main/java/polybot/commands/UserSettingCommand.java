package polybot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CooldownScope;
import net.dv8tion.jda.api.EmbedBuilder;
import polybot.storage.BotStorage;
import polybot.storage.UserSetting;
import polybot.storage.UserSettingEntry;
import polybot.util.BotUtil;

import java.awt.Color;
import java.time.Instant;

public class UserSettingCommand extends Command {

    private static final String GET_SETTING = "`%s`\n\n**Value:** `%s`";
    private static final String UPD_SETTING = "Changed `%s`\n\n*Old value:* `%s`\n**New value:** `%s`";

    public UserSettingCommand() {
        this.name = "usersettings";
        this.aliases = new String[]{"user-settings", "usersetting", "user-setting"};
        this.cooldown = 5;
        this.cooldownScope = CooldownScope.USER;
    }

    //for user settings, values will be limited

    @Override
    protected void execute(CommandEvent event) {
/*
        if (!event.isOwner()) {
            event.reply(":boar:");
            return;
        }

*/

        if (event.getArgs().isBlank() || event.getArgs().isEmpty()) {
            event.getMessage().replyEmbeds(new EmbedBuilder()
                    .setTitle("User Settings Command")
                    .setDescription("View or change user settings\n\n" +
                            "Usage: `" + event.getPrefix() + name + " (help/setting), [value]`\n" +
                            "For a list of available settings, run `" + event.getPrefix() + name + " help`\n\n" +
                            "**Examples**\n" +
                            "`" + event.getPrefix() + name + " level card theme, 00FF00`\n" +
                            "`" + event.getPrefix() + name + " level card bg, false`")
                    .setFooter("() indicates a required value, [] indicates an optional value")
                    .setColor(new Color(39, 187, 40))
                    .build()
            ).queue();
            return;
        }

        UserSetting setting;
        String value = null;

        if (event.getArgs().contains(",")) {
            String[] args = event.getArgs().split(",");


            setting = UserSetting.searchForSetting(args[0]);
            value = event.getArgs().substring(args[0].length()+1).trim();
        } else setting = UserSetting.searchForSetting(event.getArgs());

        if (setting == null) {
            //show a help menu on "help"

            if (event.getArgs().startsWith("help")) {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("Available settings")
                        //.setDescription("**Non Appendable Settings**\n")
                        .setFooter("To use a placeholder, add %s into the setting")
                        .setColor(BotUtil.IDLE);


                for (UserSetting s : UserSetting.values()) {
                    builder.appendDescription('`' + s.friendlyName(false) + "`, " + s.getDescription() + "\n");
                }
                event.getMessage().replyEmbeds(builder.setTimestamp(Instant.now()).build()).queue();
            } else event.getMessage().reply("Unknown setting: `" + event.getArgs() + "`").queue();
            return;
        }

        EmbedBuilder builder = new EmbedBuilder().setColor(BotUtil.ONLINE);
        UserSettingEntry entry = BotStorage.getUserSetting(event.getAuthor().getIdLong(), setting);

        if (value != null) {

            String oldValue = entry.getValue();
            boolean valid = entry.setValue(value);

            if (!valid) {
                //TODO: error out saying its an invalid choice for the setting
                return;
            }

            //get old value, then store the new into the entry
            //save to db and then reply to the embed
            BotStorage.setUserSetting(event.getAuthor().getIdLong(), entry);

            builder.setDescription(String.format(UPD_SETTING, setting.key(), oldValue, entry.getValue()));
        } else builder.setDescription(String.format(GET_SETTING, setting.key(), entry.getValue()));

        event.getMessage().replyEmbeds(builder.setTimestamp(Instant.now()).build()).queue();
    }
}
