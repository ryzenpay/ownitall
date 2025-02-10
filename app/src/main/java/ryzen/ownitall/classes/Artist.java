package ryzen.ownitall.classes;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.Settings;
import ryzen.ownitall.util.Levenshtein;

public class Artist {
    private static final double simularityPercentage = Settings.load().getSimilarityPercentage();
    private String name;

    /**
     * default artist constructor
     * 
     * @param name - artist name
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
        if (Levenshtein.computeSimilarityCheck(this.name.toString(), artist.toString(),
                simularityPercentage)) {
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
