package ryzen.ownitall;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.tools.Levenshtein;

public class Song {
    private String name;
    private Artist artist;
    private Duration duration;
    private double simularityPercentage;

    /**
     * default song constructor
     * 
     * @param name - String song name
     */
    public Song(String name) {
        this.name = name;
        this.artist = null;
        this.duration = null;
        this.simularityPercentage = Settings.load().getSimilarityPercentage();
    }

    @JsonCreator
    public Song(@JsonProperty("name") String name, @JsonProperty("artist") Artist artist,
            @JsonProperty("duration") Duration duration) {
        this.name = name;
        if (artist != null && !artist.isEmpty()) {
            this.artist = artist;
        } else {
            this.artist = null;
        }
        if (duration != null) {
            this.duration = duration;
        }
        this.simularityPercentage = Settings.load().getSimilarityPercentage();
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

    public void setArtist(Artist artist) {
        if (artist == null) {
            return;
        }
        this.artist = artist;
    }

    public Artist getArtist() {
        return this.artist;
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

    public void mergeSong(Song song) {
        if (song == null) {
            return;
        }
        if (song.isEmpty()) {
            return;
        }
        if (this.artist == null && song.artist != null) { // if it has more info, no better way to check
            this.name = song.name;
            this.artist = song.artist;
        }
    }

    @Override
    @JsonIgnore
    public String toString() {
        String output = this.name;
        if (this.artist != null) {
            output += " | " + this.artist.getName();
        }
        return output;
    }

    @Override
    @JsonIgnore
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
        if (Levenshtein.computeSimilarityCheck(this.toString(), song.toString(), simularityPercentage)) {
            return true;
        }
        return false;
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        int hashCode = name.toLowerCase().hashCode();
        if (this.artist != null) {
            hashCode += this.artist.hashCode();
        }
        if (this.duration != null) {
            hashCode += duration.hashCode();
        }
        return hashCode;
    }

    @JsonIgnore
    public boolean isEmpty() {
        if (name.isEmpty()) {
            return true;
        }
        return false;
    }
}
