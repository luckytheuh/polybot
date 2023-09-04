package polybot.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.FileUpload;
import polybot.PolyBot;
import polybot.TwoValues;
import polybot.commands.CommandEvent;
import polybot.commands.SlashCommandEvent;
import polybot.storage.Configuration;

import java.awt.FontMetrics;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

public class ImageUtil {
/*
    private static final Font TWEMOJI;

    static {
        Font t = null;

        try {
            t = Font.createFont(Font.TRUETYPE_FONT, Files.newInputStream(Paths.get("./fonts/twemoji.otf"))).deriveFont(32f);
        } catch (IOException | FontFormatException e) {
            PolyBot.getLogger().error("Failed to load twemoji!", e);
        }

        TWEMOJI = t;
    }
*/

    private static final ScheduledExecutorService FILE_DELETER = Executors.newSingleThreadScheduledExecutor();

    public static void markForDeletion(Path path) {
        FILE_DELETER.schedule(() -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {}
        }, 6, TimeUnit.MINUTES);
    }

    public static String searchForAttachment(Message message) {
        if (message == null) return null;

        if (!message.getEmbeds().isEmpty()) {
            MessageEmbed embed = message.getEmbeds().get(0);

            switch (embed.getType()) {
                case IMAGE, VIDEO, LINK, UNKNOWN -> {
                    return embed.getUrl();
                } case RICH -> {
                    if (embed.getImage() != null) {
                        return embed.getImage().getUrl();
                    }
                    else if (embed.getThumbnail() != null) return embed.getThumbnail().getUrl();
                }
            }

            return embed.getUrl();
        } else if (!message.getAttachments().isEmpty()) {
            return message.getAttachments().get(0).getUrl();
        } else if (!message.getStickers().isEmpty()) {

        } else if (message.getMessageReference() != null) {
            return searchForAttachment(message.getMessageReference().resolve().complete());
        }

        return null;
    }

    public static void searchAndFireSlash(SlashCommandEvent event, String name, String url, String args) {
        InteractionHook hook = event.deferReply().complete();

        if (url == null) {
            CompletableFuture.runAsync(() -> {
                MessageHistory history = event.getChannel().getHistoryBefore(event.getChannel().getLatestMessageId(), 20).complete();

                for (Message message : history.getRetrievedHistory()) {
                    String otherUrl = ImageUtil.searchForAttachment(message);
                    if (otherUrl == null) continue;

                    try (FileUpload upload = manageConnection(hook, name, otherUrl, args, event.getParams())) {
                        if (upload == null) return;

                        hook.editOriginalAttachments(upload).queue();
                    } catch (IOException ignored) {}
                    return;
                }

                hook.editOriginal("Please reply to or attach an image!").queue();
            });
            return;
        }

        CompletableFuture.runAsync(() -> {
            try (FileUpload upload = manageConnection(hook, name, url, args, event.getParams())) {
                if (upload == null) return;

                hook.editOriginalAttachments(upload).queue();
            } catch (IOException ignored) {}
        });
    }

    public static void searchAndFire(CommandEvent event, String name) {
        String url = ImageUtil.searchForAttachment(event.getMessage());
        if (url == null) {
            CompletableFuture.runAsync(() -> {
                MessageHistory history = event.getChannel().getHistoryBefore(event.getMessage().getId(), 20).complete();

                for (Message message : history.getRetrievedHistory()) {
                    String otherUrl = ImageUtil.searchForAttachment(message);
                    if (otherUrl == null) continue;

                    try (FileUpload upload = manageConnection(event, name, otherUrl)) {
                        if (upload == null) return;

                        event.reply(upload);
                    } catch (IOException ignored) {}
                    return;
                }

                event.replyMention("Please reply to or attach an image!");
            });
            return;
        }

        CompletableFuture.runAsync(() -> {
            try (FileUpload upload = manageConnection(event, name, url)) {
                if (upload == null) return;

                event.reply(upload);
            } catch (IOException ignored) {}
        });
    }

    public static FileUpload manageConnection(InteractionHook hook, String name, String link, String args, String param) {
        TwoValues<Socket, Configuration.Renderer> values = searchForRenderer();
        int renderingId = -1;

        try {
            if (values == null) {
                hook.editOriginal("There are no renderers available to handle your request, try again later!").queue();
                return null;
            }
            values.getFirst().setSoTimeout(Math.toIntExact(TimeUnit.MINUTES.toMillis(15)));

            BufferedReader reader = new BufferedReader(new InputStreamReader(values.getFirst().getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(values.getFirst().getOutputStream(), true, StandardCharsets.UTF_8);

            writer.println(new ClientRequest(name, link, args, param).toJson());
            JsonObject object = JsonParser.parseString(reader.readLine()).getAsJsonObject();
            PolyBot.getLogger().debug(object.toString());

            if (object.has("rendering")) {
                object = JsonParser.parseString(reader.readLine()).getAsJsonObject();
                PolyBot.getLogger().debug(object.toString());
            }
            boolean success = object.get("success").getAsBoolean();
            String response = object.get("response").getAsString();
            renderingId = object.get("renderId").getAsInt();

            if (success) {
                try (InputStream stream = new URL(values.getSecond().getWebAddress() + response).openStream()) {
                    byte[] bytes = stream.readAllBytes();

                    if (Message.MAX_FILE_SIZE < bytes.length) {
                        hook.editOriginal("Media exceeds the file size limit! Rendering id: `" + response + "`.").queue();
                        return null;
                    }

                    return FileUpload.fromData(bytes, response);
                }
            } else {
                PolyBot.getLogger().warn("Failure in rendering {}, {}", renderingId, response);
                hook.editOriginal(String.format("Encountered a rendering error, `%s`" + (renderingId == -1 ? "" : "\nRendering id: `%d`"), response, renderingId)).queue();
                return null;
            }
        } catch (SocketTimeoutException e) {
            PolyBot.getLogger().warn("Rendering task {} exceeded timeout of 15 minutes!", renderingId);
            hook.editOriginal("Task `" + renderingId + "` exceeded timeout of 15 minutes!").queue();
            return null;
        } catch (IOException e) {
            PolyBot.getLogger().warn("Exception in connection while rendering task " + renderingId, e);
            hook.editOriginal("An internal error occurred in the connection between the renderer, task id=`" + renderingId + "`").queue();
        } finally {
            if (values != null) {
                try {
                    values.getSecond().disconnect(values.getFirst());
                } catch (IOException ignored) {}
            }
        }

        return null;
    }

    public static FileUpload manageConnection(CommandEvent event, String name, String link) {
        TwoValues<Socket, Configuration.Renderer> values = searchForRenderer();
        int renderingId = -1;
        Message message = null;

        try {
            if (values == null) {
                event.reply("There are no renderers available to handle your request, try again later!");
                return null;
            }
            values.getFirst().setSoTimeout(Math.toIntExact(TimeUnit.MINUTES.toMillis(15)));

            BufferedReader reader = new BufferedReader(new InputStreamReader(values.getFirst().getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(values.getFirst().getOutputStream(), true, StandardCharsets.UTF_8);

            writer.println(new ClientRequest(name, link, event.stringArgs(), event.getParams()).toJson());
            JsonObject object = JsonParser.parseString(reader.readLine()).getAsJsonObject();
            PolyBot.getLogger().debug(object.toString());

            if (object.has("rendering")) {
                message = event.getMessage().reply(LoadingEmoji.random() + " Processing... This might take a while..").mentionRepliedUser(false).complete();
                object = JsonParser.parseString(reader.readLine()).getAsJsonObject();
                PolyBot.getLogger().debug(object.toString());
            }
            boolean success = object.get("success").getAsBoolean();
            String response = object.get("response").getAsString();
            renderingId = object.get("renderId").getAsInt();

            if (message != null) message.delete().queue();
            if (success) {
                try (InputStream stream = new URL(values.getSecond().getWebAddress() + response).openStream()) {
                    byte[] bytes = stream.readAllBytes();

                    if ((event.isFromGuild() && event.getGuild().getBoostTier().getMaxFileSize() < bytes.length) || Message.MAX_FILE_SIZE < bytes.length) {
                        event.replyMention("Media exceeds the file size limit! Rendering id: `" + response + "`.");
                        return null;
                    }

                    return FileUpload.fromData(bytes, response);
                }
            } else {
                PolyBot.getLogger().warn("Failure in rendering {}, {}", renderingId, response);
                event.replyMention(String.format("Encountered a rendering error, `%s`" + (renderingId == -1 ? "" : "\nRendering id: `%d`"), response, renderingId));
                return null;
            }
        } catch (SocketTimeoutException e) {
            PolyBot.getLogger().warn("Rendering task {} exceeded timeout of 15 minutes!", renderingId);
             if (message != null) message.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            event.replyMention("Task `" + renderingId + "` exceeded timeout of 15 minutes!");
            return null;
        } catch (IOException e) {
            PolyBot.getLogger().warn("Exception in connection while rendering task " + renderingId, e);
            if (message != null) message.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            event.replyMention("An internal error occurred in the connection between the renderer, task id=`" + renderingId + "`");
        } finally {
            if (values != null) {
                try {
                    values.getSecond().disconnect(values.getFirst());
                } catch (IOException ignored) {}
            }
        }

        return null;
    }
/*
    public static TwoValues<String, byte[]> fetchMedia(int id) {
        for (Configuration.Renderer renderer : Configuration.getRenderers()) {
            if (!renderer.enabled) continue;

            try (InputStream stream = new URL(renderer.getWebAddress() + id).openStream()) {
                byte[] bytes = stream.readAllBytes();

                if (Arrays.equals(bytes, "{\"message\":\"File requested not found.\"}".getBytes())) return "File not found".getBytes();
                if (Message.MAX_FILE_SIZE < bytes.length) return "File exceeds upload size limit".getBytes();
                return new TwoValues<>(bytes;
            } catch (IOException e) {
                PolyBot.getLogger().warn("Failed fetching media", e);
            }
        }

        return "File not found".getBytes();
    }
*/
    private static TwoValues<Socket, Configuration.Renderer> searchForRenderer()  {
        Configuration.Renderer renderer = Configuration.searchForRenderer();
        if (renderer == null) return null;

        try {
            Socket socket = renderer.connect();
            return new TwoValues<>(socket, renderer);
        } catch (IOException e) {
            PolyBot.getLogger().warn("Failed to connect to renderer {}:{}", renderer.getAddress(), renderer.getServerPort());
        }

        return null;
    }

    private static int getEmojiStringWidth(FontMetrics metrics, String line) {
        int width = 0;
        for (char c : line.toCharArray()) {
            if (c < 0x2000)  width+=metrics.charWidth(c);
        }

        return width;
    }

    private static String stripInvalid(String str) {
        StringBuilder builder = new StringBuilder();

        for (char c : str.toCharArray()) {
            if (c > 0x2000) continue;

            builder.append(c);
        }

        return builder.toString().trim();
    }

    public static boolean invalidCaptionArgs(String[] args) {
        for (String str : args) {
            if (!(str = stripInvalid(str)).isEmpty() && !str.isBlank()) return false;
        }

        return true;
    }

    private static boolean hitLineLimit(int width, FontMetrics metrics, String str) {
        return metrics.stringWidth(str) + metrics.getHeight()*2 > width - ((width / 25) * 2);
    }
}

enum LoadingEmoji {
    CAT_LOADING("<:catloading:805383938604138506>"),
    CONGA("<a:CONGAAAA:929507688637956207>"),
    NECO_CALL("<a:necocalling:897227418086019093>"),
    NECO_SPIN("<a:necospin:897227417997963266>"),
    TOM("<a:sitomhollandescucharabuenamusica:928446432090935307>"),
    WAITING("<:waiting:873802238462685245>"),
    WAWA("<a:wawaevolved:897219687753068574>");

    final String code;

    LoadingEmoji(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }

    public static String random() {
        return LoadingEmoji.values()[ThreadLocalRandom.current().nextInt(0, LoadingEmoji.values().length)].code;
    }
}

record ClientRequest(String name, String link, String args, String parameters) {

    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public String toString() {
        return toJson();
    }
}
