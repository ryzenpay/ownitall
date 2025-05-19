package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import ryzen.ownitall.library.LastFM;
import ryzen.ownitall.method.Jellyfin;
import ryzen.ownitall.method.Spotify;
import ryzen.ownitall.method.Youtube;
import ryzen.ownitall.method.download.SoulSeek;
import ryzen.ownitall.util.Logger;

/**
 * <p>
 * Credentials class.
 * </p>
 *
 * @author ryzen
 */
public class Credentials extends ryzen.ownitall.util.Settings {

    private static Credentials instance;

    private static final Logger logger = new Logger(Credentials.class);

    /**
     * spotify credentials
     */
    @SettingsGroup(group = { Spotify.class }, desc = "Client ID")
    public static String spotifyClientID = "";
    @SettingsGroup(group = { Spotify.class }, desc = "Client Secret")
    /** Constant <code>spotifyClientSecret=""</code> */
    public static String spotifyClientSecret = "";
    @SettingsGroup(group = { Spotify.class }, desc = "Redirect URL")
    /** Constant <code>spotifyRedirectURL=""</code> */
    public static String spotifyRedirectURL = "";

    /**
     * youtube credentials
     * 
     */
    @SettingsGroup(group = { Youtube.class }, desc = "Application Name")
    public static String youtubeApplicatioName = "";
    @SettingsGroup(group = { Youtube.class }, desc = "Client ID")
    /** Constant <code>youtubeClientID=""</code> */
    public static String youtubeClientID = "";
    @SettingsGroup(group = { Youtube.class }, desc = "Client Secret")
    /** Constant <code>youtubeClientSecret=""</code> */
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
    /** Constant <code>jellyfinUsername=""</code> */
    public static String jellyfinUsername = "";
    @SettingsGroup(group = { Jellyfin.class }, desc = "Password")
    /** Constant <code>jellyfinPassword=""</code> */
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
    /** Constant <code>soulSeekUsername=""</code> */
    public static String soulSeekUsername = "";

    @SettingsGroup(group = { SoulSeek.class }, desc = "Password")
    /** Constant <code>soulSeekPassword=""</code> */
    public static String soulSeekPassword = "";

    private Credentials() throws IOException {
        super("credentials.json");
    }

    /**
     * <p>
     * load.
     * </p>
     *
     * @return a {@link ryzen.ownitall.Credentials} object
     */
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

    /**
     * <p>
     * save.
     * </p>
     */
    public void save() {
        super.save();
    }

    /**
     * <p>
     * clear.
     * </p>
     */
    public void clear() {
        instance = null;
        super.clear();
    }

    /** {@inheritDoc} */
    public boolean set(String name, Object value) {
        return super.set(name, value);
    }

    /** {@inheritDoc} */
    public boolean isEmpty(String name) {
        return super.isEmpty(name);
    }

    public boolean isGroupEmpty(Class<?> group) {
        return super.isGroupEmpty(group);
    }

    /** {@inheritDoc} */
    public Object get(String name) {
        return super.get(name);
    }

    /** {@inheritDoc} */
    public LinkedHashMap<String, String> getGroup(Class<?> groupClass) {
        return super.getGroup(groupClass);
    }
}
