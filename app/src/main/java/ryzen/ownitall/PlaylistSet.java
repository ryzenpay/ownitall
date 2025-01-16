package ryzen.ownitall;

import java.util.LinkedHashSet;

public class PlaylistSet extends LinkedHashSet<Playlist> {

    public PlaylistSet() {
        super();
    }

    public Playlist get(Playlist playlist) {
        for (Playlist thisPlaylist : this) {
            if (thisPlaylist.equals(playlist)) {
                return thisPlaylist;
            }
        }
        return null;
    }
}
