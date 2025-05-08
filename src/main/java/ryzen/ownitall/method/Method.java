package ryzen.ownitall.method;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.util.Logger;

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
    public static final LinkedHashMap<String, Class<? extends Method>> methods;
    static {
        methods = new LinkedHashMap<>();
        methods.put("Jellyfin", Jellyfin.class);
        methods.put("Spotify", Spotify.class);
        methods.put("Youtube", Youtube.class);
        methods.put("Upload", Upload.class);
        methods.put("Download", (Class<? extends Method>) Settings.load().get("downloadMethod").getClass());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Import {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Export {
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
        LinkedHashMap<String, Class<? extends Method>> importMethods = new LinkedHashMap<>();
        for (String methodName : methods.keySet()) {
            if (methods.get(methodName).isAnnotationPresent(annotation)) {
                importMethods.put(methodName, methods.get(methodName));
            }
        }
        return importMethods;
    }

    /**
     * <p>
     * initMethod.
     * </p>
     *
     * @param methodClass a {@link java.lang.Class} object
     * @return a {@link ryzen.ownitall.method.Method} object
     * @throws java.lang.InterruptedException if any.
     */
    public static Method initMethod(Class<? extends Method> methodClass) throws InterruptedException {
        if (methodClass == null) {
            logger.debug("null method class provided in load");
            return null;
        }
        try {
            logger.debug("Initializing '" + methodClass + "' method");
            return methodClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException e) {
            logger.error("Interrupted while setting up method '" + methodClass.getSimpleName() + "'", e);
            throw new InterruptedException(e.getMessage());
        } catch (IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            logger.error("Exception creating method '" + methodClass.getSimpleName() + "'", e);
            throw new InterruptedException(e.getMessage());
        }
    }

    /**
     * <p>
     * getMethodName.
     * </p>
     *
     * @param method a {@link ryzen.ownitall.method.Method} object
     * @return a {@link java.lang.String} object
     */
    public static String getMethodName(Method method) {
        return method.getClass().getSimpleName();
    }

    /**
     * <p>
     * isCredentialsEmpty.
     * </p>
     *
     * @param type a {@link java.lang.Class} object
     * @return a boolean
     */
    public static boolean isCredentialsEmpty(Class<?> type) {
        if (type == null) {
            logger.debug("null type provided in isCredentialsEmpty");
            return true;
        }
        Credentials credentials = Credentials.load();
        LinkedHashMap<String, String> credentialVars = credentials.getGroup(type);
        if (credentialVars == null) {
            logger.debug("Unable to find credentials for '" + type.getSimpleName() + "'");
            return false;
        }
        for (String varName : credentialVars.values()) {
            if (credentials.isEmpty(varName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * isSettingsEmpty.
     * </p>
     *
     * @param type a {@link java.lang.Class} object
     * @return a boolean
     */
    public static boolean isSettingsEmpty(Class<?> type) {
        if (type == null) {
            logger.debug("null type provided in isCredentialsEmpty");
            return true;
        }
        Settings settings = Settings.load();
        LinkedHashMap<String, String> settingVars = settings.getGroup(type);
        if (settingVars == null) {
            logger.debug("Unable to find credentials for '" + type.getSimpleName() + "'");
            return false;
        }
        for (String varName : settingVars.values()) {
            if (settings.isEmpty(varName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * clearCredentials.
     * </p>
     *
     * @param type a {@link java.lang.Class} object
     * @return a boolean
     */
    public static boolean clearCredentials(Class<? extends Method> type) {
        if (type == null) {
            logger.debug("null type provided in clearCredentials");
            return true;
        }
        Settings settings = Settings.load();
        LinkedHashMap<String, String> credentialVars = Credentials.load().getGroup(type);
        if (credentialVars == null) {
            logger.debug("Unable to find credentials for '" + type.getSimpleName() + "'");
            return false;
        }
        for (String varName : credentialVars.values()) {
            if (!settings.set(varName, "")) {
                return false;
            }
        }
        logger.debug("Cleared credentials for '" + type.getSimpleName() + "'");
        return true;
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
     * @throws java.lang.InterruptedException if any.
     */
    public void syncLikedSongs() throws InterruptedException {
        logger.warn("Unsupported method for syncLikedSongs");
    }

    /**
     * <p>
     * syncPlaylists.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void syncPlaylists() throws InterruptedException {
        logger.warn("Unsupported method for syncPlaylists");
    }

    /**
     * <p>
     * syncPlaylist.
     * </p>
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     * @throws java.lang.InterruptedException if any.
     */
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        logger.warn("Unsupported method for syncPlaylist");
    }

    /**
     * <p>
     * syncAlbums.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void syncAlbums() throws InterruptedException {
        logger.warn("Unsupported method for syncAlbums");
    }

    /**
     * <p>
     * syncAlbum.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public void syncAlbum(Album album) throws InterruptedException {
        logger.warn("Unsupported method for syncAlbum");
    }
}
