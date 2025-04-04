package ryzen.ownitall.output.cli;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Main;
import ryzen.ownitall.util.Menu;

public class MainMenu {
    private static final Logger logger = LogManager.getLogger();

    public MainMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        // main menu
        options.put("Collection", this::optionCollection);
        options.put("Save", this::optionSave);
        options.put("Tools", this::optionTools);
        options.put("Settings", this::optionSettings);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "MAIN MENU");
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
        Main.save();
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
        new SettingsMenu();
    }

    private void exit() {
        logger.info("Exiting program...");
        optionSave();
        System.exit(0);
    }
}
