package polybot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import polybot.util.GuildUtil;

public abstract class RoleLimitedCommand extends Command {

    protected long restrictedToRoleId;

    public RoleLimitedCommand(long restrictedToRoleId) {
        this.restrictedToRoleId = restrictedToRoleId;
    }

    @Override
    protected final void execute(CommandEvent event) {
        if (!event.getChannelType().isGuild()) return; // If this was not from a guild
        if (!GuildUtil.memberHasRole(event.getMember(), restrictedToRoleId)) return; // If they don't have the required role

        executeCommand(event);
    }

    protected abstract void executeCommand(CommandEvent event);
}
