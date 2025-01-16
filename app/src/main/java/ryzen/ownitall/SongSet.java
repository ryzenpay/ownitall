package ryzen.ownitall;

import java.util.LinkedHashSet;

public class SongSet extends LinkedHashSet<Song> {

    public SongSet() {
        super();
    }

    public SongSet(LinkedHashSet<Song> songs) {
        super(songs);
    }

    public Song get(Song song) {
        for (Song thisSong : this) {
            if (thisSong.equals(song)) {
                return thisSong;
            }
        }
        return null;
    }
}
