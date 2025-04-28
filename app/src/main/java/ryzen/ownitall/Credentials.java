package ryzen.ownitall;

import java.io.File;
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
        credentials.put("Spotify Client ID", "spotifyClientID");
        credentials.put("Spotify Client Secret", "spotifyClientSecret");
        credentials.put("Spotify Redirect URL", "spotifyRedirectURL");
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
        credentials.put("Youtube Application Name", "youtubeApplicatioName");
        credentials.put("Youtube Client ID", "youtubeClientID");
        credentials.put("Youtube Client Secret", "youtubeClientSecret");
        return credentials;
    }

    /**
     * Last FM Credentials
     * 
     */
    public static String lastFMApiKey = "";

    public static final LinkedHashMap<String, String> getLastFMCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("LastFM API Key", "lastFMApiKey");
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
        credentials.put("JellyFin URL", "jellyfinURL");
        credentials.put("JellyFin Username", "jellyfinUsername");
        credentials.put("JellyFin Password", "jellyfinPassword");
        return credentials;
    }

    /**
     * Local credentials
     * 
     */
    /**
     * optional to hardcode local upload path
     */
    public static File localFolder = null;

    public static final LinkedHashMap<String, String> getUploadCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("Local Folder", "localFolder");
        return credentials;
    }

    /**
     * ffmpeg path (required for youtubedl)
     * 
     */
    public static File ffmpegFile = null;

    /**
     * youtube dl installation path
     * 
     */
    public static File yt_dlFile = null;

    public static final LinkedHashMap<String, String> getYT_dlCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.putAll(getUploadCredentials());
        credentials.put("YT_dl File", "yt_dlFile");
        credentials.put("FFMPeg File", "ffmpegFile");
        return credentials;
    }

    private Credentials() throws IOException {
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

    public boolean set(String name, Object value) {
        return super.set(name, value);
    }

    public void clear() {
        instance = null;
        super.clear();
    }

    public LinkedHashMap<String, Object> getAll() {
        return super.getAll();
    }

    public boolean isEmpty(String name) {
        return super.isEmpty(name);
    }
}
