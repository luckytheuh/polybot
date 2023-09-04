package polybot.util;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import polybot.PolyBot;
import polybot.storage.BotStorage;
import polybot.storage.Setting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GuildUtil {


    public static TextChannel getChannelFromSetting(Guild guild, Setting setting) {
        return guild.getTextChannelById(BotStorage.getSettingAsLong(setting, 0));
    }

    public static List<TextChannel> getChannelsFromSetting(Guild guild, Setting setting) {
        List<TextChannel> channels = new ArrayList<>();

        for (String str : BotStorage.getSettingAsList(setting)) {
            TextChannel channel = guild.getTextChannelById(str);
            if (channel != null) channels.add(channel);
        }

        return channels;
    }

    public static Role getRoleFromSetting(Guild guild, Setting setting) {
        return guild.getRoleById(BotStorage.getSettingAsLong(setting, 0));
    }

    public static boolean canHandOutRole(Role role) {
        Role botRole = role.getGuild().getRoleByBot(PolyBot.getJDA().getSelfUser());

        return role.isManaged() || role.isPublicRole() || (botRole == null || role.getPosition() > botRole.getPosition());
    }

    @Nullable
    public static Role searchForRole(Guild guild, String roleStr) {
        // Can't do anything in this case
        if (roleStr == null || roleStr.isEmpty() || roleStr.isBlank()) return null;

        if (roleStr.startsWith(" ")) roleStr = roleStr.replaceFirst(" ", "");
        roleStr = roleStr.trim();

        // The string was a number
        try {
            long id = Long.parseLong(roleStr);

            return BotUtil.isValidId(id) ? guild.getRoleById(id) : null;
        } catch (NumberFormatException ignored) {}

        // The string was a @mention
        if (roleStr.startsWith("<@") && roleStr.endsWith(">")) {
            try {
                long id = Long.parseLong(roleStr.replaceAll("[<@>]", ""));

                return BotUtil.isValidId(id) ? guild.getRoleById(id) : null;
            } catch (NumberFormatException ignored) {}

            // If the mention wasn't even a number, it's probably invalid
            return null;
        }

        // Last resort, search for the closest match
        List<Role> roles = guild.getRolesByName(roleStr, false);
        if (!roles.isEmpty()) return roles.get(0);

        //give up
        return null;
    }

    public static boolean hasPermissions(GuildMessageChannel channel, Permission... permissions) {
        return channel.getGuild().getSelfMember().hasPermission(channel, permissions);
    }

    public static boolean memberHasRole(Member member, long roleId) {
        if (!BotUtil.isValidId(roleId) || member == null) return false;

        for (Role role : member.getRoles()) {
            if (role.getIdLong() == roleId) return true;
        }

        return false;
    }

    public static void setRoles(Member member, List<Role> roles) {
        List<Role> editableList = new ArrayList<>(roles);
        editableList.removeIf(r -> !GuildUtil.canHandOutRole(r));

        member.getGuild().modifyMemberRoles(member, roles).queue();
    }

    public static void addRole(Member member, long roleId) {
        if (!BotUtil.isValidId(roleId)) return;

        Role role = member.getGuild().getRoleById(roleId);
        if (role == null) return;

        member.getGuild().addRoleToMember(member, role).queue();
    }

    public static void removeRole(Member member, long roleId) {
        if (!BotUtil.isValidId(roleId)) return;

        Role role = member.getGuild().getRoleById(roleId);
        if (role == null) return;

        member.getGuild().removeRoleFromMember(member, role).queue();
    }


}
