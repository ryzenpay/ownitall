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
    protected static String spotifyclientid = "";
    protected static String spotifyclientsecret = "";
    protected static String spotifyredirecturl = "";

    public final LinkedHashMap<String, String> getSpotifyCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("Spotify Client ID", "spotifyclientid");
        credentials.put("Spotify Client Secret", "spotifyclientsecret");
        credentials.put("Spotify Redirect URL", "spotifyredirecturl");
        return credentials;
    }

    /**
     * youtube credentials
     * 
     */
    protected static String youtubeapplicationame = "";
    protected static String youtubeclientid = "";
    protected static String youtubeclientsecret = "";

    public final LinkedHashMap<String, String> getYoutubeCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("Youtube Application Name", "youtubeapplicationame");
        credentials.put("Youtube Client ID", "youtubeclientid");
        credentials.put("Youtube Client Secret", "youtubeclientsecret");
        return credentials;
    }

    /**
     * Last FM Credentials
     * 
     */
    protected static String lastfmapikey = "";

    public final LinkedHashMap<String, String> getLastFMCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("LastFM API Key", "lastfmapikey");
        return credentials;
    }

    /**
     * Jellyfin Credentials
     * 
     */
    protected static String jellyfinurl = "";
    protected static String jellyfinusername = "";
    protected static String jellyfinpassword = "";

    public final LinkedHashMap<String, String> getJellyfinCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("JellyFin URL", "jellyfinurl");
        credentials.put("JellyFin Username", "jellyfinusername");
        credentials.put("JellyFin Password", "jellyfinpassword");
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
                logger.info("If this error persists, try to delete credentials.json");
            }
        }
        return instance;
    }

    public void save() {
        if (instance == null) {
            load();
        }
        super.save();
    }

    // TODO: limit?
    public boolean change(String name, Object value) {
        return super.change(name, value);
    }

    public void clear() {
        instance = null;
        super.clear();
    }
}
