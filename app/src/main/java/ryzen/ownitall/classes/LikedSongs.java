package ryzen.ownitall.classes;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.Settings;

//a playlist for just liked songs (as they might not be in playlists)
public class LikedSongs extends Playlist {
    private static final Logger logger = LogManager.getLogger(LikedSongs.class);
    private static final String name = Settings.load().getLikedSongsName();

    /**
     * LikedSongs construcor with no songs
     */
    public LikedSongs() {
        super(name);
    }

    /**
     * full playlist contructor
     * 
     * @param name              - playlist name
     * @param songs             - linkedhashset of song
     * @param ids               - linkedhashmap of id's
     * @param youtubePageToken  - string youtube page token
     * @param spotifyPageOffset - int spotify page token
     * @param coverImage        - string playlist coverImage
     */
    @JsonCreator
    public LikedSongs(@JsonProperty("name") String name,
            @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("ids") LinkedHashMap<String, String> ids, @JsonProperty("coverImage") String coverImage) {
        super(name, songs, ids, coverImage);
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
        if (song == null) {
            logger.debug(this.toString() + ": null song provided in contains");
            return false;
        }
        return super.contains(song);
    }

    /**
     * clear likedsongs
     */
    public void clear() {
        this.getSongs().clear();
    }
}
