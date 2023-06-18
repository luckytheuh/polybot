package polybot.levels;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import polybot.PolyBot;
import polybot.storage.BotStorage;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LevelListener extends ListenerAdapter {

    //user id, timestamp when they can get xp again
    private final Map<Long, Long> messageCache = new HashMap<>();


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // if this is dm or user is a bot, don't do anythin
        if (!event.isFromGuild() || event.getAuthor().isBot()) return;

        // TODO: check if this channel has leveling disabled
        // if (event.getChannel().getIdLong() ???) return;

        // check if it's not time to let you gain xp again
        long time = messageCache.getOrDefault(event.getAuthor().getIdLong(), 0L) ;
        if (time != 0L && time >= Instant.now().toEpochMilli()) return;


        // take current time, add 60 seconds in miliseconds to it and store it to the map
        messageCache.put(event.getAuthor().getIdLong(), Instant.now().toEpochMilli() + (1000 * 60));

        int gainedXp = 15 + ThreadLocalRandom.current().nextInt(11);

        LevelEntry entry = BotStorage.getLevelEntry(event.getAuthor().getIdLong());

        entry.setXp(entry.getXp() + gainedXp);
        if (entry.getXpToNextLevel() <= 0) {
            entry.setLevel(entry.getLevel()+1);

            // if this actually gets used in the server, this id would have to be set to the right one
            TextChannel channel = event.getGuild().getTextChannelById(804504208853696553L);
            if (channel != null) channel.sendMessage(event.getAuthor().getAsMention() + " IS NOW " + entry.getLevel() + "! <:trol:804497097809592331>").queue();
        }

        BotStorage.saveLevelEntry(entry);
    }
}
