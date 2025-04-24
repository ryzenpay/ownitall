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

public class Method {
    private static final Logger logger = LogManager.getLogger();
    public static final LinkedHashMap<String, Class<? extends MethodClass>> methods;
    public static final LinkedHashMap<Class<? extends MethodClass>, LinkedHashMap<String, String>> credentialGroups;
    private static MethodClass instance;
    // needs to be like this for it to maintain the order
    static {
        methods = new LinkedHashMap<>();
        methods.put("Jellyfin", Jellyfin.class);
        methods.put("Spotify", Spotify.class);
        methods.put("Youtube", Youtube.class);
        methods.put("Local", Local.class);
    }

    static {
        credentialGroups = new LinkedHashMap<>();
        credentialGroups.put(Spotify.class, Credentials.getSpotifyCredentials());
        credentialGroups.put(Youtube.class, Credentials.getYoutubeCredentials());
        credentialGroups.put(Jellyfin.class, Credentials.getJellyfinCredentials());
        credentialGroups.put(Local.class, Credentials.getLocalCredentials());
    }

    public static void setMethod(Class<? extends MethodClass> methodClass) throws InterruptedException {
        if (methodClass == null) {
            logger.debug("null method class provided in load");
            return;
        }
        if (instance == null || !methodClass.getName().equals(instance.getClass().getName())) {
            try {
                instance = methodClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException e) {
                logger.error("Interrupted while setting up method '" + methodClass.getSimpleName() + "'", e);
                throw new InterruptedException(e.getMessage());
            } catch (IllegalAccessException | NoSuchMethodException
                    | InvocationTargetException e) {
                logger.error("Exception creating method '" + methodClass.getSimpleName() + "'", e);
                throw new InterruptedException(e.getMessage());
            }
        }
        return;
    }

    public static MethodClass load() {
        return instance;
    }

    public static String getMethodName() {
        if (instance == null) {
            return null;
        }
        return instance.getClass().getSimpleName();
    }

    public static boolean isCredentialsEmpty(Class<? extends MethodClass> type) {
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

    public LikedSongs getLikedSongs() throws InterruptedException {
        logger.warn("Unsupported method to get liked songs");
        return null;
    }

    public void syncLikedSongs() throws InterruptedException {
        logger.warn("Unsupported method to sync liked songs");
    }

    public void uploadLikedSongs() throws InterruptedException {
        logger.warn("Unsupported method to upload liked songs");
    }

    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        logger.warn("Unsupported method to get playlists");
        return null;
    }

    public void syncPlaylists() throws InterruptedException {
        logger.warn("Unsupported method to sync playlists");
    }

    public void uploadPlaylists() throws InterruptedException {
        logger.warn("Unsupported method to upload playlists");
    }

    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        logger.warn("Unsupported method to get playlist");
        return null;
    }

    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        logger.warn("Unsupported method to sync playlist");
    }

    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        logger.warn("Unsupported method to upload playlist");
    }

    public ArrayList<Album> getAlbums() throws InterruptedException {
        logger.warn("Unsupported method to get albums");
        return null;
    }

    public void syncAlbums() throws InterruptedException {
        logger.warn("Unsupported method to sync albums");
    }

    public void uploadAlbums() throws InterruptedException {
        logger.warn("Unsupported method to upload albums");
    }

    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        logger.warn("Unsupported method to get album");
        return null;
    }

    public void syncAlbum(Album album) throws InterruptedException {
        logger.warn("Unsupported method to sync album");
    }

    public void uploadAlbum(Album album) throws InterruptedException {
        logger.warn("Unsupported method to upload album");
    }
}
