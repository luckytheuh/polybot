package polybot.cmds.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import polybot.PolyBot;
import polybot.commands.Category;
import polybot.commands.Command;
import polybot.commands.CommandEvent;
import polybot.LevelEntry;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.ColorUtil;
import polybot.util.UserUtil;

public class MergeXPCommand extends Command {

    public MergeXPCommand() {
        super("transfer", Category.MODERATOR, "Transfer XP from one user to another");

        this.isHidden = true;
        this.argsRequired = true;
        this.totalArgsRequired = 2;
        this.aliases = new String[]{"merge"};
        this.arguments = "(user from) (user to)";
        this.requiredPermission = Permission.KICK_MEMBERS;
        this.detailedHelp = """
            This command will transfer the XP from the first user specified, to the second user provided.
            User levels will be correctly calculated on transfer, and the first user will lose all XP.
                    
            **Examples**
            `&transfer lucky_the_uh Kit`
            `&merge 256202305127317505 567977785893847050`""";
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        // Check if we have valid users
        User[] users = new User[2];
        for (int i = 0; i < users.length; i++) {
            users[i] = UserUtil.searchForUser(event.args()[i]);
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
                .setTitle("Transfer failed")
                .setColor(ColorUtil.DND)
                .setDescription(message + "\n\nUsers can be provided using: `<@(id)>`, `(id)`, `(username)`\nMake sure to replace `(id)` with the user's id.")
                .build();
    }

    private MessageEmbed handleCommand(User from, User transferTo) {
        LevelEntry entryToRobFrom = BotStorage.getLevelEntry(from.getIdLong());
        LevelEntry entryToTransferTo = BotStorage.getLevelEntry(transferTo.getIdLong());

        if (entryToRobFrom == null) {
            // return saying that it cant transfer xp from someone who doesn't have any
            return new EmbedBuilder()
                    .setColor(ColorUtil.DND)
                    .setTitle("Transfer failed")
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

            if (entryToTransferTo.calculateLevel()) {
                //TODO: grant correct level rewards
                PolyBot.getLogger().info(String.format(BotStorage.getSetting(Setting.LEVEL_UP_MESSAGE), transferTo.getAsMention(), Integer.toUnsignedString(entryToTransferTo.getLevel())));
            }

            BotStorage.removeLevelEntry(from.getIdLong());
            BotStorage.saveLevelEntry(transferTo.getIdLong(), entryToTransferTo);
        }

        return new EmbedBuilder()
                .setColor(ColorUtil.ONLINE)
                .setTitle("Merge completed")
                .setDescription(from.getAsMention() + "'s xp was merged into " + transferTo.getAsMention() + "'s!")
                .setFooter(UserUtil.getUserAsName(transferTo) + " may see a level up notification")
                .build();
    }
}
