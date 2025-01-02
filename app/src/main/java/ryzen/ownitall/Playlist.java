package ryzen.ownitall;

import java.net.URI;

public class Playlist {
    private String name;
    private URI coverart;

    /**
     * Default playlist constructor without coverart
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.setName(name);
    }

    /**
     * Default playlist constructor with coverart
     * 
     * @param name     - name of playlist
     * @param coverart - constructed URI
     */
    public Playlist(String name, URI coverart) {
        this.setName(name);
        this.coverart = coverart;
    }

    /**
     * set the name of playlist class
     * 
     * @param name - desired name
     */
    private void setName(String name) {
        if (name == null) {
            return;
        }
        this.name = name;
    }

    /**
     * get the name of the current playlist class
     * 
     * @return - playlist name
     */
    public String getName() {
        return this.name;
    }

    /**
     * get coverart of current playlist class
     * 
     * @return - constructed URI
     */
    public URI getCoverart() {
        return this.coverart;
    }
}
