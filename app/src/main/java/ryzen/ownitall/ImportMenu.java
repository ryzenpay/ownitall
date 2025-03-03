package ryzen.ownitall;

import java.util.LinkedHashMap;

import ryzen.ownitall.methods.menu.ManualMenu;
import ryzen.ownitall.methods.menu.SpotifyMenu;
import ryzen.ownitall.methods.menu.UploadMenu;
import ryzen.ownitall.methods.menu.YoutubeMenu;
import ryzen.ownitall.util.Menu;

public class ImportMenu {

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
    private void optionYoutube() {
        YoutubeMenu menu = new YoutubeMenu();
        menu.youtubeImportMenu();
    }

    /**
     * import music from spotify, getting or setting credentials as needed
     */
    private void optionSpotify() {
        new SpotifyMenu().spotifyImportMenu();
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
