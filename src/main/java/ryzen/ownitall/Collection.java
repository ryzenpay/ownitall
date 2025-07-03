package ryzen.ownitall;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.MusicTools;

/**
 * <p>
 * Collection class.
 * </p>
 *
 * @author ryzen
 */
public class Collection {
    private static final Logger logger = new Logger(Collection.class);
    private static LikedSongs likedSongs = new LikedSongs();
    private static ArrayList<Playlist> playlists = new ArrayList<>();
    private static ArrayList<Album> albums = new ArrayList<>();

    /**
     * save all data from collection
     */
    public static void save() {
        new Storage().exportCollection();
    }

    /**
     * clear current collection
     */
    public static void clear() {
        clearLikedSongs();
        clearPlaylists();
        clearAlbums();
    }

    /**
     * <p>
     * clearLikedSongs.
     * </p>
     */
    public static void clearLikedSongs() {
        likedSongs.getSongs().clear();
    }

    /**
     * <p>
     * clearPlaylists.
     * </p>
     */
    public static void clearPlaylists() {
        playlists.clear();
    }

    /**
     * <p>
     * clearAlbums.
     * </p>
     */
    public static void clearAlbums() {
        albums.clear();
    }

    /**
     * merge liked songs into current collection
     *
     * @param fromLikedSongs a {@link ryzen.ownitall.classes.LikedSongs} object
     */
    public static void addLikedSongs(LikedSongs fromLikedSongs) {
        if (fromLikedSongs == null) {
            logger.debug("null liked songs passed in addLikedSongs");
            return;
        }
        likedSongs.addSongs(fromLikedSongs.getSongs()); // handled by playlist addSongs
    }

    /**
     * add liked song to collection
     *
     * @param song - constructed song to add
     */
    public static void addLikedSong(Song song) {
        if (song == null) {
            logger.debug("null song provided in addLikedSong");
            return;
        }
        likedSongs.addSong(song);
    }

    /**
     * remove liked song from collection
     *
     * @param song - constructed song to remove
     */
    public static void removeLikedSong(Song song) {
        if (song == null) {
            logger.debug("null song provided in removeLikedSong");
            return;
        }
        likedSongs.removeSong(song);
    }

    /**
     * get this collections likedsongs
     *
     * @return - constructed LikedSongs
     */
    public static LikedSongs getLikedSongs() {
        return likedSongs;
    }

    /**
     * <p>
     * getLikedSong.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     */
    public static Song getLikedSong(int hashCode) {
        for (Song song : likedSongs.getSongs()) {
            if (song.hashCode() == hashCode) {
                return song;
            }
        }
        return null;
    }

    /**
     * function to get standalone liked songs (not in any albums or playlists)
     *
     * @return - linkedhashset of standalone liked songs
     */
    public static ArrayList<Song> getStandaloneLikedSongs() {
        if (likedSongs.isEmpty()) {
            return new ArrayList<>();
        }
        LikedSongs tmplikedSongs = new LikedSongs();
        tmplikedSongs.addSongs(likedSongs.getSongs());
        for (Playlist playlist : playlists) {
            tmplikedSongs.removeSongs(playlist.getSongs());
        }
        for (Album album : albums) {
            tmplikedSongs.removeSongs(album.getSongs());
        }
        return tmplikedSongs.getSongs();
    }

    /**
     * check if song is liked (in likedSongs)
     *
     * @param song - constructed song to check if liked
     * @return - true if liked, false if not
     */
    public static boolean isLiked(Song song) {
        if (song == null) {
            logger.debug("null song provided in isLiked");
            return false;
        }
        if (likedSongs.contains(song)) {
            return true;
        }
        return false;
    }

    /**
     * merge array of albums into current collection
     *
     * @param albums - linkedhashset of albums to merge
     */
    public static void addAlbums(ArrayList<Album> albums) {
        if (albums == null) {
            logger.debug("null album array in addAlbums");
            return;
        }
        for (Album album : albums) {
            addAlbum(album);
        }
    }

    /**
     * add album to collection
     * merges if one is already existing (see contains() and equals())
     *
     * @param album - constructed album to merge
     */
    public static void addAlbum(Album album) {
        if (album == null) {
            logger.debug("null album provided in addAlbum");
            return;
        }
        Album foundAlbum = getAlbum(album);
        if (foundAlbum != null) {
            foundAlbum.merge(album);
        } else {
            albums.add(album);
        }
    }

    /**
     * remove album from collection
     *
     * @param album - album to remove
     */
    public static void removeAlbum(Album album) {
        if (album == null) {
            logger.debug("null album provided in removeAlbum");
            return;
        }
        albums.remove(album);
    }

    /**
     * get this collections albums
     *
     * @return - linkedhashset of albums
     */
    public static ArrayList<Album> getAlbums() {
        return albums;
    }

    /**
     * get album from collection
     *
     * @param album - constructed album
     * @return - found constructed album or null
     */
    public static Album getAlbum(Album album) {
        if (album == null) {
            logger.debug("null album provided in getAlbum");
            return null;
        }
        for (Album thisAlbum : albums) {
            if (thisAlbum.equals(album)) {
                return thisAlbum;
            }
        }
        return null;
    }

    /**
     * <p>
     * getAlbum.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Album} object
     */
    public static Album getAlbum(int hashCode) {
        for (Album album : albums) {
            if (album.hashCode() == hashCode) {
                return album;
            }
        }
        return null;
    }

    public static Album getAlbum(String name) {
        if (name == null) {
            logger.debug("null albumname provided in getAlbum");
            return null;
        }
        for (Album album : albums) {
            if (album.getName().equalsIgnoreCase(name)) {
                return album;
            }
        }
        return null;
    }

    /**
     * get an albums nfo String to write to a file
     *
     * @param album - album to get songs from
     * @return - string of an xml nfo file to write to a file
     */
    public static String getAlbumNFO(Album album) {
        if (album == null) {
            logger.debug("null album provided in getAlbumNFO");
            return null;
        }
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("album");
            doc.appendChild(rootElement);

            // Title
            Element title = doc.createElement("title");
            title.appendChild(doc.createTextNode(album.getName()));
            rootElement.appendChild(title);

            // Artists
            Element artistsElement = doc.createElement("artists");
            rootElement.appendChild(artistsElement);
            for (Artist artist : album.getArtists()) {
                Element artistElement = doc.createElement("artist");
                artistElement.appendChild(doc.createTextNode(artist.getName()));
                artistsElement.appendChild(artistElement);
            }

            // Songs
            Element tracksElement = doc.createElement("tracks");
            rootElement.appendChild(tracksElement);
            for (Song song : album.getSongs()) {
                Element trackElement = doc.createElement("track");

                Element trackTitle = doc.createElement("title");
                trackTitle.appendChild(doc.createTextNode(song.getName()));
                trackElement.appendChild(trackTitle);

                Element trackDuration = doc.createElement("duration");
                trackDuration.appendChild(doc.createTextNode(String.valueOf(song.getDuration())));
                trackElement.appendChild(trackDuration);

                tracksElement.appendChild(trackElement);
            }

            // Cover image
            if (album.getCoverImage() != null) {
                Element thumb = doc.createElement("thumb");
                thumb.appendChild(doc.createTextNode(getCollectionCoverFileName(album)));
                rootElement.appendChild(thumb);
            }

            // Transform the DOM to XML string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);

            return writer.toString();

        } catch (Exception e) {
            logger.error("exception generating NFO content", e);
            return null;
        }
    }

    // filter out remixes?
    // TODO: transform feat. into artist addition?
    public static void cleanAlbums() {
        for (Song song : getAllSongs()) {
            Album album = getSongAlbum(song);
            if (album != null) {
                album.addSong(song);
            }
        }
        ArrayList<Album> removeAlbums = new ArrayList<>();
        for (Album album : albums) {
            // filter out deluxes / exclusives
            if (album.getName().toLowerCase().contains("deluxe")
                    || album.getName().toLowerCase().contains("extended")) {
                Album originalAlbum = getAlbum(MusicTools.removeBrackets(album.getName()));
                if (originalAlbum != null) {
                    album.merge(originalAlbum);
                    removeAlbums.add(originalAlbum);
                    logger.info("Combined album '" + album.toString() + "' into '" + originalAlbum.toString() + "'");
                }
            }
        }
        for (Album album : removeAlbums) {
            removeAlbum(album);
        }
    }

    public static Album getSongAlbum(Song song) {
        if (song == null) {
            logger.debug("null song provided in getSongAlbum");
            return null;
        }
        if (song.getAlbumName() != null) {
            Album album = getAlbum(song.getAlbumName());
            if (album != null) {
                return album;
            }
        }
        for (Album album : albums) {
            if (album.contains(song)) {
                return album;
            }
        }
        return null;
    }

    /**
     * merge array of playlists into current collection
     *
     * @param playlists - linkedhashset of playlists to merge
     */
    public static void addPlaylists(ArrayList<Playlist> playlists) {
        if (playlists == null) {
            logger.debug("null playlist array passed in addPlaylists");
            return;
        }
        for (Playlist playlist : playlists) {
            addPlaylist(playlist);
        }
    }

    /**
     * add playlist to collection
     * merges if one already exists (see contains() and equals())
     *
     * @param playlist - constructed playlist to add
     */
    public static void addPlaylist(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in addPlaylist");
            return;
        }
        Playlist foundPlaylist = getPlaylist(playlist);
        if (foundPlaylist != null) {
            foundPlaylist.merge(playlist);
        } else {
            playlists.add(playlist);
        }
    }

    /**
     * removes playlist from collection
     *
     * @param playlist - constructed playlist to remove
     */
    public static void removePlaylist(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in removePlaylist");
            return;
        }
        playlists.remove(playlist);
    }

    /**
     * get this collections playlists
     *
     * @return - linkedhashset of playlists
     */
    public static ArrayList<Playlist> getPlaylists() {
        return playlists;
    }

    /**
     * get playlist from collection
     *
     * @param playlist - constructed playlist to find
     * @return - found playlist or null
     */
    public static Playlist getPlaylist(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in getPlaylist");
            return null;
        }
        for (Playlist thisPlaylist : playlists) {
            if (thisPlaylist.equals(playlist)) {
                return thisPlaylist;
            }
        }
        return null;
    }

    /**
     * <p>
     * getPlaylist.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Playlist} object
     */
    public static Playlist getPlaylist(int hashCode) {
        for (Playlist playlist : playlists) {
            if (playlist.hashCode() == hashCode) {
                return playlist;
            }
        }
        return null;
    }

    /**
     * get a playlists m3u String to write to a file
     * this gets all the paths for songs in albums
     *
     * @param playlist - playlist to get songs from
     * @return - string in m3u format
     */
    public static String getPlaylistM3U(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in getPlaylistM3u");
            return null;
        }
        // all relational, doesnt use downloadpath
        StringBuilder output = new StringBuilder();
        output.append("#EXTM3U").append("\n");
        // m3u playlist information
        output.append("#PLAYLIST:").append(playlist.toString()).append("\n");
        // m3u playlist cover
        if (playlist.getCoverImage() != null) {
            output.append("#EXTIMG:").append(getCollectionCoverFileName(playlist)).append("\n");
        }
        for (Song song : playlist.getSongs()) {
            output.append("#EXTINF:").append(String.valueOf(song.getDuration().toSeconds())).append(",")
                    .append(song.toString()).append("\n");
            output.append(getRelativeSongPath(song)).append("\n");
        }
        return output.toString();
    }

    /**
     * get arraylist of playlists songs which are not in albums
     *
     * @param playlist - playlist to get songs from
     * @return - arraylist of songs only in that playlist
     */
    public static ArrayList<Song> getStandalonePlaylistSongs(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist passed in getStandalonePlaylistSongs");
            return null;
        }
        Playlist tmpPlaylist = new Playlist("");
        tmpPlaylist.addSongs(playlist.getSongs());
        for (Album album : albums) {
            tmpPlaylist.removeSongs(album.getSongs());
        }
        return tmpPlaylist.getSongs();
    }

    /**
     * get the first playlist the song is part of
     *
     * @param song - song to check
     * @return - constructed playlist
     */
    public static Playlist getSongPlaylist(Song song) {
        if (song == null) {
            logger.debug("null song provided in getSongPlaylist");
            return null;
        }
        for (Playlist playlist : getPlaylists()) {
            if (playlist.contains(song)) {
                return playlist;
            }
        }
        return null;
    }

    /**
     * get total track count in playlists
     *
     * @return - int of playlist track count
     */
    public static int getPlaylistsTrackCount() {
        int trackCount = 0;
        for (Playlist playlist : playlists) {
            trackCount += playlist.size();
        }
        return trackCount;
    }

    /**
     * get total track count in albums
     *
     * @return - int of album track count
     */
    public static int getAlbumsTrackCount() {
        int trackCount = 0;
        for (Album album : albums) {
            trackCount += album.size();
        }
        return trackCount;
    }

    /**
     * get total track count with no duplicates
     *
     * @return - int of total track count
     */
    public static int getTotalSongCount() {
        int trackCount = 0;
        trackCount += getStandaloneLikedSongs().size();
        for (Playlist playlist : playlists) {
            ArrayList<Song> songs = getStandalonePlaylistSongs(playlist);
            trackCount += songs.size();
        }
        trackCount += getAlbumsTrackCount();
        return trackCount;
    }

    public static ArrayList<Song> getAllSongs() {
        ArrayList<Song> songs = new ArrayList<>();
        songs.addAll(getStandaloneLikedSongs());
        for (Playlist playlist : playlists) {
            songs.addAll(getStandalonePlaylistSongs(playlist));
        }
        for (Album album : albums) {
            songs.addAll(album.getSongs());
        }
        return songs;
    }

    /**
     * get count of playlists in collection
     *
     * @return - int of playlists in collection
     */
    public static int getPlaylistCount() {
        return playlists.size();
    }

    /**
     * get count of albums in collection
     *
     * @return - int of playlists in collection
     */
    public static int getAlbumCount() {
        return albums.size();
    }

    /**
     * <p>
     * getLikedSongCount.
     * </p>
     *
     * @return a int
     */
    public static int getLikedSongCount() {
        return likedSongs.size();
    }

    public static File getRelativeSongPath(Song song) {
        if (song == null) {
            logger.debug("null song provided in getRelativeSongPath");
            return null;
        }
        String extension = "." + Settings.downloadFormat;
        if (song.getAlbumName() != null) {
            return new File(MusicTools.sanitizeFileName(song.getAlbumName()),
                    MusicTools.sanitizeFileName(song.getName()) + extension);
        } else {
            return new File(MusicTools.sanitizeFileName(song.getName()) + extension);
        }
    }

    /**
     * <p>
     * getCollectionCoverFileName.
     * </p>
     *
     * @param collection a {@link ryzen.ownitall.classes.Playlist} object
     * @return a {@link java.lang.String} object
     */
    public static String getCollectionCoverFileName(Playlist collection) {
        if (collection == null) {
            logger.debug("null collection provided in getCollectionCoverFileName");
            return null;
        }
        return MusicTools.sanitizeFileName(collection.getName()) + ".png";
    }
}
