package ryzen.ownitall;

//https://developer.spotify.com/documentation/web-api

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import se.michaelthelin.spotify.requests.data.artists.GetArtistRequest;
import se.michaelthelin.spotify.requests.data.library.GetCurrentUsersSavedAlbumsRequest;
import se.michaelthelin.spotify.requests.data.library.GetUsersSavedTracksRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
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

import ryzen.ownitall.tools.Input;

import java.io.IOException;
import java.net.URI;
import java.awt.Desktop;

public class Spotify extends SpotifyCredentials {
    private static final Logger logger = LogManager.getLogger(Spotify.class);
    private static Settings settings = Settings.load();
    private SpotifyApi spotifyApi;
    /**
     * this variable is to limit the spotify api requests by using known artists
     * format: <Artist name, constructed artist class>
     */
    private LinkedHashMap<String, Artist> artists;

    /**
     * Default spotify constructor asking for user input
     */
    public Spotify() {
        super();
        this.artists = new LinkedHashMap<>();
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(this.getClientId())
                .setClientSecret(this.getClientSecret())
                .setRedirectUri(this.getRedirectUrl())
                .build();
        this.requestCode();
        this.setToken();
    }

    /**
     * Default spotify constructor with known values excluding token
     * 
     * @param client_id     - provided spotify developer app client id
     * @param client_secret - provided spotify developer app client secret
     * @param redirect_url  - provided spotify developer app redirect url
     */
    public Spotify(String client_id, String client_secret, String redirect_url) {
        super(client_secret, client_id, redirect_url);
        this.artists = new LinkedHashMap<>();
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(this.getClientId())
                .setClientSecret(this.getClientSecret())
                .setRedirectUri(this.getRedirectUrl())
                .build();
        this.requestCode();
        this.setToken();
    }

    /**
     * default spotify constructor taking in constructed spotify credentials (for
     * importing)
     * 
     * @param spotifyCredentials - constructed SpotifyCredentials class with
     *                           variables
     */
    public Spotify(SpotifyCredentials spotifyCredentials) {
        super(spotifyCredentials.getClientId(), spotifyCredentials.getClientSecret(),
                spotifyCredentials.getRedirectUrlString());
        this.artists = new LinkedHashMap<>();
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(this.getClientId())
                .setClientSecret(this.getClientSecret())
                .setRedirectUri(this.getRedirectUrl())
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
            this.setCode(Input.getInstance().getString());
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
        AuthorizationCodeRequest authorizationCodeRequest = this.spotifyApi.authorizationCode(this.getCode()).build();
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
     * get spotify credentials (used for exporting)
     * 
     * @return - constructed SpotifyCredentials
     */
    public SpotifyCredentials getSpotifyCredentials() {
        return new SpotifyCredentials(this.getClientId(), this.getClientSecret(), this.getRedirectUrlString());
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
     * @return - arraylist of constructed songs
     */
    public LikedSongs getLikedSongs() {
        LikedSongs likedSongs = new LikedSongs();
        int limit = 50;
        int offset = 0;
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
                        Song song = new Song(track.getName());
                        song.addArtists(this.getArtists(track.getArtists()));
                        song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                        likedSongs.addSong(song);
                    }
                    offset += limit;
                }

                if (offset >= savedTrackPaging.getTotal()) {
                    hasMore = false;
                    likedSongs.setSpotifyPageOffset(offset);
                }
            } catch (TooManyRequestsException e) {
                logger.info("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Error fetching liked songs: " + e);
                hasMore = false;
            }
        }
        return likedSongs;
    }

    /**
     * get all current user saved albums
     * 
     * @return - arraylist of constructed albums
     */
    public LinkedHashSet<Album> getAlbums() {
        LinkedHashSet<Album> albums = new LinkedHashSet<>();
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
                        Album album = new Album(savedAlbum.getAlbum().getName());
                        album.addArtists(this.getArtists(savedAlbum.getAlbum().getArtists()));
                        album.setCoverImage(savedAlbum.getAlbum().getImages()[0].getUrl());// TODO:
                                                                                           // documentation says
                                                                                           // its per size?
                        album.addSongs(
                                this.getAlbumSongs(album.getCoverImage(), savedAlbum.getAlbum().getId()));
                        albums.add(album);
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
        return albums;
    }

    /**
     * get all songs of an album
     * 
     * @param coverArt - album coverart to set on songs
     * @param albumId  - spotify ID of the album
     * @return - arraylist of constructed Songs
     */
    public ArrayList<Song> getAlbumSongs(URI coverArt, String albumId) {
        ArrayList<Song> songs = new ArrayList<>();
        int limit = 20;
        int offset = 0;
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
                        Song song = new Song(track.getName());
                        song.addArtists(this.getArtists(track.getArtists()));
                        song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
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
    public LinkedHashSet<Playlist> getPlaylists() {
        LinkedHashSet<Playlist> playlists = new LinkedHashSet<>();
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
                        playlist.addSongs(this.getPlaylistSongs(spotifyPlaylist.getId()));
                        playlist.setSpotifyPageOffset(playlist.size());
                        playlist.setCoverArt(spotifyPlaylist.getImages()[0].getUrl());
                        playlists.add(playlist);
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
        return playlists;
    }

    /**
     * get all songs from a playlist
     * 
     * @param playlistId - spotify ID for a playlist
     * @return - constructed array of Songs
     */
    public ArrayList<Song> getPlaylistSongs(String playlistId) {
        ArrayList<Song> songs = new ArrayList<>();
        int limit = 50;
        int offset = 0;
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
                            Song song = new Song(track.getName());
                            song.addArtists(this.getArtists(track.getArtists()));
                            song.setDuration(track.getDurationMs(), ChronoUnit.MILLIS);
                            // TODO: song cover
                            songs.add(song);
                        } else if (playlistTrack.getTrack() instanceof Episode) {
                            Episode episode = (Episode) playlistTrack.getTrack();
                            Song song = new Song(episode.getName());
                            // String coverImage = episode.getImages()[0].getUrl(); doesnt seem to work with
                            // the current SpotifyAPI class (returns null)
                            song.setDuration(episode.getDurationMs(), ChronoUnit.MILLIS);
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

    /**
     * process array of SpotifyAPI artists to constructed Artist
     * can also get artists pfp if set in settings
     * 
     * @param raw_artists - array of SpotifyAPI artists
     * @return - arraylist of constructed Artist
     */
    public ArrayList<Artist> getArtists(ArtistSimplified[] raw_artists) {
        ArrayList<Artist> artists = new ArrayList<>();
        for (ArtistSimplified raw_artist : raw_artists) {
            if (this.artists.containsKey(raw_artist.getName())) { // if already exists, dont get again
                artists.add(this.artists.get(raw_artist.getName()));
            } else {
                Artist artist = new Artist(raw_artist.getName());
                if (settings.isSpotifyArtistPfp()) {
                    boolean done = false;
                    while (!done) { // this is incase of api timeout
                        try {
                            GetArtistRequest getArtistRequest = this.spotifyApi.getArtist(raw_artist.getId()).build();
                            se.michaelthelin.spotify.model_objects.specification.Artist fetchedArtist = getArtistRequest
                                    .execute();
                            if (fetchedArtist.getImages().length != 0) {
                                artist.setProfilePicture(getArtistRequest.execute().getImages()[0].getUrl());
                            }
                            done = true;
                        } catch (TooManyRequestsException e) {
                            logger.info("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                            this.sleep(e.getRetryAfter());
                        } catch (Exception e) {
                            logger.error("Error obtaining artist: " + e);
                            done = true;
                        }
                    }
                }
                artists.add(artist);
                this.artists.put(artist.getName(), artist); // add them to the known database
            }
        }
        return artists;
    }
}
