package ryzen.ownitall;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;

class testSpotify {

    private Spotify spotify;

    @BeforeEach
    void setUp() {
        String client_id = "8cf63653de3c45bf9155a0cb39e06c8a";
        String client_secret = "";
        String redirect_url = "https://ryzen.rip/ownitall";
        Scanner scanner = new Scanner(System.in);
        spotify = new Spotify(scanner, client_id, client_secret, redirect_url);
        scanner.close();
    }

    @Test
    void testGetLikedSongs() {
        ArrayList<Song> likedSongs = spotify.getLikedSongs();
        assertNotNull(likedSongs);
        assertFalse(likedSongs.isEmpty());
        // Add more specific assertions about the liked songs
    }

    @Test
    void testGetAlbums() {
        LinkedHashMap<Album, ArrayList<Song>> albums = spotify.getAlbums();
        assertNotNull(albums);
        assertFalse(albums.isEmpty());
        // Add more specific assertions about the albums
    }

    @Test
    void testGetAlbumSongs() {
        String testAlbumId = "test_album_id"; // Replace with a real album ID
        ArrayList<Song> albumSongs = spotify.getAlbumSongs(testAlbumId);
        assertNotNull(albumSongs);
        assertFalse(albumSongs.isEmpty());
        // Add more specific assertions about the album songs
    }

    @Test
    void testGetPlaylists() {
        LinkedHashMap<Playlist, ArrayList<Song>> playlists = spotify.getPlaylists();
        assertNotNull(playlists);
        assertFalse(playlists.isEmpty());
        // Add more specific assertions about the playlists
    }
}
