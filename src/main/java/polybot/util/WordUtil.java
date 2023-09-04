package polybot.util;

import polybot.storage.BotStorage;
import polybot.storage.Setting;

import java.util.List;

public class WordUtil {

    public static String uppercaseFirst(String str) {
        return str.toUpperCase().charAt(0) + str.toLowerCase().substring(1);
    }

    public static String checkAgainstFilter(String str) {
        List<String> whitelist = BotStorage.getSettingAsList(Setting.WHITELISTED_KEYWORDS);
        List<String> blacklist = BotStorage.getSettingAsList(Setting.BLACKLISTED_KEYWORDS);
        String[] words = str.toLowerCase().split(" ");

        for (String word : words) {
            // Ignore whitelisted words if matched, or 75% match
            if (matchesWhitelist(whitelist, word)) continue;

            for (String keyword : blacklist) {
                // If this word straight up matches it, return it
                if (word.equalsIgnoreCase(keyword)) return keyword;

                // Check if the keyword makes up for half or more of this word, return it if it do
                if (word.contains(keyword) && keyword.length() >= word.length()/2) return keyword;
            }
        }

        return null;
    }

    public static boolean matchesWhitelist(List<String> strings, String str) {
        for (String keyword : strings) {
            if (str.equalsIgnoreCase(keyword)) return true;

            if (str.contains(keyword) && keyword.length() >= (int) (str.length()/4d*3d)) return true;
        }

        return false;
    }


/*
    public static List<Word> getWordsFromString(String str) {

    }

    public static List<Word> compactWords(List<Word> words) {

    }
*/

    public static class Word {
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
