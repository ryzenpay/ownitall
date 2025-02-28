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

    public YoutubeMenu() {
        this.youtube = new Youtube();
    }

    public void youtubeImportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        options.put("Import Liked Songs", this::optionImportLikedSongs);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "IMPORT YOUTUBE");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    private void optionImportCollection() {
        logger.info("Importing youtube music");
        ProgressBar pb = Progressbar.progressBar("Youtube Import", 3);
        pb.setExtraMessage("Liked songs");
        this.youtube.getLikedSongs();
        pb.setExtraMessage("Saved Albums").step();
        this.youtube.getAlbums();
        pb.setExtraMessage("Playlists").step();
        this.youtube.getPlaylists();
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("Done importing youtube music");
    }

    private void optionImportLikedSongs() {
        logger.info("Importing youtube liked songs...");
        this.youtube.getLikedSongs();
        logger.info("Done importing youtube liked songs");
    }
}
