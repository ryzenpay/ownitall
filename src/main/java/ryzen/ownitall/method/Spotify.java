package ryzen.ownitall.method;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.requests.authorization.authorization_code.*;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumsTracksRequest;
import se.michaelthelin.spotify.requests.data.library.*;
import se.michaelthelin.spotify.requests.data.playlists.*;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchAlbumsRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.time.temporal.ChronoUnit;
import org.apache.hc.core5.http.ParseException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.ProgressBar;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.awt.Desktop;
import com.sun.net.httpserver.HttpServer;

/**
 * <p>
 * Spotify class.
 * </p>
 *
 * @author ryzen
 */
@Method.Export
@Method.Import
public class Spotify extends Method {
    private static final Logger logger = new Logger(Spotify.class);
    private static final Library library = Library.load();
    // read and write scope
    private final String scope = "playlist-read-private,playlist-read-collaborative,user-library-read,user-library-modify,playlist-modify-private,playlist-modify-public";
    private final int TIMEOUT = 20;
    private SpotifyApi spotifyApi;
    private String code;

    /**
     * Default spotify constructor asking for user input
     *
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public Spotify() throws MissingSettingException, AuthenticationException {
        if (Method.isCredentialsEmpty(Spotify.class)) {
            logger.debug("Empty spotify credentials");
            throw new MissingSettingException(Spotify.class);
        }
        try {
            this.spotifyApi = new SpotifyApi.Builder()
                    .setClientId(
                            Settings.spotifyClientID)
                    .setClientSecret(
                            Settings.spotifyClientSecret)
                    .setRedirectUri(new URI(
                            Settings.spotifyRedirectURL))
                    .build();
        } catch (URISyntaxException e) {
            throw new AuthenticationException(e.getMessage());
        }
        try {
            this.getCode();
            this.setToken();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while authenticating with Spotify");
            throw new AuthenticationException(e.getMessage());
        }
    }

    /**
     * Start an HTTP server to intercept the Spotify API code
     *
     * @throws java.lang.InterruptedException - if an error occurs
     */
    public void getCode() throws InterruptedException {
        AtomicReference<String> codeRef = new AtomicReference<>();
        AuthorizationCodeUriRequest authorizationCodeUriRequest = this.spotifyApi.authorizationCodeUri()
                .scope(scope)
                .show_dialog(Settings.spotifyShowdialog)
                .build();
        URI authUri = authorizationCodeUriRequest.execute();
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
            server.createContext("/method/spotify", exchange -> {
                logger.debug("request received on /method/spotify");
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
            logger.info("Awaiting response at http://localhost/method/spotify");
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
            System.out.println("Please open this url: " + authUri);
            System.out.print("Please provide the code found in response url: ");
            this.code = Input.request().getString();
        } else {
            this.code = codeRef.get();
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
        logger.info("Waiting " + TIMEOUT + " seconds for code or interrupt to manually provide");
        for (int i = 0; i < TIMEOUT; i++) {
            if (codeRef.get() != null) {
                break;
            }
            Thread.sleep(1000);
        }
    }

    private void nonInteractiveSetCode(URI url, AtomicReference<String> codeRef) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.toURL().openConnection();
        con.setConnectTimeout(TIMEOUT * 1000);
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

    /**
     * set the spotifyApi access token
     * 
     * @param code - the authentication code provided in the oauth
     */
    private void setToken() throws AuthenticationException {
        AuthorizationCodeRequest authorizationCodeRequest = this.spotifyApi.authorizationCode(this.code).build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
            this.spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            this.spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Exception logging in", e);
            throw new AuthenticationException(e.getMessage());
        }
    }

    /**
     * refresh the spotify api token incase of expiring
     *
     * @return - true if succesfully refreshed, false if not
     */
    public boolean refreshToken() {
        AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = this.spotifyApi.authorizationCodeRefresh()
                .build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Exception refreshing token", e);
            return false;
        }
    }

    /**
     * sleep function for when API limit is hit
     * 
     * @param seconds - long amount of seconds to sleep for
     * @throws InterruptedException - when user interrupts
     */
    private void sleep(long seconds) throws InterruptedException {
        logger.debug("Spotify timeout sleeping for: " + seconds + "s");
        long msec = seconds * 1000;
        Thread.sleep(msec);
    }

    /**
     * {@inheritDoc}
     *
     * Get all liked songs from current spotify account and add them to collection
     */
    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        int limit = Settings.spotifySongLimit;
        int offset = 0;
        boolean hasMore = true;
        try (ProgressBar pb = new ProgressBar("Spotify Liked", -1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (hasMore) {
                interruptionHandler.throwInterruption();
                GetUsersSavedTracksRequest getUsersSavedTracksRequest = this.spotifyApi.getUsersSavedTracks()
                        .limit(limit)
                        .offset(offset)
                        .build();
                try {
                    final Paging<SavedTrack> savedTrackPaging = getUsersSavedTracksRequest.execute();
                    SavedTrack[] items = savedTrackPaging.getItems();

                    if (items.length == 0) {
                        hasMore = false;
                    } else {
                        for (SavedTrack savedTrack : items) {
                            interruptionHandler.throwInterruption();
                            Track track = savedTrack.getTrack();
                            Song song = new Song(track.getName());
                            for (ArtistSimplified artist : track.getArtists()) {
                                song.addArtist(new Artist(artist.getName()));
                            }
                            song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                            song.addId("spotify", track.getId());
                            Image[] images = track.getAlbum().getImages();
                            if (images != null && images.length > 0) {
                                song.setCoverImage(images[images.length - 1].getUrl());
                            }
                            if (library != null) {
                                Song foundSong = library.getSong(song);
                                if (foundSong != null) {
                                    song = foundSong;
                                }
                            }
                            if (song != null) {
                                likedSongs.addSong(song);
                                pb.step(song.getName());
                            }
                        }
                        offset += limit;
                        if (limit > items.length) {
                            hasMore = false;
                        }
                    }
                } catch (TooManyRequestsException e) {
                    logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                    this.sleep(e.getRetryAfter());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    logger.error("Exception fetching liked songs", e);
                    hasMore = false;
                }
            }
        }
        return likedSongs;
    }

    /** {@inheritDoc} */
    @Override
    public void syncLikedSongs() throws InterruptedException {
        logger.debug("Getting spotify liked songs to remove mismatches");
        int limit = Settings.spotifySongLimit;
        int offset = 0;
        boolean hasMore = true;
        LikedSongs likedSongs = this.getLikedSongs();
        if (likedSongs != null && !likedSongs.isEmpty()) {
            likedSongs.removeSongs(Collection.getLikedSongs().getSongs());
            ArrayList<String> songIds = new ArrayList<>();
            for (Song song : likedSongs.getSongs()) {
                String id = this.getTrackId(song);
                if (id != null) {
                    songIds.add(id);
                }
            }
            if (songIds.isEmpty()) {
                return;
            }
            try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
                while (hasMore) {
                    interruptionHandler.throwInterruption();
                    String[] currentIds = songIds.subList(offset,
                            Math.min(offset + limit, songIds.size())).toArray(new String[0]);
                    RemoveUsersSavedTracksRequest removeUsersSavedTracksRequest = spotifyApi
                            .removeUsersSavedTracks(currentIds)
                            .build();
                    try {
                        removeUsersSavedTracksRequest.execute();
                        logger.debug("deleted liked songs (" + currentIds.length + "): " + currentIds.toString());
                        offset += limit;
                        if (offset >= currentIds.length) {
                            hasMore = false;
                        }
                    } catch (TooManyRequestsException e) {
                        logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                        this.sleep(e.getRetryAfter());
                    } catch (IOException | SpotifyWebApiException | ParseException e) {
                        logger.error("Exception adding users saved tracks", e);
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        likedSongs.addSongs(Collection.getLikedSongs().getSongs());
        LikedSongs currLikedSongs = this.getLikedSongs();
        if (currLikedSongs != null) {
            likedSongs.removeSongs(currLikedSongs.getSongs());
        }
        ArrayList<String> songIds = new ArrayList<>();
        for (Song song : likedSongs.getSongs()) {
            String id = this.getTrackId(song);
            if (id != null) {
                songIds.add(id);
            }
        }
        if (songIds.isEmpty()) {
            logger.debug("No liked songs in collection");
            return;
        }
        int limit = Settings.spotifySongLimit;
        int offset = 0;
        boolean hasMore = true;
        try (ProgressBar pb = new ProgressBar("Liked Songs", songIds.size());
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (hasMore) {
                interruptionHandler.throwInterruption();
                String[] currentIds = songIds.subList(offset,
                        Math.min(offset + limit, songIds.size())).toArray(new String[0]);
                SaveTracksForUserRequest saveTracksForUserRequest = spotifyApi
                        .saveTracksForUser(
                                currentIds)
                        .build();
                try {
                    saveTracksForUserRequest.execute();
                    pb.step(currentIds.length);
                    logger.debug("added liked songs (" + currentIds.length + "): " + currentIds.toString());
                    offset += limit;
                    if (offset > currentIds.length) {
                        hasMore = false;
                    }
                } catch (TooManyRequestsException e) {
                    logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                    this.sleep(e.getRetryAfter());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    logger.error("Exception adding users saved tracks", e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * get all current user saved albums and add them to collection
     */
    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        ArrayList<Album> albums = new ArrayList<>();
        int limit = Settings.spotifyAlbumLimit;
        int offset = 0;
        boolean hasMore = true;
        try (ProgressBar pb = new ProgressBar("Spotify Albums", -1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (hasMore) {
                interruptionHandler.throwInterruption();
                GetCurrentUsersSavedAlbumsRequest getCurrentUsersSavedAlbumsRequest = this.spotifyApi
                        .getCurrentUsersSavedAlbums()
                        .limit(limit)
                        .offset(offset)
                        .build();
                try {
                    final Paging<SavedAlbum> savedAlbumPaging = getCurrentUsersSavedAlbumsRequest.execute();
                    SavedAlbum[] items = savedAlbumPaging.getItems();

                    if (items.length == 0) {
                        hasMore = false;
                    } else {
                        for (SavedAlbum savedAlbum : items) {
                            interruptionHandler.throwInterruption();
                            pb.step(savedAlbum.getAlbum().getId());
                            Album album = this.getAlbum(savedAlbum.getAlbum().getId(), savedAlbum.getAlbum().getName(),
                                    savedAlbum.getAlbum().getArtists()[0].getName());
                            if (album != null) {
                                albums.add(album);
                            }
                        }
                        offset += limit;
                        if (limit > items.length) {
                            hasMore = false;
                        }
                    }
                } catch (TooManyRequestsException e) {
                    logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                    this.sleep(e.getRetryAfter());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    logger.error("Exception getting albums", e);
                    hasMore = false;
                }
            }
        }
        return albums;
    }

    /** {@inheritDoc} */
    @Override
    public Album getAlbum(String albumId, String albumName, String artistName) throws InterruptedException {
        if (albumId == null || albumName == null) {
            logger.debug("Null albumID or AlbumName provided in getAlbum");
            return null;
        }
        Album album = new Album(albumName);
        if (artistName != null) {
            album.addArtist(new Artist(artistName));
        }
        album.addId("spotify", albumId);
        if (library != null) {
            Album foundAlbum = library.getAlbum(album);
            if (foundAlbum != null) {
                album = foundAlbum;
            }
        }
        if (album != null) {
            if (album.isEmpty()) {
                ArrayList<Song> songs = this.getAlbumSongs(albumId);
                if (songs != null && !songs.isEmpty()) {
                    album.addSongs(songs);
                }
            }
        }
        return album;
    }

    /**
     * get all songs in an album
     *
     * @param albumId - spotify album id
     * @return - linkedhashset of songs
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public ArrayList<Song> getAlbumSongs(String albumId) throws InterruptedException {
        if (albumId == null) {
            logger.debug("null albumID provided in getAlbumSongs");
            return null;
        }
        int offset = 0;
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            ArrayList<Song> songs = new ArrayList<>();
            int limit = Settings.spotifySongLimit;
            boolean hasMore = true;
            while (hasMore) {
                interruptionHandler.throwInterruption();
                GetAlbumsTracksRequest getAlbumsTracksRequest = this.spotifyApi.getAlbumsTracks(albumId)
                        .limit(limit)
                        .offset(offset)
                        .build();
                try {
                    Paging<TrackSimplified> trackSimplifiedPaging = getAlbumsTracksRequest.execute();
                    TrackSimplified[] items = trackSimplifiedPaging.getItems();
                    if (items.length == 0) {
                        hasMore = false;
                    } else {
                        for (TrackSimplified track : items) {
                            interruptionHandler.throwInterruption();
                            Song song = new Song(track.getName());
                            for (ArtistSimplified artist : track.getArtists()) {
                                song.addArtist(new Artist(artist.getName()));
                            }
                            song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                            song.addId("spotify", track.getId());
                            if (library != null) {
                                Song foundSong = library.getSong(song);
                                if (foundSong != null) {
                                    song = foundSong;
                                }
                            }
                            if (song != null) {
                                songs.add(song);
                            }
                        }
                    }
                    offset += limit;
                    if (limit > items.length) {
                        hasMore = false;
                    }
                } catch (TooManyRequestsException e) {
                    logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                    this.sleep(e.getRetryAfter());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    logger.error("Exception fetching songs for album: " + albumId + "", e);
                    hasMore = false;
                }
            }
            return songs;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbums() throws InterruptedException {
        ArrayList<Album> albums = this.getAlbums();
        if (albums != null && !albums.isEmpty()) {
            albums.removeAll(Collection.getAlbums());
            ArrayList<String> albumIds = new ArrayList<>();
            for (Album album : albums) {
                String id = this.getAlbumId(album);
                if (id != null) {
                    albumIds.add(id);
                }
            }
            if (albumIds.isEmpty()) {
                return;
            }
            int limit = Settings.spotifyAlbumLimit;
            int offset = 0;
            boolean hasMore = true;
            try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
                while (hasMore) {
                    interruptionHandler.throwInterruption();
                    String[] currentIds = albumIds.subList(offset,
                            Math.min(offset + limit, albumIds.size())).toArray(new String[0]);
                    RemoveAlbumsForCurrentUserRequest removeAlbumsForCurrentUserRequest = spotifyApi
                            .removeAlbumsForCurrentUser(currentIds)
                            .build();
                    try {
                        removeAlbumsForCurrentUserRequest.execute();
                        logger.debug("removed albums (" + currentIds.length + "): " + currentIds.toString());
                        offset += limit;
                        if (limit > currentIds.length) {
                            hasMore = false;
                        }
                    } catch (TooManyRequestsException e) {
                        logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                        this.sleep(e.getRetryAfter());
                    } catch (IOException | SpotifyWebApiException | ParseException e) {
                        logger.error("Exception adding users saved tracks", e);
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAlbums() throws InterruptedException {
        ArrayList<Album> albums = Collection.getAlbums();
        ArrayList<String> albumIds = new ArrayList<>();
        for (Album album : albums) {
            String id = this.getAlbumId(album);
            if (id != null) {
                albumIds.add(id);
            }
        }
        if (albumIds.isEmpty()) {
            logger.debug("No Saved albums in collection");
            return;
        }
        int limit = Settings.spotifyAlbumLimit;
        int offset = 0;
        boolean hasMore = true;
        try (ProgressBar pb = new ProgressBar("Spotify Albums", albumIds.size());
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (hasMore) {
                interruptionHandler.throwInterruption();
                String[] currentAlbumIds = albumIds.subList(offset,
                        Math.min(offset + limit, albumIds.size())).toArray(new String[0]);
                SaveAlbumsForCurrentUserRequest saveAlbumsForCurrentUserRequest = spotifyApi
                        .saveAlbumsForCurrentUser(currentAlbumIds)
                        .build();
                try {
                    pb.step(currentAlbumIds.length);
                    saveAlbumsForCurrentUserRequest.execute();
                    offset += limit;
                    if (limit > currentAlbumIds.length) {
                        hasMore = false;
                    }
                } catch (TooManyRequestsException e) {
                    logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                    this.sleep(e.getRetryAfter());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    logger.error("Exception adding users Albums", e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * get all playlists contributed by current spotify user and add them to
     * collection
     */
    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = new ArrayList<>();
        int limit = Settings.spotifyPlaylistLimit;
        int offset = 0;
        boolean hasMore = true;
        try (ProgressBar pb = new ProgressBar("Spotify Playlists", -1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (hasMore) {
                interruptionHandler.throwInterruption();
                GetListOfCurrentUsersPlaylistsRequest getListOfCurrentUsersPlaylistsRequest = this.spotifyApi
                        .getListOfCurrentUsersPlaylists()
                        .limit(limit)
                        .offset(offset)
                        .build();

                try {
                    final Paging<PlaylistSimplified> playlistSimplifiedPaging = getListOfCurrentUsersPlaylistsRequest
                            .execute();
                    PlaylistSimplified[] items = playlistSimplifiedPaging.getItems();

                    if (items.length == 0) {
                        hasMore = false;
                    } else {
                        for (PlaylistSimplified spotifyPlaylist : items) {
                            interruptionHandler.throwInterruption();
                            String coverImageUrl = null;
                            Image[] images = spotifyPlaylist.getImages();
                            if (images != null && images.length > 0) {
                                coverImageUrl = images[images.length - 1].getUrl();
                            }
                            pb.step(spotifyPlaylist.getName());
                            Playlist playlist = this.getPlaylist(spotifyPlaylist.getId(),
                                    spotifyPlaylist.getName());
                            if (playlist != null) {
                                playlist.setCoverImage(coverImageUrl);
                                playlists.add(playlist);
                            }
                        }
                    }
                    offset += limit;
                    if (limit > items.length) {
                        hasMore = false;
                    }
                } catch (TooManyRequestsException e) {
                    logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                    this.sleep(e.getRetryAfter());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    logger.error("Exception fetching playlists", e);
                    hasMore = false;
                }
            }
        }
        return playlists;
    }

    /** {@inheritDoc} */
    @Override
    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        if (playlistId == null || playlistName == null) {
            logger.debug("null playlistID or playlistName provided in getPlaylist");
            return null;
        }
        Playlist playlist = new Playlist(playlistName);
        ArrayList<Song> songs = this.getPlaylistSongs(playlistId);
        if (songs != null && !songs.isEmpty()) {
            playlist.addSongs(songs);
            playlist.addId("spotify", playlistId);
            return playlist;
        }
        return null;
    }

    /**
     * get all songs from a playlist
     *
     * @param playlistId - spotify ID for a playlist
     * @return - constructed array of Songs
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public ArrayList<Song> getPlaylistSongs(String playlistId) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("null playlistID provided in getPlaylistSongs");
            return null;
        }
        ArrayList<Song> songs = new ArrayList<>();
        int limit = Settings.spotifySongLimit;
        int offset = 0;
        boolean hasMore = true;
        try (ProgressBar pb = new ProgressBar(playlistId, -1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (hasMore) {
                interruptionHandler.throwInterruption();
                GetPlaylistsItemsRequest getPlaylistsItemsRequest = this.spotifyApi.getPlaylistsItems(playlistId)
                        .limit(limit)
                        .offset(offset)
                        .build();
                try {
                    final Paging<PlaylistTrack> playlistTrackPaging = getPlaylistsItemsRequest.execute();
                    PlaylistTrack[] items = playlistTrackPaging.getItems();
                    if (items.length == 0) {
                        hasMore = false;
                    } else {
                        for (PlaylistTrack playlistTrack : items) {
                            interruptionHandler.throwInterruption();
                            Song song = null;
                            if (playlistTrack.getTrack() instanceof Track) {
                                Track track = (Track) playlistTrack.getTrack();
                                pb.step(track.getName());
                                song = new Song(track.getName());
                                for (ArtistSimplified artist : track.getArtists()) {
                                    song.addArtist(new Artist(artist.getName()));
                                }
                                song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                                Image[] images = track.getAlbum().getImages();
                                if (images != null && images.length > 0) {
                                    song.setCoverImage(images[images.length - 1].getUrl());
                                }
                                song.addId("spotify", track.getId());
                            } else if (playlistTrack.getTrack() instanceof Episode) {
                                Episode episode = (Episode) playlistTrack.getTrack();
                                pb.step(episode.getName());
                                song = new Song(episode.getName());
                                song.setDuration(episode.getDurationMs(), ChronoUnit.MILLIS);
                                Image[] images = episode.getImages();
                                if (images != null && images.length > 0) {
                                    song.setCoverImage(images[images.length - 1].getUrl());
                                }
                                song.addId("spotify", episode.getId());
                            } else {
                                logger.debug("skipped non track '" + playlistTrack.toString() + "' in playlist: "
                                        + playlistId);
                                logger.info("Skipping non-Track in playlist: " + playlistId);
                                continue;
                            }
                            if (library != null) {
                                Song foundSong = library.getSong(song);
                                if (foundSong != null) {
                                    song = foundSong;
                                }
                            }
                            if (song != null) {
                                songs.add(song);
                            }
                        }
                    }
                    offset += limit;
                    if (limit > items.length) {
                        hasMore = false;
                    }
                } catch (TooManyRequestsException e) {
                    logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                    this.sleep(e.getRetryAfter());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    logger.error("Exception fetching playlist tracks", e);
                    hasMore = false;
                }
            }
            return songs;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylists() throws InterruptedException {
        logger.debug("Getting spotify playlists to remove mismatches");
        ArrayList<Playlist> playlists = this.getPlaylists();
        if (playlists != null && !playlists.isEmpty()) {
            playlists.removeAll(Collection.getPlaylists());
            for (Playlist playlist : playlists) {
                // currently not suported by spotify wrapper to delete playlists
                logger.warn("Playlist '" + playlist.getName()
                        + "' was found on spotify but not in collection, delete it to stay up to date");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = Collection.getPlaylists();
        try (ProgressBar pb = new ProgressBar("Uploading Playlists", playlists.size())) {
            for (Playlist playlist : playlists) {
                pb.step(playlist.getName());
                this.uploadPlaylist(playlist);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in playlistCleanUp");
            return;
        }
        logger.debug("Getting spotify playlist '" + playlist.getName() + "' to remove mismatches");
        String playlistId = this.getPlaylistId(playlist);
        if (playlistId != null) {
            ArrayList<Song> songs = this.getPlaylistSongs(playlistId);
            if (songs != null && !songs.isEmpty()) {
                // only delete one when there are duplicate songs
                for (int i = 0; i < songs.size(); i++) {
                    if (playlist.contains(songs.get(i))) {
                        songs.remove(i);
                    }
                }
                // songs.removeAll(playlist.getSongs());
                ArrayList<String> songIds = new ArrayList<>();
                for (Song song : songs) {
                    String id = this.getTrackId(song);
                    if (id != null) {
                        songIds.add(id);
                    }
                }
                if (songIds.isEmpty()) {
                    return;
                }
                int limit = Settings.spotifySongLimit;
                int offset = 0;
                boolean hasMore = true;
                try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
                    while (hasMore) {
                        interruptionHandler.throwInterruption();
                        String[] currentIds = songIds.subList(offset,
                                Math.min(offset + limit, songIds.size())).toArray(new String[0]);
                        JsonArray tracks = new JsonArray();
                        for (String id : currentIds) {
                            JsonObject track = new JsonObject();
                            track.addProperty("uri", "spotify:track:" + id);
                            tracks.add(track);
                        }
                        RemoveItemsFromPlaylistRequest removeItemsFromPlaylistRequest = spotifyApi
                                .removeItemsFromPlaylist(playlistId, tracks)
                                .build();
                        try {
                            removeItemsFromPlaylistRequest.execute();
                            logger.debug("removed playlist '" + playlist.getName() + "'' songs (" + currentIds.length
                                    + "): " + currentIds.toString());
                            offset += limit;
                            if (limit > currentIds.length) {
                                hasMore = false;
                            }
                        } catch (TooManyRequestsException e) {
                            logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                            this.sleep(e.getRetryAfter());
                        } catch (IOException | SpotifyWebApiException | ParseException e) {
                            logger.error("Exception adding users saved tracks", e);
                        }
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in uploadPlaylist");
            return;
        }
        String playlistId = this.getPlaylistId(playlist);
        ArrayList<Song> currentSongs = new ArrayList<>();
        if (playlistId != null) {
            currentSongs = this.getPlaylistSongs(playlistId);
        }
        ArrayList<Song> songs = new ArrayList<>(playlist.getSongs());
        if (currentSongs.isEmpty()) {
            try {
                GetCurrentUsersProfileRequest getCurrentUsersProfileRequest = spotifyApi.getCurrentUsersProfile()
                        .build();
                User user = getCurrentUsersProfileRequest.execute();
                CreatePlaylistRequest createPlaylistRequest = spotifyApi
                        .createPlaylist(user.getId(), playlist.getName())
                        .public_(false)
                        .build();
                playlistId = createPlaylistRequest.execute().getId();
                logger.debug("Created new playlist: " + playlist.getName());
                playlist.addId("spotify", playlistId);
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.debug("Exception creating user playlist");
            }
        } else {
            // filter out the existing playlist songs
            songs.removeAll(currentSongs);
        }
        ArrayList<String> songUris = new ArrayList<>();
        for (Song song : songs) {
            songUris.add("spotify:track:" + this.getTrackId(song));
        }
        if (songUris.isEmpty()) {
            logger.debug("Playlist '" + playlist.toString() + "' is empty / no update");
            return;
        }
        int limit = Settings.spotifySongLimit;
        int offset = 0;
        boolean hasMore = true;
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (hasMore) {
                interruptionHandler.throwInterruption();
                String[] currentSongUris = songUris.subList(offset,
                        Math.min(offset + limit, songUris.size())).toArray(new String[0]);
                AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi
                        .addItemsToPlaylist(playlistId, currentSongUris)
                        .build();
                try {
                    addItemsToPlaylistRequest.execute();
                    offset += limit;
                    if (limit > currentSongUris.length) {
                        hasMore = false;
                    }
                } catch (TooManyRequestsException e) {
                    logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                    this.sleep(e.getRetryAfter());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    logger.error("Exception adding users saved tracks", e);
                }
            }
        }
    }

    /**
     * <p>
     * getTrackId.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String getTrackId(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("null song provided in getTrackId");
            return null;
        }
        if (song.getId("spotify") != null) {
            return song.getId("spotify");
        }
        SearchTracksRequest searchTracksRequest = this.spotifyApi.searchTracks(song.toString())
                .limit(1)
                .build();
        while (true) {
            try {
                Track[] items = searchTracksRequest.execute().getItems();
                if (items.length == 0) {
                    logger.debug("Song '" + song.toString() + "' not found");
                    return null;
                }
                song.addId("spotify", items[0].getId());
                return items[0].getId();
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception searching for song", e);
                return null;
            }
        }
    }

    /**
     * <p>
     * getPlaylistId.
     * </p>
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String getPlaylistId(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in getPlaylistId");
            return null;
        }
        // do not use cached playlist id because the playlist might have been deleted
        int limit = Settings.spotifyPlaylistLimit;
        int offset = 0;
        boolean hasMore = true;
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (hasMore) {
                interruptionHandler.throwInterruption();
                GetListOfCurrentUsersPlaylistsRequest getListOfCurrentUsersPlaylistsRequest = this.spotifyApi
                        .getListOfCurrentUsersPlaylists()
                        .limit(limit)
                        .offset(offset)
                        .build();
                try {
                    final Paging<PlaylistSimplified> playlistSimplifiedPaging = getListOfCurrentUsersPlaylistsRequest
                            .execute();
                    PlaylistSimplified[] items = playlistSimplifiedPaging.getItems();

                    if (items.length == 0) {
                        hasMore = false;
                    } else {
                        for (PlaylistSimplified spotifyPlaylist : items) {
                            interruptionHandler.throwInterruption();
                            if (spotifyPlaylist.getName().equalsIgnoreCase(playlist.getName())) {
                                playlist.addId("spotify", spotifyPlaylist.getId());
                                return spotifyPlaylist.getId();
                            }
                        }
                    }
                    offset += limit;
                    if (limit > items.length) {
                        hasMore = false;
                    }
                } catch (TooManyRequestsException e) {
                    logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                    this.sleep(e.getRetryAfter());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    logger.error("Exception fetching playlists", e);
                    hasMore = false;
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * getAlbumId.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String getAlbumId(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided in getAlbumId");
            return null;
        }
        if (album.getId("spotify") != null) {
            return album.getId("spotify");
        }
        SearchAlbumsRequest searchAlbumsRequest = spotifyApi.searchAlbums(album.toString())
                .limit(1)
                .build();
        while (true) {
            try {
                AlbumSimplified[] items = searchAlbumsRequest.execute().getItems();
                if (items.length == 0) {
                    logger.debug("Album '" + album.toString() + "' not found");
                    return null;
                }
                album.addId("spotify", items[0].getId());
                return items[0].getId();
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception searching for Album", e);
                return null;
            }
        }
    }
}
