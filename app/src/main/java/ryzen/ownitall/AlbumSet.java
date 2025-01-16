package ryzen.ownitall;

import java.util.LinkedHashSet;

public class AlbumSet extends LinkedHashSet<Album> {

    public AlbumSet() {
        super();
    }

    public Album get(Album album) {
        for (Album thisAlbum : this) {
            if (thisAlbum.equals(album)) {
                return thisAlbum;
            }
        }
        return null;
    }
}
