package ryzen.ownitall;

import java.net.URI;

public class Playlist {
    private String name;
    private URI coverart;

    private String youtubePageToken = null; // TODO: create "update" method to save API requests
    private int spotifyPageOffset = -1;

    /**
     * Default playlist constructor without coverart
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.name = name;
    }

    /**
     * Default playlist constructor with coverart
     * 
     * @param name     - name of playlist
     * @param coverart - constructed URI
     */
    public Playlist(String name, URI coverart) {
        this.name = name;
        this.coverart = coverart;
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
