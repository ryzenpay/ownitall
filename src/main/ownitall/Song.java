package ownitall;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;

public class Song {
    private String name;
    private Set<String> artists; // the first being the main
    private Duration duration;
    // TODO: cover image

    /**
     * Default Song constructor
     * 
     * @param name     - song name
     * @param artists  - arraylist of artists (converted to set for no duplicates)
     * @param duration - java.Duration on the song's duration
     */
    public Song(String name, ArrayList<String> artists, Duration duration) {
        this.setName(name);
        this.setArtists(artists);
        this.setDuration(duration);
    }

    /**
     * set the name of song class
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
     * get the name of the current song class
     * 
     * @return - song name
     */
    public String getName() {
        return this.name;
    }

    /**
     * set song class artists
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
     * add artist to songs artists
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
     * add arraylist of artists to the song artists
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
     * remove artist from song artists array
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
     * get all artists on the song
     * 
     * @return - arraylist of artists
     */
    public ArrayList<String> getArtists() {
        return new ArrayList<>(this.artists);
    }

    /**
     * set songs duration
     * 
     * @param duration - constructed Duration class
     */
    public void setDuration(Duration duration) {
        if (duration == null) {
            return;
        }
        this.duration = duration;
    }

    /**
     * get songs duration
     * 
     * @return - constructed Duration class
     */
    public Duration getDuration() {
        return this.duration;
    }
}
