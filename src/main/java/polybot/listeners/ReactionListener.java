package polybot.listeners;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import polybot.PolyBot;
import polybot.util.UserUtil;

public class ReactionListener extends ListenerAdapter {

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getChannel().getIdLong() == 804542119410270208L || event.getChannel().getIdLong() == 1051272075123376149L
        || event.getChannel().getIdLong() == 804492702149378118L || event.getChannel().getIdLong() == 804542119410270208L) return;

        if (event.getUser() != null) log(event.getUser(), event.getChannel().asTextChannel(), event.getEmoji());
    }

    private void log(User user, TextChannel channel, EmojiUnion union) {
        /*
        TextChannel reportChannel = GuildUtil.getChannelFromSetting(channel.getGuild(), Setting.BOT_REPORT_CHANNEL);

        if (reportChannel != null) {
            reportChannel.sendMessageEmbeds(new EmbedBuilder()
                    .setAuthor(UserUtil.getUserAsName(user) + " (" + user.getIdLong() + ")", null, user.getAvatarUrl())
                    .setDescription("Unreacted emoji: " + union.getFormatted() + " in " + channel.getAsMention())
                    .setColor(new Color(241, 168, 16))
                    .build()
            ).queue();
        }
*/
        PolyBot.getLogger().info(UserUtil.getUserAsName(user) + " (" + user.getId() + ") unreacted with " + union.getFormatted() + " in " + channel.getName());
    }
}
