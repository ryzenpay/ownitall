package ryzen.ownitall.method;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.download.Download;
import ryzen.ownitall.method.interfaces.Export;
import ryzen.ownitall.method.interfaces.Import;
import ryzen.ownitall.method.interfaces.Sync;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.ProgressBar;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * Abstract Method class.
 * </p>
 *
 * @author ryzen
 */
public class Method {
    private static final Logger logger = new Logger(Method.class);
    /** Constant <code>methods</code> */
    private static final LinkedHashSet<Class<?>> methods;
    static {
        methods = new LinkedHashSet<>();
        methods.add(Jellyfin.class);
        methods.add(Spotify.class);
        methods.add(Youtube.class);
        methods.add(Upload.class);
        methods.add(Download.class);
    }

    private Object method;

    /**
     * <p>
     * initMethod.
     * </p>
     *
     * @param methodClass a {@link java.lang.Class} object
     * @return a {@link ryzen.ownitall.method.Method} object
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     * @throws java.lang.NoSuchMethodException                        if any.
     */
    public Method(Class<?> methodClass) throws MissingSettingException, AuthenticationException,
            NoSuchMethodException {
        if (methodClass == null) {
            logger.debug("null method class provided in initMethod");
            throw new NoSuchMethodException();
        }
        try {
            logger.debug("Initializing '" + methodClass.getSimpleName() + "' method");
            this.method = methodClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof MissingSettingException) {
                throw new MissingSettingException(e);
            }
            if (cause instanceof AuthenticationException) {
                throw new AuthenticationException(e);
            }
            logger.error("Exception while setting up method '" + methodClass.getSimpleName() + "'", e);
            throw new NoSuchMethodException(methodClass.getName());
        }
    }

    public static LinkedHashSet<Class<?>> getMethods() {
        return methods;
    }

    public static <type> LinkedHashSet<Class<?>> getMethods(type filter) {
        LinkedHashSet<Class<?>> filteredMethods = new LinkedHashSet<>();
        for (Class<?> method : methods) {
            for (Class<?> currInterface : method.getInterfaces()) {
                if (currInterface.equals(filter)) {
                    filteredMethods.add(method);
                }
            }
        }
        return filteredMethods;
    }

    public static Class<?> getMethod(String name) {
        if (name == null) {
            logger.debug("null name provided in getMethod");
            return null;
        }
        for (Class<?> method : methods) {
            if (method.getSimpleName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        if (this.method == null) {
            return "";
        }
        return this.method.getClass().getSimpleName();
    }

    private Import getImport() {
        return (Import) this.method;
    }

    private Export getExport() {
        return (Export) this.method;
    }

    private Sync getSync() {
        return (Sync) this.method;
    }

    /**
     * <p>
     * clearCredentials.
     * </p>
     *
     * @param type a {@link java.lang.Class} object
     * @return a boolean
     */
    public static void clearCredentials(Class<?> type) {
        if (type == null) {
            logger.debug("null type provided in clearCredentials");
            return;
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(type);
        if (credentials == null) {
            logger.debug("Unable to find credentials for '" + type.getSimpleName() + "'");
            return;
        }
        for (String credential : credentials) {
            try {
                settings.set(credential, "");
            } catch (NoSuchFieldException e) {
                logger.warn("Unable to find method setting '" + credential + "'");
            }
        }
        logger.warn("Cleared credentials for '" + type.getSimpleName() + "'");
    }

    public void importCollection() {
        logger.info("Importing '" + this + "' collection...");
        try (ProgressBar pb = new ProgressBar(this + " Import", 3)) {
            pb.step("Liked Songs");
            LikedSongs likedSongs = this.getImport().getLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
                logger.info("Imported " + likedSongs.size() + " liked songs from '" + this + "'");
            }
            pb.step("Saved Albums");
            ArrayList<Album> albums = this.getImport().getAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Imported " + albums.size() + " albums from '" + this + "'");
            }
            pb.step("Playlists");
            ArrayList<Playlist> playlists = this.getImport().getPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                logger.info(
                        "Imported " + playlists.size() + " playlists from '" + this + "'");
            }
            logger.info("Imported '" + this + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + this + "' collection");
        }
    }

    public void importLikedSongs() {
        try {
            logger.info("Importing liked songs from '" + this + "'...");
            LikedSongs likedSongs = this.getImport().getLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
                logger.info(
                        "Imported " + likedSongs.size() + " liked songs from '" + this + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + this + "' liked songs");
        }
    }

    public void importAlbums() {
        try {
            logger.info("Importing albums from '" + this + "'...");
            ArrayList<Album> albums = this.getImport().getAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Imported " + albums.size() + " albums from '" + this + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing albums");
        }
    }

    public void importAlbum(String albumId, String albumName, String albumArtistName) {
        try {
            logger.info("Importing album '" + albumId + "' from '" + this + "'...");
            Album album = this.getImport().getAlbum(albumId, albumName, albumArtistName);
            if (album != null) {
                Collection.addAlbum(album);
                logger.info("Imported album '" + album.getName() + "' (" + album.size() + ") from '" + this + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + this + "' album '" + albumName + "'");
        }
    }

    public void importPlaylists() {
        try {
            logger.info("Importing playlists from '" + this + "'...");
            ArrayList<Playlist> playlists = this.getImport().getPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                logger.info("Imported " + playlists.size() + " playlists from '" + this + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing playlists");
        }
    }

    public void importPlaylist(String playlistId, String playlistName) {
        try {
            logger.info("Importing playlist '" + playlistName + "' from '" + this + "'...");
            Playlist playlist = this.getImport().getPlaylist(playlistId, playlistName);
            if (playlist != null) {
                Collection.addPlaylist(playlist);
                logger.info(
                        "Imported playlist '" + playlist.getName() + "' (" + playlist.size() + ") from '" + this + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + this + "' playlist");
        }
    }

    public void exportCollection() {
        logger.info("Exporting '" + this + "' (" + Collection.getTotalTrackCount() + ") collection...");
        try (ProgressBar pb = new ProgressBar(this + " Export", 3)) {
            pb.step("Liked Songs");
            this.getExport().uploadLikedSongs();
            logger.info("Exported " + Collection.getLikedSongs().size() + " liked songs to '" + this + "'");
            pb.step("Saved Albums");
            this.getExport().uploadAlbums();
            logger.info("Exported " + Collection.getAlbumCount() + " albums to '" + this + "'");
            pb.step("Playlists");
            this.getExport().uploadPlaylists();
            logger.info("Exported " + Collection.getPlaylistCount() + " playlists to '" + this + "'");
            logger.info("Exported '" + this + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' music");
        }
    }

    public void exportLikedSongs() {
        try {
            logger.info("Exporting " + Collection.getLikedSongs().size() + " liked songs to '" + this + "'...");
            this.getExport().uploadLikedSongs();
            logger.info("Exported " + Collection.getLikedSongs().size() + " liked songs to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' liked songs");
        }
    }

    public void exportPlaylists() {
        logger.info("Exporting " + Collection.getPlaylistCount() + " playlists to '" + this + "'");
        try {
            this.getExport().uploadPlaylists();
            logger.info("Exported " + Collection.getPlaylistCount() + " playlists to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' playlists");
        }
    }

    public void exportPlaylist(Playlist playlist) {
        logger.info("Exporting playlist '" + playlist.getName() + "' (" + playlist.size() + ") to '" + this + "'...");
        try {
            this.getExport().uploadPlaylist(playlist);
            logger.info("Exported playlist '" + playlist.getName() + "' to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' playlist '" + playlist.getName() + "'");
        }
    }

    public void exportAlbums() {
        logger.info("Exporting " + Collection.getAlbumCount() + " albums to '" + this + "'...");
        try {
            this.getExport().uploadAlbums();
            logger.info("Exported " + Collection.getAlbumCount() + " albums to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' albums");
        }
    }

    public void exportAlbum(Album album) {
        logger.info("Exporting album '" + album.getName() + "' (" + album.size() + ") to '" + this + "'...");
        try {
            this.getExport().uploadAlbum(album);
            logger.info("Exported album '" + album.getName() + "' to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' album '" + album.getName() + "'");
        }
    }

    public void syncCollection() {
        logger.info("Syncronizing '" + this + "' collection...");
        try (ProgressBar pb = new ProgressBar(this + " Sync", 3)) {
            pb.step("Liked Songs");
            this.getSync().syncLikedSongs();
            logger.info("Syncronized " + Collection.getLikedSongCount() + " liked songs from '" + this + "'");
            pb.step("Saved Albums");
            this.getSync().syncAlbums();
            logger.info("Syncronized " + Collection.getAlbumCount() + " albums from '" + this + "'");
            pb.step("Playlists");
            this.getSync().syncPlaylists();
            logger.info("Syncronized " + Collection.getPlaylistCount() + " playlists from '" + this + "'");
            logger.info("Syncronized '" + this + "' collection");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + this + "' collection");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + this + "' collection: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + this + "' collection: " + e.getMessage());
        }
    }

    public void syncLikedSongs() {
        try {
            logger.info("Syncronizing " + Collection.getLikedSongCount() + " liked songs to '" + this + "'...");
            this.getSync().syncLikedSongs();
            logger.info("Syncronized " + Collection.getLikedSongCount() + " liked songs to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + this + "' liked songs");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + this + "' collection: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + this + "' collection: " + e.getMessage());
        }
    }

    public void syncAlbums() {
        try {
            logger.info(
                    "Syncronizing " + Collection.getAlbumCount() + " albums to '" + this + "'...");
            this.getSync().syncAlbums();
            logger.info("Syncronized " + Collection.getAlbumCount() + " albums to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + this + "' albums");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + this + "' collection: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + this + "' collection: " + e.getMessage());
        }
    }

    public void syncPlaylists() {
        try {
            logger.info("Syncronizing " + Collection.getPlaylistCount() + " playlists to '" + this + "'...");
            this.getSync().syncPlaylists();
            logger.info("Syncronized " + Collection.getPlaylistCount() + " playlists to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + this + "' playlists");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + this + "' collection: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + this + "' collection: " + e.getMessage());
        }
    }

    public void syncPlaylist(Playlist playlist) {
        try {
            logger.info("Syncronizing playlist '" + playlist + "' to '" + this + "'");
            this.getSync().syncPlaylist(playlist);
            logger.info("Syncronized playlist '" + playlist + " to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + this + "' playlist '" + playlist.getName() + "'");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + this + "' collection: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + this + "' collection: " + e.getMessage());
        }
    }
}
