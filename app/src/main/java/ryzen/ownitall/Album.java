package ryzen.ownitall;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.io.Serializable;
import java.net.URI;

public class Album implements Serializable {
    private String name;
    private LinkedHashSet<Artist> artists; // the first being the main, Set because no duplicates
    private URI coverimage;

    /**
     * Default constructor of album without album cover
     * 
     * @param name    - album name
     * @param artists - arraylist of all artists
     */
    public Album(String name, ArrayList<Artist> artists) {
        this.name = name;
        this.addArtists(artists);
    }

    /**
     * Default constructor of album with album cover
     * 
     * @param name       - album name
     * @param artists    - arraylist of all artists
     * @param coverimage - coverimage of album
     */
    public Album(String name, ArrayList<Artist> artists, URI coverimage) {
        this.name = name;
        this.addArtists(artists);
        this.coverimage = coverimage;
    }

    /**
     * get the name of the current album class
     * 
     * @return - album name
     */
    public String getName() {
        return this.name;
    }

    /**
     * get the album cover image
     * 
     * @return - URI cover image
     */
    public URI getCoverImage() {
        return this.coverimage;
    }

    /**
     * set album class artists
     * 
     * @param artists - LinkedHashSet of artists
     */
    private void addArtists(ArrayList<Artist> artists) {
        if (this.artists == null) {
            this.artists = new LinkedHashSet<>(artists);
            return;
        }
        this.artists.addAll(new LinkedHashSet<Artist>(artists));
    }

    /**
     * add artist to albums artists
     * 
     * @param artist - artist object/string ;)
     */
    public void addArtist(Artist artist) {
        if (artist == null) {
            return;
        }
        this.artists.add(artist);
    }

    /**
     * remove artist from album artists array
     * 
     * @param artist - desired artist to be removed
     */
    public void remArtist(Artist artist) {
        if (artist == null) {
            return;
        }
        this.artists.remove(artist);
    }

    /**
     * get all artists on the album
     * 
     * @return - arraylist of artists
     */
    public LinkedHashSet<Artist> getArtists() {
        return new LinkedHashSet<>(this.artists);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Album album = (Album) object;
        if (this.name.equalsIgnoreCase(album.name.toLowerCase())) { // TODO: check with artists
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode(); // TODO: check with artists
    }

}
