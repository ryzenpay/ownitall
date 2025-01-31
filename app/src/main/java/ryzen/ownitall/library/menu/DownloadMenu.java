package ryzen.ownitall.library.menu;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.library.Download;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class DownloadMenu {
    private static final Logger logger = LogManager.getLogger(DownloadMenu.class);
    private static Collection collection = Collection.load();
    private Download download;

    public DownloadMenu() {
        this.download = new Download();
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Download Library", this::optionDownloadCollection);
        options.put("Download Playlist", this::optionDownloadPlaylist);
        options.put("Download Album", this::optionDownloadAlbum);
        options.put("Download Liked Songs", this::optionDownloadLikedSongs);
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
        download.downloadPlaylists(collection.getPlaylists());
        pb.setExtraMessage("Albums").step();
        download.downloadAlbums(collection.getAlbums());
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("Done downloading music");
        // TODO: error handling for failed songs
        download.getFailedSongsReport();
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
        download.getFailedSongsReport();
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
        download.getFailedSongsReport();
    }

    private void optionDownloadLikedSongs() {
        logger.info("Downloading liked songs...");
        download.downloadLikedSongs();
        logger.info("Done downloading liked songs");
        download.getFailedSongsReport();
    }
}
