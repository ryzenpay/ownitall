package ryzen.ownitall;

//https://developer.spotify.com/documentation/web-api

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.time.Duration;

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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.awt.Desktop;

public class Spotify extends SpotifyCredentials {
    private SpotifyApi spotifyApi;

    /**
     * Default spotify constructor asking for user input
     */
    public Spotify() {
        super();
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
                .show_dialog(true)
                .build();
        URI auth_uri = authorizationCodeUriRequest.execute();

        // Open the default browser with the authorization URL
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(auth_uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Open this link:\n" + auth_uri.toString());
            System.out.println("Please provide the code it provides (in url)");
            this.setCode(Input.getInstance().getString());// TODO: gui would help this so much
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
            this.setCodeExpiration(authorizationCodeCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.err.println("Error logging in: " + e);
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
            this.setCodeExpiration(authorizationCodeCredentials.getExpiresIn());
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    public SpotifyCredentials getSpotifyCredentials() {
        return new SpotifyCredentials(this.getClientId(), this.getClientSecret(), this.getRedirectUrlString());
    }

    private void sleep(long seconds) {
        long msec = seconds * 1000;
        try {
            Thread.sleep(msec);
        } catch (Exception e) {
            System.err.println("Error in spotify sleep: " + e);
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
                        Track track = savedTrack.getTrack();
                        String songName = track.getName();
                        Duration duration = Duration.ofMillis(track.getDurationMs());
                        ArrayList<Artist> artists = Arrays.stream(track.getArtists())
                                .map(artist -> new Artist(artist.getName()))
                                .collect(Collectors.toCollection(ArrayList::new));
                        likedSongs.addSong(new Song(songName, artists, duration));
                    }

                    offset += limit;
                }

                if (offset >= savedTrackPaging.getTotal()) {
                    hasMore = false;
                    likedSongs.setSpotifyPageOffset(offset);
                }
            } catch (TooManyRequestsException e) {
                System.err.println("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.err.println("Error fetching liked songs: " + e.getMessage());
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
    public LinkedHashMap<Album, ArrayList<Song>> getAlbums() {
        LinkedHashMap<Album, ArrayList<Song>> albums = new LinkedHashMap<>();
        int limit = 20;
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
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
                        String albumName = savedAlbum.getAlbum().getName();
                        ArrayList<Artist> artists = Arrays.stream(savedAlbum.getAlbum().getArtists())
                                .map(artist -> new Artist(artist.getName()))
                                .collect(Collectors.toCollection(ArrayList::new));
                        String coverart = savedAlbum.getAlbum().getImages()[0].getUrl(); // TODO:
                                                                                         // documentation says
                                                                                         // its per size?
                        ArrayList<Song> songs = getAlbumSongs(coverart, savedAlbum.getAlbum().getId());
                        albums.put(new Album(albumName, artists, coverart), songs);

                    }
                    offset += limit;
                }
            } catch (TooManyRequestsException e) {
                System.err.println("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.err.println("Error: " + e.getMessage());
                hasMore = false;
            }
        }
        return albums;
    }

    /**
     * get all songs of an album
     * 
     * @param albumId - spotify ID of the album
     * @return - arraylist of constructed Songs
     */
    public ArrayList<Song> getAlbumSongs(String coverart, String albumId) {
        ArrayList<Song> songs = new ArrayList<>();
        int limit = 20;
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
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
                        String songName = track.getName();
                        ArrayList<Artist> artists = Arrays.stream(track.getArtists())
                                .map(artist -> new Artist(artist.getName()))
                                .collect(Collectors.toCollection(ArrayList::new));
                        Duration duration = Duration.ofMillis(track.getDurationMs());

                        songs.add(new Song(songName, artists, duration, coverart));
                    }

                    offset += limit;
                }

                if (offset >= trackSimplifiedPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                System.err.println("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.err.println("Error fetching songs for album: " + albumId + ": " + e.getMessage());
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
    public LinkedHashMap<Playlist, ArrayList<Song>> getPlaylists() {
        LinkedHashMap<Playlist, ArrayList<Song>> playlists = new LinkedHashMap<>();
        int limit = 20;
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
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
                        String playlistName = spotifyPlaylist.getName();
                        ArrayList<Song> playlistSongs = getPlaylistSongs(spotifyPlaylist.getId());
                        Playlist playlist = new Playlist(playlistName);
                        playlist.setSpotifyPageOffset(playlistSongs.size());
                        try {
                            URI coverart = new URI(spotifyPlaylist.getImages()[0].getUrl());
                            playlist.setCoverArt(coverart);
                            playlists.put(playlist, playlistSongs);
                        } catch (URISyntaxException e) {
                            playlists.put(playlist, playlistSongs);
                        }
                    }
                    offset += limit;
                }

                if (offset >= playlistSimplifiedPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                System.err.println("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.err.println("Error fetching playlists: " + e.getMessage());
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
                        if (playlistTrack.getTrack() instanceof Track) {
                            Track track = (Track) playlistTrack.getTrack();
                            String songName = track.getName();
                            ArrayList<Artist> artists = Arrays.stream(track.getArtists())
                                    .map(artist -> new Artist(artist.getName()))
                                    .collect(Collectors.toCollection(ArrayList::new));
                            Duration duration = Duration.ofMillis(track.getDurationMs());
                            songs.add(new Song(songName, artists, duration));
                        } else if (playlistTrack.getTrack() instanceof Episode) {
                            Episode episode = (Episode) playlistTrack.getTrack();
                            String episodeName = episode.getName();
                            String coverImage = episode.getImages()[0].getUrl();
                            Duration duration = Duration.ofMillis(episode.getDurationMs());
                            songs.add(new Song(episodeName, duration, coverImage));
                        } else {
                            System.out.println("Skipping non-Track in playlist: " + playlistId);
                        }
                    }

                    offset += limit;
                }

                if (offset >= playlistTrackPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (TooManyRequestsException e) {
                System.err.println("Spotify API too many requests, waiting " + e.getRetryAfter() + " seconds");
                this.sleep(e.getRetryAfter());
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.err.println("Error fetching playlist tracks: " + e.getMessage());
                hasMore = false;
            }
        }

        return songs;
    }
}
