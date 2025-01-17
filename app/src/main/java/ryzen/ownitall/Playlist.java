package ryzen.ownitall;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.tools.Levenshtein;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Playlist {
    private static final Logger logger = LogManager.getLogger(Playlist.class);
    private String name;
    private URI coverArt;
    private SongSet songs;

    private String youtubePageToken; // TODO: create "update" method to save API requests
    private int spotifyPageOffset = -1;
    double simularityPercentage;

    /**
     * Default playlist constructor
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.name = name;
        this.songs = new SongSet();
        this.simularityPercentage = Settings.load().similarityPercentage;
    }

    @JsonCreator
    public Playlist(@JsonProperty("name") String name, @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverArt") String coverArt) {
        this.name = name;
        if (songs != null && !songs.isEmpty()) {
            this.songs = new SongSet(songs);
        } else {
            this.songs = new SongSet();
        }
        if (youtubePageToken != null) {
            this.youtubePageToken = youtubePageToken;
        }
        this.spotifyPageOffset = spotifyPageOffset;
        if (coverArt != null) {
            this.setCoverArt(coverArt);
        }
        this.simularityPercentage = Settings.load().similarityPercentage;
    }

    public void merge(Playlist playlist) {
        if (playlist == null) {
            return;
        }
        this.addSongs(playlist.getSongs());
        if (this.getCoverArt() == null && playlist.getCoverArt() != null) {
            this.setCoverArt(playlist.getCoverArt());
        }
        if (playlist.getYoutubePageToken() != null) {
            this.setYoutubePageToken(playlist.getYoutubePageToken());
        }
        if (playlist.getSpotifyPageOffset() > this.getSpotifyPageOffset()) {
            this.setSpotifyPageOffset(playlist.getSpotifyPageOffset());
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
        if (coverArt == null) {
            return;
        }
        try {
            this.coverArt = new URI(coverArt);
        } catch (URISyntaxException e) {
            logger.error("Error parsing playlist cover image: " + coverArt);
        }
    }

    public void setCoverArt(URI coverArt) {
        if (coverArt == null) {
            return;
        }
        this.coverArt = coverArt;
    }

    /**
     * get coverart of current playlist class
     * 
     * @return - constructed URI
     */
    public URI getCoverArt() {
        return this.coverArt;
    }

    /**
     * add songs to playlist
     * 
     * @param songs - arraylist of constructed Song
     */
    public void addSongs(LinkedHashSet<Song> songs) {
        for (Song song : songs) {
            this.addSong(song);
        }
    }

    /**
     * add song to playlist
     * 
     * @param song - constructed Song
     */
    public void addSong(Song song) {
        if (this.songs.contains(song)) {
            this.songs.get(song).mergeSong(song);
        } else {
            this.songs.add(song);
        }
    }

    /**
     * return size/numbers of songs in playlist
     * 
     * @return - integer of size of playlist
     */
    @JsonIgnore
    public int size() {
        return this.songs.size();
    }

    /**
     * get all playlist songs
     * 
     * @return - arraylist of constructed Song
     */
    public LinkedHashSet<Song> getSongs() {
        return new SongSet(this.songs);
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
    @JsonIgnore
    public String toString() {
        return this.name.toString();
    }

    @Override
    @JsonIgnore
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
                simularityPercentage)) {
            return true;
        }
        return false;
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return this.name.toLowerCase().hashCode();
    }
}
