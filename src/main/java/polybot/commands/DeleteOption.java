package polybot.commands;

public enum DeleteOption {
    NONE(false, false),
    DELETE_BOT(false, true),
    DELETE_USER(true, false),
    DELETE_BOTH(true, true);

    final boolean delUser;
    final boolean delBot;

    DeleteOption(boolean delUser, boolean delBot) {
        this.delUser = delUser;
        this.delBot = delBot;
    }
}
