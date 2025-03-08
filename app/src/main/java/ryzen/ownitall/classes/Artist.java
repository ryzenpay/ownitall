package ryzen.ownitall.classes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Artist {
    private static final Logger logger = LogManager.getLogger(Artist.class);
    private String name;
    private URI coverImage;
    private LinkedHashMap<String, String> ids;

    /**
     * default artist constructor
     * 
     * @param name - artist name
     */
    public Artist(String name) {
        this.name = name;
        this.ids = new LinkedHashMap<>();
    }

    @JsonCreator
    public Artist(@JsonProperty("name") String name, @JsonProperty("ids") LinkedHashMap<String, String> ids,
            @JsonProperty("coverImage") String coverImage) {
        this.name = name;
        this.ids = new LinkedHashMap<>();
        if (ids != null) {
            this.addIds(ids);
        }
        if (coverImage != null) {
            this.setCoverImage(coverImage);
        }
    }

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

    public void setName(String name) {
        if (name == null) {
            logger.debug(this.toString() + ": null artist name passed to setName");
        }
        this.name = name;
    }

    /**
     * add multiple ids to artist
     * 
     * @param ids - linkedhashmap of id's
     */
    public void addIds(LinkedHashMap<String, String> ids) {
        if (ids == null) {
            logger.debug(this.toString() + ": null links provided in addId");
            return;
        }
        for (String id : ids.keySet()) {
            this.addId(id, ids.get(id));
        }
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
     * get artist id
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
     * get all artist id's
     * 
     * @return - linkedhashmap of ids
     */
    public LinkedHashMap<String, String> getIds() {
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
            logger.error(this.toString() + ": exception parsing song cover image: '" + coverImage + "'");
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

    @Override
    @JsonIgnore
    public String toString() {
        return this.name.toString().trim();
    }

    @Override
    @JsonIgnore
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Artist)) {
            return false;
        }
        Artist artist = (Artist) object;
        if (this.hashCode() == artist.hashCode()) {
            return true;
        }
        // only valid if library used
        for (String id : this.getIds().keySet()) {
            if (this.getId(id).equals(artist.getId(id))) {
                return true;
            }
        }
        if (this.toString().equalsIgnoreCase(artist.toString())) {
            return true;
        }
        return false;
    }

    @JsonIgnore
    @Override
    public int hashCode() {
        return Objects.hash(name, ids);
    }
}
