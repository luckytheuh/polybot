package polybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import polybot.util.GuildUtil;

import java.util.concurrent.TimeUnit;

public abstract class Command {

    protected boolean isHidden = false, argsRequired = false, isOwnerOnly = false, ignoreSendCheck = false, supportsParams = false;
    protected DeleteOption deleteOption = DeleteOption.NONE;
    protected String name, help, arguments, detailedHelp;
    protected long requiredRole, cooldownLength;
    protected Permission requiredPermission;
    protected int totalArgsRequired = 0;
    protected CooldownType cooldownType;
    protected String[] aliases = null;
    protected TimeUnit cooldownUnit;
    protected Category category;
    CommandManager commandManager;

    public Command(String name, String help) {
        this(name, Category.GENERAL, help);
    }

    public Command(String name, Category category, String help) {
        this.category = category;
        this.name = name;
        this.help = help;
    }

    public final void run(CommandEvent event) {
        if (!event.isFromGuild() && (requiredRole != 0 || requiredPermission != null)) return;
        if (isOwnerOnly && !event.isFromBotOwner()) return;
        if (requiredPermission != null && !event.getMember().hasPermission(requiredPermission)) return;
        if (requiredRole != 0 && !GuildUtil.memberHasRole(event.getMember(), requiredRole)) return;

        if (argsRequired) {
            if (event.args() == null) {
                replyAndDelete(event, getNoArgMessage());
                return;
            }

            if (event.args().length < totalArgsRequired) {
                replyAndDelete(event, getNotEnoughArgMessage());
                return;
            }
        }

        if (!event.isFromBotOwner() && cooldownType != null && cooldownUnit != null && cooldownLength != 0) {
            if (!event.isFromGuild() || !event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
                String key = getCooldownKey(event);
                long cd = event.manager().getCooldown(key);

                if (cd > 0) {
                    replyAndDelete(event, getCooldownMessage("<t:" + cd + ":R>"));
                    return;
                } else event.manager().applyCooldown(key, cooldownUnit, cooldownLength);
            }
        }

        fireCommand(event);
    }

    public final String getCooldownKey(CommandEvent event) {
        return switch (cooldownType) {
            case USER -> name + '|' + event.getAuthor().getIdLong();
            case CHANNEL -> name + '|' + event.getChannel().getIdLong();
            case GUILD -> name + '|' + event.getGuild().getIdLong();
            case GLOBAL -> name + "|0";
        };
    }

    public final boolean isHidden() {
        return isHidden;
    }

    public final String getName() {
        return name;
    }

    public final String getHelp() {
        return help;
    }

    public String getDetailedHelp() {
        return detailedHelp == null ? "" : detailedHelp;
    }

    public final String getArguments() {
        return arguments;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isOwnerOnly() {
        return isOwnerOnly;
    }

    public String getCooldownMessage(String formatted) {
        return "This command can be used again " + formatted + "!";
    }

    public String getNoArgMessage() {
        return "This command requires additional parameters!";
    }

    public String getNotEnoughArgMessage() {
        return "This command requires at least " + totalArgsRequired + " parameter" + (totalArgsRequired != 1 ? "s!" : '!');
    }

    final void setCommandManager(CommandManager manager) {
        this.commandManager = manager;
    }

    protected CommandManager getCommandManager() {
        return commandManager;
    }

    protected void replyAndDelete(CommandEvent event, String str) {
        replyAndDelete(event, deleteOption, str);
    }

    protected void replyAndDelete(CommandEvent event, DeleteOption option, String str) {
        if (event.isFromGuild() && !event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_SEND)) return;

        event.getMessage().reply(str).queue(message -> {
            if (option == DeleteOption.NONE) return;

            if (option.delBot) message.delete().queueAfter(5, TimeUnit.SECONDS);
            if (option.delUser) {
                if (!event.isFromGuild()) return;
                event.getMessage().delete().queueAfter(5, TimeUnit.SECONDS);
            }
        }, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE).ignore(InsufficientPermissionException.class));
    }

    protected abstract void fireCommand(CommandEvent event);
}
