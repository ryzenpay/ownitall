package ryzen.ownitall;

import java.io.Serializable;
import java.util.ArrayList;

public class Playlist implements Serializable {
    private String name;
    // TODO: coverart

    /**
     * Default constructor
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.setName(name);
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
}
