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
import java.time.temporal.ChronoUnit;
import org.apache.hc.core5.http.ParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.ProgressBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.awt.Desktop;

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
    private static final Logger logger = LogManager.getLogger(Spotify.class);
    private static final Library library = Library.load();
    // read and write scope
    private final String scope = "playlist-read-private,playlist-read-collaborative,user-library-read,user-library-modify,playlist-modify-private,playlist-modify-public";
    private SpotifyApi spotifyApi;
    private String code;

    /**
     * Default spotify constructor asking for user input
     *
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public Spotify() throws InterruptedException {
        if (Method.isCredentialsEmpty(Spotify.class)) {
            logger.debug("Empty spotify credentials");
            throw new InterruptedException("empty spotify credentials");
        }
        try {
            this.spotifyApi = new SpotifyApi.Builder()
                    .setClientId(
                            Credentials.spotifyClientID)
                    .setClientSecret(
                            Credentials.spotifyClientSecret)
                    .setRedirectUri(new URI(
                            Credentials.spotifyRedirectURL))
                    .build();
        } catch (URISyntaxException e) {
            throw new InterruptedException(e.getMessage());
        }
        this.requestCode();
        this.setToken();
    }

    /**
     * obtaining the oauth code to set the token
     * 
     * @return - the oauth code with permissions
     * @throws InterruptedException - when user interrupts
     */
    // TODO: make it work without an actual website (ex: .local.lan)
    private void requestCode() throws InterruptedException {
        AuthorizationCodeUriRequest authorizationCodeUriRequest = this.spotifyApi.authorizationCodeUri()
                .scope(scope)
                .show_dialog(Settings.spotifyShowdialog)
                .build();
        URI auth_uri = authorizationCodeUriRequest.execute();
        // Open the default browser with the authorization URL
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(auth_uri);
                this.startLocalServer();
            } catch (IOException e) {
                logger.error("Exception opening web browser", e);
            }
        } else {
            System.out.println("Open this link:\n" + auth_uri.toString());
            System.out.print("Code it presents (in url): ");
            this.code = Input.request().getString();
        }
    }

    /**
     * set the spotifyApi access token
     * 
     * @param code - the authentication code provided in the oauth
     */
    private void setToken() throws InterruptedException {
        AuthorizationCodeRequest authorizationCodeRequest = this.spotifyApi.authorizationCode(this.code).build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
            this.spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            this.spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Exception logging in", e);
            throw new InterruptedException(e.getMessage());
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
     * start temporary local server to "intercept" spotify api code
     *
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public void startLocalServer() throws InterruptedException {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            logger.info("Waiting for the authorization code...");
            Socket clientSocket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            StringBuilder request = new StringBuilder();
            while ((inputLine = in.readLine()) != null && !inputLine.isEmpty()) {
                request.append(inputLine).append("\n");
                if (inputLine.contains("code=")) {
                    break; // Stop reading after we've found the code
                }
            }

            String code = extractCodeFromRequest(request.toString());
            if (code != null) {
                this.code = code;
                logger.info("Authorization code received");
                // (frame.toFront(); frame.repaint();)
                sendResponse(clientSocket, 200, "Authorization code received successfully.");
            } else {
                logger.error("Failed to retrieve authorization code. Request: " + request.toString());
                sendResponse(clientSocket, 404, "Failed to retrieve authorization code.");
                try {
                    System.out.println("Code it provides (in url)");
                    this.code = Input.request().getString();
                } catch (InterruptedException e) {
                    logger.debug("Interrupted while getting code (failed getting from browser)", e);
                }
            }

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Code it provides (in url)");
            this.code = Input.request().getString();
        }
    }

    /**
     * extract code from the "intercepted" spotify api
     * 
     * @param request - raw response data
     * @return - String code
     */
    private String extractCodeFromRequest(String request) {
        if (request == null) {
            logger.debug("null request passed in extractCodeFromRequest");
            return null;
        }
        int codeIndex = request.indexOf("code=");
        if (codeIndex != -1) {
            int endIndex = request.indexOf("&", codeIndex);
            if (endIndex == -1) {
                endIndex = request.indexOf(" ", codeIndex);
            }
            if (endIndex == -1) {
                endIndex = request.length();
            }
            return request.substring(codeIndex + 5, endIndex);
        }
        return null;
    }

    /**
     * send response to website if received code
     * 
     * @param clientSocket - socket to send through
     * @param statusCode   - additional success message (200 = success, 404 =
     *                     failed)
     * @param message      - additional message to present on site
     * @throws IOException
     */
    private void sendResponse(Socket clientSocket, int statusCode, String message) throws IOException {
        String statusLine = "HTTP/1.1 " + statusCode + " " + (statusCode == 200 ? "OK" : "Not Found") + "\r\n";
        String contentType = "Content-Type: text/plain\r\n";
        String contentLength = "Content-Length: " + message.length() + "\r\n";
        String response = statusLine + contentType + contentLength + "\r\n" + message;
        clientSocket.getOutputStream().write(response.getBytes());
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
                logger.debug("Exception creating user playlist", e);
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
