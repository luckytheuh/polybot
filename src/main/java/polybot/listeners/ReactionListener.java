package polybot.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
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

        if (event.getUser() == null) event.retrieveUser().queue(user -> log(user, event.getEmoji()));
        else log(event.getUser(), event.getEmoji());
    }

    private void log(User user, EmojiUnion union) {
        PolyBot.getLogger().info(UserUtil.getUserAsName(user) + " (" + user.getId() + ") unreacted with " + union.getAsReactionCode());
    }
}
