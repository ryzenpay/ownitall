package ryzen.ownitall.methods.menu;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.methods.Jellyfin;
import ryzen.ownitall.util.Menu;

public class JellyfinMenu {
    private static final Logger logger = LogManager.getLogger(JellyfinMenu.class);
    private static final Settings settings = Settings.load();
    private static Collection collection = Collection.load();
    Jellyfin jellyfin;

    public JellyfinMenu() throws InterruptedException {
        jellyfin = new Jellyfin();
    }

    public void importMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "IMPORT JELLYFIN");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting jellyfin import menu choice");
        }
    }

    public void exportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Export Liked Songs (favorites)", this::optionExportLikedSongs);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "EXPORT JELLYFIN");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting jellyfin export menu choice");
        }
    }

    private void optionExportLikedSongs() {
        logger.debug("Marking all liked songs as favorites...");
        this.jellyfin.uploadLikedSongs(collection.getLikedSongs().getSongs());
        logger.debug("successfully marked all liked songs as favorites");
    }
}
