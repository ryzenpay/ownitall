package ryzen.ownitall;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.tools.Levenshtein;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Playlist {
    private static final Logger logger = LogManager.getLogger(Playlist.class);
    private static Settings settings = Settings.load();
    private String name;
    private URI coverArt;
    private LinkedHashSet<Song> songs; // arraylist cuz it can contain duplicates //no longer arraylist for
    // re-importing and merging capabilities

    private String youtubePageToken; // TODO: create "update" method to save API requests
    private int spotifyPageOffset;

    /**
     * Default playlist constructor
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.name = name;
        this.songs = new LinkedHashSet<>();
        this.coverArt = null;
        this.youtubePageToken = null;
        this.spotifyPageOffset = -1;
    }

    /**
     * merge two playlist objects together
     * 
     * @param playlist - constructed playlist to merge
     */
    public void mergePlaylist(Playlist playlist) {
        this.addSongs(playlist.getSongs());
        if (this.coverArt == null) {
            this.coverArt = playlist.getCoverart();
        }
        if (playlist.getYoutubePageToken() != null) {
            this.youtubePageToken = playlist.getYoutubePageToken();
        }
        if (playlist.getSpotifyPageOffset() > this.spotifyPageOffset) {
            this.spotifyPageOffset = playlist.getSpotifyPageOffset();
        }
    }

    /**
     * get the name of the current playlist class
     * 
     * @return - playlist name
     */
    public String getName() {
        return this.name;
    }

    /**
     * set playlist cover art
     * 
     * @param coverArt - String of coverart URL
     */
    public void setCoverArt(String coverArt) {
        try {
            this.coverArt = new URI(coverArt);
        } catch (URISyntaxException e) {
            logger.error("Error parsing cover image: " + coverArt);
        }
    }

    /**
     * get coverart of current playlist class
     * 
     * @return - constructed URI
     */
    public URI getCoverart() {
        return this.coverArt;
    }

    /**
     * add songs to playlist
     * 
     * @param songs - arraylist of constructed Song
     */
    public void addSongs(ArrayList<Song> songs) {
        this.songs.addAll(songs);
    }

    /**
     * add song to playlist
     * 
     * @param song - constructed Song
     */
    public void addSong(Song song) {
        this.songs.add(song);
    }

    /**
     * return size/numbers of songs in playlist
     * 
     * @return - integer of size of playlist
     */
    public int size() {
        return this.songs.size();
    }

    /**
     * get all playlist songs
     * 
     * @return - arraylist of constructed Song
     */
    public ArrayList<Song> getSongs() {
        return new ArrayList<>(this.songs);
    }

    /**
     * get youtube page token of youtube playlist
     * 
     * @return - String youtube page token
     */
    public String getYoutubePageToken() {
        return this.youtubePageToken;
    }

    /**
     * set youtube page token of youtube playlist
     * 
     * @param youtubePageToken - String youtube page token
     */
    public void setYoutubePageToken(String youtubePageToken) {
        this.youtubePageToken = youtubePageToken;
    }

    /**
     * get spotify offset for spotify playlist
     * 
     * @return - int spotify offset
     */
    public int getSpotifyPageOffset() {
        return this.spotifyPageOffset;
    }

    /**
     * set spotify offset for spotify playlist
     * 
     * @param spotifyPageOffset - int spotify offset
     */
    public void setSpotifyPageOffset(int spotifyPageOffset) {
        this.spotifyPageOffset = spotifyPageOffset;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Playlist playlist = (Playlist) object;
        if (this.hashCode() == playlist.hashCode()) {
            return true;
        }
        if (Levenshtein.computeSimilarityCheck(this.toString(), playlist.toString(),
                settings.getSimilarityPercentage())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.toLowerCase().hashCode();
    }

    @JsonCreator
    public Playlist(@JsonProperty("name") String name, @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverArt") String coverArt) {
        this.name = name;
        if (songs != null && !songs.isEmpty()) {
            this.songs = songs;
        } else {
            this.songs = new LinkedHashSet<>();
        }
        if (youtubePageToken != null) {
            this.youtubePageToken = youtubePageToken;
        }
        this.spotifyPageOffset = spotifyPageOffset;
        if (coverArt != null) {
            this.setCoverArt(coverArt);
        }
    }
}
