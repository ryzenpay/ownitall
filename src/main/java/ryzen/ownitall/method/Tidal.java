package ryzen.ownitall.method;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.interfaces.Import;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;
import ryzen.ownitall.util.exceptions.QueryException;

import java.awt.Desktop;
import com.sun.net.httpserver.HttpServer;

// https://developer.tidal.com/documentation
// https://developer.tidal.com/apiref
public class Tidal implements Import {
    private static final Logger logger = new Logger(Tidal.class);
    private String token;
    private String userID;
    private static final String baseUrl = "https://openapi.tidal.com/v2/";
    private static final String countryCode = "US";
    private static final String scope = "collection.read%20playlists.read";
    private final int timeout = 20; // in seconds

    public Tidal() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Tidal.class)) {
            logger.debug("Empty tidal credentials");
            throw new MissingSettingException(Tidal.class);
        }
        try {
            String codeVerifier = WebTools.generateCodeVerifier();
            String codeChallenge = WebTools.generateCodeChallenge(codeVerifier);
            this.token = this.getToken(codeVerifier, this.getCode(codeChallenge));
            this.userID = this.getUserID();
            logger.debug("Successfully authenticated into tidal as " + this.userID);
        } catch (InterruptedException e) {
            throw new AuthenticationException(e);
        }
    }

    /**
     * Start an HTTP server to intercept the Tidal API code
     *
     * @throws java.lang.InterruptedException - if an error occurs
     */
    // TODO: make spotify and tidal share this code
    private String getCode(String codeChallenge) throws InterruptedException {
        AtomicReference<String> codeRef = new AtomicReference<>();
        String authUrl = "https://login.tidal.com/authorize?response_type=code&client_id=" + Settings.tidalClientID
                + "&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Fmethod%2Ftidal&scope=" + scope
                + "&code_challenge_method=S256&code_challenge=" + codeChallenge;
        try {
            URI authUri = URI.create(authUrl);
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
            server.createContext("/method/tidal", exchange -> {
                logger.debug("request received on /method/tidal");
                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery();
                String code = extractCodeFromQuery(query);
                String responseText;

                if (code != null) {
                    codeRef.set(code);
                    logger.info("Authorization code received: " + code);
                    responseText = "Code received, you can now close this tab";
                    exchange.sendResponseHeaders(200, responseText.getBytes().length);
                } else {
                    logger.warn("Failed to retrieve authorization code. Query: " + query);
                    responseText = "an error occurred, check logs for more";
                    exchange.sendResponseHeaders(404, responseText.getBytes().length);
                }

                OutputStream responseBody = exchange.getResponseBody();
                responseBody.write(responseText.getBytes());
                responseBody.close();
            });
            server.start();
            logger.info("Awaiting response at http://localhost/method/tidal");
            if (Settings.interactive) {
                try {
                    interactiveSetCode(authUri, codeRef);
                } catch (InterruptedException e) {
                    logger.debug("Interrupted while interactively getting code");
                }
            } else {
                try {
                    nonInteractiveSetCode(authUri, codeRef);
                } catch (IOException e) {
                    logger.error("Unable to non-interactively login/get code", e);
                }
            }
            server.stop(0);
        } catch (IOException e) {
            logger.error("Failed to start local server", e);
        }
        if (codeRef.get() == null) {
            logger.info("Unable to get code automatically, please provide it manually");
            System.out.println("Please open this url: " + authUrl);
            System.out.print("Please provide the code found in response url: ");
            return Input.request().getString();
        } else {
            return codeRef.get();
        }
    }

    private String getToken(String codeVerifier, String code) throws AuthenticationException {
        try {
            URI uri = new URI("https://auth.tidal.com/v1/oauth2/token?grant_type=authorization_code&client_id="
                    + Settings.tidalClientID + "&code=" + code
                    + "&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Fmethod%2Ftidal&code_verifier=" + codeVerifier);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            JsonNode response = WebTools.query(connection);
            if (response.has("access_token")) {
                return response.path("access_token").asText();
            } else {
                throw new AuthenticationException("no access token in response: " + response.toString());
            }
        } catch (IOException | QueryException e) {
            throw new AuthenticationException(e);
        } catch (URISyntaxException e) {
            logger.error("Invalid tidal oauth url constructed", e);
            return null;
        }
    }

    private void interactiveSetCode(URI url, AtomicReference<String> codeRef) throws InterruptedException {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(url);
            } catch (IOException e) {
                logger.error("Exception opening web browser", e);
            }
        }
        logger.info("Waiting " + timeout + " seconds for code or interrupt to manually provide");
        for (int i = 0; i < timeout; i++) {
            if (codeRef.get() != null) {
                break;
            }
            Thread.sleep(1000);
        }
    }

    private void nonInteractiveSetCode(URI url, AtomicReference<String> codeRef) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.toURL().openConnection();
        con.setConnectTimeout(timeout * 1000);
        con.connect(); // TODO: requires login cookies
        logger.debug("Web request made to: '" + url + "'");
        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            con.disconnect();
            throw new IOException("Exception (" + responseCode + ") response code from web request");
        }
        con.disconnect();
    }

    /**
     * Extracts the 'code' query parameter from the request URI
     *
     * @param query - full query string from the request URI
     * @return String - extracted code if present
     */
    private String extractCodeFromQuery(String query) {
        if (query != null && query.contains("code=")) {
            String[] queryParams = query.split("&");
            for (String param : queryParams) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "code".equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }

    private String getUserID() throws AuthenticationException {
        try {
            URI uri = new URI(baseUrl + "users/me");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("accept", "application/vnd.api+json");
            connection.setRequestProperty("Authorization", "Bearer " + this.token);
            JsonNode response = WebTools.query(connection);
            if (response.has("data")) {
                return response.path("data").path("id").asText();
            } else {
                throw new AuthenticationException("no user id in response: " + response.toString());
            }
        } catch (IOException | QueryException e) {
            throw new AuthenticationException(e);
        } catch (URISyntaxException e) {
            logger.error("Invalid tidal oauth url constructed", e);
            return null;
        }
    }

    private JsonNode queryList(String path, int page) {
        // TODO: page
        if (path == null) {
            logger.debug("null or invalid type provided in query");
            return null;
        }
        try {
            URI url = new URI(baseUrl + path + "?countryCode=US&locale=en-US");
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestProperty("Accept", "application/vnd.api+json");
            connection.setRequestProperty("Authorization", "Bearer " + this.token);

            return WebTools.query(connection);
        } catch (URISyntaxException e) {
            logger.error("Exception while constructing tidal query", e);
            return null;
        } catch (QueryException | IOException e) {
            logger.warn("Exception while query tidal: " + e);
            return null;
        }
    }

    public LikedSongs getLikedSongs() {
        JsonNode response = this.queryList("userCollections/" + userID + "/relationships/tracks", 0);
        return null;
    }

    public Album getAlbum(String albumId, String albumName, String albumArtistName) {
        if (albumId == null) {
            logger.debug("null albumID provided in getAlbum");
            return null;
        }
        return null;
    }

    public ArrayList<Album> getAlbums() {
        JsonNode response = this.queryList("userCollections/" + userID + "/relationships/albums", 0);
        return null;
    }

    public Album getPlaylist(String playlistId, String playlistName) {
        if (playlistId == null) {
            logger.debug("null playlistId provided in getPlaylist");
            return null;
        }
        return null;
    }

    public ArrayList<Playlist> getPlaylists() {
        JsonNode response = this.queryList("userCollections/" + userID + "/relationships/playlists", 0);
        return null;
    }

}
