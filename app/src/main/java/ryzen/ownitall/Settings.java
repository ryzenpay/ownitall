package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Settings extends ryzen.ownitall.util.Settings {

    private static Settings instance;

    private static final Logger logger = LogManager.getLogger();

    public Settings() throws IOException {
        super(Settings.class, "settings.json");
    }
    // the defaults: (non final & protected for the ones that can be changed by
    // user)

    /**
     * default file names (without extensions)
     */
    protected File datafolder = new File("data");
    protected String likedsongsname = "liked songs";
    protected String albumfile = "albums";
    protected String likedsongfile = "likedsongs";
    protected String playlistfile = "playlists";
    protected String artistfile = "artists";
    protected String songfile = "songs";

    protected File cachefolder = new File(".cache");
    /**
     * to save credentials of anything 3rd party logins (youtube, spotify etc)
     * to prevent having to provide them each time
     */
    protected boolean savecredentials = true;

    /**
     * in the Spotify class, this decides if the user has to click "accept"
     * everytime they "log in", set to true in case you use multiple accounts and
     * want to easily switch between them
     */
    protected boolean spotifyshowdialog = false;

    /**
     * to limit number of songs in each spotify API batch query
     */
    protected Integer spotifysonglimit = 50;
    protected Integer spotifyalbumlimit = 20;
    protected Integer spotifyplaylistlimit = 20;

    /**
     * to limit number of songs in each youtube API batch query
     */
    protected Long youtubesonglimit = 50L;
    protected Long youtubeplaylistlimit = 20L;

    /**
     * only put songs in collection if they are library verified
     */
    protected boolean libraryverified = true;

    /**
     * int representative of which library to use
     */
    protected String librarytype = "";

    /**
     * youtube dl installation path
     * 
     */
    protected File youtubedlfile = null;
    /**
     * ffmpeg path (required for youtubedl)
     * 
     */
    protected File ffmpegfile = null;
    /**
     * format of music to download
     * current supported: "mp3", "flac", "wav"
     */
    protected String downloadformat = "mp3";

    /**
     * optional to hardcode local download path
     */
    protected File downloadfolder = null;

    /**
     * option to hardcode cookies file
     */
    protected File downloadcookiesfile = null;

    /**
     * option to hardcode browser to get cookies from
     * options: chrome, firefox, check yt-dlp docs,...
     */
    protected String downloadcookiesbrowser = "";

    /**
     * download all files in a hierachy method
     * playlists get their own folders
     * most applications such as jellyfin use false
     */
    protected boolean downloadhierachy = false;

    protected boolean downloadlikedsongsplaylist = true;

    /**
     * download quality of music
     * 0 - best, 10 - worst
     * also respectfully increases file size
     */
    protected Integer downloadquality = 5;
    /**
     * enable yt-dlp threading
     */
    protected Integer downloadthreads = 1;

    /**
     * optional to hardcode local upload path
     */
    protected File uploadfolder = null;

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
