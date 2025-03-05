package ryzen.ownitall.methods.menu;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.methods.Youtube;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class YoutubeMenu {
    private static final Logger logger = LogManager.getLogger(YoutubeMenu.class);
    private Youtube youtube;

    public YoutubeMenu() throws InterruptedException {
        this.youtube = new Youtube();
    }

    public void youtubeImportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        options.put("Import Liked Songs", this::optionImportLikedSongs);
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
        logger.info("Importing youtube music");
        try (ProgressBar pb = Progressbar.progressBar("Youtube Import", 3)) {
            pb.setExtraMessage("Liked songs");
            this.youtube.getLikedSongs();
            pb.setExtraMessage("Saved Albums").step();
            this.youtube.getAlbums();
            pb.setExtraMessage("Playlists").step();
            this.youtube.getPlaylists();
            pb.setExtraMessage("Done").step();
            logger.info("Done importing youtube music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing collection");
        }
    }

    private void optionImportLikedSongs() {
        logger.info("Importing youtube liked songs...");
        try {
            this.youtube.getLikedSongs();
            logger.info("Done importing youtube liked songs");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing liked song");
        }
    }
}
