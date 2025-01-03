package ryzen.ownitall;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class LikedSongs extends Playlist {

    private LinkedHashSet<Song> liked_songs;

    public LikedSongs(LinkedHashSet<Song> songs) {
        super("Liked Songs");
        this.liked_songs = new LinkedHashSet<>(songs);
    }

    public LikedSongs() {
        super("Liked Songs");
        this.liked_songs = new LinkedHashSet<>();
    }

    public ArrayList<Song> getSongs() {
        return new ArrayList<Song>(this.liked_songs);
    }

    public void addSongs(LinkedHashSet<Song> songs) {
        this.liked_songs.addAll(songs);
    }

    public int getSize() {
        return this.liked_songs.size();
    }

    public boolean checkLiked(Song song) {
        if (this.liked_songs.isEmpty()) {
            return false;
        }
        for (Song liked_song : this.liked_songs) {
            if (liked_song.equals(song)) { // TODO: song equals overwrite? (excluding the cover and duration)
                return true;
            }
        }
        return false;
    }
}
