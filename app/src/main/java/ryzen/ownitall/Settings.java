package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Settings extends ryzen.ownitall.util.Settings {

    private static Settings instance;

    private static final Logger logger = LogManager.getLogger();

    // the defaults: (non final & protected for the ones that can be changed by
    // user)

    /**
     * default file names (without extensions)
     */
    protected static File datafolder = new File("data");
    protected static String likedsongsname = "liked songs";
    protected static String albumfile = "albums";
    protected static String likedsongfile = "likedsongs";
    protected static String playlistfile = "playlists";
    protected static String artistfile = "artists";
    protected static String songfile = "songs";

    protected static File cachefolder = new File(".cache");
    /**
     * to save credentials of anything 3rd party logins (youtube, spotify etc)
     * to prevent having to provide them each time
     */
    protected static boolean savecredentials = true;

    /**
     * in the Spotify class, this decides if the user has to click "accept"
     * everytime they "log in", set to true in case you use multiple accounts and
     * want to easily switch between them
     */
    protected static boolean spotifyshowdialog = false;

    /**
     * to limit number of songs in each spotify API batch query
     */
    protected static int spotifysonglimit = 50;
    protected static int spotifyalbumlimit = 20;
    protected static int spotifyplaylistlimit = 20;

    /**
     * to limit number of songs in each youtube API batch query
     */
    protected static Long youtubesonglimit = 50L;
    protected static Long youtubeplaylistlimit = 20L;

    /**
     * only put songs in collection if they are library verified
     */
    protected static boolean libraryverified = true;

    /**
     * int representative of which library to use
     */
    protected static String librarytype = "";

    /**
     * youtube dl installation path
     * 
     */
    protected static File youtubedlfile = null;
    /**
     * ffmpeg path (required for youtubedl)
     * 
     */
    protected static File ffmpegfile = null;
    /**
     * format of music to download
     * current supported: "mp3", "flac", "wav"
     */
    protected static String downloadformat = "mp3";

    /**
     * optional to hardcode local download path
     */
    protected static File downloadfolder = null;

    /**
     * option to hardcode cookies file
     */
    protected static File downloadcookiesfile = null;

    /**
     * option to hardcode browser to get cookies from
     * options: chrome, firefox, check yt-dlp docs,...
     */
    protected static String downloadcookiesbrowser = "";

    /**
     * download all files in a hierachy method
     * playlists get their own folders
     * most applications such as jellyfin use false
     */
    protected static boolean downloadhierachy = false;

    protected static boolean downloadlikedsongsplaylist = true;

    /**
     * download quality of music
     * 0 - best, 10 - worst
     * also respectfully increases file size
     */
    protected static int downloadquality = 5;
    /**
     * enable yt-dlp threading
     */
    protected static int downloadthreads = 1;

    /**
     * optional to hardcode local upload path
     */
    protected static File uploadfolder = null;

    public Settings() throws IOException {
        super("settings.json");
    }

    public static Settings load() {
        if (instance == null) {
            try {
                instance = new Settings();
                logger.debug("New Settings instance created");
            } catch (IOException e) {
                logger.error("Failed to initialize settings", e);
                logger.info("If this error persists, try to delete settings.json");
            }
        }
        return instance;
    }

    public void clear() {
        instance = null;
        super.clear();
    }

    public LinkedHashMap<String, Object> getAll() {
        return super.getAll();
    }
}
