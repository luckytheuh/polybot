package polybot.commands;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.function.Consumer;

public class SlashCommandEvent extends SlashCommandInteractionEvent {

    private final CommandManager manager;
    private String params;

    public SlashCommandEvent(CommandManager manager, SlashCommandInteractionEvent event) {
        super(event.getJDA(), event.getResponseNumber(), event.getInteraction());
        this.manager = manager;
    }

    public CommandManager manager() {
        return manager;
    }

    public boolean isFromBotOwner() {
        return manager.getOwnerId() == getUser().getIdLong();
    }

    public String reqString(String key) {
        return getOption(key, OptionMapping::getAsString);
    }

    public boolean reqBoolean(String key) {
        return getOption(key, OptionMapping::getAsBoolean);
    }

    public long reqLong(String key) {
        return getOption(key, OptionMapping::getAsLong);
    }

    public double reqDouble(String key) {
        return getOption(key, OptionMapping::getAsDouble);
    }

    public User reqUser(String key) {
        return getOption(key, OptionMapping::getAsUser);
    }

    public Member reqMember(String key) {
        return getOption(key, OptionMapping::getAsMember);
    }

    public GuildChannel reqChannel(String key) {
        return getOption(key, OptionMapping::getAsChannel);
    }

    public Role reqRole(String key) {
        return getOption(key, OptionMapping::getAsRole);
    }

    public IMentionable reqMentionable(String key) {
        return getOption(key, OptionMapping::getAsMentionable);
    }

    public Message.Attachment reqAttachment(String key) {
        return getOption(key, OptionMapping::getAsAttachment);
    }

    public String optString(String key, String def) {
        return getOption(key, def, OptionMapping::getAsString);
    }

    public boolean optBoolean(String key, boolean def) {
        return getOption(key, def, OptionMapping::getAsBoolean);
    }

    public long optLong(String key, long def) {
        return getOption(key, def, OptionMapping::getAsLong);
    }

    public double optDouble(String key, double def) {
        return getOption(key, def, OptionMapping::getAsDouble);
    }

    public User optUser(String key, User def) {
        return getOption(key, def, OptionMapping::getAsUser);
    }

    public Member optMember(String key, Member def) {
        return getOption(key, def, OptionMapping::getAsMember);
    }

    public GuildChannel optChannel(String key, GuildChannel def) {
        return getOption(key, def, OptionMapping::getAsChannel);
    }

    public Role optRole(String key, Role def) {
        return getOption(key, def, OptionMapping::getAsRole);
    }

    public IMentionable optMentionable(String key, IMentionable def) {
        return getOption(key, def, OptionMapping::getAsMentionable);
    }

    public Message.Attachment optAttachment(String key, Message.Attachment def) {
        return getOption(key, def, OptionMapping::getAsAttachment);
    }

    public void replyShown(String str) {
        replyShown(str, null);
    }

    public void replyShown(MessageEmbed embed) {
        replyShown(embed, null);
    }

    public void replyShown(FileUpload upload) {
        replyShown(upload, null);
    }

    public void replyShown(String str, Consumer<InteractionHook> success) {
        reply(str).queue(success);
    }

    public void replyShown(MessageEmbed embed, Consumer<InteractionHook> success) {
        replyEmbeds(embed).queue(success);
    }

    public void replyShown(FileUpload upload, Consumer<InteractionHook> success) {
        replyFiles(upload).mentionRepliedUser(false).queue(success);
    }

    public void replyHidden(String str) {
        replyHidden(str, null);
    }

    public void replyHidden(MessageEmbed embed) {
        replyHidden(embed, null);
    }

    public void replyHidden(FileUpload upload) {
        replyHidden(upload, null);
    }

    public void replyHidden(String str, Consumer<InteractionHook> success) {
        reply(str).setEphemeral(true).queue(success);
    }

    public void replyHidden(MessageEmbed embed, Consumer<InteractionHook> success) {
        replyEmbeds(embed).setEphemeral(true).queue(success);
    }

    public void replyHidden(FileUpload upload, Consumer<InteractionHook> success) {
        replyFiles(upload).setEphemeral(true).queue(success);
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}
