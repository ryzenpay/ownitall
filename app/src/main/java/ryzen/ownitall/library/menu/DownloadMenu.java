package ryzen.ownitall.library.menu;

import java.io.File;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.library.Download;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class DownloadMenu {
    private static final Logger logger = LogManager.getLogger(DownloadMenu.class);
    private static final Settings settings = Settings.load();
    private static Collection collection = Collection.load();
    private Download download;

    public DownloadMenu() {
        this.download = new Download();
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Download Library", this::optionDownloadCollection);
        options.put("Download Playlist", this::optionDownloadPlaylist);
        options.put("Download Album", this::optionDownloadAlbum);
        options.put("Download Liked Songs", this::optionDownloadLikedSongs);
        options.put("Write Collection Data", this::optionCollectionData);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    /**
     * option to locally download entire collection
     */
    private void optionDownloadCollection() {
        logger.info("Downloading music...");
        ProgressBar pb = Progressbar.progressBar("Download music", 3);
        pb.setExtraMessage("Liked songs");
        download.downloadLikedSongs();
        pb.setExtraMessage("Playlists").step();
        download.downloadPlaylists();
        pb.setExtraMessage("Albums").step();
        download.downloadAlbums();
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("Done downloading music");
    }

    private void optionDownloadPlaylist() {
        logger.info("Download Playlist...");
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        for (Playlist playlist : collection.getPlaylists()) {
            options.put(playlist.toString(), playlist);
        }
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD PLAYLIST");
            if (choice.equals("Exit")) {
                break;
            } else {
                download.downloadPlaylist(options.get(choice));
                break;
            }
        }
        logger.info("Done downloading playlist");
    }

    private void optionDownloadAlbum() {
        logger.info("Downloading album...");
        LinkedHashMap<String, Album> options = new LinkedHashMap<>();
        for (Album album : collection.getAlbums()) {
            options.put(album.toString(), album);
        }
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD ALBUM");
            if (choice.equals("Exit")) {
                break;
            } else {
                download.downloadAlbum(options.get(choice));
                break;
            }
        }
        logger.info("Done donwloading album");
    }

    private void optionDownloadLikedSongs() {
        logger.info("Downloading liked songs...");
        download.downloadLikedSongs();
        logger.info("Done downloading liked songs");
    }

    private void optionCollectionData() {
        logger.info("Writing collection data (M3U, NFO, coverimages)...");
        String downloadPath = download.getDownloadPath();
        for (Album album : collection.getAlbums()) {
            File albumFolder = new File(downloadPath, album.getFolderName());
            download.writeAlbumData(album, albumFolder);
        }
        for (Playlist playlist : collection.getPlaylists()) {
            File playlistFolder;
            if (settings.isDownloadHierachy()) {
                playlistFolder = new File(download.getDownloadPath(), playlist.getFolderName());
                playlistFolder.mkdirs();
            } else {
                playlistFolder = new File(downloadPath);
            }
            download.writePlaylistData(playlist, playlistFolder);
        }
        logger.info("Done writing collection data");
    }
}
