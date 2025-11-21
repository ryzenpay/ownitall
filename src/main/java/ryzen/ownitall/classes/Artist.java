package ryzen.ownitall.classes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.util.Logger;

/**
 * <p>
 * Artist class.
 * </p>
 *
 * @author ryzen
 */
public class Artist {
    private static final Logger logger = new Logger(Artist.class);
    private String name;
    private URI coverImage;
    private LinkedHashSet<Id> ids;

    /**
     * default artist constructor
     *
     * @param name - artist name
     */
    public Artist(String name) {
        this.name = name;
        this.ids = new LinkedHashSet<>();
    }

    /**
     * <p>
     * Constructor for Artist.
     * </p>
     *
     * @param name       a {@link java.lang.String} object
     * @param ids        a {@link java.util.LinkedHashMap} object
     * @param coverImage a {@link java.lang.String} object
     */
    @JsonCreator
    public Artist(@JsonProperty("name") String name, @JsonProperty("ids") LinkedHashSet<Id> ids,
            @JsonProperty("coverImage") String coverImage) {
        this.name = name;
        this.ids = new LinkedHashSet<>();
        if (ids != null) {
            this.addIds(ids);
        }
        if (coverImage != null) {
            this.setCoverImage(coverImage);
        }
    }

    /**
     * merge content from one artist into this artist
     *
     * @param artist - artist to get info from
     */
    public void merge(Artist artist) {
        if (artist == null) {
            logger.debug(this.toString() + ": null artist passed in merge");
            return;
        }
        this.addIds(artist.getIds());
        if (this.getCoverImage() == null && artist.getCoverImage() != null) {
            this.setCoverImage(artist.getCoverImage());
        }
    }

    /**
     * get artist name
     *
     * @return - string artist name
     */
    public String getName() {
        return this.name;
    }

    /**
     * set artist name
     *
     * @param name - string name to set
     */
    public void setName(String name) {
        if (name == null) {
            logger.debug(this.toString() + ": null artist name passed to setName");
            return;
        }
        this.name = name;
    }

    /**
     * add multiple ids to artist
     *
     * @param ids - linkedhashmap of id's
     */
    public void addIds(LinkedHashSet<Id> ids) {
        if (ids == null) {
            logger.debug(this.toString() + ": null links provided in addId");
            return;
        }
        this.ids.addAll(ids);
    }

    public void addId(String key, String value) {
        this.addId(new Id(key, value));
    }

    /**
     * add id to song
     *
     * @param key - id key
     * @param id  - id
     */
    public void addId(Id id) {
        if (id == null || id.isEmpty()) {
            logger.debug(this.toString() + ": empty id in addId");
            return;
        }
        this.ids.add(id);
    }

    /**
     * get artist id
     *
     * @param key - key of id
     * @return - string id
     */
    public Id getId(String key) {
        if (key == null || key.isEmpty()) {
            logger.debug(this.toString() + ": empty key passed in getId");
            return null;
        }
        for (Id id : this.ids) {
            if (id.getKey().equals(key)) {
                return id;
            }
        }
        return null;
    }

    /**
     * get all artist id's
     *
     * @return - linkedhashmap of ids
     */
    public LinkedHashSet<Id> getIds() {
        return this.ids;
    }

    /**
     * set artist coverimage (string)
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
    public String toString() {
        return this.name.toString().trim();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Artist)) {
            return false;
        }
        Artist artist = (Artist) object;
        // only valid if library used
        for (Id id : this.ids) {
            if (artist.getId(id.getKey()) == null) {
                continue;
            }
            if (id.getValue().equals(artist.getId(id.getKey()).getValue())) {
                return true;
            }
        }
        if (this.toString().equalsIgnoreCase(artist.toString())) {
            return true;
        }
        return false;
    }
}
