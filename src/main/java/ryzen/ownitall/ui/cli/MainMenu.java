package ryzen.ownitall.ui.cli;

import java.util.LinkedHashMap;

import ryzen.ownitall.Main;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.exceptions.ClosedMenu;

/**
 * <p>
 * MainMenu class.
 * </p>
 *
 * @author ryzen
 */
public class MainMenu {
    private static final Logger logger = new Logger(MainMenu.class);

    /**
     * <p>
     * Constructor for MainMenu.
     * </p>
     */
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
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.info("Interruption caught in main menu, gracefully closing program");
            exit();
        } catch (ClosedMenu e) {
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
