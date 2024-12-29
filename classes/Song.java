package classes;

import java.time.Duration;
import java.util.ArrayList;

public class Song {
    private String name;
    private ArrayList<String> artists; // the first being the main
    private Duration duration;
    // TODO: cover image

    public Song(String name, ArrayList<String> artists, Duration duration) {
        this.name = name;
        this.artists = artists;
        this.duration = duration;
    }
}
