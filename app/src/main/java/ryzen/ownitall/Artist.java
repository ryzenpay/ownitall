package ryzen.ownitall;

import java.io.Serializable;

public class Artist implements Serializable {
    private String name;
    // private URI profilePicture; <-- this is not supported as it adds more API
    // queries and has 0 usage (very limited local usage)

    public Artist(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
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
