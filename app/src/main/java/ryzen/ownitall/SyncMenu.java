package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.methods.menu.*;
import ryzen.ownitall.util.Menu;

public class SyncMenu {
    private static final Logger logger = LogManager.getLogger(SyncMenu.class);

    /**
     * constructor for Import which also prompts user for import options
     * 
     */
    public SyncMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Spotify", this::optionSpotify);
        options.put("Local", this::optionLocal);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "IMPORT");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }

            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting import menu choice");
        }
    }

    /**
     * import music from spotify, getting or setting credentials as needed
     */
    private void optionSpotify() {
        try {
            new SpotifyMenu().sync();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting up spotify sync");
        }
    }

    /**
     * import music from local library
     */
    private void optionLocal() {
        try {
            new DownloadMenu().sync();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting up download sync");
        }
    }
}
