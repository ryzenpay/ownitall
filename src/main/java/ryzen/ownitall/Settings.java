package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import ryzen.ownitall.library.Library;
import ryzen.ownitall.method.Upload;
import ryzen.ownitall.method.download.Download;
import ryzen.ownitall.method.download.SoulSeek;
import ryzen.ownitall.method.download.YT_dl;
import ryzen.ownitall.util.Logger;

/**
 * <p>
 * Settings class.
 * </p>
 *
 * @author ryzen
 */
public class Settings extends ryzen.ownitall.util.Settings {
    private static Settings instance;
    private static final Logger logger = new Logger(Settings.class);

    // the defaults: (non final & protected for the ones that can be changed by
    // user)

    public static final String logo = "                        _  _          _  _ \n" +
            "                       (_)| |        | || |\n" +
            "  ___ __      __ _ __   _ | |_  __ _ | || |\n" +
            " / _  \\ \\ /\\ / /| '_ \\ | || __|/ _` || || |\n" +
            "| (_) |\\ V  V / | | | || || |_| (_| || || |\n" +
            " \\___/  \\_/\\_/  |_| |_||_| \\__|\\__,_||_||_|\n" +
            "                ";

    /**
     * default file names (without extensions)
     */
    public static File dataFolder = new File("data");
    /** Constant <code>likedSongName="liked songs"</code> */
    public static String likedSongName = "liked songs";
    /** Constant <code>albumFile="albums"</code> */
    public static String albumFile = "albums";
    /** Constant <code>likedSongFile="likedsongs"</code> */
    public static String likedSongFile = "likedsongs";
    /** Constant <code>playlistFile="playlists"</code> */
    public static String playlistFile = "playlists";
    /** Constant <code>artistFile="artists"</code> */
    public static String artistFile = "artists";
    /** Constant <code>songFile="songs"</code> */
    public static String songFile = "songs";

    /** Constant <code>cacheFolder</code> */
    public static File cacheFolder = new File(".cache");

    /**
     * in the Spotify class, this decides if the user has to click "accept"
     * everytime they "log in", set to true in case you use multiple accounts and
     * want to easily switch between them
     */
    public static boolean spotifyShowdialog = true;

    public static boolean spotifyInteractiveLogin = true;

    /**
     * to limit number of songs in each spotify API batch query
     */
    public static int spotifySongLimit = 50;
    /** Constant <code>spotifyAlbumLimit=20</code> */
    public static int spotifyAlbumLimit = 20;
    /** Constant <code>spotifyPlaylistLimit=20</code> */
    public static int spotifyPlaylistLimit = 20;

    /**
     * to limit number of songs in each youtube API batch query
     */
    public static Long youtubeSongLimit = 50L;
    /** Constant <code>youtubePlaylistLimit</code> */
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
     * option to hardcode cookies file
     */
    public static File yt_dlCookieFile = null;

    /**
     * option to hardcode browser to get cookies from
     * options: chrome, firefox, check yt-dlp docs,...
     */
    public static String yt_dlCookieBrowser = "";
    /**
     * format of music to download
     * current supported: "mp3", "flac", "wav"
     */
    @SettingsGroup(group = { Download.class, YT_dl.class, SoulSeek.class }, desc = "Format(ex: mp3)")
    public static String downloadFormat = "mp3";
    /**
     * class representative of which download class to use
     * default is YT_dl.class
     */
    @SettingsGroup(group = { Download.class }, desc = "Method")
    public static Class<? extends Download> downloadMethod = YT_dl.class;

    /**
     * local library path
     */
    @SettingsGroup(group = { Download.class, Upload.class, YT_dl.class, SoulSeek.class }, desc = "Local Library")
    public static File localFolder = null;

    /**
     * YT_dl credentials
     */
    /**
     * ffmpeg path (required for youtubedl)
     * 
     */
    @SettingsGroup(group = { YT_dl.class }, desc = "FFMPEG file/folder")
    public static File ffmpegFile = null;

    /**
     * youtube dl installation path
     * 
     */
    @SettingsGroup(group = { YT_dl.class }, desc = "Binary")
    public static File yt_dlFile = null;
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

    /** Constant <code>soulSeekBitRate=320</code> */
    public static int soulSeekBitRate = 320;

    private Settings() throws IOException {
        super("settings.json");
    }

    /**
     * <p>
     * load.
     * </p>
     *
     * @return a {@link ryzen.ownitall.Settings} object
     */
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

    /**
     * <p>
     * getAll.
     * </p>
     *
     * @return a {@link java.util.LinkedHashMap} object
     */
    public LinkedHashMap<String, Object> getAll() {
        return super.getAll();
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
