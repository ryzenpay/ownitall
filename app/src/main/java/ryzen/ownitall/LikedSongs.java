package ryzen.ownitall;

import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class LikedSongs extends Playlist { // different from playlist due to linkedhashset songs

    private LinkedHashSet<Song> likedSongs;

    private String youtubePageToken = null;
    private int spotifyPageOffset = -1;

    public LikedSongs(LinkedHashSet<Song> songs) {
        super("Liked Songs");
        this.likedSongs = new LinkedHashSet<>(songs);
    }

    public LikedSongs() {
        super("Liked Songs");
        this.likedSongs = new LinkedHashSet<>();
    }

    @Override
    public ArrayList<Song> getSongs() {
        return new ArrayList<Song>(this.likedSongs);
    }

    @Override
    public void addSongs(ArrayList<Song> songs) {
        this.likedSongs.addAll(songs);
    }

    public void addSongs(LinkedHashSet<Song> songs) {
        this.likedSongs.addAll(songs);
    }

    @Override
    public void addSong(Song song) {
        this.likedSongs.add(song);
    }

    @Override
    public int size() {
        return this.likedSongs.size();
    }

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
