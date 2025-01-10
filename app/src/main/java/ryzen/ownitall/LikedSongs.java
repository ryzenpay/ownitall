package ryzen.ownitall;

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
    public boolean checkLiked(Song song) {
        if (this.size() == 0) {
            return false;
        }
        for (Song likedSong : this.getSongs()) {
            if (likedSong.hashCode() == song.hashCode()) {
                return true;
            }
        }
        return false;
    }
}
