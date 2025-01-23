package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.library.Download;
import ryzen.ownitall.util.Menu;

public class ExportMenu {
    private static final Logger logger = LogManager.getLogger(ExportMenu.class);
    private Collection collection;

    /**
     * default constructor
     * 
     * @param collection - known library to export
     */
    public ExportMenu(Collection collection) {
        this.collection = collection;
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Download (YoutubeDL)", this::optionDownload);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "EXPORT");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    /**
     * option to locally download music library
     */
    private void optionDownload() {
        logger.info("Downloading music...");
        Download download = new Download();
        ProgressBar pb = Main.progressBar("Download music", 3);
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
}
