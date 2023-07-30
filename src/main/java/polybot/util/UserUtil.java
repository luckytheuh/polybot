package polybot.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.ImageProxy;
import polybot.PolyBot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class UserUtil {

    public static void downloadProfilePicture(User user, Consumer<BufferedImage> imageConsumer) {
        ImageProxy proxy = user.getAvatar() == null ? user.getDefaultAvatar() : user.getAvatar();
        proxy.download().thenAcceptAsync(stream -> {
            try {
                BufferedImage avatar = ImageIO.read(stream);
                stream.close();
                imageConsumer.accept(avatar);
            } catch (IOException ignored) {}
        });
    }

    public static User searchForUser(String userStr) {
        // Can't do anything in this case
        if (userStr == null || userStr.isEmpty() || userStr.isBlank()) return null;

        // The string was a number
        try {
            long id = Long.parseLong(userStr);

            if (BotUtil.isValidId(id)) return getUser(id);
            else return null;
        } catch (NumberFormatException ignored) {}

        // The string was a @mention
        if (userStr.startsWith("<@") && userStr.endsWith(">")) {
            try {
                long id = Long.parseLong(userStr.replace("<@", "").replace(">", ""));

                if (BotUtil.isValidId(id)) return getUser(id);
            } catch (NumberFormatException ignored) {}

            // If the mention wasn't even a number, it's probably invalid
            return null;
        }

        // Last resort, try with their username
        List<User> users = PolyBot.getJDA().getUsersByName(userStr, false);
        if (!users.isEmpty()) return users.get(0);

        //give up
        return null;
    }

    public static Member getMemberFromUser(Guild guild, User user) {
        // We can't do anything without both
        if (guild == null || user == null) return null;

        return guild.retrieveMember(user).onErrorMap(t -> null).complete();
    }

    public static String getMemberAsName(Member member) {
        return getUserAsName(member.getUser());
    }

    public static String getUserAsName(User user) {
        return user.getName() + (user.getDiscriminator().equals("0000") ? "" : "#" + user.getDiscriminator());
    }

    public static User getUser(long userId) {
        return PolyBot.getJDA().retrieveUserById(userId).onErrorMap(t -> null).complete();
    }

}
