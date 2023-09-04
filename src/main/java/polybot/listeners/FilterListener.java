package polybot.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import polybot.PolyBot;
import polybot.storage.BotStorage;
import polybot.storage.Setting;
import polybot.util.GuildUtil;
import polybot.util.UserUtil;
import polybot.util.WordUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterListener extends ListenerAdapter {

    private static final List<String> ALLOWED_URLS = List.of("discord.com", "discord.gg", "media.discordapp.net", "cdn.discordapp.com", "tenor.com", "youtube.com", "youtu.be", "twitter.com", "t.co", "en.wikipedia.com");
    private static final Pattern HTTP_PATTERN = Pattern.compile("(http|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])", Pattern.CASE_INSENSITIVE);
    //private static final Pattern SPOILER_PATTERN = Pattern.compile("\\|\\|([^:]+)\\|\\|");
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
        if (!message.isFromGuild()) return;

        Member member = UserUtil.getMemberFromUser(message.getGuild(), message.getAuthor());
        if (member != null && member.hasPermission(Permission.KICK_MEMBERS)) return;

        List<String> disallowedStickers = BotStorage.getSettingAsList(Setting.STICKER_BLACKLIST);
        for (StickerItem sticker : message.getStickers()) {
            if (disallowedStickers.contains(sticker.getId())) {
                message.delete().queue(unused -> {
                    notifyUser(message.getAuthor(), String.format("%s, that sticker isn't allowed in **%s**!", message.getAuthor().getAsMention(), message.getGuild().getName()));
                }, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.MISSING_PERMISSIONS));

                TextChannel logChannel = GuildUtil.getChannelFromSetting(message.getGuild(), Setting.MESSAGE_LOG_CHANNEL);
                if (logChannel != null) {
                    logChannel.sendMessageEmbeds(MessageListener.getDeletedEmbed(message).build()).queue();
                }

                return;
            }
        }

        final String contentRaw = message.getContentRaw();

        if (contentRaw.contains("\n")) {
            for (String line : contentRaw.split("\n")) {
                if (line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ")) {
                    message.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.MISSING_PERMISSIONS));
                    return;
                }
            }
        }

        List<String> urls = new ArrayList<>(BotStorage.getSettingAsList(Setting.LINK_WHITELIST));
        urls.addAll(ALLOWED_URLS);

        Matcher matcher = HTTP_PATTERN.matcher(contentRaw);
        while (matcher.find()) {
            String str = matcher.group().toLowerCase().replaceAll("(http|https)://", "");
            if (str.contains("/")) str = str.substring(0, str.indexOf("/"));
            if (str.startsWith("www.")) str = str.replace("www.", "");

            if (urls.contains(str)) continue;

            PolyBot.getLogger().info(str + " | " + message.getChannel().getName());

            //message.reply("URL NOT ALLOWED!").mentionRepliedUser(false).queue();
            return;
        } //for wildcard, work our way backwards or something maybe copy the wildcard from httpsrvsocket

        List<WordUtil.Word> words = new ArrayList<>();
        matcher = EMOJI_PATTERN.matcher(contentRaw);
        while (matcher.find()) {
            checkWord(words, matcher.group());
        }

        for (WordUtil.Word word : words) {
            if (word.getAppearances() > BotStorage.getSettingAsLong(Setting.EMOJI_CAP, 3)) {
                PolyBot.getLogger().info(word.getWord() + " | " + message.getChannel().getName());
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

    private void notifyUser(User user, String phrase) {
        user.openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(phrase).queue(null, new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER));
        });
    }

    void checkWord(List<WordUtil.Word> wordList, String str) {
        for (WordUtil.Word word : wordList) {
            if (word.matches(str)) {
                word.increaseOccurrences();
                return;
            }
        }

        wordList.add(new WordUtil.Word(str));
    }



}



