package ryzen.ownitall.library;

import java.util.ArrayList;
//https://developer.spotify.com/documentation/web-api
//https://github.com/spotify-web-api-java/spotify-web-api-java
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
import se.michaelthelin.spotify.requests.data.library.SaveAlbumsForCurrentUserRequest;
import se.michaelthelin.spotify.requests.data.library.SaveTracksForUserRequest;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import se.michaelthelin.spotify.requests.data.playlists.CreatePlaylistRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchAlbumsRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Episode;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.SavedAlbum;
import se.michaelthelin.spotify.model_objects.specification.SavedTrack;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.model_objects.specification.Track;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Library;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Progressbar;

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
    // read and write scope
    private static final String scope = "playlist-read-private,playlist-read-collaborative,user-library-read,user-library-modify,playlist-modify-private,playlist-modify-public";
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
                .scope(scope)
                .show_dialog(settings.isSpotifyShowDialog())
                .build();
        URI auth_uri = authorizationCodeUriRequest.execute();

        // Open the default browser with the authorization URL
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(auth_uri);
                this.startLocalServer();
            } catch (IOException e) {
                logger.error("Exception opening web browser: " + e);
            }
        } else {
            System.out.println("Open this link:\n" + auth_uri.toString());
            try {
                System.out.print("Please provide the code it presents (in url): ");
                this.code = Input.request().getString();
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting code");
            }
        }
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
            logger.error("Exception logging in: " + e);
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
            logger.error("Exception refreshing token: " + e.getMessage());
            return false;
        }
    }

    /**
     * start temporary local server to "intercept" spotify api code
     */
    public void startLocalServer() {
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
                    System.out.println("Please provide the code it provides (in url)");
                    this.code = Input.request().getString();
                } catch (InterruptedException e) {
                    logger.debug("Interrupted while getting code (failed getting from browser)");
                }
            }

            clientSocket.close();
        } catch (IOException e) {
            try {
                System.out.println("Please provide the code it provides (in url)");
                this.code = Input.request().getString();
            } catch (InterruptedException ie) {
                logger.debug("Interrupted while getting code (localserver failed)");
            }
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
            logger.debug("Empty request passed in extractCodeFromRequest");
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
     */
    private void sleep(long seconds) {
        logger.debug("Spotify timeout sleeping for: " + seconds + "s");
        long msec = seconds * 1000;
        try {
            Thread.sleep(msec);
        } catch (Exception e) {
            logger.error("Exception in spotify sleep: " + e);
        }
    }

    /**
     * Get all liked songs from current spotify account and add them to collection
     * 
     */
    public void getLikedSongs() {
        int limit = settings.getSpotifySongLimit();
        int offset = collection.getLikedSongs().getSpotifyPageOffset();
        boolean hasMore = true;
        ProgressBar pb = Progressbar.progressBar("Spotify Liked", 1);
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
                        Song song = null;
                        if (settings.isUseLibrary()) {
                            song = library.getSong(track.getName(), track.getArtists()[0].getName());
                        }
                        if (song == null && !settings.isLibraryVerified()) {
                            song = new Song(track.getName());
                            song.setArtist(new Artist(track.getArtists()[0].getName()));
                        }
                        if (song != null) {
                            song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                            song.addId("spotify", track.getId());
                            collection.addLikedSong(song);
                            pb.setExtraMessage(song.getName());
                        }
                    }
                    offset += limit;
                }

                if (offset >= savedTrackPaging.getTotal()) {
                    hasMore = false;
                    collection.getLikedSongs().setSpotifyPageOffset(offset);
                }
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception fetching liked songs: " + e);
                hasMore = false;
            }
        }
        pb.step().close();
    }

    /**
     * get all current user saved albums and add them to collection
     */
    public void getAlbums() {
        int limit = settings.getSpotifyAlbumLimit();
        int offset = 0;
        boolean hasMore = true;
        ProgressBar pb = Progressbar.progressBar("Spotify Albums", 1);
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
                        Album album = this.getAlbum(savedAlbum.getAlbum().getId(), savedAlbum.getAlbum().getName(),
                                savedAlbum.getAlbum().getArtists()[0].getName());
                        if (album != null) {
                            collection.addAlbum(album);
                            pb.setExtraMessage(album.getName());
                        }
                    }
                    offset += limit;
                }
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception getting albums: " + e);
                hasMore = false;
            }
        }
        pb.step().close();
    }

    public Album getAlbum(String albumId, String albumName, String artistName) {
        if (albumId == null || albumName == null) {
            logger.debug("Null albumID or AlbumName provided in getAlbum");
            return null;
        }
        Album album = null;
        if (settings.isUseLibrary()) {
            album = library.getAlbum(albumName, artistName);
        }
        if (album == null && !settings.isLibraryVerified()) {
            album = new Album(albumName);
            if (artistName != null) {
                album.addArtist(new Artist(artistName));
            }
            Album foundAlbum = collection.getAlbum(album);
            int offset = 0;
            if (foundAlbum != null) {
                offset = foundAlbum.getSpotifyPageOffset();
            }
            LinkedHashSet<Song> songs = this.getAlbumSongs(albumId, offset);
            if (songs != null && !songs.isEmpty()) {
                album.addSongs(songs);
                album.setSpotifyPageOffset(songs.size());
            }
        }
        if (album != null) {
            album.addId("spotify", albumId);
        }
        return album;
    }

    /**
     * get all songs in an album
     * 
     * @param albumId - spotify album id
     * @param offset  - offset to start at (if saved in album)
     * @return - linkedhashset of songs
     */
    public LinkedHashSet<Song> getAlbumSongs(String albumId, int offset) {
        if (albumId == null) {
            logger.debug("null albumID provided in getAlbumSongs");
            return null;
        }
        LinkedHashSet<Song> songs = new LinkedHashSet<>();
        int limit = settings.getSpotifyAlbumLimit();
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
                        Song song = null;
                        if (settings.isUseLibrary()) {
                            song = library.getSong(track.getName(), track.getArtists()[0].getName());
                        }
                        if (song == null && !settings.isLibraryVerified()) {
                            song = new Song(track.getName());
                            song.setArtist(new Artist(track.getArtists()[0].getName()));
                        }
                        if (song != null) {
                            song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                            song.addId("spotify", track.getId());
                            songs.add(song);
                        }
                    }
                    offset += limit;
                }
                if (offset >= trackSimplifiedPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception fetching songs for album: " + albumId + ": " + e);
                hasMore = false;
            }
        }
        return songs;
    }

    /**
     * get all playlists contributed by current spotify user and add them to
     * collection
     * 
     */
    public void getPlaylists() {
        int limit = settings.getSpotifyPlaylistLimit();
        int offset = 0;
        boolean hasMore = true;
        ProgressBar pb = Progressbar.progressBar("Spotify Playlists", 1);

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
                        String coverImageUrl = null;
                        Image[] images = spotifyPlaylist.getImages();
                        if (images != null && images.length > 0) {
                            coverImageUrl = images[images.length - 1].getUrl();
                        }
                        Playlist playlist = this.getPlaylist(spotifyPlaylist.getId(),
                                spotifyPlaylist.getName(), coverImageUrl);
                        if (playlist != null) {
                            collection.addPlaylist(playlist);
                            pb.setExtraMessage(playlist.getName());
                        }
                    }
                    offset += limit;
                }
                if (offset >= playlistSimplifiedPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception fetching playlists: " + e);
                hasMore = false;
            }
        }
        pb.step().close();
    }

    public Playlist getPlaylist(String playlistId, String playlistName, String playlistImageUrl) {
        if (playlistId == null || playlistName == null) {
            logger.debug("null playlistID or playlistName provided in getPlaylist");
            return null;
        }
        Playlist playlist = new Playlist(playlistName);
        Playlist foundPlaylist = collection.getPlaylist(playlist);
        int offset = 0;
        if (foundPlaylist != null) {
            offset = foundPlaylist.getSpotifyPageOffset();
        }
        LinkedHashSet<Song> songs = this.getPlaylistSongs(playlistId, offset);
        if (songs != null && !songs.isEmpty()) {
            playlist.addSongs(songs);
            playlist.setSpotifyPageOffset(songs.size());
            if (playlistImageUrl != null) {
                playlist.setCoverImage(playlistImageUrl);
            }
            playlist.addId("spotify", playlistId);
            return playlist;
        }
        return null;
    }

    /**
     * get all songs from a playlist
     * 
     * @param playlistId - spotify ID for a playlist
     * @param offset     - offset to update from (default to 0)
     * @return - constructed array of Songs
     */
    public LinkedHashSet<Song> getPlaylistSongs(String playlistId, int offset) {
        if (playlistId == null) {
            logger.debug("null playlistID provided in getPlaylistSongs");
            return null;
        }
        LinkedHashSet<Song> songs = new LinkedHashSet<>();
        int limit = settings.getSpotifySongLimit();
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
                        Song song = null;
                        String trackName;
                        String artistName;
                        int duration;
                        String id;
                        if (playlistTrack.getTrack() instanceof Track) {
                            Track track = (Track) playlistTrack.getTrack();
                            trackName = track.getName();
                            artistName = track.getArtists()[0].getName();
                            duration = track.getDurationMs();
                            id = track.getId();
                        } else if (playlistTrack.getTrack() instanceof Episode) {
                            Episode episode = (Episode) playlistTrack.getTrack();
                            trackName = episode.getName();
                            artistName = null;
                            duration = episode.getDurationMs();
                            id = episode.getId();
                        } else {
                            logger.info("Skipping non-Track in playlist: " + playlistId);
                            continue;
                        }
                        if (settings.isUseLibrary()) {
                            song = library.getSong(trackName, artistName);
                        }
                        if (song == null && !settings.isLibraryVerified()) {
                            song = new Song(trackName);
                            song.setArtist(new Artist(artistName));
                        }
                        if (song != null) {
                            song.setDuration(duration, ChronoUnit.MILLIS);
                            song.addId("spotify", id);
                            songs.add(song);
                        }
                    }
                    offset += limit;
                }

                if (offset >= playlistTrackPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception fetching playlist tracks: " + e);
                hasMore = false;
            }
        }
        return songs;
    }

    public String getTrackId(Song song) {
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
                    logger.debug("Song " + song.toString() + " not found");
                    return null;
                }
                song.addId("spotify", items[0].getId());
                return items[0].getId();
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception searching for song: " + e);
                return null;
            }
        }
    }

    public String getAlbumId(Album album) {
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
                    logger.debug("Album " + album.toString() + " not found");
                    return null;
                }
                album.addId("spotify", items[0].getId());
                return items[0].getId();
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception searching for Album: " + e);
                return null;
            }
        }
    }

    public void uploadLikedSongs() {
        ArrayList<String> likedSongIds = new ArrayList<>();
        for (Song likedSong : collection.getLikedSongs().getSongs()) {
            likedSongIds.add(this.getTrackId(likedSong));
        }
        if (likedSongIds.isEmpty()) {
            logger.debug("No liked songs in collection");
            return;
        }
        int limit = settings.getSpotifySongLimit();
        int offset = 0;
        boolean hasMore = true;
        while (hasMore) {
            String[] currentLikedSongsIds = likedSongIds.subList(offset,
                    Math.min(offset + limit, likedSongIds.size())).toArray(new String[0]);
            SaveTracksForUserRequest saveTracksForUserRequest = spotifyApi
                    .saveTracksForUser(currentLikedSongsIds)
                    .build();
            try {
                saveTracksForUserRequest.execute();
                offset += limit;
                if (offset >= likedSongIds.size()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception adding users saved tracks: " + e);
            }
        }
    }

    public void uploadPlaylists() {
        LinkedHashSet<Playlist> playlists = collection.getPlaylists();
        ProgressBar pb = Progressbar.progressBar("Uploading Playlists", playlists.size());
        for (Playlist playlist : playlists) {
            pb.setExtraMessage(playlist.getName()).step();
            this.uploadPlaylist(playlist);
        }
        pb.setExtraMessage("Done").close();
    }

    public void uploadPlaylist(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in uploadPlaylist");
            return;
        }
        String playlistId = playlist.getId("spotify");
        LinkedHashSet<Song> currentSongs = new LinkedHashSet<>();
        if (playlistId != null) {
            // TODO: this still references to a deleted / archived playlist and therefore
            // doesnt trigger to make new one
            currentSongs = this.getPlaylistSongs(playlistId, 0);
        }
        LinkedHashSet<Song> songs = new LinkedHashSet<>(playlist.getSongs());
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
                playlist.addId("spotify", playlistId);
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.debug("Exception creating user playlist: " + e);
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
            logger.debug("Playlist " + playlist.toString() + " is empty / no update");
            return;
        }
        int limit = settings.getSpotifySongLimit();
        int offset = 0;
        boolean hasMore = true;
        while (hasMore) {
            String[] currentSongUris = songUris.subList(offset,
                    Math.min(offset + limit, songUris.size())).toArray(new String[0]);
            AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi
                    .addItemsToPlaylist(playlistId, currentSongUris)
                    .build();
            try {
                addItemsToPlaylistRequest.execute();
                offset += limit;
                if (offset >= songUris.size()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception adding users saved tracks: " + e);
            }
        }
    }

    public void uploadAlbums() {
        ArrayList<String> albumIds = new ArrayList<>();
        for (Album album : collection.getAlbums()) {
            albumIds.add(this.getAlbumId(album));
        }
        if (albumIds.isEmpty()) {
            logger.debug("No Saved albums in collection");
            return;
        }
        int limit = settings.getSpotifyAlbumLimit();
        int offset = 0;
        boolean hasMore = true;
        while (hasMore) {
            String[] currentAlbumIds = albumIds.subList(offset,
                    Math.min(offset + limit, albumIds.size())).toArray(new String[0]);
            SaveAlbumsForCurrentUserRequest saveAlbumsForCurrentUserRequest = spotifyApi
                    .saveAlbumsForCurrentUser(currentAlbumIds)
                    .build();
            try {
                saveAlbumsForCurrentUserRequest.execute();
                offset += limit;
                if (offset >= albumIds.size()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                logger.debug("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Exception adding users Albums: " + e);
            }
        }
    }
}
