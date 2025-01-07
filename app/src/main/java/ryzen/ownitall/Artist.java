package ryzen.ownitall;

import java.io.Serializable;
import java.net.URI;

public class Artist implements Serializable {
    private String name;
    private URI profilePicture; // TODO: artist pfp

    public Artist(String name) {
        this.name = name;
    }

    public Artist(String name, URI profilePicture) {
        this.name = name;
        this.profilePicture = profilePicture;
    }

    public String getName() {
        return this.name;
    }

    public void setProfilePicture(URI profilePicrture) {
        this.profilePicture = profilePicrture;
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
