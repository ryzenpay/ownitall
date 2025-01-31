package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ryzen.ownitall.util.Input;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
public class Settings extends ryzen.ownitall.util.Settings { // TODO: non-interactive mode
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
    protected String likedSongName = "liked songs";
    protected String albumFile = "albums";
    protected String likedSongFile = "likedsongs";
    protected String playlistFile = "playlists";

    protected String cacheFolderPath = ".cache";
    protected String artistFile = "artists";
    protected String songFile = "songs";
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
     * similarity percentage used to check if artists, songs, albums or playlists
     * are equals (to merge or not)
     */
    protected double similarityPercentage = 90.0;

    /**
     * if to use lastFM to fact check all data
     * 
     */
    protected boolean useLibrary = true;

    /**
     * only put songs in collection if they are library verified
     */
    protected boolean libraryVerified = true;

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
     */
    protected boolean downloadHierachy = true;

    /**
     * dump all liked songs into liked songs (true) or only the standalone ones
     */
    protected boolean downloadAllLikedSongs = false;

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
            } catch (Exception e) {
                logger.error(e);
                logger.info("If this persists, delete the file: " + settingsFilePath);
            }
        }
        return instance;
    }

    public void clear() {
        instance = null;
        this.clearSettings(settingsFilePath);
    }

    public void saveSettings() {
        try {
            super.saveSettings(settingsFilePath);
        } catch (Exception e) {
            logger.error("Error saving settings: " + e);
        }
    }

    public String getDataFolderPath() {
        return dataFolderPath;
    }

    public void setDataFolderPath(String dataFolderPath) {
        this.dataFolderPath = dataFolderPath;
    }

    public String getLikedSongName() {
        return likedSongName;
    }

    public void setLikedSongName(String likedSongName) {
        this.likedSongName = likedSongName;
    }

    public String getAlbumFile() {
        return albumFile;
    }

    public void setAlbumFile(String albumFile) {
        this.albumFile = albumFile;
    }

    public String getLikedSongFile() {
        return likedSongFile;
    }

    public void setLikedSongFile(String likedSongFile) {
        this.likedSongFile = likedSongFile;
    }

    public String getPlaylistFile() {
        return playlistFile;
    }

    public void setPlaylistFile(String playlistFile) {
        this.playlistFile = playlistFile;
    }

    public String getCacheFolderPath() {
        return cacheFolderPath;
    }

    public void setCacheFolderPath(String cacheFolderPath) {
        this.cacheFolderPath = cacheFolderPath;
    }

    public String getArtistFile() {
        return artistFile;
    }

    public void setArtistFile(String artistFile) {
        this.artistFile = artistFile;
    }

    public String getSongFile() {
        return songFile;
    }

    public void setSongFile(String songFile) {
        this.songFile = songFile;
    }

    public boolean isSaveCredentials() {
        return saveCredentials;
    }

    public void setSaveCredentials(boolean saveCredentials) {
        this.saveCredentials = saveCredentials;
    }

    public boolean isSpotifyShowDialog() {
        return spotifyShowDialog;
    }

    public void setSpotifyShowDialog(boolean spotifyShowDialog) {
        this.spotifyShowDialog = spotifyShowDialog;
    }

    public int getSpotifySongLimit() {
        return spotifySongLimit;
    }

    public void setSpotifySongLimit(int spotifySongLimit) {
        this.spotifySongLimit = spotifySongLimit;
    }

    public int getSpotifyAlbumLimit() {
        return spotifyAlbumLimit;
    }

    public void setSpotifyAlbumLimit(int spotifyAlbumLimit) {
        this.spotifyAlbumLimit = spotifyAlbumLimit;
    }

    public int getSpotifyPlaylistLimit() {
        return spotifyPlaylistLimit;
    }

    public void setSpotifyPlaylistLimit(int spotifyPlaylistLimit) {
        this.spotifyPlaylistLimit = spotifyPlaylistLimit;
    }

    public Long getYoutubeSongLimit() {
        return youtubeSongLimit;
    }

    public void setYoutubeSongLimit(Long youtubeSongLimit) {
        this.youtubeSongLimit = youtubeSongLimit;
    }

    public Long getYoutubePlaylistLimit() {
        return youtubePlaylistLimit;
    }

    public void setYoutubePlaylistLimit(Long youtubePlaylistLimit) {
        this.youtubePlaylistLimit = youtubePlaylistLimit;
    }

    public int getSoundCloudSongLimit() {
        return soundCloudSongLimit;
    }

    public void setSoundCloudSongLimit(int soundCloudSongLimit) {
        this.soundCloudSongLimit = soundCloudSongLimit;
    }

    public int getSoundCloudAlbumLimit() {
        return soundCloudAlbumLimit;
    }

    public void setSoundCloudAlbumLimit(int soundCloudAlbumLimit) {
        this.soundCloudAlbumLimit = soundCloudAlbumLimit;
    }

    public int getSoundCloudPlaylistLimit() {
        return soundCloudPlaylistLimit;
    }

    public void setSoundCloudPlaylistLimit(int soundCloudPlaylistLimit) {
        this.soundCloudPlaylistLimit = soundCloudPlaylistLimit;
    }

    public double getSimilarityPercentage() {
        return similarityPercentage;
    }

    public void setSimilarityPercentage(double similarityPercentage) {
        this.similarityPercentage = similarityPercentage;
    }

    public boolean isUseLibrary() {
        return useLibrary;
    }

    public void setUseLibrary(boolean useLibrary) {
        this.useLibrary = useLibrary;
    }

    public String getYoutubedlPath() {
        return youtubedlPath;
    }

    public void setYoutubedlPath(String youtubedlPath) {
        this.youtubedlPath = youtubedlPath;
    }

    public void setYoutubedlPath() {
        logger.info("A guide to obtaining the following variables is in the readme");
        System.out.print("Please provide local Youtube DL executable path: ");
        youtubedlPath = Input.request().getFile(true).getAbsolutePath();
    }

    public String getDownloadFormat() {
        return downloadFormat;
    }

    public void setDownloadFormat(String downloadFormat) {
        this.downloadFormat = downloadFormat;
    }

    public int getDownloadQuality() {
        return downloadQuality;
    }

    public void setDownloadQuality(int downloadQuality) {
        this.downloadQuality = downloadQuality;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public void setFfmpegPath() {
        logger.info("A guide to obtaining the following variables is in the readme");
        System.out.print("Please provide local FFMPEG executable path: ");
        ffmpegPath = Input.request().getFile(true).getAbsolutePath();

    }

    public String getDownloadFolder() {
        return downloadFolder;
    }

    public void setDownloadFolder(String downloadFolder) {
        this.downloadFolder = downloadFolder;
    }

    public boolean isDownloadHierachy() {
        return downloadHierachy;
    }

    public void setDownloadHierachy(boolean downloadHierachy) {
        this.downloadHierachy = downloadHierachy;
    }

    public String getUploadFolder() {
        return uploadFolder;
    }

    public void setUploadFolder(String uploadFolder) {
        this.uploadFolder = uploadFolder;
    }

    public int getDownloadThreads() {
        return downloadThreads;
    }

    public void setDownloadThreads(int downloadThreads) {
        this.downloadThreads = downloadThreads;
    }

    public void setSpotifySongLimit(Integer spotifySongLimit) {
        this.spotifySongLimit = spotifySongLimit;
    }

    public void setSpotifyAlbumLimit(Integer spotifyAlbumLimit) {
        this.spotifyAlbumLimit = spotifyAlbumLimit;
    }

    public void setSpotifyPlaylistLimit(Integer spotifyPlaylistLimit) {
        this.spotifyPlaylistLimit = spotifyPlaylistLimit;
    }

    public void setSoundCloudSongLimit(Integer soundCloudSongLimit) {
        this.soundCloudSongLimit = soundCloudSongLimit;
    }

    public void setSoundCloudAlbumLimit(Integer soundCloudAlbumLimit) {
        this.soundCloudAlbumLimit = soundCloudAlbumLimit;
    }

    public void setSoundCloudPlaylistLimit(Integer soundCloudPlaylistLimit) {
        this.soundCloudPlaylistLimit = soundCloudPlaylistLimit;
    }

    public boolean isLibraryVerified() {
        return libraryVerified;
    }

    public void setLibraryVerified(boolean libraryVerified) {
        this.libraryVerified = libraryVerified;
    }

    public void setDownloadQuality(Integer downloadQuality) {
        this.downloadQuality = downloadQuality;
    }

    public void setDownloadThreads(Integer downloadThreads) {
        this.downloadThreads = downloadThreads;
    }

    public boolean isDownloadAllLikedSongs() {
        return downloadAllLikedSongs;
    }

    public void setDownloadAllLikedSongs(boolean downloadAllLikedSongs) {
        this.downloadAllLikedSongs = downloadAllLikedSongs;
    }

    public String getDownloadCookiesFile() {
        return downloadCookiesFile;
    }

    public void setDownloadCookiesFile(String downloadCookiesFile) {
        this.downloadCookiesFile = downloadCookiesFile;
    }

    public String getDownloadCookiesBrowser() {
        return downloadCookiesBrowser;
    }

    public void setDownloadCookiesBrowser(String downloadCookiesBrowser) {
        this.downloadCookiesBrowser = downloadCookiesBrowser;
    }
}
