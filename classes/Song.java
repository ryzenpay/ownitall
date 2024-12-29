package classes;

import java.time.Duration;

public class Song {
    private String name;
    private String artist;
    private Duration duration;
    // TODO: cover image

    public Song(String name, String artist, Duration duration) {
        this.name = name;
        this.artist = artist;
        this.duration = duration;
    }
}
