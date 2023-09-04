package polybot.commands;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.function.Consumer;

public class ContextMenuEvent extends MessageContextInteractionEvent {

    private final CommandManager manager;

    public ContextMenuEvent(CommandManager manager, MessageContextInteractionEvent event) {
        super(event.getJDA(), event.getResponseNumber(), event.getInteraction());
        this.manager = manager;
    }

    public final boolean isFromBotOwner() {
        return getUser().getIdLong() == manager.getOwnerId();
    }

    public CommandManager manager() {
        return manager;
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
}
