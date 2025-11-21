package ryzen.ownitall.method;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.interfaces.Import;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;
import ryzen.ownitall.util.exceptions.QueryException;

// its paid LOL
public class AppleMusic implements Import {
    private static final Logger logger = new Logger(AppleMusic.class);

    private String token;
    private static final String baseUrl = "https://api.music.apple.com/v1";

    public AppleMusic() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(AppleMusic.class)) {
            logger.debug("Empty AppleMusic credentials");
            throw new MissingSettingException(AppleMusic.class);
        }
        this.token = WebTools.getOauthToken(
                "https://appleid.apple.com/auth/oauth2/v2/token?grant_type=authorization_code",
                "https://appleid.apple.com/auth/authorize?response_type=code",
                Settings.appleClientID, Settings.appleClientSecret, null);
        logger.debug("Successfully authenticated into apple music");
    }

    private JsonNode query(String path, ArrayList<String> include) {
        String flags = "?l=en-US";
        if (include != null) {
            for (String includeEntry : include) {
                flags += "&include=" + includeEntry;
            }
        }
        try {
            URI url = new URI(baseUrl + path + flags);
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestProperty("Accept", "application/vnd.api+json");
            connection.setRequestProperty("Authorization", "Bearer " + this.token);

            WebTools.queryPacer(500);
            return WebTools.query(connection);
        } catch (URISyntaxException e) {
            logger.error("Exception while constructing apple music query", e);
            return null;
        } catch (QueryException | IOException | InterruptedException e) {
            logger.warn("Exception while querying apple music: " + e);
            return null;
        }
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
        logger.warn("Unsupported atm");
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
        logger.warn("Unsupported atm");
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
        logger.warn("Unsupported atm");
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
        logger.warn("Unsupported atm");
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
        logger.warn("Unsupported atm");
        return null;
    }
}
