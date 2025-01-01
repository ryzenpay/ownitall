package ryzen.ownitall;

//https://developer.spotify.com/documentation/web-api

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.time.Duration;
import java.util.stream.Collectors;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;

import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumsTracksRequest;
import se.michaelthelin.spotify.requests.data.library.GetCurrentUsersSavedAlbumsRequest;
import se.michaelthelin.spotify.requests.data.library.GetUsersSavedTracksRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.SavedAlbum;
import se.michaelthelin.spotify.model_objects.specification.SavedTrack;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.net.URI;

public class Spotify {
    private SpotifyApi spotifyApi;

    /**
     * Default spotify constructor asking for user input
     */
    public Spotify(Scanner scanner) {
        System.out.println("The following details can be obtained here: https://developer.spotify.com/dashboard");
        System.out.println("Please provide your client id: ");
        String client_id = scanner.nextLine();
        System.out.println("Please provide your client secret: ");
        String client_secret = scanner.nextLine();
        System.out.println("Please provide redirect url:");
        URI redirect_url = SpotifyHttpManager.makeUri(scanner.nextLine());
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(client_id)
                .setClientSecret(client_secret)
                .setRedirectUri(redirect_url)
                .build();
        String code = this.getCode(scanner);
        this.setToken(code);
    }

    /**
     * Default spotify constructor without needing user input
     * 
     * @param client_id     - provided spotify developer app client id
     * @param client_secret - provided spotify developer app client secret
     * @param redirect_url  - provided spotify developer app redirect url
     */
    public Spotify(Scanner scanner, String client_id, String client_secret, String redirect_url) {
        URI redirect_url_uri = SpotifyHttpManager.makeUri(redirect_url);
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(client_id)
                .setClientSecret(client_secret)
                .setRedirectUri(redirect_url_uri)
                .build();
        String code = this.getCode(scanner);
        this.setToken(code);
    }

    /**
     * set the SpotifyAPI access token
     * 
     * @param code - the authentication code provided in the oauth
     */
    private void setToken(String code) {
        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            System.out.println("Token Expires in: " + authorizationCodeCredentials.getExpiresIn()); // TODO: track time
                                                                                                    // and check
            // this?
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.err.println("Error logging in: " + e);
        }
    }

    /**
     * obtaining the oauth code to set the token
     * 
     * @return - the oauth code with permissions
     */
    private String getCode(Scanner scanner) {
        AuthorizationCodeUriRequest authorizationCodeUriRequest = this.spotifyApi.authorizationCodeUri()
                .scope("user-library-read,playlist-read-private")
                .show_dialog(true)
                .build();
        URI auth_uri = authorizationCodeUriRequest.execute();
        System.out.println("Open this link:\n" + auth_uri.toString());
        System.out.println("Please provide the code it provides (in url)");
        String code = scanner.nextLine(); // TODO: gui would help this so much
        return code;
    }

    /**
     * Get all liked songs from current spotify account
     * 
     * @return - arraylist of constructed songs
     */
    public ArrayList<Song> getLikedSongs() {
        ArrayList<Song> likedSongs = new ArrayList<>();
        int limit = 50;
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            GetUsersSavedTracksRequest getUsersSavedTracksRequest = spotifyApi.getUsersSavedTracks()
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
                        ArrayList<String> artists = Arrays.stream(track.getArtists())
                                .map(artist -> artist.getName())
                                .collect(Collectors.toCollection(ArrayList::new));
                        Duration duration = Duration.ofMillis(track.getDurationMs());

                        likedSongs.add(new Song(songName, artists, duration));
                    }

                    offset += limit;
                }

                if (offset >= savedTrackPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.out.println("Error fetching liked songs: " + e.getMessage());
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
        int limit = 50;
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
                        ArrayList<String> artists = Arrays.stream(savedAlbum.getAlbum().getArtists())
                                .map(artist -> artist.getName())
                                .collect(Collectors.toCollection(ArrayList::new));
                        ArrayList<Song> songs = getAlbumSongs(savedAlbum.getAlbum().getId());

                        albums.put(new Album(albumName, artists), songs);
                    }

                    offset += limit;
                }
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.out.println("Error: " + e.getMessage());
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
    public ArrayList<Song> getAlbumSongs(String albumId) {
        ArrayList<Song> songs = new ArrayList<>();
        int limit = 50;
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            GetAlbumsTracksRequest getAlbumsTracksRequest = spotifyApi.getAlbumsTracks(albumId)
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
                        ArrayList<String> artists = Arrays.stream(track.getArtists())
                                .map(artist -> artist.getName())
                                .collect(Collectors.toCollection(ArrayList::new));
                        Duration duration = Duration.ofMillis(track.getDurationMs());

                        songs.add(new Song(songName, artists, duration));
                    }

                    offset += limit;
                }

                if (offset >= trackSimplifiedPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.out.println("Error fetching songs for album " + albumId + ": " + e.getMessage());
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
        int limit = 50;
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            GetListOfCurrentUsersPlaylistsRequest getListOfCurrentUsersPlaylistsRequest = spotifyApi
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
                    for (PlaylistSimplified playlist : items) {
                        String playlistName = playlist.getName();
                        ArrayList<Song> playlistSongs = getPlaylistSongs(playlist.getId());
                        playlists.put(new Playlist(playlistName), playlistSongs);
                    }

                    offset += limit;
                }

                if (offset >= playlistSimplifiedPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.out.println("Error fetching playlists: " + e.getMessage());
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
        int limit = 100;
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            GetPlaylistsItemsRequest getPlaylistsItemsRequest = spotifyApi.getPlaylistsItems(playlistId)
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
                        try {
                            Track track = (Track) playlistTrack.getTrack();
                            String songName = track.getName();
                            ArrayList<String> artists = Arrays.stream(track.getArtists())
                                    .map(ArtistSimplified::getName)
                                    .collect(Collectors.toCollection(ArrayList::new));
                            Duration duration = Duration.ofMillis(track.getDurationMs());

                            songs.add(new Song(songName, artists, duration));
                        } catch (ClassCastException e) {
                            // TODO: handle episodes (people use to prevent copyright)
                            System.out.println("Skipped a non-Track item in the playlist: " + playlistId);
                        }
                    }

                    offset += limit;
                }

                if (offset >= playlistTrackPaging.getTotal()) {
                    hasMore = false;
                }
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.out.println("Error fetching playlist tracks: " + e.getMessage());
                hasMore = false;
            }
        }

        return songs;
    }

}
