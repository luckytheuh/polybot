package polybot.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import polybot.storage.BotStorage;
import polybot.storage.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterListener extends ListenerAdapter {

    private static final List<String> ALLOWED_URLS = List.of("discord.com", "discord.gg", "media.discordapp.net", "cdn.discordapp.com", "tenor.com", "youtube.com", "youtu.be", "twitter.com", "t.co", "en.wikipedia.com");
    private static final Pattern HTTP_PATTERN = Pattern.compile("(http|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([^:]+):");

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        //TODO: website filter with wildcard whitelist
        // spam filter
        // one of the spam filters is sending messages really fast (5 msg in 5 sec)


        handle(event.getMessage());
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        handle(event.getMessage());
    }

    private void handle(Message message) {
        if (message.getAuthor().isBot()) return;

        final String contentRaw = message.getContentRaw();

        List<String> urls = BotStorage.getSettingAsList(Setting.LINK_WHITELIST);
        urls.addAll(ALLOWED_URLS);

        Matcher matcher = HTTP_PATTERN.matcher(contentRaw);
        while (matcher.find()) {
            String str = matcher.group().toLowerCase().replaceAll("(http|https)://", "");
            if (str.contains("/")) str = str.substring(0, str.indexOf("/"));

            if (urls.contains(str)) continue;

            System.out.println(str + " | " + message.getChannel().getName());

            //message.reply("URL NOT ALLOWED!").mentionRepliedUser(false).queue();
            return;
        } //for wildcard, work our way backwards or something maybe copy the wildcard from httpsrvsocket

        List<Word> words = new ArrayList<>();
        matcher = EMOJI_PATTERN.matcher(contentRaw);
        while (matcher.find()) {
            checkWord(words, matcher.group());
        }

        for (Word word : words) {
            if (word.appearances > 3) {
                System.out.println(word.word + " | " + message.getChannel().getName());
                //message.reply("TOO MUCH EMOJI, NOT ALLOWED!").mentionRepliedUser(false).queue();
                return;
            }
        }
        words.clear();

/*
//using the String#contains method, we could use that to gain additional common words
        //We should use the number of unique words against the total number of words in the message to then compute if this is spammy
        String[] splitWords = contentRaw.split(" ");
        List<Word> words = new ArrayList<>();
        Arrays.stream(splitWords).forEach(s -> checkWord(words, s));

        words.sort(Comparator.comparingInt(Word::getAppearances));

        int unique = words.size();

        int repeated = splitWords.length - unique;

        System.out.println("unique words=" + unique + ", repeated count=" + repeated + ", og word count=" + splitWords.length);*/
    }

    void checkWord(List<Word> wordList, String str) {
        for (Word word : wordList) {
            if (word.matches(str)) {
                word.increaseOccurrences();
                return;
            }
        }

        wordList.add(new Word(str));
    }

    private static class Word {
        private final String word;
        private int appearances;

        public Word(String word) {
            this.word = word;
            appearances = 1;
        }

        public boolean matches(String other) {
            return word.equals(other) || other.contains(word);
        }

        public String getWord() {
            return word;
        }

        public int getAppearances() {
            return appearances;
        }

        public void increaseOccurrences() {
            appearances++;
        }
    }

}



