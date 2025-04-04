package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Credentials extends ryzen.ownitall.util.Settings {

    private static final Logger logger = LogManager.getLogger();

    public Credentials() throws IOException {
        super(Credentials.class, "credentials.json");
    }

    private static Credentials instance;
    /**
     * spotify credentials
     */
    protected String spotifyclientid = "";
    protected String spotifyclientsecret = "";
    protected String spotifyredirecturl = "";

    public LinkedHashMap<String, String> getSpotifyCredentials() {
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
    protected String youtubeapplicationame = "";
    protected String youtubeclientid = "";
    protected String youtubeclientsecret = "";

    public LinkedHashMap<String, String> getYoutubeCredentials() {
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
    protected String lastfmapikey = "";

    public LinkedHashMap<String, String> getLastFMCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("LastFM API Key", "lastfmapikey");
        return credentials;
    }

    /**
     * Jellyfin Credentials
     * 
     */
    protected String jellyfinurl = "";
    protected String jellyfinusername = "";
    protected String jellyfinpassword = "";

    public LinkedHashMap<String, String> getJellyfinCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("JellyFin URL", "jellyfinurl");
        credentials.put("JellyFin Username", "jellyfinusername");
        credentials.put("JellyFin Password", "jellyfinpassword");
        return credentials;
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

    public void clear() {
        instance = null;
        super.clear();
    }
}
