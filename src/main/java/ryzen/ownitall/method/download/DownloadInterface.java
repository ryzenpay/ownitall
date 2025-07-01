package ryzen.ownitall.method.download;

import java.io.File;
import java.util.ArrayList;

import ryzen.ownitall.classes.Song;

/**
 * <p>
 * DownloadInterface interface.
 * </p>
 *
 * @author ryzen
 */
public interface DownloadInterface {

    public ArrayList<String> createCommand(Song song, File downloadFile) throws InterruptedException;

    public void handleError(int errorCode) throws DownloadException;
}
