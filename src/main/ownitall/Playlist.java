package ownitall;

import java.util.ArrayList;

public class Playlist {
    private String name;
    private ArrayList<Song> songs;
    // TODO: coverart

    /**
     * Default constructor for playlist with constructed Song array
     * 
     * @param name  - name of the playlist
     * @param songs - constructed Song array of songs
     */
    public Playlist(String name, ArrayList<Song> songs) {
        this.setName(name);
        this.setSongs(songs);
    }

    /**
     * Default constructor for playlist without Song array
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.setName(name);
        this.setSongs(new ArrayList<>());
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
     * set the songs of the playlist
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
     * adds constructed Song to the playlist
     * 
     * @param song - constructed Song class
     */
    public void addSong(Song song) {
        if (song == null) {
            return;
        }
        /*
         * for (String artist : song.getArtists()) { // TODO: is this needed? (checks if
         * song artists are in playlist artists)
         * if (this.getArtists().contains(artist)) {
         * this.songs.add(song);
         * return;
         * }
         * }
         */
        this.songs.add(song);
    }

    /**
     * remove song from the playlist
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
     * get all songs in the playlist
     * 
     * @return arraylist of constructed Song's
     */
    public ArrayList<Song> getSongs() {
        return new ArrayList<>(this.songs);
    }
}
