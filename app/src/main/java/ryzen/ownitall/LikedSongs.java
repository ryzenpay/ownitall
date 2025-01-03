package ryzen.ownitall;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class LikedSongs extends Playlist {

    private LinkedHashSet<Song> likedSongs;

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
}
