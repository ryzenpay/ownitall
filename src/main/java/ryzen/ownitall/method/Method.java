package ryzen.ownitall.method;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * Abstract Method class.
 * </p>
 *
 * @author ryzen
 */
abstract public class Method {
    private static final Logger logger = new Logger(Method.class);
    /** Constant <code>methods</code> */
    private static final LinkedHashMap<String, Class<? extends Method>> methods;
    static {
        methods = new LinkedHashMap<>();
        methods.put("Jellyfin", Jellyfin.class);
        methods.put("Spotify", Spotify.class);
        methods.put("Youtube", Youtube.class);
        methods.put("Upload", Upload.class);
        methods.put("Download", getMethod(Settings.load().get("downloadMethod").toString()));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Import {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Export {
    }

    public static LinkedHashMap<String, Class<? extends Method>> getMethods() {
        return methods;
    }

    /**
     * <p>
     * Getter for the field <code>methods</code>.
     * </p>
     *
     * @param annotation a {@link java.lang.Class} object
     * @return a {@link java.util.LinkedHashMap} object
     */
    public static LinkedHashMap<String, Class<? extends Method>> getMethods(Class<? extends Annotation> annotation) {
        LinkedHashMap<String, Class<? extends Method>> filteredMethods = new LinkedHashMap<>();
        for (String methodName : methods.keySet()) {
            Class<? extends Method> method = methods.get(methodName);
            if (method != null) {
                if (method.isAnnotationPresent(annotation)) {
                    filteredMethods.put(methodName, methods.get(methodName));
                }
            } else {
                logger.warn("Unable to find method '" + methodName + "'");
            }
        }
        return filteredMethods;
    }

    public static Class<? extends Method> getMethod(String name) {
        if (name == null) {
            logger.debug("null name provided in getMethod");
            return null;
        }
        // needed for wrapper classes such as Download
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        try {
            return (Class<? extends Method>) Class.forName(name);
        } catch (ClassNotFoundException e) {
            logger.error("Unable to find method '" + name + "'", e);
        }
        return null;
    }

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
    public static Method initMethod(Class<? extends Method> methodClass)
            throws MissingSettingException, AuthenticationException,
            NoSuchMethodException {
        if (methodClass == null) {
            logger.debug("null method class provided in initMethod");
            return null;
        }
        try {
            logger.debug("Initializing '" + methodClass + "' method");
            return methodClass.getDeclaredConstructor().newInstance();
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

    /**
     * <p>
     * clearCredentials.
     * </p>
     *
     * @param type a {@link java.lang.Class} object
     * @return a boolean
     */
    public static void clearCredentials(Class<? extends Method> type) {
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
        logger.debug("Cleared credentials for '" + type.getSimpleName() + "'");
    }

    /**
     * <p>
     * getLikedSongs.
     * </p>
     *
     * @return a {@link ryzen.ownitall.classes.LikedSongs} object
     * @throws java.lang.InterruptedException if any.
     */
    public LikedSongs getLikedSongs() throws InterruptedException {
        logger.warn("Unsupported method for importLikedSongs");
        return null;
    }

    /**
     * <p>
     * getPlaylists.
     * </p>
     *
     * @return a {@link java.util.ArrayList} object
     * @throws java.lang.InterruptedException if any.
     */
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        logger.warn("Unsupported method for importPlaylists");
        return null;
    }

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
    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        logger.warn("Unsupported method for importPlaylist");
        return null;
    }

    /**
     * <p>
     * getAlbums.
     * </p>
     *
     * @return a {@link java.util.ArrayList} object
     * @throws java.lang.InterruptedException if any.
     */
    public ArrayList<Album> getAlbums() throws InterruptedException {
        logger.warn("Unsupported method for importAlbums");
        return null;
    }

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
    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        logger.warn("Unsupported method for importAlbum");
        return null;
    }

    /**
     * <p>
     * uploadLikedSongs.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadLikedSongs() throws InterruptedException {
        logger.warn("Unsupported method for exportLikedSongs");
    }

    /**
     * <p>
     * uploadPlaylists.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadPlaylists() throws InterruptedException {
        logger.warn("Unsupported method for exportPlaylist");
    }

    /**
     * <p>
     * uploadPlaylist.
     * </p>
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        logger.warn("Unsupported method for exportPlaylist");
    }

    /**
     * <p>
     * uploadAlbums.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadAlbums() throws InterruptedException {
        logger.warn("Unsupported method for exportAlbums");
    }

    /**
     * <p>
     * uploadAlbum.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadAlbum(Album album) throws InterruptedException {
        logger.warn("Unsupported method for exportAlbum");
    }

    /**
     * <p>
     * syncLikedSongs.
     * </p>
     *
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncLikedSongs() throws InterruptedException, MissingSettingException, AuthenticationException {
        logger.warn("Unsupported method for syncLikedSongs");
    }

    /**
     * <p>
     * syncPlaylists.
     * </p>
     *
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncPlaylists() throws InterruptedException, MissingSettingException, AuthenticationException {
        logger.warn("Unsupported method for syncPlaylists");
    }

    /**
     * <p>
     * syncPlaylist.
     * </p>
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncPlaylist(Playlist playlist) throws InterruptedException, MissingSettingException,
            AuthenticationException {
        logger.warn("Unsupported method for syncPlaylist");
    }

    /**
     * <p>
     * syncAlbums.
     * </p>
     *
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncAlbums() throws InterruptedException, MissingSettingException, AuthenticationException {
        logger.warn("Unsupported method for syncAlbums");
    }

    /**
     * <p>
     * syncAlbum.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncAlbum(Album album) throws InterruptedException, MissingSettingException, AuthenticationException {
        logger.warn("Unsupported method for syncAlbum");
    }
}
