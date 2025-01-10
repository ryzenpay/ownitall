package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.tools.Input;
import ryzen.ownitall.tools.Menu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Settings {
    private static final Logger logger = LogManager.getLogger(Settings.class);
    @JsonIgnore
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC);
    // TODO: move spotify and youtube credentials to here?
    // the defaults: (protected for the ones that can be changed by user)

    /**
     * default file names (without extensions)
     */
    protected String dataFolderPath = "data";
    protected String likedSongName = "liked songs";
    protected String albumFile = "albums";
    protected String likedSongFile = "likedsongs";
    protected String playlistFile = "playlists";
    protected String spotifyCredentialsFile = "spotifyCredentials";
    protected String youtubeCredentialsFile = "youtubeCredentials";

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
    protected boolean spotifyPlaylistSongCover = false; // TODO: get cover image of songs in playlist
    /**
     * to limit number of songs in each youtube API batch query
     */
    protected Long youtubeSongLimit = 50L;
    protected Long youtubePlaylistLimit = 20L;

    /**
     * similarity percentage used to check if artists, songs, albums or playlists
     * are equals (to merge or not)
     */
    protected double similarityPercentage = 90.0;

    /**
     * location of where to store and get settings
     */
    @JsonIgnore
    private final String settingsFolderPath = ".appdata";
    @JsonIgnore
    private final String settingsFilePath = "settings.json";

    /**
     * default settings constructor with default settings
     */
    public Settings() {
    }

    /**
     * to get custom settings set by user, load them
     * 
     * @return - compiled Settings with custom settings
     */
    @JsonIgnore
    public static Settings load() {
        Settings settings = new Settings();
        settings.loadSettings();
        return settings;
    }

    /**
     * check if settings folder exists, if not make it (to prevent errors)
     */
    @JsonIgnore
    private void setSettingsFolder() {
        File settingsFolder = new File(this.settingsFolderPath);
        if (!settingsFolder.exists()) {
            settingsFolder.mkdirs(); // Create folder if it does not exist
        }
    }

    /**
     * load settings from saved file
     */
    @JsonIgnore
    public void loadSettings() {
        setSettingsFolder();
        File settingsFile = new File(settingsFolderPath, settingsFilePath);

        if (!settingsFile.exists() || settingsFile.length() == 0) {
            this.saveSettings();
            return;
        }

        try {
            Settings importedSettings = objectMapper.readValue(settingsFile, Settings.class);
            if (importedSettings != null) {
                this.setSettings(importedSettings);
            }
        } catch (IOException e) {
            logger.error("Error importing settings: " + e);
            logger.info("If this persists, delete the file: " + settingsFile.getAbsolutePath());
        }
    }

    /**
     * save settings to predefined file
     */
    @JsonIgnore
    public void saveSettings() {
        this.setSettingsFolder();
        File settingsFile = new File(settingsFolderPath, settingsFilePath);
        try {
            objectMapper.writeValue(settingsFile, this);
        } catch (IOException e) {
            logger.error("Error saving settings: " + e);
        }
    }

    /**
     * flexibly get all settings
     * 
     * @return - ArrayList of all setting varialbes as Object
     */
    @JsonIgnore
    public ArrayList<Field> getAllSettings() {
        ArrayList<Field> allSettings = new ArrayList<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isProtected(field.getModifiers())) { // only allow option to change the protected ones
                try {
                    allSettings.add(field); // Add field value to the list
                } catch (Exception e) {
                    logger.error("Error getting all settings: " + e);
                }
            }
        }
        return allSettings;
    }

    /**
     * print menu of settings and values, prompt user for which to change
     */
    @JsonIgnore
    public void changeSettings() {
        System.out.println("Choose a setting to change: ");
        Map<String, String> options = new HashMap<>();
        try {
            while (true) {
                for (Field setting : this.getAllSettings()) {
                    options.put(setting.getName(), setting.get(this).toString());
                }
                String choice = Menu.optionMenuWithValue(options, "SETTINGS");
                if (choice == "Exit") {
                    break;
                } else {
                    try {
                        if (this.changeSetting(choice)) {
                            logger.info(
                                    "Setting successfully changed, the program might need a restart for this to take shape");
                        } else {
                            logger.error("Unsuccessfully changed setting, read the log for more information");
                        }
                    } catch (IllegalAccessException e) {
                        logger.error("Error updating setting: " + e);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            logger.error("Error listing settings: " + e);
        }
    }

    /**
     * 
     * @param setting - desired setting to modify
     * @return - true if modified, false if not
     * @throws IllegalAccessException - if unaccessible setting is being modified
     */
    @JsonIgnore
    public boolean changeSetting(String settingName) throws IllegalAccessException {
        try {
            Field setting = this.getClass().getDeclaredField(settingName);
            System.out.print("Enter new value for " + setting.getName() + ": ");
            if (setting.getType() == boolean.class) {
                boolean input = Input.getInstance().getBool();
                setting.set(this, input);
                return true;
            } else if (setting.getType() == String.class) {
                String input = Input.getInstance().getString();
                setting.set(this, input);
                return true;
            } else if (setting.getType() == Integer.class) {
                int input = Input.getInstance().getInt();
                setting.set(this, input);
                return true;
            } else if (setting.getType() == long.class) {
                long input = Input.getInstance().getLong();
                setting.set(this, input);
                return true;
            } else {
                System.out
                        .println("Modifying settings of the type " + setting.getType() + " is currently not supported");
            }
        } catch (NoSuchFieldException e) {
            logger.error("Error modifying setting: " + e);
        }
        return false;
    }

    /**
     * copy over settings from constructed Settings to this
     * 
     * @param setting - constructed Settings
     */
    @JsonIgnore
    public void setSettings(Settings setting) {
        for (Field field : setting.getClass().getDeclaredFields()) {
            if (Modifier.isProtected(field.getModifiers())) {
                try {
                    Object value = field.get(setting);
                    field.set(this, value);
                } catch (IllegalAccessException e) {
                    logger.error("Error copying over settings: " + e);
                }
            }
        }
    }

    /**
     * check if settings correctly imported
     * 
     * @return - true if errors, false if none
     */
    @JsonIgnore
    public boolean isNull() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (Modifier.isProtected(field.getModifiers())) {
                try {
                    if (field.get(this) == null) {
                        return false;
                    }
                } catch (IllegalAccessException e) {
                    logger.error("Error checking if settings isNull");
                    return false;
                }
            }
        }
        return true;
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

    public void setSpotifyArtistPfp(boolean spotifyArtistPfp) {
        this.spotifyArtistPfp = spotifyArtistPfp;
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

    public void setSpotifyCredentialsFile(String spotifyCredentialsFile) {
        this.spotifyCredentialsFile = spotifyCredentialsFile;
    }

    public void setYoutubeCredentialsFile(String youtubeCredentialsFile) {
        this.youtubeCredentialsFile = youtubeCredentialsFile;
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

    public String getSpotifyCredentialsFile() {
        return spotifyCredentialsFile;
    }

    public String getYoutubeCredentialsFile() {
        return youtubeCredentialsFile;
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

    public boolean isSpotifyArtistPfp() {
        return spotifyArtistPfp;
    }

    public boolean isSpotifyPlaylistSongCover() {
        return spotifyPlaylistSongCover;
    }

    public void setSpotifyPlaylistSongCover(boolean spotifyPlaylistSongCover) {
        this.spotifyPlaylistSongCover = spotifyPlaylistSongCover;
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
}
