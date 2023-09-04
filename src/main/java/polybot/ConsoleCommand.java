package polybot;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import polybot.storage.BotStorage;
import polybot.storage.Configuration;
import polybot.storage.Setting;
import polybot.util.UserUtil;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public enum ConsoleCommand { //TODO: givexp, rmxp
    BLACKLIST(args -> {
        if (BotStorage.getSetting(Setting.USER_BLACKLIST).contains(args)) {
            BotStorage.setSetting(Setting.USER_BLACKLIST, args, true);
            PolyBot.getLogger().info("Removed " + args + " from the command blacklist");
        } else {
            BotStorage.setSetting(Setting.USER_BLACKLIST, args, false);
            PolyBot.getLogger().info("Added " + args + " to the command blacklist");
        }
    }),/* GIVEXP(args -> {
        if (args.isBlank() || args.isEmpty()) {
            PolyBot.getLogger().warn("Please specify a channel/user!");
            return;
        }

    }),*/ GENXP(args -> {
        String[] split = args.split(" ");
        if (split.length < 2) {
            PolyBot.getLogger().warn("Provide a user and number messages!");
            return;
        }

        long userId = Long.parseLong(split[0]);
        LevelEntry entry = BotStorage.getLevelEntry(userId);
        if (entry == null) entry = new LevelEntry(userId, 0, 0, BotStorage.getTotalRankedUsers(), 0, false);

        int messages = Integer.parseInt(split[1]);
        while (messages > 0) {
            messages--;
            int gainedXp = 15 + ThreadLocalRandom.current().nextInt(11);

            entry.setXp(entry.getXp() + gainedXp);
            entry.setMessages(entry.getMessages() + 1);
        }
        entry.calculateLevel();

        PolyBot.getLogger().info("{} is now lvl {}", userId, entry.getLevel());
        BotStorage.saveLevelEntry(userId, entry);
    }),/* RMXP(args -> {

    }),*/ SEND(args -> {
        if (args.isBlank() || args.isEmpty()) {
            PolyBot.getLogger().warn("Please specify a channel/user!");
            return;
        }

        String[] split = args.split(" ");
        String message = String.join(" ", Arrays.copyOfRange(split, 1, split.length));

        if (message.isBlank() || message.isEmpty()) {
            PolyBot.getLogger().warn("Please specify a message!");
            return;
        }

        User user = UserUtil.searchForUser(split[0]);
        if (user == null) {
            try {
                long id = Long.parseLong(split[0]);
                TextChannel channel = PolyBot.getJDA().getGuilds().get(0).getTextChannelById(id);

                if (channel == null) {
                    PolyBot.getLogger().warn("Unknown channel: " + split[0]);
                    return;
                }

                channel.sendMessage(message).queue(msg -> PolyBot.getLogger().info("Sent \"" + msg.getContentRaw() + "\" into " + msg.getChannel().getName()));
                return;
            } catch (NumberFormatException ignored) {}
            PolyBot.getLogger().warn("Unknown: " + split[0]);
            return;
        }

        if (user.isBot()) {
            PolyBot.getLogger().warn("Cannot dm bot user: " + split[0]);
            return;
        }

        user.openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(message).queue(msg -> PolyBot.getLogger().info("Sent \"" + msg.getContentRaw() + "\" into " + msg.getChannel().getName()));
        }, throwable -> PolyBot.getLogger().warn("Failed to open DM with user " + user.getName(), throwable));
    }), RELOAD(args -> Configuration.reload()),
    STOP(args -> {
        PolyBot.getJDA().shutdown();
        BotStorage.closeStorage();
        System.exit(0);
    });


    final Consumer<String> commandConsumer;

    ConsoleCommand(Consumer<String> commandConsumer) {
        this.commandConsumer = commandConsumer;
    }

    public void run(String args) {
        commandConsumer.accept(args);
    }
}
