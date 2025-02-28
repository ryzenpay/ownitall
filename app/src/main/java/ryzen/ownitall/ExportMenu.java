package ryzen.ownitall;

import java.util.LinkedHashMap;

import ryzen.ownitall.methods.menu.*;
import ryzen.ownitall.util.Menu;

public class ExportMenu {
    /**
     * default constructor
     * 
     */
    public ExportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Download", this::optionDownload);
        options.put("Spotify", this::optionSpotify);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "EXPORT");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    /**
     * download music locally
     */
    private void optionDownload() {
        new DownloadMenu();
    }

    private void optionSpotify() {
        new SpotifyMenu().spotifyExportMenu();
    }
}
