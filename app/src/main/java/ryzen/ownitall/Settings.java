package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Settings {
    ObjectMapper objectMapper;
    // TODO: move spotify and youtube credentials to here?
    // the defaults: (protected for the ones that can be changed by user)
    /**
     * default file names (without extensions)
     */
    protected String dataFolder = "data";
    protected String likedSongName = "liked songs";
    protected String albumFile = "albums";
    protected String likedSongFile = "likedsongs";
    protected String playlistFile = "playlists";

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
     * to get the profile picture of the artist, default to false as it hugely
     * increases API requests
     */
    protected boolean spotifyArtistPfp = false;
    /**
     * to limit number of songs in each youtube API batch query
     */
    protected Long youtubeSongLimit = 50L;
    protected Long youtubePlaylistLimit = 20L;

    /**
     * similarity percentage used to check if artists, songs, albums or playlists
     * are equals (to merge or not)
     */
    protected int similarityPercentage = 90;

    // current settings with changed values
    private Settings currentSettings;

    /**
     * location of where to store and get settings
     */
    private final String settingsFilePath = ".appdata/settings.json";

    public Settings() {
        this.objectMapper = new ObjectMapper();
        this.currentSettings = null;
        this.setSettingsFolder();
    }

    public Settings loadSettings() {
        this.setSettingsFolder();
        Settings settings;
        File settingsFile = new File(this.settingsFilePath);
        if (!settingsFile.exists()) {
            return null;
        }
        try {
            settings = this.objectMapper.readValue(settingsFile, Settings.class);
        } catch (IOException e) {
            System.err.println("Error importing settings: " + e);
            e.printStackTrace();
            System.err.println("If this persists, delete the file:" + settingsFile.getAbsolutePath());
            return null;
        }
        return settings;
    }

    public void saveSettings() {
        this.setSettingsFolder();
        try {
            this.objectMapper.writeValue(new File(this.settingsFilePath), this.currentSettings);
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e);
            e.printStackTrace();
        }
    }

    /**
     * function which is called to check if datafolder exists and create if deleted
     * in middle of process
     * for future improvements, use an interceptor but requires another class (bleh)
     * ^ or a dynamic proxy whatever
     */
    private void setSettingsFolder() {
        File settingsFile = new File(this.settingsFilePath);
        if (!settingsFile.exists()) { // create folder if it does not exist
            settingsFile.mkdirs();
            this.saveSettings();
        } else {
            Settings loadedSettings = this.loadSettings();
            if (loadedSettings != null) { // check if successfully imported
                setCurrentSettings(loadedSettings);
            } else {
                this.saveSettings();
            }
        }
    }

    /**
     * flexibly get all settings
     * 
     * @return
     */
    public ArrayList<Object> getAllSettings() {
        ArrayList<Object> allSettings = new ArrayList<>();
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (Modifier.isProtected(field.getModifiers())) { // only allow option to change the protected ones
                try {
                    allSettings.add(field.get(this)); // Add field value to the list
                } catch (IllegalAccessException e) {
                    e.printStackTrace(); // Handle potential exceptions
                }
            }
        }
        return allSettings;
    }

    public String getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
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

    public boolean isSpotifyArtistPfp() {
        return spotifyArtistPfp;
    }

    public void setSpotifyArtistPfp(boolean spotifyArtistPfp) {
        this.spotifyArtistPfp = spotifyArtistPfp;
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

    public Settings getCurrentSettings() {
        return currentSettings;
    }

    public void setCurrentSettings(Settings currentSettings) {
        this.currentSettings = currentSettings;
    }

    public boolean isSaveCredentials() {
        return saveCredentials;
    }

    public void setSaveCredentials(boolean saveCredentials) {
        this.saveCredentials = saveCredentials;
    }

    public int getSimilarityPercentage() {
        return similarityPercentage;
    }

    public void setSimilarityPercentage(int similarityPercentage) {
        this.similarityPercentage = similarityPercentage;
    }
}
