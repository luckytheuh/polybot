package polybot.storage;

import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;
import polybot.PolyBot;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Configuration {
    private static final List<Renderer> RENDERERS = new ArrayList<>();
    private static final Properties PROPERTIES = new Properties();

    public static void reload() {
        RENDERERS.clear();
        PROPERTIES.clear();

        try {
            PROPERTIES.load(Files.newBufferedReader(Paths.get("bot.properties")));
            for (String str : getSetting("renderers")) {
                String[] args = str.split(",");
                RENDERERS.add(new Renderer(args[0], Boolean.parseBoolean(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])));
            }

            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            if (getSetting("debugging", false)) root.setLevel(Level.DEBUG);
            else root.setLevel(Level.INFO);
        } catch (IOException e) {
            PolyBot.getLogger().warn("Failed to load configuration");
        }
    }

    public static String getSetting(String key, String def) {
        return PROPERTIES.getProperty(key, def);
    }

    public static List<String> getSetting(String key) {
        String value = getSetting(key, "");
        if (value.isBlank() || value.isEmpty()) return Collections.emptyList();
        return List.of(value.split("\\|"));
    }

    public static int getSetting(String key, int def) {
        try {
            return Integer.parseInt(PROPERTIES.getProperty(key, Integer.toString(def)));
        } catch (NumberFormatException ignored) {}
        return def;
    }

    public static long getSetting(String key, long def) {
        try {
            return Long.parseLong(PROPERTIES.getProperty(key, Long.toString(def)));
        } catch (NumberFormatException ignored) {}
        return def;
    }

    public static boolean getSetting(String key, boolean def) {
        return Boolean.parseBoolean(getSetting(key, String.valueOf(def)));
    }

    public static List<Renderer> getRenderers() {
        return new ArrayList<>(RENDERERS);
    }

    public static Renderer searchForRenderer() {
        Optional<Renderer> renderer = RENDERERS.stream().filter(r -> r.enabled).min(Comparator.comparingInt(Renderer::getActiveTasks));
        return renderer.orElse(null);
    }

    public static class Renderer {
        private final int srvPort, webPort;
        private final String address;
        private int activeTasks;
        public boolean enabled;

        public Renderer(String address, boolean enabled, int srvPort, int webPort) {
            this.address = address;
            this.enabled = enabled;
            this.srvPort = srvPort;
            this.webPort = webPort;
        }

        public Socket connect() throws IOException {
            if (!enabled) return null;
            Socket socket = new Socket(address, srvPort);
            activeTasks++;
            return socket;
        }

        public void disconnect(Socket socket) throws IOException {
            activeTasks--;
            socket.close();
        }

        public int getActiveTasks() {
            return activeTasks;
        }

        public String getWebAddress() {
            return "http://" + address + ':' + webPort + '/';
        }

        public String getAddress() {
            return address;
        }

        public int getServerPort() {
            return srvPort;
        }

        public int getWebPort() {
            return webPort;
        }
    }
}


