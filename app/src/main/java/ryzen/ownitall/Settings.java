package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
public class Settings extends ryzen.ownitall.tools.Settings {
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
    protected String youtubedlPath = "";
    /**
     * download path of where to put downloaded music
     */
    protected String downloadPath = "";
    /**
     * format of music to download
     * current supported: "mp3", "flac", "wav"
     */
    protected String downloadFormat = "mp3";

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

    @JsonIgnore
    public void clear() {
        instance = null;
        File credentialsFile = new File(settingsFilePath);
        credentialsFile.delete();
    }

    public void saveSettings() {
        try {
            super.saveSettings(settingsFilePath);
        } catch (Exception e) {
            logger.error("Error saving settings: " + e);
        }
    }

    public void setDataFolderPath(String dataFolderPath) {
        this.dataFolderPath = dataFolderPath;
    }

    public void setLikedSongName(String likedSongName) {
        this.likedSongName = likedSongName;
    }

    public void setAlbumFile(String albumFile) {
        this.albumFile = albumFile;
    }

    public void setLikedSongFile(String likedSongFile) {
        this.likedSongFile = likedSongFile;
    }

    public void setPlaylistFile(String playlistFile) {
        this.playlistFile = playlistFile;
    }

    public void setSpotifyShowDialog(boolean spotifyShowDialog) {
        this.spotifyShowDialog = spotifyShowDialog;
    }

    public void setSpotifySongLimit(int spotifySongLimit) {
        this.spotifySongLimit = spotifySongLimit;
    }

    public void setSpotifyAlbumLimit(int spotifyAlbumLimit) {
        this.spotifyAlbumLimit = spotifyAlbumLimit;
    }

    public void setSpotifyPlaylistLimit(int spotifyPlaylistLimit) {
        this.spotifyPlaylistLimit = spotifyPlaylistLimit;
    }

    public void setYoutubeSongLimit(Long youtubeSongLimit) {
        this.youtubeSongLimit = youtubeSongLimit;
    }

    public void setYoutubePlaylistLimit(Long youtubePlaylistLimit) {
        this.youtubePlaylistLimit = youtubePlaylistLimit;
    }

    public void setSaveCredentials(boolean saveCredentials) {
        this.saveCredentials = saveCredentials;
    }

    public void setSimilarityPercentage(double similarityPercentage) {
        this.similarityPercentage = similarityPercentage;
    }

    public String getDataFolderPath() {
        return dataFolderPath;
    }

    public String getLikedSongName() {
        return likedSongName;
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

    public boolean isUseLibrary() {
        return useLibrary;
    }

    public Long getYoutubeSongLimit() {
        return youtubeSongLimit;
    }

    public Long getYoutubePlaylistLimit() {
        return youtubePlaylistLimit;
    }

    public double getSimilarityPercentage() {
        return similarityPercentage;
    }

    public String getCacheFolderPath() {
        return this.cacheFolderPath;
    }

    public String getArtistFile() {
        return artistFile;
    }

    public String getSongFile() {
        return songFile;
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

    public String getYoutubedlPath() {
        return youtubedlPath;
    }

    public void setYoutubedlPath(String path) {
        this.youtubedlPath = path;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String path) {
        this.downloadPath = path;
    }

    public String getDownloadFormat() {
        return downloadFormat;
    }

    public void setDownloadFormat(String format) {
        this.downloadFormat = format;
    }
}
