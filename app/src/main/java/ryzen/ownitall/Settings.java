package ryzen.ownitall;

import java.io.File;

public class Settings {
    // TODO: move spotify and youtube credentials to here?
    // the defaults:
    private String dataFolder = "data";
    private String likedSongName = "liked songs"; // in local, liked songs class
    private String albumFile = "albums.ser";
    private String likedSongFile = "likedsongs.ser";
    private String playlistFile = "playlists.ser";

    /**
     * to save credentials of anything 3rd party logins (youtube, spotify etc)
     * to prevent having to provide them each time
     */
    private boolean saveCredentials = true;
    /**
     * in the Spotify class, this decides if the user has to click "accept"
     * everytime they "log in", set to true in case you use multiple accounts and
     * want to easily switch between them
     */
    private boolean spotifyShowDialog = false;
    /**
     * to limit number of songs in each spotify API batch query
     */
    private int spotifySongLimit = 50;
    private int spotifyAlbumLimit = 20;
    private int spotifyPlaylistLimit = 20;
    /**
     * to get the profile picture of the artist, default to false as it hugely
     * increases API requests
     */
    private boolean spotifyArtistPfp = false;
    private Long youtubeSongLimit = 50L;
    private Long youtubePlaylistLimit = 20L;

    /**
     * similarity percentage used to check if artists, songs, albums or playlists
     * are equals (to merge or not)
     */
    private int similarityPercentage = 90;
    // default to the default settings
    private Settings currentSettings = new Settings();

    public Settings() {

    }

    private Settings(File currentSettingsPath) {

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
