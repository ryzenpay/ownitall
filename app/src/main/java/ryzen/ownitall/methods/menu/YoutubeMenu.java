package ryzen.ownitall.methods.menu;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.methods.Youtube;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class YoutubeMenu {
    private static final Logger logger = LogManager.getLogger(YoutubeMenu.class);
    private static Collection collection = Collection.load();
    private Youtube youtube;

    public YoutubeMenu() throws InterruptedException {
        this.youtube = new Youtube();
    }

    public void importMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        options.put("Import Liked Songs", this::optionImportLikedSongs);
        options.put("Import playlists", this::optionImportPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "IMPORT YOUTUBE");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting youtube import menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing youtube music");
        try (ProgressBar pb = Progressbar.progressBar("Youtube Import", 3)) {
            pb.setExtraMessage("Liked songs");
            collection.addLikedSongs(this.youtube.getLikedSongs());
            pb.setExtraMessage("Saved Albums").step();
            collection.addAlbums(this.youtube.getAlbums());
            pb.setExtraMessage("Playlists").step();
            collection.addPlaylists(this.youtube.getPlaylists());
            pb.setExtraMessage("Done").step();
            logger.debug("Done importing youtube music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing collection");
        }
    }

    private void optionImportLikedSongs() {
        logger.debug("Importing youtube liked songs...");
        try {
            collection.addLikedSongs(this.youtube.getLikedSongs());
            logger.debug("Done importing youtube liked songs");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing liked song");
        }
    }

    private void optionImportPlaylists() {
        logger.debug("Importing youtube playlists...");
        try {
            collection.addPlaylists(this.youtube.getPlaylists());
            logger.debug("Done importing youtube playlists");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing playlists");
        }
    }
}
