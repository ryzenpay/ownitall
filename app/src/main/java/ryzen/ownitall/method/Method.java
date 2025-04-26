package ryzen.ownitall.method;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;

public class Method {
    private static final Logger logger = LogManager.getLogger();
    public static final LinkedHashMap<String, Class<? extends MethodClass>> methods;
    public static final LinkedHashMap<Class<? extends MethodClass>, LinkedHashMap<String, String>> credentialGroups;
    private MethodClass instance;
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

    public Method(Class<? extends MethodClass> methodClass) throws InterruptedException {
        this.setMethod(methodClass);
    }

    public String getMethodName() {
        return instance.getClass().getSimpleName();
    }

    public void setMethod(Class<? extends MethodClass> methodClass) throws InterruptedException {
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

    public static boolean clearCredentials(Class<? extends MethodClass> type) {
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

    public LikedSongs importLikedSongs() throws InterruptedException {
        return instance.getLikedSongs();
    }

    public ArrayList<Playlist> importPlaylists() throws InterruptedException {
        return instance.getPlaylists();
    }

    public Playlist importPlaylist(String playlistId, String playlistName) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("null playlist id provided in importPlaylist");
            return null;
        }
        return instance.getPlaylist(playlistId, playlistName);
    }

    public ArrayList<Album> importAlbums() throws InterruptedException {
        return instance.getAlbums();
    }

    public Album importAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        if (albumId == null) {
            logger.debug("null album id provided in importAlbum");
            return null;
        }
        return instance.getAlbum(albumId, albumName, albumArtistName);
    }

    public void exportLikedSongs() throws InterruptedException {
        instance.uploadLikedSongs();
    }

    public void exportPlaylists() throws InterruptedException {
        instance.uploadPlaylists();
    }

    public void exportPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided to exportPlaylist");
            return;
        }
        instance.uploadPlaylist(playlist);
    }

    public void exportAlbums() throws InterruptedException {
        instance.uploadAlbums();
    }

    public void exportAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided to exportPlaylist");
            return;
        }
        instance.uploadAlbum(album);
    }

    public void syncLikedSongs() throws InterruptedException {
        instance.syncLikedSongs();
        instance.uploadLikedSongs();
    }

    public void syncPlaylists() throws InterruptedException {
        instance.syncPlaylists();
        for (Playlist playlist : Collection.getPlaylists()) {
            instance.syncPlaylist(playlist);
            instance.uploadPlaylist(playlist);
        }
    }

    public void syncAlbums() throws InterruptedException {
        instance.syncAlbums();
        for (Album album : Collection.getAlbums()) {
            instance.syncAlbum(album);
            instance.uploadAlbum(album);
        }
    }
}
