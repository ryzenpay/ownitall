package ryzen.ownitall;

import java.util.LinkedHashSet;
import java.util.Set;
import java.io.Serializable;
import java.util.ArrayList;

public class Album implements Serializable {
    private String name;
    private Set<String> artists; // the first being the main, Set because no duplicates
    // TODO: coverimage

    /**
     * Default constructor of album
     * 
     * @param name    - album name
     * @param artists - arraylist of all artists
     */
    public Album(String name, ArrayList<String> artists) {
        this.setName(name);
        this.setArtists(artists);
    }

    /**
     * set the name of album class
     * 
     * @param name - desired name
     */
    private void setName(String name) {
        if (name == null) {
            return;
        }
        this.name = name;
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
     * set album class artists
     * 
     * @param artists - LinkedHashSet of artists
     */
    private void setArtists(ArrayList<String> artists) {
        if (artists == null) {
            return;
        }
        this.artists = new LinkedHashSet<>(artists);
    }

    /**
     * add artist to albums artists
     * 
     * @param artist - artist object/string ;)
     */
    public void addArtist(String artist) {
        if (artist == null) {
            return;
        }
        this.artists.add(artist);
    }

    /**
     * add arraylist of artists to the album artists
     * 
     * @param artists - arraylist of artists
     */
    public void addArtists(ArrayList<String> artists) {
        if (artists == null) {
            return;
        }
        if (artists.isEmpty()) {
            return;
        }
        this.artists.addAll(artists);
    }

    /**
     * remove artist from album artists array
     * 
     * @param artist - desired artist to be removed
     */
    public void remArtist(String artist) {
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
    public ArrayList<String> getArtists() {
        return new ArrayList<>(this.artists);
    }

}
