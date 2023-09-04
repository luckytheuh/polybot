package polybot.cmds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import polybot.Constants;
import polybot.commands.*;
import polybot.util.ColorUtil;
import polybot.util.GuildUtil;
import polybot.util.WordUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
//new Color(39, 187, 40)
@ListenerCommandCombo
public class HelpCommand extends SlashCommand implements EventListener {
    private static final String COMMAND_TEMPLATE = "`%s`\n%s";
    private static final EmbedBuilder BUILDER_TEMPLATE = new EmbedBuilder()
            .setColor(ColorUtil.ONLINE).setFooter("() indicates a required value, [] indicates an optional value");

    public HelpCommand() {
        super("help", "self explanatory");

        this.options = Collections.singletonList(new OptionData(OptionType.STRING, "command", "Fetch help on a specific command", false, true));
        this.detailedHelp = """
            Provide the name or alias of a command in the arguments to view a more detailed help menu for that command.
            
            **Example**: `&help usersettings`
            """;
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        boolean isStaff = event.isFromGuild() && event.getChannel().asTextChannel().getParentCategoryIdLong() == Constants.STAFF_CATEGORY;

        ReplyCallbackAction action = event.replyEmbeds(getHelpEmbed(Category.GENERAL, event.optString("command", null), isStaff));
        if (event.optString("command", null) == null) action.addActionRow(createHelpSelectMenu(Category.GENERAL, isStaff, false));
        action.queue();
    }


    @Override
    protected void fireCommand(CommandEvent event) {
        boolean isStaff = event.isFromGuild() && event.getMessage().getCategory().getIdLong() == Constants.STAFF_CATEGORY;

        MessageCreateAction action;

        if (!event.isFromGuild() || GuildUtil.hasPermissions(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_EMBED_LINKS))
            action = event.getMessage().replyEmbeds(getHelpEmbed(Category.GENERAL, event.stringArgs(), isStaff));
        else action = event.getMessage().reply(getHelpMessage(Category.GENERAL, event.stringArgs(), isStaff));

        if (event.args() == null) action.addActionRow(createHelpSelectMenu(Category.GENERAL, isStaff, false));
        action.queue();
    }

    private void onMenuInteract(StringSelectInteractionEvent event) { //If menu is over 15 minutes old and someone tried to interact, disable menu and reply with error
        if (event.getComponentId().startsWith("help")) {
            boolean isStaff = event.isFromGuild() && event.getChannel().asTextChannel().getParentCategoryIdLong() == Constants.STAFF_CATEGORY;
            Category category = Category.values()[Integer.parseInt(event.getValues().get(0))];

            MessageEditCallbackAction action;

            if (event.getMessage().getEmbeds().isEmpty()) action = event.editMessage(getHelpMessage(category, null, isStaff));
            else action = event.editMessageEmbeds(getHelpEmbed(category, null, isStaff));

            action.setComponents(ActionRow.of(createHelpSelectMenu(category, isStaff, false))).queue();
        }
    }

    private MessageEmbed getHelpEmbed(Category category, String commandName, boolean isStaff) {
        if (commandName != null && !commandName.isBlank() && !commandName.isEmpty()) {
            Command command = getCommandManager().getCommandWithName(commandName, true);

            if (command == null) {
                return new EmbedBuilder()
                        .setDescription("Unknown command: " + commandName)
                        .setColor(ColorUtil.DND)
                        .build();
            }

            return new EmbedBuilder(BUILDER_TEMPLATE)
                    .setTitle(WordUtil.uppercaseFirst(command.getName()) + " Help")
                    .setDescription(String.format("%s\nUsage: `%s`\n\n%s", command.getHelp(), format(command), command.getDetailedHelp()))
                    .build();
        }

        List<Command> commandList = new ArrayList<>(getCommandManager().getCommandsWithCategory(category));
        commandList.sort(Comparator.comparingInt(value -> value.getName().charAt(0)));
        commandList.removeIf(Command::isOwnerOnly);

        if (!isStaff) commandList.removeIf(Command::isHidden);

        EmbedBuilder builder = new EmbedBuilder(BUILDER_TEMPLATE).setTitle(WordUtil.uppercaseFirst(category.name()) + " Commands");
        for (Command command : commandList) {
            builder.addField(format(command), command.getHelp(), true);
        }

        return builder.build();
    }

    private String getHelpMessage(Category category, String commandName, boolean isStaff) {
        if (commandName != null && !commandName.isBlank() && !commandName.isEmpty()) {
            Command command = getCommandManager().getCommandWithName(commandName, isStaff);

            if (command == null) {
                return "Unknown command: ".concat(commandName);
            }

            return String.format("## %s Help \n%s\nUsage: `%s`\n\n%s", WordUtil.uppercaseFirst(command.getName()), command.getHelp(), format(command), command.getDetailedHelp());
        }

        List<Command> commandList = new ArrayList<>(getCommandManager().getCommandsWithCategory(category));
        commandList.sort(Comparator.comparingInt(value -> value.getName().charAt(0)));
        commandList.removeIf(Command::isOwnerOnly);

        if (!isStaff) commandList.removeIf(Command::isHidden);

        StringBuilder builder = new StringBuilder("**Command List**\n\n");
        for (Command command : commandList) {

            builder.append(String.format("`%s`\n%s", format(command), command.getHelp()));
            builder.append("\n\n");
        }

        return builder.toString();
    }

    private StringSelectMenu createHelpSelectMenu(Category def, boolean isMod, boolean isDisabled) {
        StringSelectMenu.Builder builder = StringSelectMenu.create("help").setDisabled(isDisabled);

        for (Category cat : Category.values()) {
            if (!isMod && cat == Category.MODERATOR) continue;

            builder.addOptions(SelectOption.of(WordUtil.uppercaseFirst(cat.name()), String.valueOf(cat.ordinal()))
                    .withDescription(cat.getDescription())
                    .withDefault(cat == def));
        }

        return builder.build();
    }

    private String format(Command command) {
        return getCommandManager().getPrefix() + command.getName() + (command.getArguments() == null ? "" : ' ' + command.getArguments());
    }

    @Override
    public CommandData getCommandData() {
        this.addToAutoCompleteList("command", getCommandManager().getCommands().stream().map(Command::getName).toList());
        return super.getCommandData();
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof StringSelectInteractionEvent interactionEvent) onMenuInteract(interactionEvent);
    }
}
