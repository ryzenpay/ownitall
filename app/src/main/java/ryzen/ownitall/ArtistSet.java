package ryzen.ownitall;

import java.util.LinkedHashSet;

public class ArtistSet extends LinkedHashSet<Artist> {

    public ArtistSet() {
        super();
    }

    public ArtistSet(LinkedHashSet<Artist> artists) {
        super(artists);
    }

    public Artist get(Artist artist) {
        for (Artist thisArtist : this) {
            if (thisArtist.equals(artist)) {
                return thisArtist;
            }
        }
        return null;
    }

    public Artist get(int i) {
        int x = 0;
        for (Artist thisArtist : this) {
            if (x == i) {
                return thisArtist;
            }
            x++;
        }
        return null;
    }
}
