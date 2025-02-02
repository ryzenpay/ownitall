package ryzen.ownitall.classes;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.Settings;
import ryzen.ownitall.util.Levenshtein;
import ryzen.ownitall.util.MusicTools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Playlist {
    private static final Logger logger = LogManager.getLogger(Playlist.class);
    protected static final double simularityPercentage = Settings.load().getSimilarityPercentage();
    protected static final String downloadFormat = Settings.load().getDownloadFormat();
    private String name;
    private URI coverImage;
    private LinkedHashSet<Song> songs;

    /**
     * to save api requests
     */
    private String youtubePageToken = null;
    private int spotifyPageOffset = 0;

    /**
     * Default playlist constructor
     * 
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.name = name;
        this.songs = new LinkedHashSet<>();
    }

    @JsonCreator
    public Playlist(@JsonProperty("name") String name,
            @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverArt") String coverArt) {
        this.name = name;
        this.songs = new LinkedHashSet<>();
        this.addSongs(songs);
        this.setYoutubePageToken(youtubePageToken);
        this.setSpotifyPageOffset(spotifyPageOffset);
        this.setCoverImage(coverArt);
    }

    public void merge(Playlist playlist) {
        if (playlist == null || playlist.isEmpty()) {
            logger.debug(this.toString() + ": empty playlist provided in merge");
            return;
        }
        this.addSongs(playlist.getSongs());
        if (this.getCoverImage() == null && playlist.getCoverImage() != null) {
            this.setCoverImage(playlist.getCoverImage());
        }
        if (playlist.getYoutubePageToken() != null) {
            this.youtubePageToken = playlist.getYoutubePageToken();
        }
        if (playlist.getSpotifyPageOffset() > this.getSpotifyPageOffset()) {
            this.spotifyPageOffset = playlist.spotifyPageOffset;
        }
    }

    /**
     * get the name of the current playlist class
     * 
     * @return - playlist name
     */
    public String getName() {
        return this.name;
    }

    @JsonIgnore
    public String getM3U() {
        // m3u header
        StringBuilder output = new StringBuilder();
        output.append("#EXTM3U").append("\n");
        // m3u playlist information
        output.append("#PLAYLIST:").append(this.toString()).append("\n");
        // m3u playlist cover
        output.append("#EXTIMG:").append("cover.jpg").append("\n");
        // m3u playlist contents
        for (Song song : this.songs) {
            File file = new File(MusicTools.sanitizeFileName(song.getName()) + "." + downloadFormat);
            output.append("#EXTINF:").append(String.valueOf(song.getDuration().toSeconds())).append(",")
                    .append(song.toString()).append("\n");
            output.append(file.getPath()).append("\n");
        }
        return output.toString();
    }

    /**
     * set playlist cover art
     * 
     * @param coverImage - String of coverart URL
     */
    public void setCoverImage(String coverImage) {
        if (coverImage == null || coverImage.isEmpty()) {
            logger.debug(this.toString() + ": empty String coverimage provided");
            return;
        }
        try {
            this.coverImage = new URI(coverImage);
        } catch (URISyntaxException e) {
            logger.error("Error parsing playlist cover image: " + coverImage);
        }
    }

    public void setCoverImage(URI coverImage) {
        if (coverImage == null) {
            logger.debug(this.toString() + ": empty URI coverimage provided");
            return;
        }
        this.coverImage = coverImage;
    }

    /**
     * get coverart of current playlist class
     * 
     * @return - constructed URI
     */
    public URI getCoverImage() {
        return this.coverImage;
    }

    /**
     * add songs to playlist
     * 
     * @param songs - arraylist of constructed Song
     */
    public void addSongs(LinkedHashSet<Song> songs) {
        if (songs == null || songs.isEmpty()) {
            logger.debug(this.toString() + ": empty songs array provided in addSongs");
            return;
        }
        for (Song song : songs) {
            this.addSong(song);
        }
    }

    /**
     * add song to playlist
     * 
     * @param song - constructed Song
     */
    public void addSong(Song song) {
        if (song == null || song.isEmpty()) {
            logger.debug(this.toString() + ": empty song provided in addsong");
            return;
        }
        if (this.songs.contains(song)) {
            this.getSong(song).merge(song);
        } else {
            this.songs.add(song);
        }
    }

    public void removeSong(Song song) {
        if (song == null || song.isEmpty()) {
            logger.debug(this.toString() + ": empty song provided in removeSong");
            return;
        }
        this.songs.remove(song);
    }

    public Song getSong(Song song) {
        if (song == null || song.isEmpty()) {
            logger.debug(this.toString() + ": empty song provided in getSong");
            return null;
        }
        for (Song thisSong : this.songs) {
            if (thisSong.equals(song)) {
                return thisSong;
            }
        }
        return null;
    }

    /**
     * return size/numbers of songs in playlist
     * 
     * @return - integer of size of playlist
     */
    @JsonIgnore
    public int size() {
        return this.songs.size();
    }

    /**
     * get all playlist songs
     * 
     * @return - arraylist of constructed Song
     */
    public LinkedHashSet<Song> getSongs() {
        return this.songs;
    }

    /**
     * get youtube page token of youtube playlist
     * 
     * @return - String youtube page token
     */
    public String getYoutubePageToken() {
        return this.youtubePageToken;
    }

    /**
     * set youtube page token of youtube playlist
     * 
     * @param youtubePageToken - String youtube page token
     */
    public void setYoutubePageToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.debug(this.toString() + ": empty youtube page token provided");
            return;
        }
        this.youtubePageToken = token;
    }

    /**
     * get spotify offset for spotify playlist
     * 
     * @return - int spotify offset
     */
    public int getSpotifyPageOffset() {
        return this.spotifyPageOffset;
    }

    /**
     * set spotify offset for spotify playlist
     * 
     * @param spotifyPageOffset - int spotify offset
     */
    public void setSpotifyPageOffset(int offset) {
        if (offset < 0) {
            logger.debug(this.toString() + ": provided spotify offset is below 0");
            return;
        }
        this.spotifyPageOffset = offset;
    }

    @JsonIgnore
    public String getFolderName() {
        return MusicTools.sanitizeFileName(this.name);
    }

    @JsonIgnore
    public Duration getTotalDuration() {
        Duration totalDuration = Duration.ZERO;
        for (Song song : this.songs) {
            totalDuration = totalDuration.plus(song.getDuration());
        }
        return totalDuration;
    }

    @Override
    @JsonIgnore
    public String toString() {
        return this.name.toString().trim();
    }

    @Override
    @JsonIgnore
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Playlist)) {
            return false;
        }
        Playlist playlist = (Playlist) object;
        if (this.getFolderName().equalsIgnoreCase(playlist.getFolderName())) {
            return true;
        }
        if (Levenshtein.computeSimilarityCheck(this.toString(), playlist.toString(),
                simularityPercentage)) {
            return true;
        }
        return false;
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return Objects.hash(this.name.toLowerCase().trim());
    }

    @JsonIgnore
    public boolean isEmpty() {
        if (this.name.isEmpty()) {
            return true;
        }
        return this.songs.isEmpty();
    }

    public void clear() {
        this.songs.clear();
    }
}
