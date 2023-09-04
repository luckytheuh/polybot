package polybot.cmds;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import polybot.LevelEntry;
import polybot.PolyBot;
import polybot.commands.Category;
import polybot.commands.CommandEvent;
import polybot.commands.SlashCommand;
import polybot.commands.SlashCommandEvent;
import polybot.storage.BotStorage;
import polybot.util.BotUtil;
import polybot.util.GuildUtil;
import polybot.util.LevelUtil;
import polybot.util.UserUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class RankCommand extends SlashCommand {

    public RankCommand() {
        super("rank", Category.LEVELING, "Display your or someone else's level card");
        this.arguments = "[user]";
        this.aliases = new String[]{"level"};
        this.options = Collections.singletonList(new OptionData(OptionType.USER, "user", "User to rank check", false));
        this.detailedHelp = """
            **Examples**
            `&rank polygondonut`
            `&rank 210575087659646976`""";
    }

    @Override
    protected void fireSlashCommand(SlashCommandEvent event) {
        if (!event.isFromGuild()) return;

        User user = event.optUser("user", event.getUser());
        Member member = UserUtil.getMemberFromUser(event.getGuild(), user);

        String res = doChecks(user);
        if (res != null) {
            event.reply(res).queue();
            return;
        }

        LevelEntry entry = BotStorage.getLevelEntry(user.getIdLong());
        if (entry == null) {
            event.reply("🚫 **" + UserUtil.getUserAsName(user) + "** isn't ranked yet.").queue();
            return;
        }

        if (!event.isFromGuild() || GuildUtil.hasPermissions(event.getGuildChannel(), Permission.MESSAGE_ATTACH_FILES)) {
            try {
                BufferedImage card = LevelUtil.getLevelCard(user, member != null ? member.getOnlineStatus() : null, entry);

                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ImageIO.write(card, "png", byteStream);

                event.replyFiles(FileUpload.fromData(byteStream.toByteArray(), user.getId() + ".png")).queue();
                return;
            } catch (IOException ignored) {}
        }

        // Last ditch effort, reply w/text on image fail or cant upload images
        event.reply(LevelUtil.getLevelMessage(user, entry)).queue();
    }

    @Override
    protected void fireCommand(CommandEvent event) {
        if (!event.isFromGuild()) return;

        User user;
        if (event.args() != null) {
            user = UserUtil.searchForUser(event.args()[0]);

            if (user == null) {
                event.getChannel().sendMessage("I am not able to find the user you provided!").queue();
                return;
            }
        } else user = event.getAuthor();

        // Try to get their status, null if not found
        Member member = UserUtil.getMemberFromUser(event.getGuild(), user);

        String res = doChecks(user);
        if (res != null) {
            event.reply(res);
            return;
        }

        CompletableFuture.runAsync(() -> {
            LevelEntry entry = BotStorage.getLevelEntry(user.getIdLong());
            if (entry == null) {
                event.reply("🚫 **" + UserUtil.getUserAsName(user) + "** isn't ranked yet.");
                return;
            }

            if (GuildUtil.hasPermissions(event.getMessage().getGuildChannel(), Permission.MESSAGE_ATTACH_FILES)) {
                BotUtil.uploadImage(event.getChannel(), LevelUtil.getLevelCard(user, member != null ? member.getOnlineStatus() : null, entry), user.getId() + ".png", false);
            } else {
                event.reply(LevelUtil.getLevelMessage(user, entry));
            }
        });
    }

    private String doChecks(User user) {
        if (user.isBot() && !PolyBot.isSelfUser(user)) { //TODO: allow customizing messages
            return "🚫 " + user.getAsMention() + " is a **bot**! Bots are unrankable except me. :sunglasses:**.";
        }

        return null;
    }
}
