package ryzen.ownitall.library;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;

/**
 * <p>LibraryInterface interface.</p>
 *
 * @author ryzen
 */
public interface LibraryInterface {
    /**
     * <p>
     * getAlbum.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @return a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public Album getAlbum(Album album) throws InterruptedException;

    /**
     * <p>
     * getSong.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    public Song getSong(Song song) throws InterruptedException;

    /**
     * <p>
     * getArtist.
     * </p>
     *
     * @param artist a {@link ryzen.ownitall.classes.Artist} object
     * @return a {@link ryzen.ownitall.classes.Artist} object
     * @throws java.lang.InterruptedException if any.
     */
    public Artist getArtist(Artist artist) throws InterruptedException;
}
