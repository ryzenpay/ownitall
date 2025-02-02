package ryzen.ownitall.classes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LikedSongs extends Playlist { // different from playlist due to linkedhashset songs
    private static final Logger logger = LogManager.getLogger(LikedSongs.class);

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
            logger.debug(this.toString() + ": empty song provided in contains");
            return false;
        }
        return this.getSongs().contains(song);
    }
}
