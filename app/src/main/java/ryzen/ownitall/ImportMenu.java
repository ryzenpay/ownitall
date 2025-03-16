package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.methods.menu.*;
import ryzen.ownitall.util.Menu;

public class ImportMenu {
    private static final Logger logger = LogManager.getLogger(ImportMenu.class);

    /**
     * constructor for Import which also prompts user for import options
     * 
     */
    public ImportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Youtube", this::optionYoutube);
        options.put("Spotify", this::optionSpotify);
        options.put("Local", this::optionLocal);
        options.put("Manual", this::optionManual);
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
     * import music from youtube, getting or setting credentials as needed
     */
    private void optionYoutube() {
        try {
            new YoutubeMenu().importMenu();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting up youtube import menu");
        }
    }

    /**
     * import music from spotify, getting or setting credentials as needed
     */
    private void optionSpotify() {
        try {
            new SpotifyMenu().importMenu();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting up spotify import menu");
        }
    }

    /**
     * import music from local library
     */
    private void optionLocal() {
        new UploadMenu();
    }

    private void optionManual() {
        new ManualMenu();
    }

}
