package ryzen.ownitall.classes;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.Settings;
import ryzen.ownitall.util.Levenshtein;
import ryzen.ownitall.util.MusicTools;

public class Song {
    private static final Logger logger = LogManager.getLogger(Song.class);
    private static final double simularityPercentage = Settings.load().getSimilarityPercentage();
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
        this.setArtist(artist);
        this.links = new LinkedHashMap<>();
        this.addLinks(links);
        this.setDuration(duration);
        this.setCoverImage(coverImage);
    }

    /**
     * get the name of the current song class
     * 
     * @return - string song name
     */
    public String getName() {
        return this.name;
    }

    public void setArtist(Artist artist) {
        if (artist == null || artist.isEmpty()) {
            logger.debug(this.toString() + ": empty constructed artist provided in setArtist");
            return;
        }
        this.artist = artist;

    }

    public void setArtist(String artistName) {
        if (artistName == null || artistName.isEmpty()) {
            logger.debug(this.toString() + ": empty string artist provided in setArtist");
            return;
        }
        this.artist = new Artist(artistName);
    }

    public Artist getArtist() {
        return this.artist;
    }

    public void addLink(String key, String url) {
        this.links.put(key, url);
    }

    public void addLinks(LinkedHashMap<String, String> links) {
        if (links == null || links.isEmpty()) {
            logger.debug(this.toString() + ": empty links provided in addLink");
            return;
        }
        this.links.putAll(links);
    }

    @JsonIgnore
    public String getLink(String key) {
        if (key == null || key.isEmpty()) {
            logger.debug(this.toString() + ": empty key passed in getLink");
            return null;
        }
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
        if (unit == null) {
            logger.debug(this.toString() + ": no duration unit provided in setDuration");
            return;
        }
        this.duration = Duration.of(duration, unit);
    }

    public void setDuration(Duration duration) {
        if (duration == null || duration.isZero()) {
            logger.debug(this.toString() + ": empty or zero duration provided in setDuration");
            return;
        }
        this.duration = duration;
    }

    public void setCoverImage(String coverImage) {
        if (coverImage == null || coverImage.isEmpty()) {
            logger.debug(this.toString() + ": empty String coverimage provided in setCoverImage");
            return;
        }
        try {
            this.coverImage = new URI(coverImage);
        } catch (URISyntaxException e) {
            logger.error("Error parsing Song cover image: " + coverImage);
        }
    }

    public URI getCoverImage() {
        return this.coverImage;
    }

    public void merge(Song song) {
        if (song == null || song.isEmpty()) {
            logger.debug(this.toString() + ": empty song provided in merge");
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

    @JsonIgnore
    public String getFileName() {
        return MusicTools.sanitizeFileName(this.name);
    }

    @Override
    @JsonIgnore
    public String toString() {
        String output = this.name.trim();
        if (this.artist != null) {
            output += " - " + this.artist.getName().trim();
        }
        return output;
    }

    @Override
    @JsonIgnore
    // TODO: beef these up, still conflict when exporting and then re-importing
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Song)) {
            return false;
        }
        Song song = (Song) object;
        // only valid if library used
        if (this.getLink("lastfm") != null) {
            if (this.getLink("lastfm").equals(song.getLink("lastfm"))) {
                return true;
            }
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
        return Objects.hash(this.name.toLowerCase().trim(), artist);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return this.name.isEmpty();
    }
}
