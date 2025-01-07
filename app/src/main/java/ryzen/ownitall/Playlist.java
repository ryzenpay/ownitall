package ryzen.ownitall;

import java.net.URI;
import java.util.ArrayList;

public class Playlist {
    private String name;
    private URI coverart;
    private ArrayList<Song> songs; // arraylist cuz it can contain duplicates

    private String youtubePageToken = null; // TODO: create "update" method to save API requests
    private int spotifyPageOffset = -1;

    /**
     * Default playlist constructor without coverart
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
        this.coverart = null;
    }

    /**
     * get the name of the current playlist class
     * 
     * @return - playlist name
     */
    public String getName() {
        return this.name;
    }

    public void setCoverArt(URI coverArt) {
        this.coverart = coverArt;
    }

    /**
     * get coverart of current playlist class
     * 
     * @return - constructed URI
     */
    public URI getCoverart() {
        return this.coverart;
    }

    public void addSongs(ArrayList<Song> songs) {
        this.songs.addAll(songs);
    }

    public void addSong(Song song) {
        this.songs.add(song);
    }

    public int size() {
        return this.songs.size();
    }

    public ArrayList<Song> getSongs() {
        return this.songs;
    }

    public String getYoutubePageToken() {
        return this.youtubePageToken;
    }

    public void setYoutubePageToken(String youtubePageToken) {
        this.youtubePageToken = youtubePageToken;
    }

    public int getSpotifyPageOffset() {
        return this.spotifyPageOffset;
    }

    public void setSpotifyPageOffset(int spotifyPageOffset) {
        this.spotifyPageOffset = spotifyPageOffset;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Playlist playlist = (Playlist) object;
        if (this.hashCode() == playlist.hashCode()) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode += name.hashCode();
        return hashCode;
    }
}
