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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;

public class Collection {
    private static final Logger logger = LogManager.getLogger(Collection.class);
    private static final Settings settings = Settings.load();
    private static Collection instance;
    private LikedSongs likedSongs;
    private ArrayList<Playlist> playlists;
    private ArrayList<Album> albums;

    /**
     * default constructor initializing arrays
     */
    public Collection() {
        this.likedSongs = new LikedSongs();
        this.playlists = new ArrayList<>();
        this.albums = new ArrayList<>();
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
        Storage.load().exportCollection();
    }

    /**
     * clear current collection
     */
    public void clear() {
        this.clearLikedSongs();
        this.clearPlaylists();
        this.clearAlbums();
    }

    public void clearLikedSongs() {
        this.likedSongs.getSongs().clear();
    }

    public void clearPlaylists() {
        this.playlists.clear();
    }

    public void clearAlbums() {
        this.albums.clear();
    }

    /**
     * merge array of albums into current collection
     * 
     * @param albums - linkedhashset of albums to merge
     */
    public void addAlbums(ArrayList<Album> albums) {
        if (albums == null) {
            logger.debug("null album array in addAlbums");
            return;
        }
        for (Album album : albums) {
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
     * @param playlists - linkedhashset of playlists to merge
     */
    public void addPlaylists(ArrayList<Playlist> playlists) {
        if (playlists == null) {
            logger.debug("null playlist array passed in addPlaylists");
            return;
        }
        for (Playlist playlist : playlists) {
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
     * @param likedSongs - array of constructed Song
     */
    public void addLikedSongs(LikedSongs likedSongs) {
        if (likedSongs == null) {
            logger.debug("null liked songs passed in addLikedSongs");
            return;
        }
        this.likedSongs.addSongs(likedSongs.getSongs()); // handled by playlist addSongs
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
    public ArrayList<Song> getStandaloneLikedSongs() {
        if (this.likedSongs.isEmpty()) {
            return new ArrayList<>();
        }
        LikedSongs likedSongs = new LikedSongs();
        likedSongs.addSongs(this.likedSongs.getSongs());
        for (Playlist playlist : this.playlists) {
            likedSongs.removeSongs(playlist.getSongs());
        }
        for (Album album : this.albums) {
            likedSongs.removeSongs(album.getSongs());
        }
        return likedSongs.getSongs();
    }

    /**
     * get arraylist of playlists songs which are not in albums
     * 
     * @param playlist - playlist to get songs from
     * @return - arraylist of songs only in that playlist
     */
    public ArrayList<Song> getStandalonePlaylistSongs(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist passed in getStandalonePlaylistSongs");
            return null;
        }
        Playlist tmpPlaylist = new Playlist("");
        tmpPlaylist.addSongs(playlist.getSongs());
        for (Album album : this.albums) {
            tmpPlaylist.removeSongs(album.getSongs());
        }
        return tmpPlaylist.getSongs();
    }

    /**
     * get the album a song is part of
     * 
     * @param song - song to check
     * @return - constructed album
     */
    public Album getSongAlbum(Song song) {
        if (song == null) {
            logger.debug("null song provided in getSongAlbum");
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
     * get the first playlist the song is part of
     * 
     * @param song - song to check
     * @return - constructed playlist
     */
    public Playlist getSongPlaylist(Song song) {
        if (song == null) {
            logger.debug("null song provided in getSongPlaylist");
            return null;
        }
        for (Playlist playlist : this.getPlaylists()) {
            if (playlist.contains(song)) {
                return playlist;
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
        if (this.likedSongs.contains(song)) {
            return true;
        }
        return false;
    }

    /**
     * get this collections albums
     * 
     * @return - linkedhashset of albums
     */
    public ArrayList<Album> getAlbums() {
        return this.albums;
    }

    /**
     * get album from collection
     * 
     * @param album - constructed album
     * @return - found constructed album or null
     */
    public Album getAlbum(Album album) {
        if (album == null) {
            logger.debug("null album provided in getAlbum");
            return null;
        }
        for (Album thisAlbum : this.albums) {
            if (thisAlbum.equals(album)) {
                return thisAlbum;
            }
        }

        return null;
    }

    /**
     * get this collections playlists
     * 
     * @return - linkedhashset of playlists
     */
    public ArrayList<Playlist> getPlaylists() {
        return this.playlists;
    }

    /**
     * get playlist from collection
     * 
     * @param playlist - constructed playlist to find
     * @return - found playlist or null
     */
    public Playlist getPlaylist(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in getPlaylist");
            return null;
        }
        for (Playlist thisPlaylist : this.playlists) {
            if (thisPlaylist.equals(playlist)) {
                return thisPlaylist;
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
    public String getPlaylistM3U(Playlist playlist) {
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
            output.append("#EXTIMG:").append(playlist.getFolderName() + ".png").append("\n");
        }
        for (Song song : playlist.getSongs()) {
            File songFile = new File(song.getFileName());
            if (!settings.isDownloadHierachy()) {
                Album foundAlbum = this.getSongAlbum(song);
                if (foundAlbum != null) {
                    songFile = new File(foundAlbum.getFolderName(), song.getFileName());
                }
            }
            output.append("#EXTINF:").append(String.valueOf(song.getDuration().toSeconds())).append(",")
                    .append(song.toString()).append("\n");
            output.append(songFile.getPath()).append("\n");
        }
        return output.toString();
    }

    /**
     * get an albums nfo String to write to a file
     * 
     * @param album - album to get songs from
     * @return - string of an xml nfo file to write to a file
     */
    public String getAlbumNFO(Album album) {
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
                thumb.appendChild(doc.createTextNode(album.getFolderName() + ".png"));
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
            logger.error("exception generating NFO content: " + e);
            return null;
        }
    }

    /**
     * get total track count in playlists
     * 
     * @return - int of playlist track count
     */
    public int getPlaylistsTrackCount() {
        int trackCount = 0;
        for (Playlist playlist : this.playlists) {
            trackCount += playlist.size();
        }
        return trackCount;
    }

    /**
     * get total track count in albums
     * 
     * @return - int of album track count
     */
    public int getAlbumsTrackCount() {
        int trackCount = 0;
        for (Album album : this.albums) {
            trackCount += album.size();
        }
        return trackCount;
    }

    /**
     * get total track count with no duplicates
     * 
     * @return - int of total track count
     */
    public int getTotalTrackCount() {
        int trackCount = 0;
        trackCount += this.getStandaloneLikedSongs().size();
        for (Playlist playlist : this.playlists) {
            ArrayList<Song> songs = this.getStandalonePlaylistSongs(playlist);
            trackCount += songs.size();
        }
        trackCount += this.getAlbumsTrackCount();
        return trackCount;
    }

    /**
     * get count of playlists in collection
     * 
     * @return - int of playlists in collection
     */
    public int getPlaylistCount() {
        return this.playlists.size();
    }

    /**
     * get count of albums in collection
     * 
     * @return - int of playlists in collection
     */
    public int getAlbumCount() {
        return this.albums.size();
    }
}
