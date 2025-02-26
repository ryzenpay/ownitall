package ryzen.ownitall.classes;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.LinkedHashMap;
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
    private String name;
    private URI coverImage;
    private LinkedHashSet<Song> songs;
    private LinkedHashMap<String, String> ids;

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
        this.ids = new LinkedHashMap<>();
    }

    /**
     * full playlist contructor
     * 
     * @param name              - playlist name
     * @param songs             - linkedhashset of song
     * @param ids               - linkedhashmap of id's
     * @param youtubePageToken  - string youtube page token
     * @param spotifyPageOffset - int spotify page token
     * @param coverImage        - string playlist coverImage
     */
    @JsonCreator
    public Playlist(@JsonProperty("name") String name,
            @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("ids") LinkedHashMap<String, String> ids,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverImage") String coverImage) {
        this.name = name;
        this.songs = new LinkedHashSet<>();
        this.ids = new LinkedHashMap<>();
        if (songs != null) {
            this.addSongs(songs);
        }
        if (ids != null) {
            this.addIds(ids);
        }
        if (youtubePageToken != null) {
            this.setYoutubePageToken(youtubePageToken);
        }
        this.setSpotifyPageOffset(spotifyPageOffset);
        if (coverImage != null) {
            this.setCoverImage(coverImage);
        }
    }

    /**
     * merge playlist into current playlist
     * used when existing playlist found and want to add new
     * 
     * @param playlist - constructed playlist to merge into this
     */
    public void merge(Playlist playlist) {
        if (playlist == null) {
            logger.debug(this.toString() + ": null playlist provided in merge");
            return;
        }
        this.addSongs(playlist.getSongs());
        if (this.getCoverImage() == null && playlist.getCoverImage() != null) {
            this.setCoverImage(playlist.getCoverImage());
        }
        if (playlist.getYoutubePageToken() != null) {
            this.setYoutubePageToken(playlist.getYoutubePageToken());
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

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            logger.debug(this.toString() + ": null or empty collection name passed to setName");
        }
        this.name = name;
    }

    /**
     * get m3u data to write to m3u file for playlist
     * 
     * @return - string data
     */
    @JsonIgnore
    public String getM3UHeader() {
        // m3u header
        StringBuilder output = new StringBuilder();
        output.append("#EXTM3U").append("\n");
        // m3u playlist information
        output.append("#PLAYLIST:").append(this.toString()).append("\n");
        // m3u playlist cover
        if (this.coverImage != null) {
            output.append("#EXTIMG:").append(this.getFolderName() + ".png").append("\n");
        }
        return output.toString();
    }

    /**
     * set playlist cover art
     * 
     * @param coverImage - String of coverImage URL
     */
    public void setCoverImage(String coverImage) {
        if (coverImage == null || coverImage.isEmpty()) {
            logger.debug(this.toString() + ": empty String coverimage provided");
            return;
        }
        try {
            this.coverImage = new URI(coverImage);
        } catch (URISyntaxException e) {
            logger.error(this.toString() + ": exception parsing playlist cover image: " + coverImage);
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
     * get coverimage of current playlist class
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
        this.songs.addAll(songs);
        // dont use .addAll because of Album override
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
        if (song == null) {
            logger.debug(this.toString() + ": null song provided in addsong");
            return;
        }
        this.songs.add(song);
    }

    /**
     * remove song from playlist
     * 
     * @param song - song to remove
     */
    public void removeSong(Song song) {
        if (song == null) {
            logger.debug(this.toString() + ": null song provided in removeSong");
            return;
        }
        this.songs.remove(song);
    }

    /**
     * get song in playlist
     * 
     * @param song - song to find
     * @return - constructed found song or null
     */
    public Song getSong(Song song) {
        if (song == null) {
            logger.debug(this.toString() + ": null song provided in getSong");
            return null;
        }
        if (!this.songs.contains(song)) {
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

    @JsonIgnore
    public boolean contains(Song song) {
        if (song == null) {
            logger.debug(this.toString() + ": null song provided in contains");
            return false;
        }
        return this.songs.contains(song);
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
     * @param token - String youtube page token
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
     * @param offset - int spotify offset
     */
    public void setSpotifyPageOffset(int offset) {
        if (offset < 0) {
            logger.debug(this.toString() + ": provided spotify offset is below 0, defaulting to 0");
            offset = 0;
        }
        this.spotifyPageOffset = offset;
    }

    /**
     * get playlist folder name
     * respects UTF-8
     * 
     * @return - string UTF-8 foldername
     */
    @JsonIgnore
    public String getFolderName() {
        return MusicTools.sanitizeFileName(this.getName());
    }

    /**
     * get total playlist duration
     * 
     * @return - total Duration
     */
    @JsonIgnore
    public Duration getTotalDuration() {
        Duration totalDuration = Duration.ZERO;
        for (Song song : this.songs) {
            totalDuration = totalDuration.plus(song.getDuration());
        }
        return totalDuration;
    }

    /**
     * add id to playlist id's
     * 
     * @param key - key to add (spotify, youtube, ...)
     * @param id  - id to add
     */
    public void addId(String key, String id) {
        if (key == null || id == null || key.isEmpty() || id.isEmpty()) {
            logger.debug(this.toString() + ": empty key or url in addId");
            return;
        }
        this.ids.put(key, id);
    }

    /**
     * add multiple id's to playlist
     * 
     * @param ids - linkedhashmap of id's to add
     */
    public void addIds(LinkedHashMap<String, String> ids) {
        if (ids == null) {
            logger.debug(this.toString() + ": null ids array provided in addIds");
            return;
        }
        this.ids.putAll(ids);
    }

    /**
     * get id from playlist id's
     * 
     * @param key - key of id to return
     * @return - string id
     */
    @JsonIgnore
    public String getId(String key) {
        if (key == null || key.isEmpty()) {
            logger.debug(this.toString() + ": empty key provided in getId");
            return null;
        }
        return this.ids.get(key);
    }

    /**
     * get all playlist id's
     * 
     * @return - linkedhashmap of id's
     */
    public LinkedHashMap<String, String> getIds() {
        return this.ids;
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
}
