package ryzen.ownitall.library;

//https://developer.spotify.com/documentation/web-api

import java.util.LinkedHashSet;
import java.time.temporal.ChronoUnit;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;

import org.apache.hc.core5.http.ParseException;

import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumsTracksRequest;
import se.michaelthelin.spotify.requests.data.library.GetCurrentUsersSavedAlbumsRequest;
import se.michaelthelin.spotify.requests.data.library.GetUsersSavedTracksRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Episode;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.SavedAlbum;
import se.michaelthelin.spotify.model_objects.specification.SavedTrack;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Library;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.awt.Desktop;

public class Spotify {
    private static final Logger logger = LogManager.getLogger(Spotify.class);
    private static final Settings settings = Settings.load();
    private static final Credentials credentials = Credentials.load();
    private static Library library = Library.load();
    private static Collection collection = Collection.load();
    private SpotifyApi spotifyApi;
    private String code;

    /**
     * Default spotify constructor asking for user input
     */
    public Spotify() {
        if (credentials.spotifyIsEmpty()) {
            credentials.setSpotifyCredentials();
        }
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(credentials.getSpotifyClientId())
                .setClientSecret(credentials.getSpotifyClientSecret())
                .setRedirectUri(credentials.getSpotifyRedirectUrl())
                .build();
        this.requestCode();
        this.setToken();
    }

    /**
     * obtaining the oauth code to set the token
     * 
     * @return - the oauth code with permissions
     */
    private void requestCode() {
        AuthorizationCodeUriRequest authorizationCodeUriRequest = this.spotifyApi.authorizationCodeUri()
                .scope("user-library-read,playlist-read-private")
                .show_dialog(settings.isSpotifyShowDialog())
                .build();
        URI auth_uri = authorizationCodeUriRequest.execute();

        // Open the default browser with the authorization URL
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(auth_uri);
            } catch (IOException e) {
                logger.error("Error opening web browser: " + e);
            }
        } else {
            System.out.println("Open this link:\n" + auth_uri.toString());
            System.out.print("Please provide the code it presents (in url): ");
            this.code = Input.request().getString();
            return;
        }

        // Start a local server to receive the code
        this.startLocalServer();
    }

    /**
     * set the spotifyApi access token
     * 
     * @param code - the authentication code provided in the oauth
     */
    private void setToken() {
        AuthorizationCodeRequest authorizationCodeRequest = this.spotifyApi.authorizationCode(this.code).build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
            this.spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            this.spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error logging in: " + e);
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
            logger.error("Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * start temporary local server to "intercept" spotify api code
     */
    public void startLocalServer() { // TODO: make this work (cors error)
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
                System.out.println("Please provide the code it provides (in url)");
                this.code = Input.request().getString();
            }

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Please provide the code it provides (in url)");
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
     */
    private void sleep(long seconds) {
        long msec = seconds * 1000;
        try {
            Thread.sleep(msec);
        } catch (Exception e) {
            logger.error("Error in spotify sleep: " + e);
        }
    }

    /**
     * Get all liked songs from current spotify account
     * 
     * @param offset - offset to continue from, default to 0
     * @return - arraylist of constructed songs
     */
    public void getLikedSongs() {
        int limit = 50;
        int offset = collection.getLikedSongs().getSpotifyPageOffset();
        boolean hasMore = true;

        while (hasMore) {
            GetUsersSavedTracksRequest getUsersSavedTracksRequest = this.spotifyApi.getUsersSavedTracks()
                    .limit(settings.getSpotifySongLimit())
                    .offset(offset)
                    .build();

            try {
                final Paging<SavedTrack> savedTrackPaging = getUsersSavedTracksRequest.execute();
                SavedTrack[] items = savedTrackPaging.getItems();

                if (items.length == 0) {
                    hasMore = false;
                } else {
                    for (SavedTrack savedTrack : items) {
                        Track track = savedTrack.getTrack();
                        Song song = library.getSong(track.getName(), track.getArtists()[0].getName());
                        song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                        song.addLink("spotify", track.getUri());
                        collection.addLikedSong(song);
                    }
                    offset += limit;
                }

                if (offset >= savedTrackPaging.getTotal()) {
                    hasMore = false;
                    collection.getLikedSongs().setSpotifyPageOffset(offset);
                }
            } catch (TooManyRequestsException e) {
                logger.info("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Error fetching liked songs: " + e);
                hasMore = false;
            }
        }
    }

    /**
     * get all current user saved albums
     * 
     * @return - arraylist of constructed albums
     */
    public void getAlbums() {
        int limit = 20;
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            GetCurrentUsersSavedAlbumsRequest getCurrentUsersSavedAlbumsRequest = this.spotifyApi
                    .getCurrentUsersSavedAlbums()
                    .limit(settings.getSpotifyAlbumLimit())
                    .offset(offset)
                    .build();
            try {
                final Paging<SavedAlbum> savedAlbumPaging = getCurrentUsersSavedAlbumsRequest.execute();
                SavedAlbum[] items = savedAlbumPaging.getItems();

                if (items.length == 0) {
                    hasMore = false;
                } else {
                    for (SavedAlbum savedAlbum : items) {
                        Album album = library.getAlbum(savedAlbum.getAlbum().getName(),
                                savedAlbum.getAlbum().getArtists()[0].getName());
                        Album foundAlbum = collection.getAlbum(album);
                        LinkedHashSet<Song> songs;
                        if (foundAlbum != null) {
                            songs = this.getAlbumSongs(savedAlbum.getAlbum().getId(),
                                    foundAlbum.getSpotifyPageOffset());
                        } else {
                            songs = this.getAlbumSongs(savedAlbum.getAlbum().getId(), 0);
                        }
                        if (!songs.isEmpty()) {
                            album.addSongs(songs);
                            album.setSpotifyPageOffset(songs.size());
                            album.addLink("spotify", savedAlbum.getAlbum().getUri());
                            album.setCoverImage(savedAlbum.getAlbum().getImages()[0].getUrl());
                            collection.addAlbum(album);
                        }
                    }
                    offset += limit;
                }
            } catch (TooManyRequestsException e) {
                logger.info("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Error: " + e);
                hasMore = false;
            }
        }
    }

    /**
     * get all songs in an album
     * 
     * @param albumId - spotify album id
     * @param offset  - offset to start at (if saved in album)
     * @return - linkedhashset of songs
     */
    public LinkedHashSet<Song> getAlbumSongs(String albumId, int offset) {
        LinkedHashSet<Song> songs = new LinkedHashSet<>();
        int limit = 20;
        boolean hasMore = true;

        while (hasMore) {
            GetAlbumsTracksRequest getAlbumsTracksRequest = this.spotifyApi.getAlbumsTracks(albumId)
                    .limit(settings.getSpotifySongLimit())
                    .offset(offset)
                    .build();

            try {
                Paging<TrackSimplified> trackSimplifiedPaging = getAlbumsTracksRequest.execute();
                TrackSimplified[] items = trackSimplifiedPaging.getItems();

                if (items.length == 0) {
                    hasMore = false;
                } else {
                    for (TrackSimplified track : items) {
                        Song song = library.getSong(track.getName(), track.getArtists()[0].getName());
                        song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                        song.addLink("spotify", track.getUri());
                        songs.add(song);
                    }
                    offset += limit;
                }
                if (offset >= trackSimplifiedPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                logger.info("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Error fetching songs for album: " + albumId + ": " + e);
                hasMore = false;
            }
        }

        return songs;
    }

    /**
     * get all playlists contributed by current spotify user
     * 
     * @return - arraylist of constructed Playlists
     */
    public void getPlaylists() {
        int limit = 20;
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            GetListOfCurrentUsersPlaylistsRequest getListOfCurrentUsersPlaylistsRequest = this.spotifyApi
                    .getListOfCurrentUsersPlaylists()
                    .limit(settings.getSpotifyPlaylistLimit())
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
                        Playlist playlist = new Playlist(spotifyPlaylist.getName());
                        Playlist foundPlaylist = collection.getPlaylist(playlist);
                        LinkedHashSet<Song> songs;
                        if (foundPlaylist != null) {
                            songs = this.getPlaylistSongs(spotifyPlaylist.getId(),
                                    foundPlaylist.getSpotifyPageOffset());
                        } else {
                            songs = this.getPlaylistSongs(spotifyPlaylist.getId(), 0);
                        }
                        if (!songs.isEmpty()) {
                            playlist.addSongs(songs);
                            playlist.setSpotifyPageOffset(songs.size());
                            playlist.setCoverImage(spotifyPlaylist.getImages()[0].getUrl());
                            playlist.addLink("spotify", spotifyPlaylist.getUri());
                            collection.addPlaylist(playlist);
                        }
                    }
                    offset += limit;
                }
                if (offset >= playlistSimplifiedPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                logger.info("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Error fetching playlists: " + e);
                hasMore = false;
            }
        }
    }

    /**
     * get all songs from a playlist
     * 
     * @param playlistId - spotify ID for a playlist
     * @param offset     - offset to update from (default to 0)
     * @return - constructed array of Songs
     */
    public LinkedHashSet<Song> getPlaylistSongs(String playlistId, int offset) {
        LinkedHashSet<Song> songs = new LinkedHashSet<>();
        int limit = 50;
        boolean hasMore = true;
        while (hasMore) {
            GetPlaylistsItemsRequest getPlaylistsItemsRequest = this.spotifyApi.getPlaylistsItems(playlistId)
                    .limit(settings.getSpotifySongLimit())
                    .offset(offset)
                    .build();
            try {
                final Paging<PlaylistTrack> playlistTrackPaging = getPlaylistsItemsRequest.execute();
                PlaylistTrack[] items = playlistTrackPaging.getItems();

                if (items.length == 0) {
                    hasMore = false;
                } else {
                    for (PlaylistTrack playlistTrack : items) {
                        if (playlistTrack.getTrack() instanceof Track) {
                            Track track = (Track) playlistTrack.getTrack();
                            Song song = library.getSong(track.getName(), track.getArtists()[0].getName());
                            song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                            song.addLink("spotify", track.getUri());
                            songs.add(song);
                        } else if (playlistTrack.getTrack() instanceof Episode) {
                            Episode episode = (Episode) playlistTrack.getTrack();
                            Song song = library.getSong(episode.getName(), null);
                            // String coverImage = episode.getImages()[0].getUrl(); doesnt seem to work with
                            // the current SpotifyAPI class (returns null)
                            song.setDuration(episode.getDurationMs(), ChronoUnit.MILLIS);
                            song.addLink("spotify", episode.getUri());
                            songs.add(song);
                        } else {
                            logger.info("Skipping non-Track in playlist: " + playlistId);
                        }
                    }
                    offset += limit;
                }

                if (offset >= playlistTrackPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                logger.info("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Error fetching playlist tracks: " + e);
                hasMore = false;
            }
        }
        return songs;
    }
}
