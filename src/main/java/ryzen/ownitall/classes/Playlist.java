package ryzen.ownitall.classes;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import ryzen.ownitall.util.Logger;

/**
 * <p>
 * Playlist class.
 * </p>
 *
 * @author ryzen
 */
@Entity
@Table(name = "Playlist")
public class Playlist {
    private static final Logger logger = new Logger(Playlist.class);
    private String name;
    private URI coverImage;

    @OneToMany
    private ArrayList<Song> songs;
    @OneToMany
    private LinkedHashSet<Id> ids;

    /**
     * Default playlist constructor
     *
     * @param name - name of the playlist
     */
    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
        this.ids = new LinkedHashSet<>();
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
        this.addIds(playlist.getIds());
        if (this.getCoverImage() == null && playlist.getCoverImage() != null) {
            this.setCoverImage(playlist.getCoverImage());
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

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     */
    public void setName(String name) {
        if (name == null) {
            logger.debug(this.toString() + ": null playlist name passed to setName");
            return;
        }
        this.name = name;
    }

    /**
     * set playlist cover art
     *
     * @param coverImage - String of coverImage URL
     */
    public void setCoverImage(String coverImage) {
        if (coverImage == null) {
            logger.debug(this.toString() + ": null String coverimage provided");
            return;
        }
        try {
            this.coverImage = new URI(coverImage);
        } catch (URISyntaxException e) {
            logger.error(this.toString() + ": exception parsing playlist cover image: '" + coverImage + "'", e);
        }
    }

    /**
     * set playlist coverimage (URI)
     *
     * @param coverImage - constructed URI to set as coverimage
     */
    public void setCoverImage(URI coverImage) {
        if (coverImage == null) {
            logger.debug(this.toString() + ": null URI coverimage provided");
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
    public void addSongs(ArrayList<Song> songs) {
        if (songs == null) {
            logger.debug(this.toString() + ": null songs array provided in addSongs");
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
        if (song == null) {
            logger.debug(this.toString() + ": null song provided in addsong");
            return;
        }
        Song foundSong = this.getSong(song);
        if (foundSong != null) {
            foundSong.merge(song);
        } else {
            this.songs.add(song);
        }
    }

    /**
     * remove arraylist of songs
     * orchestrator for removeSong(Song song)
     *
     * @param songs - arraylist of songs to remove
     */
    public void removeSongs(ArrayList<Song> songs) {
        if (songs == null) {
            logger.debug(this.toString() + ": null songs provided in removesongs");
            return;
        }
        for (Song song : songs) {
            this.removeSong(song);
        }
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
        for (Song thisSong : this.songs) {
            if (thisSong.equals(song)) {
                return thisSong;
            }
        }
        return null;
    }

    public Song getSong(int songHash) {
        for (Song song : this.getSongs()) {
            if (song.hashCode() == songHash) {
                return song;
            }
        }
        return null;
    }

    /**
     * return size/numbers of songs in playlist
     *
     * @return - integer of size of playlist
     */
    public int size() {
        return this.songs.size();
    }

    /**
     * check if playlist has no songs
     *
     * @return - true if empty
     */
    public boolean isEmpty() {
        return this.songs.isEmpty();
    }

    /**
     * check if playlist contains a song
     *
     * @param song - song to check
     * @return - true if song is contained
     */
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
    public ArrayList<Song> getSongs() {
        return this.songs;
    }

    /**
     * get total playlist duration
     *
     * @return - total Duration
     */
    public Duration getTotalDuration() {
        Duration totalDuration = Duration.ZERO;
        for (Song song : this.songs) {
            totalDuration = totalDuration.plus(song.getDuration());
        }
        return totalDuration;
    }

    /**
     * add multiple id's to playlist
     *
     * @param ids - linkedhashmap of id's to add
     */
    public void addIds(LinkedHashSet<Id> ids) {
        if (ids == null) {
            logger.debug(this.toString() + ": null ids array provided in addIds");
            return;
        }
        this.ids.addAll(ids);
    }

    public void addId(String key, String value) {
        this.addId(new Id(key, value));
    }

    /**
     * add id to playlist id's
     *
     * @param key - key to add (spotify, youtube, ...)
     * @param id  - id to add
     */
    public void addId(Id id) {
        if (id == null || id.isEmpty()) {
            logger.debug(this.toString() + ": empty id in addId");
            return;
        }
        this.ids.add(id);
    }

    /**
     * get id from playlist id's
     *
     * @param key - key of id to return
     * @return - string id
     */
    public Id getId(String key) {
        if (key == null || key.isEmpty()) {
            logger.debug(this.toString() + ": empty key provided in getId");
            return null;
        }
        for (Id id : this.ids) {
            if (id.getKey().equals(key)) {
                return id;
            }
        }
        return null;
    }

    /**
     * get all playlist id's
     *
     * @return - linkedhashmap of id's
     */
    public LinkedHashSet<Id> getIds() {
        return this.ids;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.name.toString().trim();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Playlist)) {
            return false;
        }
        Playlist playlist = (Playlist) object;
        if (this.toString().equalsIgnoreCase(playlist.toString())) {
            return true;
        }
        return false;
    }
}
