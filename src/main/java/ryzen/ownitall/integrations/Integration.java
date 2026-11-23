package ryzen.ownitall.integrations;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.util.ClassLoader;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.ProgressBar;
import ryzen.ownitall.util.WebTools.AuthenticationException;

/**
 * <p>
 * Abstract Integration class.
 * </p>
 *
 * @author ryzen
 */
public class Integration {
    private static final Logger logger = new Logger(Integration.class);
    /** Constant <code>integrations</code> */
    private Object integration;

    /**
     * <p>
     * Integration
     * </p>
     *
     * @param clazz a {@link java.lang.Class} object
     * @throws ryzen.ownitall.Settings.MissingSettingException                     if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException                if any.
     * @throws ryzen.ownitall.integrations.Integration.InvalidIntegrationException if any.
     */
    public Integration(Class<?> clazz) throws MissingSettingException, AuthenticationException,
            InvalidIntegrationException {
        if (clazz == null) {
            logger.debug("null integration class provided in Integration");
            throw new InvalidIntegrationException();
        }
        try {
            logger.debug("Initializing '" + clazz.getSimpleName() + "' integration");
            this.integration = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof MissingSettingException) {
                throw new MissingSettingException(e);
            }
            if (cause instanceof AuthenticationException) {
                throw new AuthenticationException(e);
            }
            logger.error("Exception while setting up integration '" + clazz.getSimpleName() + "'", e);
            throw new InvalidIntegrationException(clazz.getName());
        }
    }

    public String getIntegrationName() {
        if (this.integration == null) {
            return "";
        }
        return this.integration.getClass().getSimpleName();
    }

    /**
     * <p>
     * Getter for the field <code>integrations</code>.
     * </p>
     *
     * @return a {@link java.util.LinkedHashSet} object
     */
    public static LinkedHashSet<Class<?>> getIntegrations() {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        ClassLoader loader = ClassLoader.load();
        classes.addAll(loader.getSubClasses(Import.class));
        classes.addAll(loader.getSubClasses(Export.class));
        classes.addAll(loader.getSubClasses(Sync.class));
        return classes;
    }

    /**
     * <p>
     * Getter for the field <code>integrations</code>.
     * </p>
     *
     * @param <T>   a type class
     * @param clazz a {@link java.lang.Class} object
     * @return      a {@link java.util.LinkedHashSet} object
     */
    public static <T> LinkedHashSet<Class<? extends T>> getIntegrations(Class<T> clazz) {
        return ClassLoader.load().getSubClasses(clazz);
    }

    /**
     * <p>
     * Getter for the field <code>integration</code>.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link java.lang.Class} object
     */
    public static Class<?> getIntegration(String name) {
        if (name == null) {
            logger.debug("null name provided in getIntegration");
            return null;
        }
        for (Class<?> integration : getIntegrations()) {
            if (integration.getSimpleName().equals(name)) {
                return integration;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (this.integration == null) {
            return "";
        }
        return this.integration.getClass().getSimpleName();
    }

    private Import getImport() {
        return (Import) this.integration;
    }

    private Export getExport() {
        return (Export) this.integration;
    }

    private Sync getSync() {
        return (Sync) this.integration;
    }

    /**
     * <p>
     * clearCredentials.
     * </p>
     *
     * @param clazz a {@link java.lang.Class} object
     */
    public static void clearCredentials(Class<?> clazz) {
        if (clazz == null) {
            logger.debug("null type provided in clearCredentials");
            return;
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(clazz);
        if (credentials == null) {
            logger.debug("Unable to find credentials for '" + clazz.getSimpleName() + "'");
            return;
        }
        for (String credential : credentials) {
            try {
                settings.set(credential, "");
            } catch (NoSuchFieldException e) {
                logger.warn("Unable to find integration setting '" + credential + "'");
            }
        }
        logger.warn("Cleared credentials for '" + clazz.getSimpleName() + "'");
    }

    /**
     * <p>
     * importCollection.
     * </p>
     */
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

    /**
     * <p>
     * importLikedSongs.
     * </p>
     */
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

    /**
     * <p>
     * importAlbums.
     * </p>
     */
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

    /**
     * <p>
     * importAlbum.
     * </p>
     *
     * @param albumId         a {@link java.lang.String} object
     * @param albumName       a {@link java.lang.String} object
     * @param albumArtistName a {@link java.lang.String} object
     */
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

    /**
     * <p>
     * importPlaylists.
     * </p>
     */
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

    /**
     * <p>
     * importPlaylist.
     * </p>
     *
     * @param playlistId   a {@link java.lang.String} object
     * @param playlistName a {@link java.lang.String} object
     */
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

    /**
     * <p>
     * exportCollection.
     * </p>
     */
    public void exportCollection() {
        logger.info("Exporting '" + this + "' (" + Collection.getTotalSongCount() + ") collection...");
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

    /**
     * <p>
     * exportLikedSongs.
     * </p>
     */
    public void exportLikedSongs() {
        try {
            logger.info("Exporting " + Collection.getLikedSongs().size() + " liked songs to '" + this + "'...");
            this.getExport().uploadLikedSongs();
            logger.info("Exported " + Collection.getLikedSongs().size() + " liked songs to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' liked songs");
        }
    }

    /**
     * <p>
     * exportPlaylists.
     * </p>
     */
    public void exportPlaylists() {
        logger.info("Exporting " + Collection.getPlaylistCount() + " playlists to '" + this + "'");
        try {
            this.getExport().uploadPlaylists();
            logger.info("Exported " + Collection.getPlaylistCount() + " playlists to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' playlists");
        }
    }

    /**
     * <p>
     * exportPlaylist.
     * </p>
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     */
    public void exportPlaylist(Playlist playlist) {
        logger.info("Exporting playlist '" + playlist.getName() + "' (" + playlist.size() + ") to '" + this + "'...");
        try {
            this.getExport().uploadPlaylist(playlist);
            logger.info("Exported playlist '" + playlist.getName() + "' to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' playlist '" + playlist.getName() + "'");
        }
    }

    /**
     * <p>
     * exportAlbums.
     * </p>
     */
    public void exportAlbums() {
        logger.info("Exporting " + Collection.getAlbumCount() + " albums to '" + this + "'...");
        try {
            this.getExport().uploadAlbums();
            logger.info("Exported " + Collection.getAlbumCount() + " albums to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' albums");
        }
    }

    /**
     * <p>
     * exportAlbum.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     */
    public void exportAlbum(Album album) {
        logger.info("Exporting album '" + album.getName() + "' (" + album.size() + ") to '" + this + "'...");
        try {
            this.getExport().uploadAlbum(album);
            logger.info("Exported album '" + album.getName() + "' to '" + this + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + this + "' album '" + album.getName() + "'");
        }
    }

    /**
     * <p>
     * syncCollection.
     * </p>
     */
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

    /**
     * <p>
     * syncLikedSongs.
     * </p>
     */
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

    /**
     * <p>
     * syncAlbums.
     * </p>
     */
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

    /**
     * <p>
     * syncPlaylists.
     * </p>
     */
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

    /**
     * <p>
     * syncPlaylist.
     * </p>
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     */
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

    public static class InvalidIntegrationException extends Exception {

        public InvalidIntegrationException() {
            super();
        }

        public InvalidIntegrationException(String message) {
            super(message);
        }
    }

    /**
     * <p>
     * Export interface.
     * </p>
     *
     * @author ryzen
     */
    public static interface Export {

        /**
         * <p>
         * uploadLikedSongs.
         * </p>
         *
         * @throws java.lang.InterruptedException if any.
         */
        public void uploadLikedSongs() throws InterruptedException;

        /**
         * <p>
         * uploadPlaylists.
         * </p>
         *
         * @throws java.lang.InterruptedException if any.
         */
        public void uploadPlaylists() throws InterruptedException;

        /**
         * <p>
         * uploadPlaylist.
         * </p>
         *
         * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
         * @throws java.lang.InterruptedException if any.
         */
        public void uploadPlaylist(Playlist playlist) throws InterruptedException;

        /**
         * <p>
         * uploadAlbums.
         * </p>
         *
         * @throws java.lang.InterruptedException if any.
         */
        public void uploadAlbums() throws InterruptedException;

        /**
         * <p>
         * uploadAlbum.
         * </p>
         *
         * @param album a {@link ryzen.ownitall.classes.Album} object
         * @throws java.lang.InterruptedException if any.
         */
        public void uploadAlbum(Album album) throws InterruptedException;

    }

    /**
     * <p>
     * Import interface.
     * </p>
     *
     * @author ryzen
     */
    public static interface Import {
        /**
         * <p>
         * getLikedSongs.
         * </p>
         *
         * @return a {@link ryzen.ownitall.classes.LikedSongs} object
         * @throws java.lang.InterruptedException if any.
         */
        public LikedSongs getLikedSongs() throws InterruptedException;

        /**
         * <p>
         * getPlaylists.
         * </p>
         *
         * @return a {@link java.util.ArrayList} object
         * @throws java.lang.InterruptedException if any.
         */
        public ArrayList<Playlist> getPlaylists() throws InterruptedException;

        /**
         * <p>
         * getPlaylist.
         * </p>
         *
         * @param playlistId   a {@link java.lang.String} object
         * @param playlistName a {@link java.lang.String} object
         * @return a {@link ryzen.ownitall.classes.Playlist} object
         * @throws java.lang.InterruptedException if any.
         */
        public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException;

        /**
         * <p>
         * getAlbums.
         * </p>
         *
         * @return a {@link java.util.ArrayList} object
         * @throws java.lang.InterruptedException if any.
         */
        public ArrayList<Album> getAlbums() throws InterruptedException;

        /**
         * <p>
         * getAlbum.
         * </p>
         *
         * @param albumId         a {@link java.lang.String} object
         * @param albumName       a {@link java.lang.String} object
         * @param albumArtistName a {@link java.lang.String} object
         * @return a {@link ryzen.ownitall.classes.Album} object
         * @throws java.lang.InterruptedException if any.
         */
        public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException;
    }

    /**
     * <p>
     * Sync interface.
     * </p>
     *
     * @author ryzen
     */
    public static interface Sync {
        /**
         * <p>
         * syncLikedSongs.
         * </p>
         *
         * @throws java.lang.InterruptedException                       if any.
         * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
         * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
         */
        public void syncLikedSongs() throws InterruptedException, MissingSettingException, AuthenticationException;

        /**
         * <p>
         * syncPlaylists.
         * </p>
         *
         * @throws java.lang.InterruptedException                       if any.
         * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
         * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
         */
        public void syncPlaylists() throws InterruptedException, MissingSettingException, AuthenticationException;

        /**
         * <p>
         * syncPlaylist.
         * </p>
         *
         * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
         * @throws java.lang.InterruptedException                       if any.
         * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
         * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
         */
        public void syncPlaylist(Playlist playlist)
                throws InterruptedException, MissingSettingException, AuthenticationException;

        /**
         * <p>
         * syncAlbums.
         * </p>
         *
         * @throws java.lang.InterruptedException                       if any.
         * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
         * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
         */
        public void syncAlbums() throws InterruptedException, MissingSettingException, AuthenticationException;

        /**
         * <p>
         * syncAlbum.
         * </p>
         *
         * @param album a {@link ryzen.ownitall.classes.Album} object
         * @throws java.lang.InterruptedException                       if any.
         * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
         * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
         */
        public void syncAlbum(Album album) throws InterruptedException, MissingSettingException, AuthenticationException;
    }
}
