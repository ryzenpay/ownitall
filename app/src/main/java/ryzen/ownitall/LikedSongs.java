package ryzen.ownitall;

import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class LikedSongs extends Playlist { // different from playlist due to linkedhashset songs

    private LinkedHashSet<Song> likedSongs;

    private String youtubePageToken = null;
    private int spotifyPageOffset = -1;

    /**
     * LikedSongs constructor with songs
     * 
     * @param songs - linkedhashset of constructed Song
     */
    public LikedSongs(LinkedHashSet<Song> songs) {
        super("Liked Songs");
        this.likedSongs = new LinkedHashSet<>(songs);
    }

    /**
     * LikedSongs construcor with no songs
     */
    public LikedSongs() {
        super("Liked Songs");
        this.likedSongs = new LinkedHashSet<>();
    }

    /**
     * get array of liked songs
     * 
     * @return - ArrayList of constructed Song
     */
    @Override
    public ArrayList<Song> getSongs() {
        return new ArrayList<Song>(this.likedSongs);
    }

    /**
     * add array of songs to liked songs
     * 
     * @param songs - arraylist of constructed Song to add
     */
    @Override
    public void addSongs(ArrayList<Song> songs) {
        this.likedSongs.addAll(songs);
    }

    /**
     * add array of songs to liked songs
     * 
     * @param songs - linkedhashset of constructed Song to add
     */
    public void addSongs(LinkedHashSet<Song> songs) {
        this.likedSongs.addAll(songs);
    }

    /**
     * add invididual song to liked songs
     * 
     * @param song - constructed Song to add
     */
    @Override
    public void addSong(Song song) {
        this.likedSongs.add(song);
    }

    /**
     * get size/number of songs in liked songs
     * 
     * @return - integer of liked songs size
     */
    @Override
    public int size() {
        return this.likedSongs.size();
    }

    /**
     * check if a song is a liked song
     * uses the hashcode to compare
     * 
     * @param song - constructed Song to check
     * @return - true if liked, false if not
     */
    public boolean checkLiked(Song song) {
        if (this.likedSongs.isEmpty()) {
            return false;
        }
        for (Song likedSong : this.likedSongs) {
            if (likedSong.hashCode() == song.hashCode()) {
                return true;
            }
        }
        return false;
    }

    /**
     * get youtube page token of youtube liked songs
     * 
     * @return - String of token to continue from
     */
    public String getYoutubePageToken() {
        return this.youtubePageToken;
    }

    /**
     * set youtube page token of youtube liked songs
     * 
     * @param yotubePageToken - String of token to continue from
     */
    public void setYoutubePageToken(String youtubePageToken) {
        this.youtubePageToken = youtubePageToken;
    }

    /**
     * get spotify offset for spotify liked songs
     * 
     * @return - int of spotify page offset
     */
    public int getSpotifyPageOffset() {
        return this.spotifyPageOffset;
    }

    /**
     * set spotify page offset for spotify liked songs
     * 
     * @param spotifyPageOffset - int of spotify page offset
     */
    public void setSpotifyPageOffset(int spotifyPageOffset) {
        this.spotifyPageOffset = spotifyPageOffset;
    }

    @JsonCreator
    public LikedSongs(@JsonProperty("name") String name, @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverArt") String coverArt) {
        super(name);
        if (songs != null && !songs.isEmpty()) {
            this.likedSongs = songs;
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
