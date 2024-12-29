package ownitall;

import java.util.ArrayList;

public class Playlist {
    private String name;
    private ArrayList<Song> songs;

    /**
     * Default constructor for playlist with constructed Song array
     * 
     * @param name  - name of the playlist
     * @param songs - constructed Song array of songs
     */
    public Playlist(String name, ArrayList<Song> songs) {
        this.name = name;
        this.songs = songs;
    }

    /**
     * Default constructor for playlist without Song array
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
    }
}
