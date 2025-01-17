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

    public Song get(int i) {
        int x = 0;
        for (Song thisSong : this) {
            if (x == i) {
                return thisSong;
            }
            x++;
        }
        return null;
    }
}
