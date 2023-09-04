package polybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import polybot.util.GuildUtil;

public abstract class ContextMenu extends polybot.commands.Command {

    protected String menuName;

    public ContextMenu(String name, String menuName, String help) {
        super(name, help);
        this.menuName = menuName;
    }

    public final void run(ContextMenuEvent event) {
        if (!event.isFromGuild() && (requiredRole != 0 || requiredPermission != null)) return;
        if (isOwnerOnly && !event.isFromBotOwner()) return;
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

        fireContextMenu(event);
    }

    public CommandData getCommandData() {
        CommandData data = Commands.context(Command.Type.MESSAGE, menuName);

        if (requiredPermission != null) data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(requiredPermission));
        else data.setDefaultPermissions(DefaultMemberPermissions.ENABLED);

        return data;
    }

    public final String getCooldownKey(ContextMenuEvent event) {
        return switch (cooldownType) {
            case USER -> name + '|' + event.getUser().getIdLong();
            case CHANNEL -> name + '|' + event.getChannel().getIdLong();
            case GUILD -> name + '|' + event.getGuild().getIdLong();
            case GLOBAL -> name + "|0";
        };
    }

    private void terminate(ContextMenuEvent event, String message) {
        event.reply(message).setEphemeral(true).queue();
    }

    protected abstract void fireContextMenu(ContextMenuEvent event);
}
