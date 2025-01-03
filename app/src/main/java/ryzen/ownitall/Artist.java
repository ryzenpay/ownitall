package ryzen.ownitall;

import java.io.Serializable;
import java.net.URI;

public class Artist implements Serializable {
    private String name;
    private URI profilePicture;

    public Artist(String name) {
        this.name = name;
    }

    public Artist(String name, String profilePicture) { // TODO: spotify artist pfp
        this.name = name;
        try {
            this.profilePicture = new URI(profilePicture);
        } catch (Exception e) {
            System.err.println("Error converting artist profile picture: " + e);
        }
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
