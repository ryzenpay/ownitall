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
    protected int spotifySongLimit = 50;
    protected int spotifyAlbumLimit = 20;
    protected int spotifyPlaylistLimit = 20;

    /**
     * to limit number of songs in each youtube API batch query
     */
    protected Long youtubeSongLimit = 50L;
    protected Long youtubePlaylistLimit = 20L;

    /**
     * to limit number of songs in each soundcloud API batch query
     */
    protected int soundCloudSongLimit = 50;
    protected int soundCloudAlbumLimit = 20;
    protected int soundCloudPlaylistLimit = 20;

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
     * youtube dl installation path
     * 
     */
    protected String youtubedlPath = ""; // TODO: default path from ./resources
    /**
     * ffmpeg path (required for youtubedl)
     * 
     */
    protected String ffmpegPath = ""; // TODO: default path from ./resources
    /**
     * format of music to download
     * current supported: "mp3", "flac", "wav"
     */
    protected String downloadFormat = "mp3";
    /**
     * download quality of music
     * 0 - best, 10 - worst
     * also respectfully increases file size
     */
    protected int downloadQuality = 5;

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
}
