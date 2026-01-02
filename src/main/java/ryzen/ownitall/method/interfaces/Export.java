package ryzen.ownitall.method.interfaces;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;

/**
 * <p>
 * Export interface.
 * </p>
 *
 * @author ryzen
 */
public interface Export {

    /**
     * <p>
     * uploadLikedSongs.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadLikedSongs() throws InterruptedException;

    /**
     * <p>
     * uploadPlaylists.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadPlaylists() throws InterruptedException;

    /**
     * <p>
     * uploadPlaylist.
     * </p>
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadPlaylist(Playlist playlist) throws InterruptedException;

    /**
     * <p>
     * uploadAlbums.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadAlbums() throws InterruptedException;

    /**
     * <p>
     * uploadAlbum.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadAlbum(Album album) throws InterruptedException;

}
