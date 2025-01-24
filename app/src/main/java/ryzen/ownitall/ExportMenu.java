package ryzen.ownitall;

import java.util.LinkedHashMap;

import ryzen.ownitall.util.Menu;
import ryzen.ownitall.library.menu.*;

public class ExportMenu {
    /**
     * default constructor
     * 
     * @param collection - known library to export
     */
    public ExportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Download (YoutubeDL)", this::optionDownload);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "EXPORT");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    private void optionDownload() {
        new DownloadMenu();
    }
}
