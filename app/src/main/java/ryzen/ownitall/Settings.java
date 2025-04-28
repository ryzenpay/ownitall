package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.library.Library;
import ryzen.ownitall.method.download.Download;
import ryzen.ownitall.method.download.YT_dl;

public class Settings extends ryzen.ownitall.util.Settings {

    private static Settings instance;

    private static final Logger logger = LogManager.getLogger();

    // the defaults: (non final & protected for the ones that can be changed by
    // user)

    /**
     * default file names (without extensions)
     */
    public static File dataFolder = new File("data");
    public static String likedSongName = "liked songs";
    public static String albumFile = "albums";
    public static String likedSongFile = "likedsongs";
    public static String playlistFile = "playlists";
    public static String artistFile = "artists";
    public static String songFile = "songs";

    public static File cacheFolder = new File(".cache");
    /**
     * to save credentials of anything 3rd party logins (youtube, spotify etc)
     * to prevent having to provide them each time
     */
    public static boolean saveCredentials = true;

    /**
     * in the Spotify class, this decides if the user has to click "accept"
     * everytime they "log in", set to true in case you use multiple accounts and
     * want to easily switch between them
     */
    public static boolean spotifyShowdialog = true;

    /**
     * to limit number of songs in each spotify API batch query
     */
    public static int spotifySongLimit = 50;
    public static int spotifyAlbumLimit = 20;
    public static int spotifyPlaylistLimit = 20;

    /**
     * to limit number of songs in each youtube API batch query
     */
    public static Long youtubeSongLimit = 50L;
    public static Long youtubePlaylistLimit = 20L;

    /**
     * only put songs in collection if they are library verified
     */
    public static boolean libraryVerified = true;

    /**
     * class representative of which library to use
     * null for none
     */
    public static Class<? extends Library> libraryType = null;

    /**
     * class representative of which download class to use
     * default is YT_dl.class
     */
    public static Class<? extends Download> downloadType = YT_dl.class;
    /**
     * format of music to download
     * current supported: "mp3", "flac", "wav"
     */
    public static String downloadFormat = "mp3";

    /**
     * option to hardcode cookies file
     */
    public static File yt_dlCookieFile = null;

    /**
     * option to hardcode browser to get cookies from
     * options: chrome, firefox, check yt-dlp docs,...
     */
    public static String yt_dlCookieBrowser = "";

    /**
     * download all files in a hierachy method
     * if true:
     * playlists get their own folders
     * albums still have their own folders
     * liked songs are placed in likedsongs folder
     * if false:
     * most applications such as jellyfin use false
     * albums still have their own folders
     * playlists and liked songs are merged in root folder with m3u files
     */
    public static boolean downloadHierachy = false;

    /**
     * incase download hierachy is set to false but still want an m3u
     */
    public static boolean downloadLikedsongPlaylist = true;

    /**
     * download quality of music
     * 0 - best, 10 - worst
     * also respectfully increases file size
     */
    public static int yt_dlQuality = 5;
    /**
     * enable yt-dlp threading
     */
    public static int downloadThreads = 1;

    private Settings() throws IOException {
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

    public LinkedHashMap<String, Object> getAll() {
        return super.getAll();
    }
}
