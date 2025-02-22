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
    private static final String downloadFormat = Settings.load().getDownloadFormat();
    private String name;
    private Artist artist;
    private Duration duration;
    private URI coverImage;
    private LinkedHashMap<String, String> ids;

    /**
     * default song constructor
     * 
     * @param name - String song name
     */
    public Song(String name) {
        this.name = name;
        this.ids = new LinkedHashMap<>();

    }

    /**
     * full song constructor
     * 
     * @param name       - song name
     * @param artist     - constructed song artist
     * @param ids        - linkedhashmap song ids
     * @param duration   - Duration duration
     * @param coverImage - string coverimage
     */
    @JsonCreator
    public Song(@JsonProperty("name") String name, @JsonProperty("artist") Artist artist,
            @JsonProperty("ids") LinkedHashMap<String, String> ids,
            @JsonProperty("duration") long duration, @JsonProperty("coverImage") String coverImage) {
        this.name = name;
        if (artist != null) {
            this.setArtist(artist);
        }
        this.ids = new LinkedHashMap<>();
        if (ids != null) {
            this.addIds(ids);
        }
        this.setDuration(duration, ChronoUnit.SECONDS);
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

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            logger.debug(this.toString() + ": null or empty song name passed to setName");
        }
        this.name = name;
    }

    /**
     * set song artist
     * 
     * @param artist - artist to set
     */
    public void setArtist(Artist artist) {
        if (artist == null || artist.isEmpty()) {
            logger.debug(this.toString() + ": empty artist provided in setArtist");
            return;
        }
        this.artist = artist;
    }

    /**
     * get song artist
     * 
     * @return - constructed artist
     */
    public Artist getArtist() {
        return this.artist;
    }

    /**
     * add id to song
     * 
     * @param key - id key
     * @param id  - id
     */
    public void addId(String key, String id) {
        if (key == null || id == null || key.isEmpty() || id.isEmpty()) {
            logger.debug(this.toString() + ": empty key or id in addId");
            return;
        }
        this.ids.put(key, id);
    }

    /**
     * add multiple ids to song
     * 
     * @param ids - linkedhashmap of id's
     */
    public void addIds(LinkedHashMap<String, String> ids) {
        if (ids == null) {
            logger.debug(this.toString() + ": null links provided in addId");
            return;
        }
        this.ids.putAll(ids);
    }

    /**
     * get song id
     * 
     * @param key - key of id
     * @return - string id
     */
    @JsonIgnore
    public String getId(String key) {
        if (key == null || key.isEmpty()) {
            logger.debug(this.toString() + ": empty key passed in getId");
            return null;
        }
        return this.ids.get(key);
    }

    /**
     * get all song id's
     * 
     * @return - linkedhashmap of ids
     */
    public LinkedHashMap<String, String> getIds() {
        return this.ids;
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

    /**
     * set songs duration
     * 
     * @param duration - Duration
     */
    public void setDuration(Duration duration) {
        if (duration == null || duration.isZero()) {
            logger.debug(this.toString() + ": empty or zero duration provided in setDuration");
            return;
        }
        this.duration = duration;
    }

    /**
     * set song coverimage (string)
     * 
     * @param coverImage - string coverimage
     */
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

    /**
     * set song coverimage (URI)
     * 
     * @param coverImage - URI coverimage
     */
    public void setCoverImage(URI coverImage) {
        if (coverImage == null) {
            logger.debug(this.toString() + ": empty URI coverImage provided in setCoverImage");
            return;
        }
        this.coverImage = coverImage;
    }

    /**
     * get coverimage
     * 
     * @return - URI coverimage
     */
    public URI getCoverImage() {
        return this.coverImage;
    }

    /**
     * get song UTF-8 file name
     * 
     * @return - UTF-8 file name
     */
    @JsonIgnore
    public String getFileName() {
        String fileName = MusicTools.sanitizeFileName(this.getName());
        if (fileName == null || fileName.isEmpty()) {
            fileName = String.valueOf(this.hashCode());
        }
        return fileName + "." + downloadFormat;
    }

    @Override
    @JsonIgnore
    public String toString() {
        String output = this.getName().trim();
        if (this.artist != null) {
            output += " - " + this.getArtist().getName().trim();
        }
        return output;
    }

    @Override
    @JsonIgnore
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Song)) {
            return false;
        }
        Song song = (Song) object;
        // only valid if library used
        if (this.getId("lastfm") != null && song.getId("lastfm") != null) {
            if (this.getId("lastfm").equals(song.getId("lastfm"))) {
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
}
