package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.library.LastFM;
import ryzen.ownitall.method.Jellyfin;
import ryzen.ownitall.method.Spotify;
import ryzen.ownitall.method.Youtube;
import ryzen.ownitall.method.download.SoulSeek;

public class Credentials extends ryzen.ownitall.util.Settings {

    private static Credentials instance;

    private static final Logger logger = LogManager.getLogger(Credentials.class);

    /**
     * spotify credentials
     */
    @SettingsGroup(group = { Spotify.class }, desc = "Client ID")
    public static String spotifyClientID = "";
    @SettingsGroup(group = { Spotify.class }, desc = "Client Secret")
    public static String spotifyClientSecret = "";
    @SettingsGroup(group = { Spotify.class }, desc = "Redirect URL")
    public static String spotifyRedirectURL = "";

    /**
     * youtube credentials
     * 
     */
    @SettingsGroup(group = { Youtube.class }, desc = "Application Name")
    public static String youtubeApplicatioName = "";
    @SettingsGroup(group = { Youtube.class }, desc = "Client ID")
    public static String youtubeClientID = "";
    @SettingsGroup(group = { Youtube.class }, desc = "Client Secret")
    public static String youtubeClientSecret = "";

    /**
     * Last FM Credentials
     * 
     */

    @SettingsGroup(group = { LastFM.class }, desc = "API Key")
    public static String lastFMApiKey = "";

    /**
     * Jellyfin Credentials
     * 
     */
    @SettingsGroup(group = { Jellyfin.class }, desc = "URL")
    public static String jellyfinURL = "";
    @SettingsGroup(group = { Jellyfin.class }, desc = "Username")
    public static String jellyfinUsername = "";
    @SettingsGroup(group = { Jellyfin.class }, desc = "Password")
    public static String jellyfinPassword = "";

    /**
     * SoulSeek Credentials
     */

    /**
     * soulseek dl installation path
     */
    @SettingsGroup(group = { SoulSeek.class }, desc = "Binary")
    public static File soulSeekFile = null;

    @SettingsGroup(group = { SoulSeek.class }, desc = "Username")
    public static String soulSeekUsername = "";

    @SettingsGroup(group = { SoulSeek.class }, desc = "Password")
    public static String soulSeekPassword = "";

    private Credentials() throws IOException {
        super("credentials.json");
    }

    public static Credentials load() {
        if (instance == null) {
            try {
                instance = new Credentials();
                logger.debug("New Credentials instance created");
            } catch (IOException e) {
                logger.error("Failed to initialize credentials", e);
                logger.info("If this error persists, try to delete credentials.json");
            }
        }
        return instance;
    }

    public void save() {
        super.save();
    }

    public void clear() {
        instance = null;
        super.clear();
    }

    public boolean set(String name, Object value) {
        return super.set(name, value);
    }

    public boolean isEmpty(String name) {
        return super.isEmpty(name);
    }

    public Object get(String name) {
        return super.get(name);
    }

    public LinkedHashMap<String, String> getGroup(Class<?> groupClass) {
        return super.getGroup(groupClass);
    }
}
