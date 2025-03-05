package ryzen.ownitall;

import java.io.File;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.classes.Album;
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

    /**
     * initialize collection instance
     * 
     * @return - existing or new collection instance
     */
    public static Collection load() {
        if (instance == null) {
            instance = new Collection();
            logger.debug("New instance created");
        }
        return instance;
    }

    /**
     * save all data from collection
     */
    public void save() {
        Sync.load().exportCollection();
    }

    /**
     * clear current collection
     */
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

    /**
     * add album to collection
     * merges if one is already existing (see contains() and equals())
     * 
     * @param album - constructed album to merge
     */
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

    /**
     * remove album from collection
     * 
     * @param album - album to remove
     */
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
            logger.debug("empty playlist array passed in addPlaylists");
            return;
        }
        for (Playlist playlist : mergePlaylists) {
            this.addPlaylist(playlist);
        }
    }

    /**
     * add playlist to collection
     * merges if one already exists (see contains() and equals())
     * 
     * @param playlist - constructed playlist to add
     */
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

    /**
     * removes playlist from collection
     * 
     * @param playlist - constructed playlist to remove
     */
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
     * @param songs - array of constructed Song
     */
    public void addLikedSongs(LinkedHashSet<Song> songs) {
        if (songs == null || songs.isEmpty()) {
            return;
        }
        this.likedSongs.addSongs(songs); // handled by playlist addSongs
    }

    /**
     * add liked song to collection
     * 
     * @param song - constructed song to add
     */
    public void addLikedSong(Song song) {
        if (song == null) {
            logger.debug("null song provided in addLikedSong");
            return;
        }
        this.likedSongs.addSong(song);
    }

    /**
     * remove liked song from collection
     * 
     * @param song - constructed song to remove
     */
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
        LinkedHashSet<Song> likedSongs = new LinkedHashSet<>(this.likedSongs.getSongs());
        for (Playlist playlist : this.playlists) {
            likedSongs.removeAll(playlist.getSongs());
        }
        for (Album album : this.albums) {
            likedSongs.removeAll(album.getSongs());
        }
        return likedSongs;
    }

    public LinkedHashSet<Song> getStandalonePlaylistSongs(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist passed in getStandalonePlaylistSongs");
            return new LinkedHashSet<>();
        }
        LinkedHashSet<Song> songs = new LinkedHashSet<>(playlist.getSongs());
        for (Album album : this.albums) {
            songs.removeAll(album.getSongs());
        }
        return songs;
    }

    /**
     * check if song is already in an album, and if so return the album folder
     * 
     * @param song - song to check
     * @return - album folder
     */
    public Album getSongAlbum(Song song) {
        if (song == null) {
            logger.debug("null song provided in checkAlbumSongs");
            return null;
        }
        for (Album album : this.getAlbums()) {
            if (album.contains(song)) {
                return album;
            }
        }
        return null;
    }

    /**
     * check if song is liked (in likedSongs)
     * 
     * @param song - constructed song to check if liked
     * @return - true if liked, false if not
     */
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

    /**
     * get album from collection
     * 
     * @param album - constructed album
     * @return - found constructed album or null
     */
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

    /**
     * get playlist from collection
     * 
     * @param playlist - constructed playlist to find
     * @return - found playlist or null
     */
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

    /**
     * get specific song from linkedhashset
     * 
     * @param songs - linkedhashset of all songs
     * @param song  - specific constructed song to find
     * @return - found constructed song
     */
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

    public String getPlaylistM3U(Playlist playlist) {
        // all relational, doesnt use downloadpath
        StringBuilder output = new StringBuilder();
        output.append(playlist.getM3UHeader());
        for (Song song : playlist.getSongs()) {
            File songFile;
            Album foundAlbum = this.getSongAlbum(song);
            if (foundAlbum == null) {
                songFile = new File(song.getFileName());
            } else {
                songFile = new File(foundAlbum.getFolderName(), song.getFileName());
            }
            output.append("#EXTINF:").append(String.valueOf(song.getDuration().toSeconds())).append(",")
                    .append(song.toString()).append("\n");
            output.append(songFile.getPath()).append("\n");
        }
        return output.toString();
    }

    public int getPlaylistsTrackCount() {
        int trackCount = 0;
        for (Playlist playlist : this.playlists) {
            trackCount += playlist.size();
        }
        return trackCount;
    }

    public int getAlbumsTrackCount() {
        int trackCount = 0;
        for (Album album : this.albums) {
            trackCount += album.size();
        }
        return trackCount;
    }

    public int getTotalTrackCount() {
        int trackCount = 0;
        trackCount += this.getStandaloneLikedSongs().size();
        trackCount += this.getPlaylistsTrackCount();
        trackCount += this.getAlbumsTrackCount();
        return trackCount;
    }

    public int getPlaylistCount() {
        return this.playlists.size();
    }

    public int getAlbumCount() {
        return this.albums.size();
    }
}
