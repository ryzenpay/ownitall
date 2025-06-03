package ryzen.ownitall.method.interfaces;

import java.util.ArrayList;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;

/**
 * <p>Import interface.</p>
 *
 * @author ryzen
 */
public interface Import {
    /**
     * <p>
     * getLikedSongs.
     * </p>
     *
     * @return a {@link ryzen.ownitall.classes.LikedSongs} object
     * @throws java.lang.InterruptedException if any.
     */
    public LikedSongs getLikedSongs() throws InterruptedException;

    /**
     * <p>
     * getPlaylists.
     * </p>
     *
     * @return a {@link java.util.ArrayList} object
     * @throws java.lang.InterruptedException if any.
     */
    public ArrayList<Playlist> getPlaylists() throws InterruptedException;

    /**
     * <p>
     * getPlaylist.
     * </p>
     *
     * @param playlistId   a {@link java.lang.String} object
     * @param playlistName a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Playlist} object
     * @throws java.lang.InterruptedException if any.
     */
    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException;

    /**
     * <p>
     * getAlbums.
     * </p>
     *
     * @return a {@link java.util.ArrayList} object
     * @throws java.lang.InterruptedException if any.
     */
    public ArrayList<Album> getAlbums() throws InterruptedException;

    /**
     * <p>
     * getAlbum.
     * </p>
     *
     * @param albumId         a {@link java.lang.String} object
     * @param albumName       a {@link java.lang.String} object
     * @param albumArtistName a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException;
}
