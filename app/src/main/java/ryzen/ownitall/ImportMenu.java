package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.library.menu.SpotifyMenu;
import ryzen.ownitall.library.menu.UploadMenu;
import ryzen.ownitall.library.menu.YoutubeMenu;
import ryzen.ownitall.util.Menu;

public class ImportMenu {
    private static final Logger logger = LogManager.getLogger(ImportMenu.class);
    private static Settings settings = Settings.load();
    private static Credentials credentials = Credentials.load();

    /**
     * constructor for Import which also prompts user for import options
     * 
     */
    public ImportMenu() {
        if (settings.useLibrary && credentials.lastFMIsEmpty()) {
            logger.info("No local LastFM API key found");
            credentials.setLastFMCredentials();
        }
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Youtube", this::importYoutube);
        options.put("Spotify", this::importSpotify);
        options.put("Upload", this::uploadMenu);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "IMPORT");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    /**
     * import music from youtube, getting or setting credentials as needed
     */
    private void importYoutube() {
        YoutubeMenu menu = new YoutubeMenu();
        menu.youtubeImportMenu();
    }

    /**
     * import music from spotify, getting or setting credentials as needed
     */
    private void importSpotify() {
        SpotifyMenu menu = new SpotifyMenu();
        menu.SpotifyImportMenu();
    }

    private void uploadMenu() {
        new UploadMenu();
    }

}
