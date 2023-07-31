package polybot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.Permission;

public abstract class PermissionLimitedCommand extends Command {

    protected Permission permission;

    public PermissionLimitedCommand(Permission permission) {
        this.permission = permission;
        this.guildOnly = false;
    }

    @Override
    protected final void execute(CommandEvent event) {
        if (!event.getChannelType().isGuild()) return; // If this was not from a guild
        if (!event.isOwner()) if (!event.getMember().hasPermission(permission)) return; // If they don't have the required role

        executeCommand(event);
    }

    protected abstract void executeCommand(CommandEvent event);
}
