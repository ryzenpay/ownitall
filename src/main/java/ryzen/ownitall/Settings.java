package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import ryzen.ownitall.library.LastFM;
import ryzen.ownitall.method.Jellyfin;
import ryzen.ownitall.method.Spotify;
import ryzen.ownitall.method.Upload;
import ryzen.ownitall.method.Youtube;
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

    /**
     * Constant <code>logo="                        _  _          _"{trunked}</code>
     */
    public static final String logo = "                        _  _          _  _ \n" +
            "                       (_)| |        | || |\n" +
            "  ___ __      __ _ __   _ | |_  __ _ | || |\n" +
            " / _  \\ \\ /\\ / /| '_ \\ | || __|/ _` || || |\n" +
            "| (_) |\\ V  V / | | | || || |_| (_| || || |\n" +
            " \\___/  \\_/\\_/  |_| |_||_| \\__|\\__,_||_||_|\n" +
            "                ";

    /** Constant <code>interactive=true</code> */
    public static boolean interactive = true;

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
    @Options(options = { "LastFM", "MusicBrainz" })
    public static String libraryType = null;

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
    @Group(group = { Download.class, YT_dl.class, SoulSeek.class }, desc = "Format(ex: mp3)")
    @Options(options = { "mp3", "flac", "wav" })
    public static String downloadFormat = "mp3";
    /**
     * class representative of which download class to use
     * default is YT_dl.class
     */
    @Group(group = { Download.class }, desc = "Method")
    @Options(options = { "YT_dl", "SoulSeek" })
    public static String downloadMethod = "YT_dl";

    /**
     * local library path
     */
    @Group(group = { Download.class, Upload.class, YT_dl.class, SoulSeek.class }, desc = "Local Library")
    public static File localFolder = null;

    /**
     * YT_dl credentials
     */
    /**
     * ffmpeg path (required for youtubedl)
     * 
     */
    @Group(group = { YT_dl.class }, desc = "FFMPEG file/folder")
    public static File ffmpegFile = null;

    /**
     * youtube dl installation path
     * 
     */
    @Group(group = { YT_dl.class }, desc = "Binary")
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

    /**
     * SoulSeek Credentials
     */

    /**
     * soulseek dl installation path
     */
    @Group(group = { SoulSeek.class }, desc = "Binary")
    public static File soulSeekFile = null;

    @Group(group = { SoulSeek.class }, desc = "Username")
    @Secret
    /** Constant <code>soulSeekUsername=""</code> */
    public static String soulSeekUsername = "";

    @Group(group = { SoulSeek.class }, desc = "Password")
    @Secret
    public static String soulSeekPassword = "";

    /**
     * spotify credentials
     */
    @Group(group = { Spotify.class }, desc = "Client ID")
    @Secret
    public static String spotifyClientID = "";
    @Group(group = { Spotify.class }, desc = "Client Secret")
    @Secret
    /** Constant <code>spotifyClientSecret=""</code> */
    public static String spotifyClientSecret = "";
    @Group(group = { Spotify.class }, desc = "Redirect URL")
    @Secret
    /** Constant <code>spotifyRedirectURL=""</code> */
    public static String spotifyRedirectURL = "";

    /**
     * youtube credentials
     * 
     */
    @Group(group = { Youtube.class }, desc = "Application Name")
    @Secret
    public static String youtubeApplicatioName = "";
    @Group(group = { Youtube.class }, desc = "Client ID")
    @Secret
    /** Constant <code>youtubeClientID=""</code> */
    public static String youtubeClientID = "";
    @Group(group = { Youtube.class }, desc = "Client Secret")
    @Secret
    /** Constant <code>youtubeClientSecret=""</code> */
    public static String youtubeClientSecret = "";

    /**
     * Last FM Credentials
     * 
     */

    @Group(group = { LastFM.class }, desc = "API Key")
    @Secret
    public static String lastFMApiKey = "";

    /**
     * Jellyfin Credentials
     * 
     */
    @Group(group = { Jellyfin.class }, desc = "URL")
    @Secret
    public static String jellyfinURL = "";
    @Group(group = { Jellyfin.class }, desc = "Username")
    @Secret
    /** Constant <code>jellyfinUsername=""</code> */
    public static String jellyfinUsername = "";
    @Group(group = { Jellyfin.class }, desc = "Password")
    @Secret
    /** Constant <code>jellyfinPassword=""</code> */
    public static String jellyfinPassword = "";

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
    public void set(String name, Object value) throws NoSuchFieldException {
        super.set(name, value);
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

    /** {@inheritDoc} */
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
