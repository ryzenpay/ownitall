package ryzen.ownitall;

import java.util.LinkedHashMap;

import ryzen.ownitall.library.menu.SpotifyMenu;
import ryzen.ownitall.library.menu.UploadMenu;
import ryzen.ownitall.library.menu.YoutubeMenu;
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

}
