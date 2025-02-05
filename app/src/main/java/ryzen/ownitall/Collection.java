package ryzen.ownitall;

import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Progressbar;

public class Collection {
    private static final Logger logger = LogManager.getLogger(Collection.class);
    private static Collection instance;
    private LikedSongs likedSongs;
    private LinkedHashSet<Playlist> playlists;
    private LinkedHashSet<Album> albums;

    /**
     * default constructor initializing arrays
     */
    public Collection() {
        this.likedSongs = new LikedSongs();
        this.playlists = new LinkedHashSet<>();
        this.albums = new LinkedHashSet<>();
    }

    public static Collection load() {
        if (instance == null) {
            instance = new Collection();
            logger.debug("New instance created");
        }
        return instance;
    }

    public void save() {
        Sync.load().exportCollection(this);
    }

    public void clear() {
        this.likedSongs.clear();
        this.playlists.clear();
        this.albums.clear();
    }

    /**
     * merge array of albums into current collection
     * 
     * @param mergeAlbums - linkedhashset of albums to merge
     */
    public void addAlbums(LinkedHashSet<Album> mergeAlbums) {
        if (mergeAlbums == null || mergeAlbums.isEmpty()) {
            logger.debug("empty album array in addAlbums");
            return;
        }
        for (Album album : mergeAlbums) {
            this.addAlbum(album);
        }
    }

    public void addAlbum(Album album) {
        if (album == null) {
            logger.debug("null album provided in addAlbum");
            return;
        }
        Album foundAlbum = this.getAlbum(album);
        if (foundAlbum != null) {
            foundAlbum.merge(album);
        } else {
            this.albums.add(album);
        }
    }

    public void removeAlbum(Album album) {
        if (album == null) {
            logger.debug("null album provided in removeAlbum");
            return;
        }
        this.albums.remove(album);
    }

    /**
     * merge array of playlists into current collection
     * 
     * @param mergePlaylists - linkedhashset of playlists to merge
     */
    public void addPlaylists(LinkedHashSet<Playlist> mergePlaylists) {
        if (mergePlaylists == null || mergePlaylists.isEmpty()) {
            logger.info("empty playlist array passed in addPlaylists");
            return;
        }
        for (Playlist playlist : mergePlaylists) {
            this.addPlaylist(playlist);
        }
    }

    public void addPlaylist(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in addPlaylist");
            return;
        }
        Playlist foundPlaylist = this.getPlaylist(playlist);
        if (foundPlaylist != null) {
            foundPlaylist.merge(playlist);
        } else {
            this.playlists.add(playlist);
        }
    }

    public void removePlaylist(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in removePlaylist");
            return;
        }
        this.playlists.remove(playlist);
    }

    /**
     * merge liked songs into current collection
     * 
     * @param mergeLikedSongs - constructed LikedSongs
     */
    public void addLikedSongs(LinkedHashSet<Song> songs) {
        if (songs == null || songs.isEmpty()) {
            return;
        }
        this.likedSongs.addSongs(songs); // handled by playlist addSongs
    }

    public void addLikedSong(Song song) {
        if (song == null) {
            logger.debug("null song provided in addLikedSong");
            return;
        }
        this.likedSongs.addSong(song);
    }

    public void removeLikedSong(Song song) {
        if (song == null) {
            logger.debug("null song provided in removeLikedSong");
            return;
        }
        this.likedSongs.removeSong(song);
    }

    /**
     * merge a collection into the current collection
     * orchestrates the merge albums, playlists and liked songs
     * 
     * @param collection - collection to get values to merge into this collection
     */
    public void mergeCollection(Collection collection) {
        logger.info("Updating Music Collection");
        ProgressBar pb = Progressbar.progressBar("Update Collection", 3);
        pb.setExtraMessage("Albums");
        this.addAlbums(collection.getAlbums());
        pb.setExtraMessage("Playlists").step();
        this.addPlaylists(collection.getPlaylists());
        pb.setExtraMessage("Liked Songs").step();
        this.addLikedSongs(collection.getLikedSongs().getSongs());
        pb.setExtraMessage("Done").step();
        pb.close();
    }

    /**
     * get this collections likedsongs
     * 
     * @return - constructed LikedSongs
     */
    public LikedSongs getLikedSongs() {
        return this.likedSongs;
    }

    /**
     * function to get standalone liked songs (not in any albums or playlists)
     * 
     * @return - linkedhashset of standalone liked songs
     */
    public LinkedHashSet<Song> getStandaloneLikedSongs() {
        if (this.likedSongs.size() == 0) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<Song> allTracks = new LinkedHashSet<>();
        LinkedHashSet<Song> likedSongs = new LinkedHashSet<>(this.likedSongs.getSongs());
        for (Playlist playlist : this.playlists) {
            allTracks.addAll(playlist.getSongs());
        }
        for (Album album : this.albums) {
            allTracks.addAll(album.getSongs());
        }
        likedSongs.removeAll(allTracks);
        return likedSongs;
    }

    public boolean isLiked(Song song) {
        if (song == null) {
            logger.debug("null song provided in isLiked");
            return false;
        }
        return this.likedSongs.contains(song);
    }

    /**
     * get this collections albums
     * 
     * @return - linkedhashset of albums
     */
    public LinkedHashSet<Album> getAlbums() {
        return new LinkedHashSet<>(this.albums);
    }

    /**
     * get specific album in passed array
     * 
     * @param albums - array to search from
     * @param album  - album to find
     * @return - found album or null
     */
    public static Album getAlbum(LinkedHashSet<Album> albums, Album album) {
        if (albums.contains(album)) {
            for (Album thisAlbum : albums) {
                if (thisAlbum.equals(album)) {
                    return thisAlbum;
                }
            }
        }
        return null;
    }

    public Album getAlbum(Album album) {
        if (this.albums.contains(album)) {
            for (Album thisAlbum : this.albums) {
                if (thisAlbum.equals(album)) {
                    return thisAlbum;
                }
            }
        }
        return null;
    }

    /**
     * get this collections playlists
     * 
     * @return - linkedhashset of playlists
     */
    public LinkedHashSet<Playlist> getPlaylists() {
        return new LinkedHashSet<>(this.playlists);
    }

    /**
     * get specific playlist in array of passed playlists
     * 
     * @param playlists - array of playlists
     * @param playlist  - playlist to find in array of playlists
     * @return - found playlist or null
     */
    public static Playlist getPlaylist(LinkedHashSet<Playlist> playlists, Playlist playlist) {
        if (playlists.contains(playlist)) {
            for (Playlist thisPlaylist : playlists) {
                if (thisPlaylist.equals(playlist)) {
                    return thisPlaylist;
                }
            }
        }
        return null;
    }

    public Playlist getPlaylist(Playlist playlist) {
        if (this.playlists.contains(playlist)) {
            for (Playlist thisPlaylist : this.playlists) {
                if (thisPlaylist.equals(playlist)) {
                    return thisPlaylist;
                }
            }
        }
        return null;
    }

    public static Song getSong(LinkedHashSet<Song> songs, Song song) {
        if (songs.contains(song)) {
            for (Song thisSong : songs) {
                if (thisSong.equals(song)) {
                    return thisSong;
                }
            }
        }
        return null;
    }

    public static Artist getArtist(LinkedHashSet<Artist> artists, Artist artist) {
        if (artists.contains(artist)) {
            for (Artist thisArtist : artists) {
                if (thisArtist.equals(artist)) {
                    return thisArtist;
                }
            }
        }
        return null;
    }
}
