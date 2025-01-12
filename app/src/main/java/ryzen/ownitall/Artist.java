package ryzen.ownitall;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.tools.Levenshtein;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Artist {
    private static final Logger logger = LogManager.getLogger(Artist.class);
    private static Settings settings = Settings.load();
    private String name;
    private URI profilePicture;

    /**
     * default artist constructor setting name and initializing values
     * 
     * @param name - string artist name
     */
    public Artist(String name) {
        this.name = name;
        this.profilePicture = null;
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
     * set artists profile picture
     * 
     * @param profilePicture - string url of artists profile picture
     */
    public void setProfilePicture(String profilePicture) {
        try {
            this.profilePicture = new URI(profilePicture);
        } catch (URISyntaxException e) {
            logger.error("Error parsing cover image: " + profilePicture);
        }
    }

    /**
     * get artists profile picture
     * 
     * @return - constructed URI of artists profile picture
     */
    public URI getProfilePicture() {
        return this.profilePicture;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Artist artist = (Artist) object;
        if (this.hashCode() == artist.hashCode()) {
            return true;
        }
        if (Levenshtein.computeSimilarityCheck(this.name.toString(), artist.toString(),
                settings.getSimilarityPercentage())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.toLowerCase().hashCode();
    }

    @JsonCreator
    public Artist(@JsonProperty("name") String name, @JsonProperty("profilePicture") String profilePicture) {
        this.name = name;
        if (profilePicture != null) {
            this.setProfilePicture(profilePicture);
        }
    }
}
