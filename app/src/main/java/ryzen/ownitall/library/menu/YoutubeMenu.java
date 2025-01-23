package ryzen.ownitall.library.menu;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.library.Youtube;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class YoutubeMenu {
    private static final Logger logger = LogManager.getLogger(YoutubeMenu.class);
    private Collection collection;
    private Youtube youtube;

    public YoutubeMenu() {
        this.collection = new Collection();
        this.youtube = new Youtube();
    }

    public void youtubeImportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "YOUTUBE");
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
        this.collection.mergeCollection(this.youtube.getCollection());
        pb.close();
        logger.info("Done importing youtube music");
    }

    public Collection getCollection() {
        return this.collection;
    }
}
