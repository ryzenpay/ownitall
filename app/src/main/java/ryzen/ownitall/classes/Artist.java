package ryzen.ownitall.classes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.Settings;
import ryzen.ownitall.util.Levenshtein;

public class Artist {
    private static double simularityPercentage = Settings.load().getSimilarityPercentage();
    private String name;

    @JsonCreator
    public Artist(@JsonProperty("name") String name) {
        this.name = name;
    }

    public Artist(Artist artist) {
        this.name = artist.getName();
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
