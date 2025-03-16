package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.methods.Jellyfin;
import ryzen.ownitall.methods.menu.*;
import ryzen.ownitall.util.Menu;

public class ExportMenu {
    private static final Logger logger = LogManager.getLogger(ExportMenu.class);

    /**
     * default constructor
     * 
     */
    public ExportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Download", this::optionDownload);
        options.put("Spotify", this::optionSpotify);
        options.put("Jellyfin", this::optionJellyfin);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "EXPORT");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }

            }
        } catch (InterruptedException e) {
            logger.debug("Interruption caught while getting export menu choice");
        }
    }

    /**
     * download music locally
     */
    private void optionDownload() {
        try {
            new DownloadMenu();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting download menu");
        }
    }

    private void optionSpotify() {
        try {
            new SpotifyMenu().exportMenu();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting spotify export menu");
        }
    }

    private void optionJellyfin() {
        try {
            new JellyfinMenu().exportMenu();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting up jellyfin");
        }
    }
}
