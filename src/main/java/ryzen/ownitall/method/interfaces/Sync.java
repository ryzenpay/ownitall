package ryzen.ownitall.method.interfaces;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>Sync interface.</p>
 *
 * @author ryzen
 */
public interface Sync {
    /**
     * <p>
     * syncLikedSongs.
     * </p>
     *
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncLikedSongs() throws InterruptedException, MissingSettingException, AuthenticationException;

    /**
     * <p>
     * syncPlaylists.
     * </p>
     *
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncPlaylists() throws InterruptedException, MissingSettingException, AuthenticationException;

    /**
     * <p>
     * syncPlaylist.
     * </p>
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncPlaylist(Playlist playlist)
            throws InterruptedException, MissingSettingException, AuthenticationException;

    /**
     * <p>
     * syncAlbums.
     * </p>
     *
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncAlbums() throws InterruptedException, MissingSettingException, AuthenticationException;

    /**
     * <p>
     * syncAlbum.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public void syncAlbum(Album album) throws InterruptedException, MissingSettingException, AuthenticationException;
}
