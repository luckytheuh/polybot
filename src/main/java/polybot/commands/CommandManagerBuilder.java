package polybot.commands;

import java.util.ArrayList;
import java.util.List;

public class CommandManagerBuilder {
    private final List<Command> commands = new ArrayList<>();
    private final List<SlashCommand> slashCommands = new ArrayList<>();
    private final List<ContextMenu> contextMenus = new ArrayList<>();
    private long ownerId = 0;
    private String prefix;
    private String[] altPrefixes = null;

    public CommandManagerBuilder(String prefix) {
        this.prefix = prefix;
    }

    public CommandManagerBuilder addCommand(Command command) {
        this.commands.add(command);
        return this;
    }

    public CommandManagerBuilder addCommands(Command... commands) {
        return addCommands(List.of(commands));
    }

    public CommandManagerBuilder addCommands(List<Command> commands) {
        this.commands.addAll(commands);
        return this;
    }

    public CommandManagerBuilder addSlashCommand(SlashCommand slashCommand) {
        this.slashCommands.add(slashCommand);
        return this;
    }

    public CommandManagerBuilder addSlashCommands(SlashCommand... slashCommands) {
        return addSlashCommands(List.of(slashCommands));
    }

    public CommandManagerBuilder addSlashCommands(List<SlashCommand> slashCommands) {
        this.slashCommands.addAll(slashCommands);
        return this;
    }

    public CommandManagerBuilder addContextMenu(ContextMenu contextMenu) {
        this.contextMenus.add(contextMenu);
        return this;
    }

    public CommandManagerBuilder addContextMenus(ContextMenu... contextMenus) {
        return addContextMenus(List.of(contextMenus));
    }

    public CommandManagerBuilder addContextMenus(List<ContextMenu> contextMenus) {
        this.contextMenus.addAll(contextMenus);
        return this;
    }

    public CommandManagerBuilder setOwnerId(long ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public CommandManagerBuilder setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public CommandManagerBuilder setAltPrefixes(String... altPrefixes) {
        this.altPrefixes = altPrefixes;
        return this;
    }

    public CommandManager build() {
        return new CommandManager(commands, slashCommands, contextMenus, ownerId, prefix, altPrefixes);
    }
}
