package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ryzen.ownitall.util.Input;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
public class Settings extends ryzen.ownitall.util.Settings {
    @JsonIgnore
    private static Settings instance;
    @JsonIgnore
    private static final Logger logger = LogManager.getLogger(Settings.class);
    @JsonIgnore
    private final static String settingsFilePath = "settings.json";
    // the defaults: (non final & protected for the ones that can be changed by
    // user)

    /**
     * default file names (without extensions)
     */
    protected String dataFolderPath = "data";
    protected String likedSongsName = "liked songs";
    protected String albumFile = "albums";
    protected String likedSongFile = "likedsongs";
    protected String playlistFile = "playlists";
    protected String artistFile = "artists";
    protected String songFile = "songs";

    protected String cacheFolderPath = ".cache";
    /**
     * to save credentials of anything 3rd party logins (youtube, spotify etc)
     * to prevent having to provide them each time
     */
    protected boolean saveCredentials = true;

    /**
     * in the Spotify class, this decides if the user has to click "accept"
     * everytime they "log in", set to true in case you use multiple accounts and
     * want to easily switch between them
     */
    protected boolean spotifyShowDialog = false;

    /**
     * to limit number of songs in each spotify API batch query
     */
    protected Integer spotifySongLimit = 50;
    protected Integer spotifyAlbumLimit = 20;
    protected Integer spotifyPlaylistLimit = 20;

    /**
     * delete any tracks in playlists and liked songs which are not in collection
     */
    public boolean spotifyDelete = true;

    /**
     * to limit number of songs in each youtube API batch query
     */
    protected Long youtubeSongLimit = 50L;
    protected Long youtubePlaylistLimit = 20L;

    /**
     * to limit number of songs in each soundcloud API batch query
     */
    protected Integer soundCloudSongLimit = 50;
    protected Integer soundCloudAlbumLimit = 20;
    protected Integer soundCloudPlaylistLimit = 20;

    /**
     * only put songs in collection if they are library verified
     */
    protected boolean libraryVerified = true;

    /**
     * int representative of which library to use
     * 0 - dont use library
     * 1 - lastFM
     * 2 - musicBrainz
     */
    protected int libraryType = 1;

    /**
     * youtube dl installation path
     * 
     */
    protected String youtubedlPath = "";
    /**
     * ffmpeg path (required for youtubedl)
     * 
     */
    protected String ffmpegPath = "";
    /**
     * format of music to download
     * current supported: "mp3", "flac", "wav"
     */
    protected String downloadFormat = "mp3";

    /**
     * optional to hardcode local download path
     */
    protected String downloadFolder = "";

    /**
     * option to hardcode cookies file
     */
    protected String downloadCookiesFile = "";

    /**
     * option to hardcode browser to get cookies from
     * options: chrome, firefox, check yt-dlp docs,...
     */
    protected String downloadCookiesBrowser = "";

    /**
     * download all files in a hierachy method
     * playlists get their own folders
     * most applications such as jellyfin use false
     */
    protected boolean downloadHierachy = false;

    protected boolean downloadLikedSongsPlaylist = true;

    /**
     * Delete files found in library which are not in collection
     * this can be used to clean up local copy after mistakes
     */
    protected boolean downloadDelete = true;

    /**
     * download quality of music
     * 0 - best, 10 - worst
     * also respectfully increases file size
     */
    protected Integer downloadQuality = 5;
    /**
     * enable yt-dlp threading
     */
    protected Integer downloadThreads = 1;

    /**
     * optional to hardcode local upload path
     */
    protected String uploadFolder = "";

    @JsonIgnore
    public static Settings load() {
        if (instance == null) {
            instance = new Settings();
            try {
                instance.importSettings(Settings.class, settingsFilePath);
            } catch (IOException e) {
                logger.error("Exception loading settings: " + e);
                logger.warn("If this persists, delete the file: '" + settingsFilePath + "'");
            }
            logger.debug("New instance created");
        }
        return instance;
    }

    public void clear() {
        instance = null;
        this.clearSettings(settingsFilePath);
    }

    public void save() {
        super.save(settingsFilePath);
    }

    public String getDataFolderPath() {
        return dataFolderPath;
    }

    public String getLikedSongsName() {
        return likedSongsName;
    }

    public String getAlbumFile() {
        return albumFile;
    }

    public String getLikedSongFile() {
        return likedSongFile;
    }

    public String getPlaylistFile() {
        return playlistFile;
    }

    public String getCacheFolderPath() {
        return cacheFolderPath;
    }

    public boolean isSaveCredentials() {
        return saveCredentials;
    }

    public boolean isSpotifyShowDialog() {
        return spotifyShowDialog;
    }

    public int getSpotifySongLimit() {
        return spotifySongLimit;
    }

    public int getSpotifyAlbumLimit() {
        return spotifyAlbumLimit;
    }

    public int getSpotifyPlaylistLimit() {
        return spotifyPlaylistLimit;
    }

    public boolean isSpotifyDelete() {
        return this.spotifyDelete;
    }

    public Long getYoutubeSongLimit() {
        return youtubeSongLimit;
    }

    public Long getYoutubePlaylistLimit() {
        return youtubePlaylistLimit;
    }

    public int getSoundCloudSongLimit() {
        return soundCloudSongLimit;
    }

    public int getSoundCloudAlbumLimit() {
        return soundCloudAlbumLimit;
    }

    public int getSoundCloudPlaylistLimit() {
        return soundCloudPlaylistLimit;
    }

    public int getLibrayType() {
        return this.libraryType;
    }

    public String getYoutubedlPath() {
        return youtubedlPath;
    }

    public void setYoutubedlPath() throws InterruptedException {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Local Youtube DL executable path: ");
            youtubedlPath = Input.request().getFile(true).getAbsolutePath();
        } catch (InterruptedException e) {
            logger.debug("Interrutped while setting youtubedl path");
            throw e;
        }
    }

    public String getDownloadFormat() {
        return downloadFormat;
    }

    public int getDownloadQuality() {
        return downloadQuality;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath() throws InterruptedException {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Local FFMPEG executable path: ");
            ffmpegPath = Input.request().getFile(true).getAbsolutePath();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting FFMPEG executable path");
            throw e;
        }
    }

    public String getDownloadFolder() {
        return downloadFolder;
    }

    public boolean isDownloadHierachy() {
        return downloadHierachy;
    }

    public String getUploadFolder() {
        return uploadFolder;
    }

    public int getDownloadThreads() {
        return downloadThreads;
    }

    public boolean isLibraryVerified() {
        return libraryVerified;
    }

    public String getDownloadCookiesFile() {
        return downloadCookiesFile;
    }

    public String getDownloadCookiesBrowser() {
        return downloadCookiesBrowser;
    }

    /**
     * @return the artistFile
     */
    public String getArtistFile() {
        return artistFile;
    }

    /**
     * @return the songFile
     */
    public String getSongFile() {
        return songFile;
    }

    /**
     * @return the libraryType
     */
    public int getLibraryType() {
        return libraryType;
    }

    /**
     * @return the downloadDelete
     */
    public boolean isDownloadDelete() {
        return downloadDelete;
    }

    public boolean isDownloadLikedSongsPlaylist() {
        return downloadLikedSongsPlaylist;
    }
}
