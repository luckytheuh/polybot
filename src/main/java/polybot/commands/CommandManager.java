package polybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import polybot.PolyBot;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.GuildUtil;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CommandManager implements EventListener {
    private final List<Command> commands;
    private final List<SlashCommand> slashCommands;
    private final List<ContextMenu> contextMenus;
    private final Map<String, OffsetDateTime> cooldownMap = new HashMap<>();
    private final long ownerId;
    private final String prefix;
    private final String[] altPrefixes;

    public CommandManager(List<Command> commands, List<SlashCommand> slashCommands, List<ContextMenu> contextMenus, long ownerId, String prefix, String[] altPrefixes) {
        this.commands = commands;
        this.slashCommands = slashCommands;
        this.contextMenus = contextMenus;
        this.ownerId = ownerId;
        this.altPrefixes = altPrefixes;
        this.prefix = prefix;

        for (Command c : commands) c.setCommandManager(this);
        for (SlashCommand c : slashCommands) c.setCommandManager(this);
        for (ContextMenu c : contextMenus) c.setCommandManager(this);
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof MessageReceivedEvent messageEvent) onMessage(messageEvent);
        else if (event instanceof SlashCommandInteractionEvent slashEvent) onSlash(slashEvent);
        else if (event instanceof MessageContextInteractionEvent interactionEvent) onApp(interactionEvent);
        else if (event instanceof CommandAutoCompleteInteractionEvent autoCompleteEvent) onAutoComplete(autoCompleteEvent);
        else if (event instanceof ReadyEvent readyEvent) onReady(readyEvent);
    }

    public long getCooldown(String name) {
        if (cooldownMap.containsKey(name)) {
            if (cooldownMap.get(name).isBefore(OffsetDateTime.now())) {
                cooldownMap.remove(name);
                return 0;
            }

            return cooldownMap.get(name).toEpochSecond();
        }

        return 0;
    }

    public void applyCooldown(String name, TimeUnit unit, long duration) {
        cooldownMap.put(name, OffsetDateTime.now().plusSeconds(unit.toSeconds(duration)));
    }

    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public List<Command> getCommandsWithCategory(Category category) {
        List<Command> commandList = new ArrayList<>(commands);
        commandList.removeIf(command -> command.category != category);
        return commandList;
    }

    public Command getCommandWithName(String name, boolean includeHidden) {
        for (Command command : commands) {
            if (!includeHidden && (command.isOwnerOnly || command.isHidden)) continue;

            if (isRightCommand(name, command)) return command;
        }
        return null;
    }

    public List<SlashCommand> getSlashCommands() {
        return Collections.unmodifiableList(slashCommands);
    }

    public List<SlashCommand> getSlashCommandsWithCategory(Category category) {
        List<SlashCommand> slashCommandList = new ArrayList<>(slashCommands);
        slashCommandList.removeIf(command -> command.category != category);
        return slashCommandList;
    }

    private void onMessage(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Map<String, String> blacklistMap = BotStorage.getSettingAsMap(Setting.CHANNEL_BLACKLIST);
        if (event.isFromGuild() && !isModerator(event.getMember())) {
            if (blacklistMap.containsKey(event.getChannel().getId()) && blacklistMap.get(event.getChannel().getId()) == null) return;
        }
        Category channelBlacklistedFrom = Category.searchFromName(blacklistMap.get(event.getChannel().getId()));

        blacklistMap = BotStorage.getSettingAsMap(Setting.USER_BLACKLIST);
        if (blacklistMap.containsKey(event.getAuthor().getId()) && blacklistMap.get(event.getAuthor().getId()) == null) return;
        Category blacklistedFrom = Category.searchFromName(blacklistMap.get(event.getAuthor().getId()));

        boolean cantSendMessages = event.isFromGuild() && !GuildUtil.hasPermissions(event.getGuildChannel(), Permission.MESSAGE_SEND);

        String usedPrefix = isRightPrefix(event.getMessage().getContentRaw());

        if (usedPrefix == null) return;
        String[] args = event.getMessage().getContentRaw().split(" ");
        String command = args[0].substring(usedPrefix.length()).trim();

        if (command.isBlank() || command.isEmpty()) return;
        for (Command cmd : commands) {
            if (!isRightCommand(command, cmd)) continue;
            if (cmd.category == blacklistedFrom || (!isModerator(event.getMember()) && cmd.category == channelBlacklistedFrom)) return;
            if (!cmd.ignoreSendCheck) {
                if (cantSendMessages) return;

                if (event.isFromGuild()) {
                    // if excluded from mod check, continue
                    if (!BotStorage.getSettingAsList(Setting.EXCLUDED_FROM_MOD_CHECK).contains(event.getChannel().getId())) {
                        // if the robot role cant talk here, but we can and a mod isn't using us, return
                        Role botRole = GuildUtil.getRoleFromSetting(event.getGuild(), Setting.ROBOT_ROLE);
                        if (botRole != null && !botRole.hasPermission(event.getGuildChannel(), Permission.MESSAGE_SEND) && !isModerator(event.getMember()))
                            return;
                    }
                }
            }

            if (args.length == 1) args = null;
            else args = Arrays.copyOfRange(args, 1, args.length);

            cmd.run(new CommandEvent(this, event, usedPrefix, command, args != null ? String.join(" ", args) : "", args));
            break;
        }
    }

    private void onSlash(SlashCommandInteractionEvent event) {
        Map<String, String> blacklistMap = BotStorage.getSettingAsMap(Setting.CHANNEL_BLACKLIST);
        if (event.isFromGuild() && !isModerator(event.getMember())) {
            if (blacklistMap.containsKey(event.getChannel().getId()) && blacklistMap.get(event.getChannel().getId()) == null) return;
        }
        Category channelBlacklistedFrom = Category.searchFromName(blacklistMap.get(event.getChannel().getId()));
        
        blacklistMap = BotStorage.getSettingAsMap(Setting.USER_BLACKLIST);
        if (blacklistMap.containsKey(event.getUser().getId()) && blacklistMap.get(event.getUser().getId()) == null) return;
        Category blacklistedFrom = Category.searchFromName(blacklistMap.get(event.getUser().getId()));

        for (SlashCommand slashCommand : slashCommands) {
            if (!event.getName().equalsIgnoreCase(slashCommand.name)) continue;
            if (slashCommand.category == blacklistedFrom || (!isModerator(event.getMember()) && slashCommand.category == channelBlacklistedFrom)) return;

            slashCommand.run(new SlashCommandEvent(this, event));
            break;
        }
    }

    private void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        for (SlashCommand slashCommand : slashCommands) {
            if (!event.getName().equalsIgnoreCase(slashCommand.name)) continue;
            if (slashCommand.autoCompleteMap == null) continue;

            List<String> autoCompleteList = slashCommand.autoCompleteMap.getOrDefault(event.getFocusedOption().getName(), null);
            if (autoCompleteList == null) break;

            List<net.dv8tion.jda.api.interactions.commands.Command.Choice> options = autoCompleteList.stream()
                    .filter(word -> word.startsWith(event.getFocusedOption().getValue()))
                    .map(word -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(word, word))
                    .limit(25)
                    .toList();

            event.replyChoices(options).queue();
            break;
        }
    }

    private void onApp(MessageContextInteractionEvent event) {
        for (ContextMenu contextMenu : contextMenus) {
            if (!event.getName().equalsIgnoreCase(contextMenu.menuName)) continue;

            contextMenu.run(new ContextMenuEvent(this, event));
            break;
        }
    }

    private void onReady(ReadyEvent event) {
        List<CommandData> data = new ArrayList<>();

        for (SlashCommand command : slashCommands) data.add(command.getCommandData());
        for (ContextMenu menu : contextMenus) data.add(menu.getCommandData());

        event.getJDA().updateCommands().addCommands(data).queue(cmds -> PolyBot.getLogger().debug("Added " + cmds.size() + " interaction(s)!"));
    }

    private String isRightPrefix(String str) {
        if (str.startsWith(prefix)) return prefix;

        if (altPrefixes == null) return null;

        for (String pref : altPrefixes) {
            if (str.startsWith(pref)) return pref;
        }
        return null;
    }

    private boolean isRightCommand(String str, Command command) {
        if (str.equalsIgnoreCase(command.name)) return true;

        if (command.aliases != null) {
            for (String alias : command.aliases) {
                if (str.equalsIgnoreCase(alias)) return true;
            }
        }

        return false;
    }

    private boolean isModerator(Member member) {
        return member != null && member.hasPermission(Permission.KICK_MEMBERS);
    }

    public long getOwnerId() {
        return ownerId;
    }

    public String getPrefix() {
        return prefix;
    }

    public String[] getAltPrefixes() {
        return altPrefixes;
    }

}
