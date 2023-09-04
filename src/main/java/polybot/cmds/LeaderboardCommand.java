package polybot.cmds;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import polybot.commands.*;
import polybot.LevelEntry;
import polybot.storage.BotStorage;
import polybot.util.BotUtil;
import polybot.util.LevelUtil;
import polybot.util.UserUtil;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LeaderboardCommand extends SlashCommand {

    public LeaderboardCommand() {
        super("leaderboard", Category.LEVELING, "View the level leaderboard");

        this.cooldownLength = 15;
        this.cooldownUnit = TimeUnit.SECONDS;
        this.cooldownType = CooldownType.GLOBAL;
        this.arguments = "[page/(self or user id)]";
        this.aliases = new String[]{"levels", "lb"};
        this.options = List.of(
                new OptionData(OptionType.INTEGER, "page", "Page to display").setRequiredRange(0, 10000),
                new OptionData(OptionType.USER, "user", "User to display")
        );
        this.detailedHelp = """
            To view where you are at on the leaderboard, include `self` or find the page number using `&rank`.
                    
            **Examples**
            `&leaderboard 5`
            `&lb self`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        event.deferReply().queue();

        CompletableFuture.runAsync(() -> {
            User user = event.optUser("user", null);
            int page = (int) event.optLong("page", 0);

            if (user != null) {
                LevelEntry entry = BotStorage.getLevelEntry(user.getIdLong());
                if (entry != null) page = LevelUtil.getPageNumber(entry);
                else {
                    event.replyShown(UserUtil.getUserAsName(user) + " isn't ranked yet!");
                    return;
                }
            } else user = event.getUser();

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try {
                ImageIO.write(LevelUtil.generateLeaderboard(user.getIdLong(), page), "png", byteStream);
            } catch (IOException e) {
                event.getHook().editOriginal("Failed to generate the level card!").queue();
                return;
            }

            event.getHook().editOriginalAttachments(FileUpload.fromData(byteStream.toByteArray(), "leaderboard.png")).queue();
        });
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        event.getChannel().sendTyping().queue();
        CompletableFuture.runAsync(() -> {
            User user = event.getAuthor();

            int page = 0;
            if (event.args() != null) {
                try {
                    page = Integer.parseInt(event.args()[0]);
                } catch (NumberFormatException e) {
                    if (!event.args()[0].equalsIgnoreCase("self")) user = UserUtil.searchForUser(event.args()[0]);

                    if (user == null) {
                        replyAndDelete(event, "Unknown value provided, please provide a number, a valid user, or `self`!");
                        return;
                    }

                    LevelEntry entry = BotStorage.getLevelEntry(user.getIdLong());

                    if (entry != null) page = LevelUtil.getPageNumber(entry);
                    else {
                        replyAndDelete(event, UserUtil.getUserAsName(user) + " isn't ranked yet!");
                        return;
                    }
                }
            }

            BotUtil.uploadImage(event.getChannel(), LevelUtil.generateLeaderboard(user.getIdLong(), page), "leaderboard.png", true).setMessageReference(event.getMessage()).queue();
        });
    }
}
