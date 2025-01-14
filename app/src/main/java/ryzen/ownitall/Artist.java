package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ryzen.ownitall.tools.Levenshtein;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Artist {
    private static Settings settings = Settings.load();
    private String name;

    /**
     * default artist constructor setting name and initializing values
     * 
     * @param name - string artist name
     */
    @JsonCreator
    public Artist(@JsonProperty("name") String name) {
        this.name = name;
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
}
