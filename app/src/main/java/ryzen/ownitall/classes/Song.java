package ryzen.ownitall.classes;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.Settings;
import ryzen.ownitall.util.Levenshtein;

public class Song {
    private static final Logger logger = LogManager.getLogger(Song.class);
    private static double simularityPercentage = Settings.load().getSimilarityPercentage();
    private String name;
    private Artist artist;
    private Duration duration;
    private URI coverImage;
    private LinkedHashMap<String, String> links;

    /**
     * default song constructor
     * 
     * @param name - String song name
     */
    public Song(String name) {
        this.name = name;
        this.links = new LinkedHashMap<>();

    }

    @JsonCreator
    public Song(@JsonProperty("name") String name, @JsonProperty("artist") Artist artist,
            @JsonProperty("links") LinkedHashMap<String, String> links,
            @JsonProperty("duration") Duration duration, @JsonProperty("coverImage") String coverImage) {
        this.name = name;
        if (artist != null && !artist.isEmpty()) {
            this.artist = artist;
        } else {
            this.artist = null;
        }
        if (links != null && !links.isEmpty()) {
            this.links = links;
        } else {
            this.links = new LinkedHashMap<>();
        }
        if (duration != null) {
            this.duration = duration;
        }
        if (coverImage != null) {
            this.setCoverImage(coverImage);
        }
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
        if (artist == null || artist.isEmpty()) {
            return;
        }
        this.artist = artist;
    }

    public Artist getArtist() {
        return this.artist;
    }

    public void addLink(String key, String url) {
        this.links.put(key, url);
    }

    public void addLinks(LinkedHashMap<String, String> links) {
        this.links.putAll(links);
    }

    public String getLink(String key) {
        return this.links.get(key);
    }

    public LinkedHashMap<String, String> getLinks() {
        return this.links;
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

    public void setCoverImage(String coverImage) {
        if (coverImage == null) {
            return;
        }
        try {
            this.coverImage = new URI(coverImage);
        } catch (URISyntaxException e) {
            logger.error("Error parsing Song cover image: " + coverImage);
        }
    }

    public void setCoverImage(URI coverImage) {
        if (coverImage == null) {
            return;
        }
        this.coverImage = coverImage;
    }

    public URI getCoverImage() {
        return this.coverImage;
    }

    public void merge(Song song) {
        if (song == null || song.isEmpty()) {
            return;
        }
        if (song.isEmpty()) {
            return;
        }
        if (this.artist == null && song.artist != null) { // if it has more info, no better way to check
            this.name = song.name;
            this.artist = song.artist;
        }
        if (this.coverImage == null && song.getCoverImage() != null) {
            this.coverImage = song.getCoverImage();
        }
        this.addLinks(song.getLinks());
    }

    @Override
    @JsonIgnore
    public String toString() {
        String output = this.name;
        if (this.artist != null) {
            output += " - " + this.artist.getName();
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
