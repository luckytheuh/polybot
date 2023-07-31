package polybot.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import polybot.storage.BotStorage;
import polybot.storage.Setting;

public class GuildUtil {

    public static TextChannel getChannelFromSetting(Guild guild, Setting setting) {
        return guild.getTextChannelById(BotStorage.getSettingAsLong(setting, 0));
    }

    public static boolean memberHasRole(Member member, long roleId) {
        if (roleId == 0) return false;

        for (Role role : member.getRoles()) {
            if (role.getIdLong() == roleId) return true;
        }

        return false;
    }

}
