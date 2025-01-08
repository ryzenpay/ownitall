package ryzen.ownitall;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Playlist {
    private String name;
    private URI coverArt;
    private ArrayList<Song> songs; // arraylist cuz it can contain duplicates

    private String youtubePageToken = null; // TODO: create "update" method to save API requests
    private int spotifyPageOffset = -1;

    /**
     * Default playlist constructor without coverart
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
        this.coverArt = null;
    }

    /**
     * get the name of the current playlist class
     * 
     * @return - playlist name
     */
    public String getName() {
        return this.name;
    }

    public void setCoverArt(String coverArt) {
        try {
            this.coverArt = new URI(coverArt);
        } catch (URISyntaxException e) {
            System.err.println("Error parsing cover image: " + coverArt);
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

    public void addSongs(ArrayList<Song> songs) {
        this.songs.addAll(songs);
    }

    public void addSong(Song song) {
        this.songs.add(song);
    }

    public int size() {
        return this.songs.size();
    }

    public ArrayList<Song> getSongs() {
        return this.songs;
    }

    public String getYoutubePageToken() {
        return this.youtubePageToken;
    }

    public void setYoutubePageToken(String youtubePageToken) {
        this.youtubePageToken = youtubePageToken;
    }

    public int getSpotifyPageOffset() {
        return this.spotifyPageOffset;
    }

    public void setSpotifyPageOffset(int spotifyPageOffset) {
        this.spotifyPageOffset = spotifyPageOffset;
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
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode += name.hashCode();
        return hashCode;
    }

    @JsonCreator
    public Playlist(@JsonProperty("name") String name, @JsonProperty("songs") ArrayList<Song> songs,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverArt") String coverArt) {
        this.name = name;
        if (songs != null && !songs.isEmpty()) {
            this.songs = songs;
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
