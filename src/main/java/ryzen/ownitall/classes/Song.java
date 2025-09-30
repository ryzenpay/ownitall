package ryzen.ownitall.classes;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.util.Logger;

/**
 * <p>
 * Song class.
 * </p>
 *
 * @author ryzen
 */
public class Song {
    private static final Logger logger = new Logger(Song.class);
    private String name;
    private ArrayList<Artist> artists;
    private Duration duration;
    private String albumName;
    private URI coverImage;
    private LinkedHashMap<String, String> ids;

    /**
     * default song constructor
     *
     * @param name - String song name
     */
    // TODO: transform feat. into artist addition?
    public Song(String name) {
        this.name = name;
        this.ids = new LinkedHashMap<>();
        this.artists = new ArrayList<>();

    }

    @JsonCreator
    /**
     * Full song constructor
     * also used for json importing
     *
     * @param name       - song name
     * @param artists    - arraylist of song artists
     * @param ids        - arraylist of external ids associated to the song
     * @param duration   - song duration in seconds
     * @param albumName  - name of album song is part of
     * @param coverImage - song coverimage
     */
    public Song(@JsonProperty("name") String name, @JsonProperty("artists") ArrayList<Artist> artists,
            @JsonProperty("ids") LinkedHashMap<String, String> ids,
            @JsonProperty("duration") double duration, @JsonProperty("albumName") String albumName,
            @JsonProperty("coverImage") String coverImage) {
        this.name = name;
        this.artists = new ArrayList<>();
        if (artists != null) {
            this.addArtists(artists);
        }
        this.ids = new LinkedHashMap<>();
        if (ids != null) {
            this.addIds(ids);
        }
        this.setDuration((long) duration, ChronoUnit.SECONDS);
        if (albumName != null) {
            this.setAlbumName(albumName);
        }
        if (coverImage != null) {
            this.setCoverImage(coverImage);
        }
    }

    /**
     * merge a song into this song
     *
     * @param song - song to get details from
     */
    public void merge(Song song) {
        if (song == null) {
            logger.debug(this.toString() + ": null song provided in merge");
            return;
        }
        this.addArtists(song.getArtists());
        if (this.getDuration().isZero() && !song.getDuration().isZero()) {
            this.setDuration(song.getDuration());
        }
        if (this.getAlbumName() == null && song.getAlbumName() != null) {
            this.setAlbumName(song.getAlbumName());
        }
        if (this.getCoverImage() == null && song.getCoverImage() != null) {
            this.setCoverImage(song.getCoverImage());
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
     * @param name - string to set song name
     */
    public void setName(String name) {
        if (name == null) {
            logger.debug(this.toString() + ": null song name passed to setName");
            return;
        }
        this.name = name;
    }

    /**
     * <p>
     * addArtists.
     * </p>
     *
     * @param artists a {@link java.util.ArrayList} object
     */
    public void addArtists(ArrayList<Artist> artists) {
        if (artists == null) {
            logger.debug(this.toString() + ": null artists provided in addArtists");
            return;
        }
        for (Artist artist : artists) {
            this.addArtist(artist);
        }
    }

    /**
     * set song artist
     *
     * @param artist - artist to set
     */
    public void addArtist(Artist artist) {
        if (artist == null) {
            logger.debug(this.toString() + ": null artist provided in addArtist");
            return;
        }
        Artist foundArtist = this.getArtist(artist);
        if (foundArtist != null) {
            foundArtist.merge(artist);
        } else {
            this.artists.add(artist);
        }
    }

    /**
     * <p>
     * Getter for the field <code>artists</code>.
     * </p>
     *
     * @return a {@link java.util.ArrayList} object
     */
    public ArrayList<Artist> getArtists() {
        return this.artists;
    }

    /**
     * get song artist
     *
     * @return - constructed artist
     * @param artist a {@link ryzen.ownitall.classes.Artist} object
     */
    @JsonIgnore
    public Artist getArtist(Artist artist) {
        if (artist == null) {
            logger.debug(this.toString() + ": null artist provided in getArtist");
            return null;
        }
        for (Artist thisArtist : this.artists) {
            if (thisArtist.equals(artist)) {
                return thisArtist;
            }
        }
        return null;
    }

    /**
     * <p>
     * getMainArtist.
     * </p>
     *
     * @return a {@link ryzen.ownitall.classes.Artist} object
     */
    @JsonIgnore
    public Artist getMainArtist() {
        if (this.artists.isEmpty()) {
            return null;
        }
        return this.artists.get(0);
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
        if (duration == null) {
            logger.debug(this.toString() + ": null duration provided in setDuration");
            return;
        }
        this.duration = duration;
    }

    /**
     * set songs album name
     *
     * @param albumName - string album name
     */
    public void setAlbumName(String albumName) {
        if (albumName == null) {
            logger.debug(this.toString() + ": null albumName provided in setAlbumName");
            return;
        }
        this.albumName = albumName;
    }

    /**
     * get songs album name
     *
     * @return - String song albumname
     */
    public String getAlbumName() {
        return this.albumName;
    }

    /**
     * set song coverimage (string)
     *
     * @param coverImage - string coverimage
     */
    public void setCoverImage(String coverImage) {
        if (coverImage == null) {
            logger.debug(this.toString() + ": null String coverimage provided in setCoverImage");
            return;
        }
        try {
            this.coverImage = new URI(coverImage);
        } catch (URISyntaxException e) {
            logger.error(this.toString() + ": exception parsing song cover image: '" + coverImage + "'", e);
        }
    }

    /**
     * set song coverimage (URI)
     *
     * @param coverImage - URI coverimage
     */
    public void setCoverImage(URI coverImage) {
        if (coverImage == null) {
            logger.debug(this.toString() + ": null URI coverImage provided in setCoverImage");
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

    /** {@inheritDoc} */
    @Override
    @JsonIgnore
    public String toString() {
        String output = this.getName().trim();
        if (this.getMainArtist() != null) {
            output += " - " + this.getMainArtist().toString().trim();
        }
        return output;
    }

    /** {@inheritDoc} */
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
        for (String id : this.getIds().keySet()) {
            if (this.getId(id).equals(song.getId(id))) {
                return true;
            }
        }
        if (this.toString().equalsIgnoreCase(song.toString())) {
            return true;
        }
        if (song.getName().toLowerCase().contains(this.getName().toLowerCase())) {
            if (this.getMainArtist() != null && song.getMainArtist() != null
                    && this.getMainArtist().equals(song.getMainArtist())) {
                if (this.getAlbumName() != null && this.getAlbumName().equalsIgnoreCase(song.getAlbumName())) {
                    return true;
                }
            }
            if (this.getCoverImage() != null && this.getCoverImage().equals(song.getCoverImage())) {
                return true;
            }
        }
        return false;
    }
}
