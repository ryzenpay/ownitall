package ryzen.ownitall;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

class testSpotify {

    private Spotify spotify;

    @BeforeEach
    void setUp() {
        String client_id = "TESTING CLIENT ID";
        String client_secret = "TESTING CLIENT SECRET";
        spotify = new Spotify(client_id, client_secret);
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
        ArrayList<Album> albums = spotify.getAlbums();
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
        ArrayList<Playlist> playlists = spotify.getPlaylists();
        assertNotNull(playlists);
        assertFalse(playlists.isEmpty());
        // Add more specific assertions about the playlists
    }

    @Test
    void testGetPlaylistSongs() {
        String testPlaylistId = "test_playlist_id"; // Replace with a real playlist ID
        ArrayList<Song> playlistSongs = spotify.getPlaylistSongs(testPlaylistId);
        assertNotNull(playlistSongs);
        assertFalse(playlistSongs.isEmpty());
        // Add more specific assertions about the playlist songs
    }
}
