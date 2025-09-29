package ryzen.ownitall.method;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
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
//TODO: export and sync
//TODO: progress bars
public class Tidal implements Import {
    private static final Logger logger = new Logger(Tidal.class);
    private String token;
    private String userID;
    private static final String baseUrl = "https://openapi.tidal.com/v2";
    private static final String scope = "collection.read%20collection.write%20playlists.read%20playlists.read";
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
    // TODO: centralize in webtools + share with spotify
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
            URI uri = new URI(baseUrl + "/users/me");
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

    private JsonNode query(String path, String pageCursor, String include) {
        String flags = "?countryCode=US&locale=en-US";
        if (pageCursor != null) {
            flags += "&page[cursor]=" + pageCursor;
        }
        if (include != null) {
            flags += "&include=" + include;
        }
        try {
            URI url = new URI(baseUrl + path + flags);
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestProperty("Accept", "application/vnd.api+json");
            connection.setRequestProperty("Authorization", "Bearer " + this.token);

            WebTools.queryPacer(500);
            return WebTools.query(connection);
        } catch (URISyntaxException e) {
            logger.error("Exception while constructing tidal query", e);
            return null;
        } catch (QueryException | IOException | InterruptedException e) {
            logger.warn("Exception while querying tidal: " + e);
            return null;
        }
    }

    private ArrayList<Artist> getRelatedArtists(JsonNode relationships) {
        if (relationships == null) {
            logger.debug("null relationships provided to getRelatedArtists");
            return null;
        }
        ArrayList<Artist> artists = new ArrayList<>();
        JsonNode artistsNode = relationships.path("artists");
        if (artistsNode != null) {
            String path = artistsNode.path("links").path("self").asText();
            if (path == null || path.isEmpty()) {
                logger.debug("no artists present in relationship");
                return null;
            }
            JsonNode response = this.query(path.split("\\?")[0], null, "artists");
            if (response == null) {
                logger.debug("null response receieved in getRelatedArtists");
                return null;
            }
            JsonNode artistItems = response.path("included");
            if (artistItems != null && artistItems.isArray()) {
                for (JsonNode artistItem : artistItems) {
                    String id = artistItem.path("id").asText();
                    JsonNode attributes = artistItem.path("attributes");
                    String name = attributes.path("name").asText();
                    Artist artist = new Artist(name);
                    artist.addId("tidal", id);
                    artists.add(artist);
                }
            } else {
                logger.debug("response in getRelatedArtists missing artists");
                return null;
            }
        } else {
            logger.debug("relationship in getRelatedArtists does not include artists");
            return null;
        }
        return artists;
    }

    private String getRelatedCoverArt(JsonNode relationships) {
        if (relationships == null) {
            logger.debug("null relationships provided to getRelatedCoverArt");
            return null;
        }
        JsonNode coverArtNode = relationships.path("coverArt");
        if (coverArtNode != null) {
            String path = coverArtNode.path("links").path("self").asText();
            if (path == null || path.isEmpty()) {
                logger.debug("no coverart present in relationship");
                return null;
            }
            JsonNode response = this.query(path.split("\\?")[0], null, "coverArt");
            if (response == null) {
                logger.debug("null response received in getRelatedCoverArt");
                return null;
            }
            JsonNode coverArtItems = response.path("included").path("attributes").path("files");
            if (coverArtItems != null && coverArtItems.isArray()) {
                return coverArtItems.get(0).path("href").asText();
            } else {
                logger.debug("relatedCoverArt response is missing cover arts");
                return null;
            }
        } else {
            logger.debug("relationship in getRelatedCoverArt does not include cover art");
            return null;
        }
    }

    // TODO: get album
    private Song getSong(JsonNode songItem) {
        if (songItem == null) {
            logger.debug("null songItem provided in getSong");
            return null;
        }
        String id = songItem.path("id").asText();
        JsonNode attributes = songItem.path("attributes");
        String title = attributes.path("title").asText();
        Song song = new Song(title);
        song.addId("tidal", id);
        Duration duration = Duration.parse(attributes.path("duration").asText());
        if (duration != null) {
            song.setDuration(duration);
        }
        JsonNode relationships = songItem.path("relationships");
        ArrayList<Artist> artists = this.getRelatedArtists(relationships);
        if (artists != null) {
            song.addArtists(artists);
        }
        String coverArt = this.getRelatedCoverArt(relationships);
        if (coverArt != null) {
            song.setCoverImage(coverArt);
        }
        return song;
    }

    private ArrayList<Song> getRelatedSongs(JsonNode relationships) {
        if (relationships == null) {
            logger.debug("null relationships to getRelatedItems");
            return null;
        }
        ArrayList<Song> songs = new ArrayList<>();
        JsonNode itemsNode = relationships.path("items");
        if (itemsNode != null) {
            String path = itemsNode.path("links").path("self").asText();
            String pageCursor = null;
            while (true) {
                JsonNode response = this.query(path.split("\\?")[0], pageCursor, "items");
                if (response == null) {
                    logger.debug("null response received in getRelatedSongs");
                    return null;
                }
                JsonNode songItems = response.path("included");
                if (songItems != null && songItems.isArray()) {
                    for (JsonNode songItem : songItems) {
                        Song song = this.getSong(songItem);
                        if (song != null) {
                            songs.add(song);
                        }
                    }
                } else {
                    logger.debug("missing songs in getRelatedSongs");
                }
                JsonNode links = response.path("links");
                if (links.has("next")) {
                    pageCursor = links.path("meta").path("nextCursor").asText();
                } else {
                    break;
                }
            }
        } else {
            logger.debug("relationship in getRelatedSongs does not include items");
            return null;
        }
        return songs;
    }

    public LikedSongs getLikedSongs() {
        LikedSongs likedSongs = new LikedSongs();
        String pageCursor = null;
        while (true) {
            JsonNode response = this.query("/userCollections/" + userID + "/relationships/tracks", pageCursor,
                    "tracks");
            if (response == null) {
                logger.warn("null response received in tidal likedsongs query");
                break;
            }
            JsonNode songs = response.path("included");
            if (songs != null && songs.isArray()) {
                for (JsonNode songItem : songs) {
                    Song song = this.getSong(songItem);
                    if (song != null) {
                        likedSongs.addSong(song);
                    }
                }
            } else {
                logger.debug("No songs in tidal likedsongs query: " + response);
            }
            JsonNode links = response.path("links");
            if (links.has("next")) {
                pageCursor = links.path("meta").path("nextCursor").asText();
            } else {
                break;
            }
        }
        return likedSongs;
    }

    public Album getAlbum(String albumId, String albumName, String albumArtistName) {
        if (albumId == null) {
            logger.debug("null albumID provided in getAlbum");
            return null;
        }
        JsonNode response = this.query("/albums/" + albumId, null, "artists,coverArt,items");
        if (response == null) {
            logger.debug("null response received in getAlbum");
            return null;
        }
        JsonNode albumItems = response.path("included");
        if (albumItems != null && albumItems.isArray()) {
            JsonNode albumItem = albumItems.get(0);
            String id = albumItem.path("id").asText();
            JsonNode attributes = albumItem.path("attributes");
            String title = attributes.path("title").asText();
            Album album = new Album(title);
            album.addId("tidal", id);
            JsonNode relationships = albumItem.path("relationships");
            ArrayList<Artist> artists = this.getRelatedArtists(relationships);
            if (artists != null) {
                album.addArtists(artists);
            }
            String coverArt = this.getRelatedCoverArt(relationships);
            if (coverArt != null) {
                album.setCoverImage(coverArt);
            }
            ArrayList<Song> songs = this.getRelatedSongs(relationships);
            if (songs != null) {
                album.addSongs(songs);
            }
            return album;
        } else {
            logger.debug("getAlbum response is missing albums");
            return null;
        }
    }

    public ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<>();
        String pageCursor = null;
        while (true) {
            JsonNode response = this.query("/userCollections/" + userID + "/relationships/albums", pageCursor,
                    "albums");
            if (response == null) {
                logger.debug("null response received in getAlbums");
                break;
            }
            JsonNode albumEntries = response.path("included");
            if (albumEntries != null && albumEntries.isArray()) {
                for (JsonNode albumEntry : albumEntries) {
                    String id = albumEntry.path("id").asText();
                    JsonNode attributes = albumEntry.path("attributes");
                    String title = attributes.path("title").asText();
                    Album album = this.getAlbum(id, title, null);
                    if (album != null) {
                        albums.add(album);
                    }
                }
            } else {
                logger.debug("No albums in tidal albums query: " + response);
            }
            JsonNode links = response.path("links");
            if (links.has("next")) {
                pageCursor = links.path("meta").path("nextCursor").asText();
            } else {
                break;
            }
        }
        return albums;
    }

    public Playlist getPlaylist(String playlistId, String playlistName) {
        if (playlistId == null) {
            logger.debug("null playlistId provided in getPlaylist");
            return null;
        }
        JsonNode response = this.query("/playlists/" + playlistId, null, "coverArt,items");
        if (response == null) {
            logger.debug("null response received in getPlaylist");
            return null;
        }
        JsonNode playlistItems = response.path("included");
        if (playlistItems != null && playlistItems.isArray()) {
            JsonNode playlistItem = playlistItems.get(0);
            String id = playlistItem.path("id").asText();
            JsonNode attributes = playlistItem.path("attributes");
            String name = attributes.path("name").asText();
            Playlist playlist = new Playlist(name);
            playlist.addId("tidal", id);
            JsonNode relationships = playlistItem.path("relationships");
            String coverArt = this.getRelatedCoverArt(relationships);
            if (coverArt != null) {
                playlist.setCoverImage(coverArt);
            }
            ArrayList<Song> songs = this.getRelatedSongs(relationships);
            if (songs != null) {
                playlist.addSongs(songs);
            }
            return playlist;
        } else {
            logger.debug("getPlaylist response is missing playlists");
            return null;
        }
    }

    public ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<>();
        String pageCursor = null;
        while (true) {
            JsonNode response = this.query("/userCollections/" + userID + "/relationships/playlists", pageCursor,
                    "playlists");
            if (response == null) {
                logger.debug("null response received in getPlaylists");
                break;
            }
            JsonNode playlistEntries = response.path("included");
            if (playlistEntries != null && playlistEntries.isArray()) {
                for (JsonNode playlistEntry : playlistEntries) {
                    String id = playlistEntry.path("id").asText();
                    JsonNode attributes = playlistEntry.path("attributes");
                    String name = attributes.path("name").asText();
                    Playlist playlist = this.getPlaylist(id, name);
                    if (playlist != null) {
                        playlists.add(playlist);
                    }
                }
            } else {
                logger.debug("No playlists in tidal playlists query: " + response);
            }
            JsonNode links = response.path("links");
            if (links.has("next")) {
                pageCursor = links.path("meta").path("nextCursor").asText();
            } else {
                break;
            }
        }
        return playlists;
    }
}
