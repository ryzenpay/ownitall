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
    private Collection collection;

    public DownloadMenu(Collection collection) {
        this.collection = collection;
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
        Download download = new Download();
        ProgressBar pb = Progressbar.progressBar("Download music", 3);
        pb.setExtraMessage("Liked songs");
        download.downloadLikedSongs(this.collection.getLikedSongs());
        pb.setExtraMessage("Playlists").step();
        download.downloadPlaylists(this.collection.getPlaylists());
        pb.setExtraMessage("Albums").step();
        download.downloadAlbums(this.collection.getAlbums());
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("Done downloading music");
    }

    private void optionDownloadPlaylist() {
        logger.info("Download Playlist...");
        Download download = new Download();
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        for (Playlist playlist : this.collection.getPlaylists()) {
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
        Download download = new Download();
        LinkedHashMap<String, Album> options = new LinkedHashMap<>();
        for (Album album : this.collection.getAlbums()) {
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
        Download download = new Download();
        download.downloadLikedSongs(this.collection.getLikedSongs());
        logger.info("Done downloading liked songs");
    }
}
