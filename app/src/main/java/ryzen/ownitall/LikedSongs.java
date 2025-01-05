package ryzen.ownitall;

import java.util.LinkedHashSet;

public class LikedSongs extends Playlist {

    private LinkedHashSet<Song> likedSongs;

    private String youtubePageToken;
    private int spotifyPageOffset;

    public LikedSongs(LinkedHashSet<Song> songs) {
        super("Liked Songs");
        this.likedSongs = new LinkedHashSet<>(songs);
    }

    public LikedSongs() {
        super("Liked Songs");
        this.likedSongs = new LinkedHashSet<>();
    }

    public LinkedHashSet<Song> getSongs() {
        return new LinkedHashSet<Song>(this.likedSongs);
    }

    public void addSongs(LinkedHashSet<Song> songs) {
        this.likedSongs.addAll(songs);
    }

    public void addSong(Song song) {
        this.likedSongs.add(song);
    }

    public int getSize() {
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
}
