package ryzen.ownitall.classes;

import ryzen.ownitall.Settings;

//a playlist for just liked songs (as they might not be in playlists)
/**
 * <p>LikedSongs class.</p>
 *
 * @author ryzen
 */
public class LikedSongs extends Playlist {
    /**
     * LikedSongs construcor with no songs
     */
    public LikedSongs() {
        super(Settings.likedSongName);
    }
}
