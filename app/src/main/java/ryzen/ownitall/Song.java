package ryzen.ownitall;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import ryzen.ownitall.tools.Levenshtein;

public class Song {
    private static Settings settings = Settings.load();
    private String name;
    private LinkedHashSet<Artist> artists; // the first being the main
    private Duration duration;

    /**
     * default song constructor
     * 
     * @param name - String song name
     */
    public Song(String name) {
        this.name = name;
        this.artists = new LinkedHashSet<>();
        this.duration = null;
    }

    /**
     * get the name of the current song class
     * 
     * @return - string song name
     */
    public String getName() {
        return this.name;
    }

    /**
     * set song name
     * 
     * @param name - string song name
     */
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

    /**
     * get main artist of song
     * currently the first artist added
     * 
     * @return - constructed Artist
     */
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
     * set songs duration
     * 
     * @param duration - Long in duration
     * @param unit     - ChronoUnit of measurement for duration
     */
    public void setDuration(long duration, ChronoUnit unit) {
        this.duration = Duration.of(duration, unit);
    }

    /**
     * set songs duration with constructed Duration
     * 
     * @param duration - constructed Duration
     */
    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        String output = this.name;
        if (!this.artists.isEmpty()) {
            output += " | " + this.getMainArtist();
        }
        return output;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Song song = (Song) object;
        if (this.hashCode() == song.hashCode()) {
            return true;
        }
        // also checks artists as they have their own "equals" and compare
        if (Levenshtein.computeSimilarityCheck(this.toString(), song.toString(), settings.getSimilarityPercentage())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = name.toLowerCase().hashCode();
        if (!this.artists.isEmpty()) {
            hashCode += this.getMainArtist().toString().toLowerCase().hashCode();
        }
        if (this.duration != null) {
            hashCode += duration.hashCode();
        }
        return hashCode;
    }

    @JsonCreator
    public Song(@JsonProperty("name") String name, @JsonProperty("artists") LinkedHashSet<Artist> artists,
            @JsonProperty("duration") Duration duration) {
        this.name = name;
        if (artists != null && !artists.isEmpty()) {
            this.artists = artists;
        } else {
            this.artists = new LinkedHashSet<>();
        }
        if (duration != null) {
            this.duration = duration;
        }
    }
}
