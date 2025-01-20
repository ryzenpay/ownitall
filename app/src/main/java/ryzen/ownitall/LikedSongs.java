package ryzen.ownitall;

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
        if (this.size() == 0) {
            return false;
        }
        if (this.getSongs().contains(song)) {
            return true;
        }
        return false;
    }
}
