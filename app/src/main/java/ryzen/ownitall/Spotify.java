package ryzen.ownitall;

//https://developer.spotify.com/documentation/web-api

import java.util.ArrayList;
import java.util.Arrays;

import java.util.Scanner;
import java.time.Duration;
import java.util.stream.Collectors;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;

import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
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

public class Spotify {
    private SpotifyApi spotifyApi;

    public Spotify() {
        this.setToken();
    }

    private void setToken() {
        Scanner scanner = new Scanner(System.in); // TODO: save these creds?
        System.out.println("The following details can be obtained here: https://developer.spotify.com/dashboard");
        System.out.println("Please provide your client id: ");
        String client_id = scanner.nextLine();
        System.out.println("Please provide your client secret: ");
        String client_secret = scanner.nextLine();
        scanner.close();
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(client_id)
                .setClientSecret(client_secret)
                .build();
        ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
                .build();
        try {
            ClientCredentials clientCredentials = clientCredentialsRequest.execute();
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());
            System.out.println("Token Expires in: " + clientCredentials.getExpiresIn()); // TODO: track time and check
                                                                                         // this?
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.err.println("Error logging in: " + e);
        }
    }

    private ArrayList<Song> getLikedSongs() {
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

    public ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<>();
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

                        albums.add(new Album(albumName, artists, songs));
                    }

                    offset += limit;
                }

                System.out.println("Fetched " + albums.size() + " albums so far.");
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.out.println("Error: " + e.getMessage());
                hasMore = false;
            }
        }

        System.out.println("Total albums fetched: " + albums.size());
        return albums;
    }

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

    private ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<>();
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
                        playlists.add(new Playlist(playlistName, playlistSongs));
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

    private ArrayList<Song> getPlaylistSongs(String playlistId) {
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
                        Track track = (Track) playlistTrack.getTrack();
                        String songName = track.getName();
                        ArrayList<String> artists = Arrays.stream(track.getArtists())
                                .map(ArtistSimplified::getName)
                                .collect(Collectors.toCollection(ArrayList::new));
                        Duration duration = Duration.ofMillis(track.getDurationMs());

                        songs.add(new Song(songName, artists, duration));
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
