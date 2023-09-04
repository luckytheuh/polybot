package polybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import polybot.util.GuildUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SlashCommand extends Command {

    protected List<OptionData> options = null;
    protected Map<String, List<String>> autoCompleteMap = null;

    public SlashCommand(String name, String help) {
        super(name, help);
    }

    public SlashCommand(String name, Category category, String help) {
        super(name, category, help);
    }

    protected final void run(SlashCommandEvent event) {
        if (!event.isFromGuild() && (requiredRole != 0 || requiredPermission != null)) {
            terminate(event, "This command only works in a guild!");
            return;
        }

        if (requiredPermission != null && !event.getMember().hasPermission(requiredPermission)) return;
        if (requiredRole != 0 && !GuildUtil.memberHasRole(event.getMember(), requiredRole)) return;

        if (!event.isFromBotOwner() && cooldownType != null && cooldownUnit != null && cooldownLength != 0) {
            if (!event.isFromGuild() || !event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
                String key = getCooldownKey(event);
                long cd = event.manager().getCooldown(key);

                if (cd > 0) {
                    terminate(event, getCooldownMessage("<t:" + cd + ":R>"));
                    return;
                } else event.manager().applyCooldown(key, cooldownUnit, cooldownLength);
            }
        }

        fireSlashCommand(event);
    }

    protected void addToAutoCompleteList(String optionName, String... values) {
        addToAutoCompleteList(optionName, List.of(values));
    }

    protected void addToAutoCompleteList(String optionName, List<String> values) {
        if (autoCompleteMap == null) autoCompleteMap = new HashMap<>();
        autoCompleteMap.put(optionName, values);
    }


    protected abstract void fireSlashCommand(SlashCommandEvent event);

    public CommandData getCommandData() {
        SlashCommandData data = Commands.slash(name, help);

        if (options != null) data.addOptions(options);

        if (requiredPermission != null) data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(requiredPermission));
        else data.setDefaultPermissions(DefaultMemberPermissions.ENABLED);

        return data;
    }

    public final String getCooldownKey(SlashCommandEvent event) {
        return switch (cooldownType) {
            case USER -> name + '|' + event.getUser().getIdLong();
            case CHANNEL -> name + '|' + event.getChannel().getIdLong();
            case GUILD -> name + '|' + event.getGuild().getIdLong();
            case GLOBAL -> name + "|0";
        };
    }

    private void terminate(SlashCommandEvent event, String str) {
        event.reply(str).setEphemeral(true).queue();
    }
}
