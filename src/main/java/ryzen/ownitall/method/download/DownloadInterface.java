package ryzen.ownitall.method.download;

import java.io.File;

import ryzen.ownitall.classes.Song;

/**
 * <p>DownloadInterface interface.</p>
 *
 * @author ryzen
 */
public interface DownloadInterface {
    /**
     * <p>downloadSong.</p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @param path a {@link java.io.File} object
     */
    public void downloadSong(Song song, File path);
}
