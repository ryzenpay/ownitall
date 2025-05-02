package ryzen.ownitall.method;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;

@SuppressWarnings("static-access")
abstract public class Method {
    private static final Logger logger = LogManager.getLogger(Method.class);
    public static final LinkedHashMap<String, Class<? extends Method>> methods;
    static {
        methods = new LinkedHashMap<>();
        methods.put("Jellyfin", Jellyfin.class);
        methods.put("Spotify", Spotify.class);
        methods.put("Youtube", Youtube.class);
        methods.put("Upload", Upload.class);
        // TODO: hardcoded to what is defaulted in settings
        // because it loads at startup before settings initializes
        methods.put("Download", Settings.load().downloadMethod);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Import {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Export {
    }

    public static LinkedHashMap<String, Class<? extends Method>> getMethods(Class<? extends Annotation> annotation) {
        LinkedHashMap<String, Class<? extends Method>> importMethods = new LinkedHashMap<>();
        for (String methodName : methods.keySet()) {
            if (methods.get(methodName).isAnnotationPresent(annotation)) {
                importMethods.put(methodName, methods.get(methodName));
            }
        }
        return importMethods;
    }

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

    public static String getMethodName(Method method) {
        return method.getClass().getSimpleName();
    }

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

    public LikedSongs getLikedSongs() throws InterruptedException {
        logger.warn("Unsupported method for importLikedSongs");
        return null;
    }

    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        logger.warn("Unsupported method for importPlaylists");
        return null;
    }

    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        logger.warn("Unsupported method for importPlaylist");
        return null;
    }

    public ArrayList<Album> getAlbums() throws InterruptedException {
        logger.warn("Unsupported method for importAlbums");
        return null;
    }

    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        logger.warn("Unsupported method for importAlbum");
        return null;
    }

    public void uploadLikedSongs() throws InterruptedException {
        logger.warn("Unsupported method for exportLikedSongs");
    }

    public void uploadPlaylists() throws InterruptedException {
        logger.warn("Unsupported method for exportPlaylist");
    }

    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        logger.warn("Unsupported method for exportPlaylist");
    }

    public void uploadAlbums() throws InterruptedException {
        logger.warn("Unsupported method for exportAlbums");
    }

    public void uploadAlbum(Album album) throws InterruptedException {
        logger.warn("Unsupported method for exportAlbum");
    }

    public void syncLikedSongs() throws InterruptedException {
        logger.warn("Unsupported method for syncLikedSongs");
    }

    public void syncPlaylists() throws InterruptedException {
        logger.warn("Unsupported method for syncPlaylists");
    }

    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        logger.warn("Unsupported method for syncPlaylist");
    }

    public void syncAlbums() throws InterruptedException {
        logger.warn("Unsupported method for syncAlbums");
    }

    public void syncAlbum(Album album) throws InterruptedException {
        logger.warn("Unsupported method for syncAlbum");
    }
}
