package ryzen.ownitall;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
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
    public Song(String name, ArrayList<Artist> artists, Duration duration) {
        this.name = name;
        this.addArtists(artists);
        this.duration = duration;
    }

    /**
     * song constructor only knowing the name
     * 
     * @return
     */
    public Song(String name, Duration duration) {
        this.name = name;
        this.duration = duration;
    }

    public Song(String name, ArrayList<Artist> artists, Duration duration, String coverImage) {
        this.name = name;
        this.addArtists(artists);
        this.duration = duration;
        this.setCoverImage(coverImage);
    }

    /**
     * default song constructor without artists (for spotify episodes)
     * 
     * @param name
     * @param duration
     * @param coverImage
     */
    public Song(String name, Duration duration, String coverImage) {
        this.name = name;
        this.duration = duration;
        this.setCoverImage(coverImage);
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
    private void addArtists(ArrayList<Artist> artists) {
        if (this.artists == null) {
            this.artists = new LinkedHashSet<>();
        }
        this.artists.addAll(new LinkedHashSet<Artist>(artists));
    }

    /**
     * remove artist from song artists array
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

    private void setCoverImage(String coverImage) {
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
