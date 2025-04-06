package ryzen.ownitall;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Credentials extends ryzen.ownitall.util.Settings {
    private static final Logger logger = LogManager.getLogger();

    private static Credentials instance;
    /**
     * spotify credentials
     */
    public static String spotifyClientID = "";
    public static String spotifyClientSecret = "";
    public static String spotifyRedirectURL = "";

    public static final LinkedHashMap<String, String> getSpotifyCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("Spotify Client ID", spotifyClientID);
        credentials.put("Spotify Client Secret", spotifyClientSecret);
        credentials.put("Spotify Redirect URL", spotifyRedirectURL);
        return credentials;
    }

    /**
     * youtube credentials
     * 
     */
    public static String youtubeApplicatioName = "";
    public static String youtubeClientID = "";
    public static String youtubeClientSecret = "";

    public static final LinkedHashMap<String, String> getYoutubeCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("Youtube Application Name", youtubeApplicatioName);
        credentials.put("Youtube Client ID", youtubeClientID);
        credentials.put("Youtube Client Secret", youtubeClientSecret);
        return credentials;
    }

    /**
     * Last FM Credentials
     * 
     */
    public static String lastFMApiKey = "";

    public static final LinkedHashMap<String, String> getLastFMCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("LastFM API Key", lastFMApiKey);
        return credentials;
    }

    /**
     * Jellyfin Credentials
     * 
     */
    public static String jellyfinURL = "";
    public static String jellyfinUsername = "";
    public static String jellyfinPassword = "";

    public static final LinkedHashMap<String, String> getJellyfinCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("JellyFin URL", jellyfinURL);
        credentials.put("JellyFin Username", jellyfinUsername);
        credentials.put("JellyFin Password", jellyfinPassword);
        return credentials;
    }

    public Credentials() throws IOException {
        super("credentials.json");
    }

    public static Credentials load() {
        if (instance == null) {
            try {
                instance = new Credentials();
                logger.debug("New Credential instance created");
            } catch (IOException e) {
                logger.error("Failed to initialize credentials", e);
            }
        }
        return instance;
    }

    public void save() {
        super.save();
    }

    public boolean change(String name, Object value) {
        return super.change(name, value);
    }

    public void clear() {
        instance = null;
        super.clear();
    }

    public LinkedHashMap<String, Object> getAll() {
        return super.getAll();
    }
}
