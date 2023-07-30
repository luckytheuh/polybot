package polybot.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import polybot.PolyBot;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.UserUtil;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterListener extends ListenerAdapter {

    private static final Pattern HTTP_PATTERN = Pattern.compile("https?://[^\\s<]+[^<.,:;\"')\\]\\s]", Pattern.CASE_INSENSITIVE);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        //TODO: website filter with whitelist
        // emoji filter
        // spam filter
        // one of the spam filters is sending messages really fast (5 msg in 5 sec)


        handle(event.getMessage());
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        handle(event.getMessage());
    }

    private void handle(Message message) {
        final String contentRaw = message.getContentRaw();
        //TODO: implement all filters in this message so we dont need to copy and paste the code twice
        // on initial send, and on further edits

        //split on space and check number of occurrences in message

        Matcher urlMatcher = HTTP_PATTERN.matcher(contentRaw);

        if (urlMatcher.matches()) {
            PolyBot.getLogger().warn("URL MATCH for " + UserUtil.getUserAsName(message.getAuthor()));

            List<String> urls = Arrays.stream(BotStorage.getSetting(Setting.LINK_WHITELIST).split(",")).toList();

            for (int i = 0; i < urlMatcher.groupCount(); i++) {
                if (!urls.contains(urlMatcher.group(i))) {
                    PolyBot.getLogger().warn("URL NOT ALLOWED '" + urlMatcher.group(i) + "' for " + UserUtil.getUserAsName(message.getAuthor()));
                    return;
                }
            }
        }

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



