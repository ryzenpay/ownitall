package ryzen.ownitall.classes;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LikedSongs extends Playlist { // different from playlist due to linkedhashset songs

    /**
     * LikedSongs construcor with no songs
     */
    public LikedSongs() {
        super("Liked Songs");
    }

    /**
     * check if a song is a liked song
     * uses the hashcode to compare
     * 
     * @param song - constructed Song to check
     * @return - true if liked, false if not
     */
    @JsonIgnore
    public boolean contains(Song song) {
        if (song == null || song.isEmpty()) {
            return false;
        }
        return this.getSongs().contains(song);
    }
}
