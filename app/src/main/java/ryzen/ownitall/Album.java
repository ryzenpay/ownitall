package ryzen.ownitall;

import java.util.LinkedHashSet;

import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;

public class Album {
    private String name;
    LinkedHashSet<Song> songs;
    private LinkedHashSet<Artist> artists; // the first being the main, Set because no duplicates
    private URI coverImage;

    /**
     * Default constructor of album without album cover
     * 
     * @param name    - album name
     * @param artists - arraylist of all artists
     */
    public Album(String name) {
        this.name = name;
        this.songs = new LinkedHashSet<>();
        this.artists = new LinkedHashSet<>();
        this.coverImage = null;
    }

    /**
     * get the name of the current album class
     * 
     * @return - album name
     */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCoverImage(String coverImage) {
        try {
            this.coverImage = new URI(coverImage);
        } catch (URISyntaxException e) {
            System.err.println("Error parsing cover image: " + coverImage);
        }
    }

    /**
     * get the album cover image
     * 
     * @return - URI cover image
     */
    public URI getCoverImage() {
        if (this.coverImage == null) {
            return null;
        }
        return this.coverImage;
    }

    public void addSongs(ArrayList<Song> songs) {
        this.songs.addAll(new LinkedHashSet<Song>(songs));
    }

    /**
     * set album class artists
     * 
     * @param artists - LinkedHashSet of artists
     */
    public void addArtists(ArrayList<Artist> artists) {
        this.artists.addAll(new LinkedHashSet<Artist>(artists));
    }

    /**
     * add artist to albums artists
     * 
     * @param artist - artist object/string ;)
     */
    public void addArtist(Artist artist) {
        this.artists.add(artist);
    }

    /**
     * get all artists on the album
     * 
     * @return - arraylist of artists
     */
    public LinkedHashSet<Artist> getArtists() {
        return new LinkedHashSet<>(this.artists);
    }

    public ArrayList<Song> getSongs() {
        return new ArrayList<>(this.songs);
    }

    public Artist getMainArtist() {
        ArrayList<Artist> artists = new ArrayList<>(this.artists);
        return artists.get(0);
    }

    public int size() {
        return this.songs.size();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Album album = (Album) object;
        if (this.hashCode() == album.hashCode()) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() + this.getMainArtist().hashCode();
    }
}
