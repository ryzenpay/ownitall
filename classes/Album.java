package classes;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;

public class Album {
    private String name;
    private Set<String> artists; // the first being the main, Set because no duplicates
    private ArrayList<Song> songs;
    // TODO: coverimage

    /**
     * Default constructor of album with songs
     * 
     * @param name    - album name
     * @param artists - arraylist of all artists
     * @param songs   - constructed arraylist of Song
     */
    public Album(String name, Set<String> artists, ArrayList<Song> songs) {
        this.setName(name);
        this.setArtists(artists);
        this.setSongs(songs);
    }

    /**
     * Default constructor of album without songs
     * 
     * @param name    - album name
     * @param artists - arraylist of all artists
     */
    public Album(String name, Set<String> artists) {
        this.setName(name);
        this.setArtists(artists);
        this.setSongs(new ArrayList<>());
    }

    private void setName(String name) {
        if (name == null) {
            return;
        }
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    private void setArtists(Set<String> artists) {
        if (artists == null) {
            return;
        }
        this.artists = new LinkedHashSet<>(artists);
    }

    public void addArtist(String artist) {
        if (artist == null) {
            return;
        }
        this.artists.add(artist);
    }

    public void addArtists(ArrayList<String> artists) {
        if (artists == null) {
            return;
        }
        if (artists.isEmpty()) {
            return;
        }
        this.artists.addAll(artists);
    }

    public ArrayList<String> getArtists() {
        return new ArrayList<>(this.artists);
    }

    private void setSongs(ArrayList<Song> songs) {
        if (songs == null) {
            return;
        }
        this.songs = new ArrayList<>(songs);
    }

    public ArrayList<Song> getSongs() {
        return new ArrayList<>(this.songs);
    }
}
