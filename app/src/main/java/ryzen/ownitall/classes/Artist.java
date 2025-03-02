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

import ryzen.ownitall.Settings;

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

    /**
     * get artist name
     * 
     * @return - string artist name
     */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            logger.debug(this.toString() + ": null or empty artist name passed to setName");
        }
        this.name = name;
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
     * add multiple ids to artist
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
        if (coverImage == null || coverImage.isEmpty()) {
            logger.debug(this.toString() + ": empty String coverimage provided in setCoverImage");
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
        // only valid if library used
        for (String id : this.getIds().keySet()) {
            if (this.getId(id).equals(artist.getId(id))) {
                return true;
            }
        }
        if (this.toString().equals(artist.toString())) {
            return true;
        }
        return false;
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return Objects.hashCode(this.name.toLowerCase().trim());
    }

    @JsonIgnore
    public boolean isEmpty() {
        return this.name.isEmpty();
    }
}
