package ryzen.ownitall.method;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.download.YT_dl;

public class Method {
    private static final Logger logger = LogManager.getLogger();
    public static final LinkedHashMap<String, Class<? extends Method>> methods;
    public static final LinkedHashMap<Class<? extends Method>, LinkedHashMap<String, String>> credentialGroups;
    // needs to be like this for it to maintain the order
    static {
        methods = new LinkedHashMap<>();
        methods.put("Jellyfin", Jellyfin.class);
        methods.put("Spotify", Spotify.class);
        methods.put("Youtube", Youtube.class);
        methods.put("Upload", Upload.class);
        methods.put("YT-dlp", YT_dl.class);
    }

    static {
        credentialGroups = new LinkedHashMap<>();
        credentialGroups.put(Spotify.class, Credentials.getSpotifyCredentials());
        credentialGroups.put(Youtube.class, Credentials.getYoutubeCredentials());
        credentialGroups.put(Jellyfin.class, Credentials.getJellyfinCredentials());
        credentialGroups.put(Upload.class, Credentials.getUploadCredentials());
    }

    public static Method initMethod(Class<? extends Method> methodClass) throws InterruptedException {
        if (methodClass == null) {
            logger.debug("null method class provided in load");
            return null;
        }
        try {
            Method method = methodClass.getDeclaredConstructor().newInstance();
            return method;
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
        LinkedHashMap<String, String> credentialVars = credentialGroups.get(type);
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

    public static boolean clearCredentials(Class<? extends Method> type) {
        if (type == null) {
            logger.debug("null type provided in clearCredentials");
            return true;
        }
        Credentials credentials = Credentials.load();
        LinkedHashMap<String, String> credentialVars = credentialGroups.get(type);
        if (credentialVars == null) {
            logger.debug("Unable to find credentials for '" + type.getSimpleName() + "'");
            return false;
        }
        for (String varName : credentialVars.values()) {
            if (!credentials.set(varName, "")) {
                return false;
            }
        }
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
