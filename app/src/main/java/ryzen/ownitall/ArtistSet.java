package ryzen.ownitall;

import java.util.LinkedHashSet;

public class ArtistSet extends LinkedHashSet<Artist> {

    public ArtistSet() {
        super();
    }

    public Artist get(Artist artist) {
        for (Artist thisArtist : this) {
            if (thisArtist.equals(artist)) {
                return thisArtist;
            }
        }
        return null;
    }
}
