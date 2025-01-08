package ryzen.ownitall;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;

import java.util.ArrayList;

public class Song {
    private String name;
    private LinkedHashSet<Artist> artists; // the first being the main
    private Duration duration;
    URI coverImage;

    /**
     * Default Song constructor
     * 
     * @param name     - song name
     * @param artists  - arraylist of artists (converted to set for no duplicates)
     * @param duration - java.Duration on the song's duration
     */
    public Song(String name) {
        this.name = name;
        this.artists = new LinkedHashSet<>();
        this.duration = null;
        this.coverImage = null;
    }

    /**
     * get the name of the current song class
     * 
     * @return - song name
     */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * add artist to songs artists
     * 
     * @param artist - artist object/string ;)
     */
    public void addArtist(Artist artist) {
        if (artist == null) {
            this.artists = new LinkedHashSet<>();
        }
        this.artists.add(artist);
    }

    /**
     * set song artists
     * 
     * @param artists - LinkedHashSet of artists
     */
    public void addArtists(ArrayList<Artist> artists) {
        if (this.artists == null) {
            this.artists = new LinkedHashSet<>();
        }
        this.artists.addAll(new LinkedHashSet<Artist>(artists));
    }

    /**
     * get all artists on the song
     * 
     * @return - arraylist of artists
     */
    public ArrayList<Artist> getArtists() {
        if (this.artists == null) {
            return new ArrayList<Artist>();
        }
        return new ArrayList<>(this.artists);
    }

    private Artist getMainArtist() {
        ArrayList<Artist> artists = new ArrayList<>(this.artists);
        return artists.get(0);
    }

    /**
     * get songs duration
     * 
     * @return - constructed Duration class
     */
    public Duration getDuration() {
        if (this.duration == null) {
            return Duration.ZERO;
        }
        return this.duration;
    }

    /**
     * duration in terms of seconds
     * 
     * @param duration
     */
    public void setDuration(long duration, ChronoUnit unit) {
        this.duration = Duration.of(duration, unit);
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setCoverImage(String coverImage) {
        try {
            this.coverImage = new URI(coverImage);
        } catch (URISyntaxException e) {
            System.err.println("Error parsing cover image: " + coverImage);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Song song = (Song) o;
        if (this.hashCode() == song.hashCode()) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.artists == null) {
            return name.hashCode() + duration.hashCode();
        }
        return name.hashCode() + this.getMainArtist().hashCode() + duration.hashCode(); // TODO: similarity search (%
                                                                                        // check)
    }
}
