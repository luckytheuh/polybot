package polybot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import polybot.PermissionLimitedCommand;
import polybot.util.BotUtil;
import polybot.levels.LevelEntry;
import polybot.storage.BotStorage;
import polybot.util.UserUtil;

public class MergeXPCommand extends PermissionLimitedCommand {

    public MergeXPCommand() {
        super(Permission.KICK_MEMBERS);
        this.name = "merge";
        this.aliases = new String[]{"transfer"};
    }

    @Override
    protected void executeCommand(CommandEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE) return;

        String[] args = event.getArgs().split(" ");

        if (args.length < 2) {
            // Assume they ran it without any args
            event.getMessage().replyEmbeds(new EmbedBuilder()
                    .setColor(BotUtil.IDLE)
                    .setTitle("Merge XP command")
                    .setDescription("Give a user's XP to another user. \n\nUsage: `!" + name + " (user who will receive the xp) (user to take xp from)")
                    .build()).queue();
            return;
        }

        // Check if we don't have enough information to proceed
        if (args[0].isEmpty() || args[0].isBlank() || args[1].isEmpty() || args[1].isBlank()) {
            event.getMessage().replyEmbeds(failCommand("Please provide two valid users.")).queue();
            return;
        }

        // Check if we have valid users
        User[] users = new User[2];
        for (int i = 0; i < users.length; i++) {
            users[i] = UserUtil.searchForUser(args[i]);
            if (users[i] == null) {
                event.getMessage().replyEmbeds(failCommand("User " + (i+1) + " was not found. Please provide two valid users.")).queue();
                return;
            }

            if (users[i].isBot() || users[i].isSystem()) {
                event.getMessage().replyEmbeds(failCommand("User " + (i+1) + " is either a bot or a system account.")).queue();
                return;
            }
        }

        event.getMessage().replyEmbeds(handleCommand(users[0], users[1])).queue();
    }

    private MessageEmbed failCommand(String message) {
        return new EmbedBuilder()
                .setTitle("Merge failed")
                .setColor(BotUtil.DND)
                .setDescription(message + "\n\nUsers can be provided using: `<@(id)>`, `(id)`, `(username)`\nMake sure to replace `(id)` with the user's id.")
                .build();
    }

    private MessageEmbed handleCommand(User transferTo, User from) {
        LevelEntry entryToTransferTo = BotStorage.getLevelEntry(transferTo.getIdLong());
        LevelEntry entryToRobFrom = BotStorage.getLevelEntry(from.getIdLong());

        if (entryToRobFrom == null) {
            // return saying that it cant transfer xp from someone who doesn't have any
            return new EmbedBuilder()
                    .setColor(BotUtil.DND)
                    .setTitle("Merge failed")
                    .setDescription(from.getAsMention() + " does not have any XP!")
                    .build();
        }

        // ez transfer lol
        if (entryToTransferTo == null) {
            BotStorage.removeLevelEntry(from.getIdLong());
            BotStorage.saveLevelEntry(transferTo.getIdLong(), entryToRobFrom); // It'll be saved with the new user id
        } else {
            // Don't combine levels, as that isn't a good idea
            entryToTransferTo.setXp(entryToTransferTo.getXp() + entryToRobFrom.getXp());

            BotStorage.removeLevelEntry(from.getIdLong());
            BotStorage.saveLevelEntry(transferTo.getIdLong(), entryToTransferTo);
        }

        return new EmbedBuilder()
                .setColor(BotUtil.ONLINE)
                .setTitle("Merge completed")
                .setDescription(from.getAsMention() + "'s xp was merged into " + transferTo.getAsMention() + "'s!")
                .setFooter(UserUtil.getUserAsName(transferTo) + "'s level card will become accurate upon gaining additional xp.")
                .build();
    }
}
