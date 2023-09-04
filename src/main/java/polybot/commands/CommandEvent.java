package polybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public final class CommandEvent extends MessageReceivedEvent {
    private final String prefixUsed, commandUsed;
    private final CommandManager manager;
    private String params, argsProvided;
    private String[] args;

    public CommandEvent(CommandManager manager, MessageReceivedEvent event, String prefixUsed, String commandUsed, String argsProvided, String[] args) {
        super(event.getJDA(), event.getResponseNumber(), event.getMessage());
        this.manager = manager;
        this.prefixUsed = prefixUsed;
        this.commandUsed = commandUsed;
        this.argsProvided = argsProvided;
        this.args = args;
    }

    public void send(String str) {
        send(str, null);
    }

    public void send(MessageEmbed embed) {
        send(embed, null);
    }

    public void send(FileUpload upload) {
        send(upload, null);
    }

    public void send(String str, Consumer<Message> success) {
        getMessage().getChannel().sendMessage(str).queue(success);
    }

    public void send(MessageEmbed embed, Consumer<Message> success) {
        if (!canSendEmbed()) return;
        getMessage().getChannel().sendMessageEmbeds(embed).queue(success);
    }

    public void send(FileUpload upload, Consumer<Message> success) {
        if (!canUploadFile()) return;
        getMessage().getChannel().sendFiles(upload).queue(success);
    }

    public void reply(String str) {
        reply(str, null);
    }

    public void reply(MessageEmbed embed) {
        reply(embed, null);
    }

    public void reply(FileUpload upload) {
        reply(upload, null);
    }

    public void reply(String str, Consumer<Message> success) {
        getMessage().reply(str).mentionRepliedUser(false).queue(success);
    }

    public void reply(MessageEmbed embed, Consumer<Message> success) {
        if (!canSendEmbed()) return;
        getMessage().replyEmbeds(embed).mentionRepliedUser(false).queue(success);
    }

    public void reply(FileUpload upload, Consumer<Message> success) {
        if (!canUploadFile()) return;
        getMessage().replyFiles(upload).mentionRepliedUser(false).queue(success);
    }

    public void replyMention(String str) {
        replyMention(str, null);
    }

    public void replyMention(MessageEmbed embed) {
        replyMention(embed, null);
    }

    public void replyMention(FileUpload upload) {
        replyMention(upload, null);
    }

    public void replyMention(String str, Consumer<Message> success) {
        getMessage().reply(str).queue(success);
    }

    public void replyMention(MessageEmbed embed, Consumer<Message> success) {
        if (!canSendEmbed()) return;
        getMessage().replyEmbeds(embed).queue(success);
    }

    public void replyMention(FileUpload upload, Consumer<Message> success) {
        if (!canUploadFile()) return;
        getMessage().replyFiles(upload).queue(success);
    }

    public void replyInDm(String str) {
        if (getMessage().isFromType(ChannelType.PRIVATE)) send(str);
        else getAuthor().openPrivateChannel().queue(dm -> dm.sendMessage(str).queue());
    }

    public void replyInDm(MessageEmbed embed) {
        if (getMessage().isFromType(ChannelType.PRIVATE)) send(embed);
        else getAuthor().openPrivateChannel().queue(dm -> dm.sendMessageEmbeds(embed).queue());
    }

    public void replyInDm(FileUpload upload) {
        if (getMessage().isFromType(ChannelType.PRIVATE)) send(upload);
        else getAuthor().openPrivateChannel().queue(dm -> dm.sendFiles(upload).queue());
    }

    public boolean canSendEmbed() {
        return !isFromGuild() || getGuild().getSelfMember().hasPermission(getGuildChannel(), Permission.MESSAGE_EMBED_LINKS);
    }

    public boolean canUploadFile() {
        return !isFromGuild() || getGuild().getSelfMember().hasPermission(getGuildChannel(), Permission.MESSAGE_ATTACH_FILES);
    }

    public boolean hasAllMention() {
        return getMessage().getMentions().mentionsEveryone() || getMessage().getContentRaw().contains("@everyone") || getMessage().getContentRaw().contains("@here");
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public CommandManager manager() {
        return manager;
    }

    public String getPrefix() {
        return prefixUsed;
    }

    public String commandUsed() {
        return commandUsed;
    }

    public String[] args() {
        return args;
    }

    public String stringArgs() {
        return argsProvided;
    }

    public void offsetArgs(int by) {
        this.args = Arrays.copyOfRange(this.args, by, this.args.length);
        argsProvided = String.join(" ", this.args);
    }

    public boolean isFromBotOwner() {
        return manager.getOwnerId() == getAuthor().getIdLong();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CommandEvent) obj;
        return Objects.equals(this.manager, that.manager) &&
                Objects.equals(this.commandUsed, that.commandUsed) &&
                Arrays.equals(this.args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(manager, commandUsed, Arrays.hashCode(args));
    }

    @Override
    public String toString() {
        return "CommandEvent[manager=" + manager + ", commandUsed=" + commandUsed + ", args=" + Arrays.toString(args) + ']';
    }
}
