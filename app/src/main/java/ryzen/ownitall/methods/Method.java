package ryzen.ownitall.methods;

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
    private static final Credentials credentials = Credentials.load();
    public static final LinkedHashMap<String, Class<? extends Method>> methods;
    public static final LinkedHashMap<Class<?>, LinkedHashMap<String, String>> credentialGroups;
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
        credentialGroups.put(Spotify.class, credentials.getSpotifyCredentials());
        credentialGroups.put(Youtube.class, credentials.getYoutubeCredentials());
        credentialGroups.put(Jellyfin.class, credentials.getJellyfinCredentials());
    }

    public static boolean isCredentialsEmpty(Class<?> type) {
        if (type == null) {
            logger.debug("null type provided in isCredentialsEmpty");
            return true;
        }
        LinkedHashMap<String, String> credentialVars = credentialGroups.get(type);
        if (credentialVars == null) {
            logger.debug("Unable to find credentials for '" + type.getSimpleName() + "'");
            return false;
        }
        for (String varName : credentialVars.values()) {
            if (!credentials.isEmpty(varName)) {
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
