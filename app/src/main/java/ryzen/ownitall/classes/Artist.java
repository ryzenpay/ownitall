package ryzen.ownitall.classes;

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.Settings;
import ryzen.ownitall.util.Levenshtein;

public class Artist {
    private static final Logger logger = LogManager.getLogger(Artist.class);
    private static double simularityPercentage = Settings.load().getSimilarityPercentage();
    private String name;
    private URL coverImage;

    public Artist(String name) {
        this.name = name;
    }

    @JsonCreator
    public Artist(@JsonProperty("name") String name, @JsonProperty("coverImage") String coverImage) {
        this.name = name;
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

    @Override
    public String toString() {
        return this.name;
    }

    public void setCoverImage(String coverImage) {
        if (coverImage == null) {
            return;
        }
        try {
            this.coverImage = new URL(coverImage);
        } catch (MalformedURLException e) {
            logger.error("Error parsing Song cover image: " + coverImage);
        }
    }

    public void setCoverImage(URL coverImage) {
        if (coverImage == null) {
            return;
        }
        this.coverImage = coverImage;
    }

    public URL getCoverImage() {
        return this.coverImage;
    }

    @Override
    @JsonIgnore
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
                simularityPercentage)) {
            return true;
        }
        return false;
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return this.name.toLowerCase().hashCode();
    }

    @JsonIgnore
    public boolean isEmpty() {
        if (name.isEmpty()) {
            return true;
        }
        return false;
    }
}
