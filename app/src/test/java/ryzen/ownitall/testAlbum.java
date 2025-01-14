package ryzen.ownitall;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class testAlbum {

    @Test
    public void testConstructor() {
        Album album = new Album(null);
        assertNotNull(album);
        assertNull(album.getName());
        album.setName("test");
        assertNotNull(album.getName());
        assertEquals(album.getName(), "test");
        assertEquals(album.size(), 0);
        assertEquals(album.getArtists().size(), 0);
    }

    @Test
    public void testArtist() {
        Album album = new Album("album");
        album.addArtist(null);
        assertEquals(album.getArtists().size(), 1);
        Artist artist = new Artist("artist");
        album.addArtist(artist);
        assertEquals(album.getArtists().size(), 2);
        assertTrue(album.getArtists().contains(artist));
        album.addArtist(artist);
        assertEquals(album.getArtists().size(), 2);
        Artist artist2 = new Artist("artist2");
        album.addArtist(artist2);
        assertEquals(album.getArtists().size(), 3);
        assertTrue(album.getArtists().contains(artist2));
    }

    @Test
    public void testMerge() {
        Album album = new Album("album");
        album.mergeAlbum(null);
        Album album2 = new Album("album2");
        Song song = new Song("song");
        Artist artist = new Artist("artist");
        album.addArtist(artist);
        album.addSong(song);
        Song song2 = new Song("song2");
        album2.addSong(song2);
        album2.addArtist(artist);
        album.mergeAlbum(album2);
        assertEquals(album.size(), 2);
    }
}
