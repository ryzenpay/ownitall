package ryzen.ownitall;

import java.net.URI;
import java.net.URISyntaxException;

public class Artist {
    private String name;
    private URI profilePicture;

    public Artist(String name) {
        this.name = name;
    }

    public Artist(String name, String profilePicture) {
        this.name = name;
        this.setProfilePicture(profilePicture);
    }

    public String getName() {
        return this.name;
    }

    private void setProfilePicture(String profilePicture) {
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
    public int hashCode() {
        return this.name.hashCode();
    }
}
