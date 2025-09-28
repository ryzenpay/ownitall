package ryzen.ownitall;

import java.io.File;
import java.io.IOException;

import ryzen.ownitall.library.LastFM;
import ryzen.ownitall.method.Jellyfin;
import ryzen.ownitall.method.Spotify;
import ryzen.ownitall.method.Tidal;
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
    @Name("Data Folder")
    public static File dataFolder = new File("data");
    /** Constant <code>likedSongName="liked songs"</code> */
    @Name("Liked Songs Playlist Name")
    public static String likedSongName = "liked songs";
    /** Constant <code>albumFile="albums"</code> */
    @Name("Album Collection File Name")
    public static String albumFile = "albums";
    /** Constant <code>likedSongFile="likedsongs"</code> */
    @Name("LikedSongs Collection File Name")
    public static String likedSongFile = "likedsongs";
    /** Constant <code>playlistFile="playlists"</code> */
    @Name("Playlist Collection File Name")
    public static String playlistFile = "playlists";
    /** Constant <code>artistFile="artists"</code> */
    @Name("Artist Collection File Name")
    public static String artistFile = "artists";
    /** Constant <code>songFile="songs"</code> */
    @Name("Songs Collection File Name")
    public static String songFile = "songs";

    /** Constant <code>cacheFolder</code> */
    @Name("Cache Folder")
    public static File cacheFolder = new File(".cache");

    ///
    ///
    /// METHODS
    ///
    ///

    ///
    ///
    /// JELLYFIN
    ///
    ///
    @Group(Jellyfin.class)
    @Name("JellyFin URL")
    /** Constant <code>jellyfinURL=""</code> */
    public static String jellyfinURL = "";
    @Group(Jellyfin.class)
    @Name("JellyFin Username")
    /** Constant <code>jellyfinUsername=""</code> */
    public static String jellyfinUsername = "";
    @Group(Jellyfin.class)
    @Secret
    @Name("JellyFin Password")
    /** Constant <code>jellyfinPassword=""</code> */
    public static String jellyfinPassword = "";

    ///
    ///
    /// Spotify
    ///
    ///

    /**
     * to limit number of songs in each spotify API batch query
     */
    @Name("Spotify API song limit")
    public static int spotifySongLimit = 50;
    /** Constant <code>spotifyAlbumLimit=20</code> */
    @Name("Spotify API album limit")
    public static int spotifyAlbumLimit = 20;
    /** Constant <code>spotifyPlaylistLimit=20</code> */
    @Name("Spotify API playlist limit")
    public static int spotifyPlaylistLimit = 20;

    @Group(Spotify.class)
    @Name("Spotify Client ID")
    /** Constant <code>spotifyClientID=""</code> */
    public static String spotifyClientID = "";
    @Group(Spotify.class)
    @Secret
    @Name("Spotify Client Secret")
    /** Constant <code>spotifyClientSecret=""</code> */
    public static String spotifyClientSecret = "";
    @Group(Spotify.class)
    @Name("Spotify Redirect URL")
    /** Constant <code>spotifyRedirectURL=""</code> */
    public static String spotifyRedirectURL = "";

    ///
    ///
    /// YOUTUBE
    ///
    ///

    /**
     * to limit number of songs in each youtube API batch query
     */
    @Name("Youtube API song limit")
    public static Long youtubeSongLimit = 50L;
    /** Constant <code>youtubePlaylistLimit</code> */
    @Name("Youtube API playlist limit")
    public static Long youtubePlaylistLimit = 20L;

    @Group(Youtube.class)
    @Name("Google Application Name")
    /** Constant <code>youtubeApplicatioName=""</code> */
    public static String youtubeApplicatioName = "";
    @Group(Youtube.class)
    @Secret
    @Name("Google Client ID")
    /** Constant <code>youtubeClientID=""</code> */
    public static String youtubeClientID = "";
    @Group(Youtube.class)
    @Secret
    @Name("Google Client Secret")
    /** Constant <code>youtubeClientSecret=""</code> */
    public static String youtubeClientSecret = "";

    ///
    ///
    /// TIDAL
    ///
    ///
    @Group(Tidal.class)
    @Secret
    @Name("Tidal Client ID")
    public static String tidalClientID = "";

    @Group(Tidal.class)
    @Secret
    @Name("Tidal Client Secret")
    public static String tidalClientSecret = "";

    ///
    ///
    /// LIBRARY
    ///
    ///
    /**
     * only put songs in collection if they are library verified
     */
    @Name("Verify using library")
    public static boolean libraryVerified = true;

    /**
     * class representative of which library to use
     * null for none
     */
    @Name("Library Type")
    @Options(options = { "LastFM", "MusicBrainz" })
    public static String libraryType = "";

    ///
    ///
    /// LASTFM
    ///
    ///
    @Group(LastFM.class)
    @Secret
    @Name("LastFM API Key")
    /** Constant <code>lastFMApiKey=""</code> */
    public static String lastFMApiKey = "";

    ///
    ///
    /// DOWNLOAD
    ///
    ///

    /**
     * local library path
     */
    @Group({ Download.class, Upload.class, YT_dl.class, SoulSeek.class })
    @Name("Local Library")
    public static File localFolder = null;
    /**
     * format of music to download
     * current supported: "mp3", "flac", "wav"
     */
    @Group({ Download.class, YT_dl.class, SoulSeek.class })
    @Options(options = { "mp3", "flac", "wav" })
    @Name("Format (ex: mp3)")
    public static String downloadFormat = "mp3";
    /**
     * class representative of which download class to use
     * default is YT_dl.class
     */
    @Group(Download.class)
    @Options(options = { "YT_dl", "SoulSeek" })
    @Name("Download Method")
    public static String downloadMethod = "YT_dl";

    @Group(YT_dl.class)
    @Name("Search Youtube")
    public static boolean ytdlUseYoutube = false;

    /**
     * enable multithreading
     */
    public static int downloadThreads = 1;

    ///
    ///
    /// YT_DL
    ///
    ///
    /**
     * option to hardcode cookies file
     */
    @Name("YT_DL cookie file")
    public static File yt_dlCookieFile = null;

    /**
     * option to hardcode browser to get cookies from
     * options: chrome, firefox, check yt-dlp docs,...
     */
    @Name("YT_DL cookie browser")
    public static String yt_dlCookieBrowser = "";

    /**
     * ffmpeg path (required for youtubedl)
     * 
     */
    @Group(YT_dl.class)
    @Name("FFMPEG file/folder")
    public static File ffmpegFile = null;

    /**
     * youtube dl installation path
     * 
     */
    @Group(YT_dl.class)
    @Name("YT_DL binary")
    public static File yt_dlFile = null;

    /**
     * download quality of music
     * 0 - best, 10 - worst
     * also respectfully increases file size
     */
    public static int yt_dlQuality = 5;

    ///
    ///
    /// SOULSEEK
    ///
    ///

    /** Constant <code>soulSeekBitRate=320</code> */
    public static int soulSeekBitRate = 320;

    /**
     * soulseek dl installation path
     */
    @Group(SoulSeek.class)
    @Name("SoulSeek DL Binary")
    public static File soulSeekFile = null;

    @Group(SoulSeek.class)
    @Name("SoulSeek Username")
    /** Constant <code>soulSeekUsername=""</code> */
    public static String soulSeekUsername = "";

    @Group(SoulSeek.class)
    @Secret
    @Name("SoulSeek Password")
    /** Constant <code>soulSeekPassword=""</code> */
    public static String soulSeekPassword = "";

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
     * clear.
     * </p>
     */
    public void clear() {
        instance = null;
        super.clear();
    }
}
