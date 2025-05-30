package ryzen.ownitall.method.download;

import java.io.File;

import ryzen.ownitall.classes.Song;

public interface DownloadInterface {
    public void downloadSong(Song song, File path);
}
