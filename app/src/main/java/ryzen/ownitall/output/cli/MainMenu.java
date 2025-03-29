package ryzen.ownitall.output.cli;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.CLIMenu;

public class MainMenu {
    private static final Logger logger = LogManager.getLogger(MainMenu.class);

    public MainMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        // main menu
        options.put("Collection", this::optionCollection);
        options.put("Save", this::optionSave);
        options.put("Tools", this::optionTools);
        options.put("Settings", this::optionSettings);
        try {
            while (true) {
                String choice = CLIMenu.optionMenu(options.keySet(), "MAIN MENU");
                if (choice.equals("Exit")) {
                    exit();
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.info("Interruption caught in main menu, gracefully closing program");
            exit();
        }
    }

    /**
     * collection menu
     */
    private void optionCollection() {
        new CollectionMenu();
    }

    /**
     * save current library to local files
     */
    private void optionSave() {
        Settings settings = Settings.load();
        Collection.load().save();
        settings.save();
        if (settings.isSaveCredentials()) {
            Credentials.load().save();
        }
        if (Library.checkInstance()) {
            Library.load(); // caches in the load
        }
    }

    /**
     * tools menu
     */
    private void optionTools() {
        new ToolsMenu();
    }

    /**
     * change settings menu
     */
    private void optionSettings() {
        Settings.load().changeSettings();
    }

    private void exit() {
        logger.info("Exiting program...");
        optionSave();
        System.exit(0);
    }
}
