package ryzen.ownitall;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Artist {
    private String name;
    private URI profilePicture;

    public Artist(String name) {
        this.name = name;
        this.profilePicture = null;
    }

    public String getName() {
        return this.name;
    }

    public void setProfilePicture(String profilePicture) {
        try {
            this.profilePicture = new URI(profilePicture);
        } catch (URISyntaxException e) {
            System.err.println("Error parsing cover image: " + profilePicture);
        }
    }

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
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @JsonCreator
    public Artist(@JsonProperty("name") String name, @JsonProperty("profilePicture") String profilePicture) {
        this.name = name;
        if (profilePicture != null) {
            this.setProfilePicture(profilePicture);
        }
    }
}
