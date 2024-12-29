package ownitall;

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
    public Album(String name, ArrayList<String> artists, ArrayList<Song> songs) {
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
    public Album(String name, ArrayList<String> artists) {
        this.setName(name);
        this.setArtists(artists);
        this.setSongs(new ArrayList<>());
    }

    /**
     * set the name of album class
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
     * get the name of the current album class
     * 
     * @return - album name
     */
    public String getName() {
        return this.name;
    }

    /**
     * set album class artists
     * 
     * @param artists - LinkedHashSet of artists
     */
    private void setArtists(ArrayList<String> artists) {
        if (artists == null) {
            return;
        }
        this.artists = new LinkedHashSet<>(artists);
    }

    /**
     * add artist to albums artists
     * 
     * @param artist - artist object/string ;)
     */
    public void addArtist(String artist) {
        if (artist == null) {
            return;
        }
        this.artists.add(artist);
    }

    /**
     * add arraylist of artists to the album artists
     * 
     * @param artists - arraylist of artists
     */
    public void addArtists(ArrayList<String> artists) {
        if (artists == null) {
            return;
        }
        if (artists.isEmpty()) {
            return;
        }
        this.artists.addAll(artists);
    }

    /**
     * remove artist from album artists array
     * 
     * @param artist - desired artist to be removed
     */
    public void remArtist(String artist) {
        if (artist == null) {
            return;
        }
        this.artists.remove(artist);
    }

    /**
     * get all artists on the album
     * 
     * @return - arraylist of artists
     */
    public ArrayList<String> getArtists() {
        return new ArrayList<>(this.artists);
    }

    /**
     * set the songs of the album
     * 
     * @param songs - arraylist of constructed Song's
     */
    private void setSongs(ArrayList<Song> songs) {
        if (songs == null) {
            return;
        }
        this.songs = new ArrayList<>(songs);
    }

    /**
     * adds constructed Song to the album
     * 
     * @param song - constructed Song class
     */
    public void addSong(Song song) {
        if (song == null) {
            return;
        }
        /*
         * for (String artist : song.getArtists()) { // TODO: is this needed? (checks if
         * song artists are in album artists)
         * if (this.getArtists().contains(artist)) {
         * this.songs.add(song);
         * return;
         * }
         * }
         */
        this.songs.add(song);
    }

    /**
     * remove song from the album
     * note: this is only the first occurence
     * 
     * @param song - desired constructed Song to be removed
     */
    public void remSong(Song song) {
        if (song == null) {
            return;
        }
        this.songs.remove(song); // TODO: does this require Song.equals()? only removes first occurence
    }

    /**
     * get all songs in the album
     * 
     * @return arraylist of constructed Song's
     */
    public ArrayList<Song> getSongs() {
        return new ArrayList<>(this.songs);
    }
}
