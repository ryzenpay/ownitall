package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.util.Menu;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final Settings settings = Settings.load();
    private static final Sync sync = Sync.load();

    /**
     * main function launching the main ownitall menu
     * 
     * @param args - possible arguments to pass (not defined)
     */
    public static void main(String[] args) {
        Menu.clearScreen();
        Runtime.getRuntime().addShutdownHook(new Thread(Main::optionSave));
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        Collection.load().mergeCollection(sync.importCollection());
        // main menu
        options.put("Import", Main::optionImport);
        options.put("Export", Main::optionExport);
        options.put("Inventory", Main::optionInventory);
        options.put("Save", Main::optionSave);
        options.put("Tools", Main::optionTools);
        options.put("Settings", Main::optionSettings);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "MAIN MENU");
            if (choice.equals("Exit")) {
                System.out.println("Exiting program. Goodbye!");
                logger.info("Exiting program...");
                System.exit(0);
            } else {
                options.get(choice).run();
            }
        }
    }

    /**
     * import menu
     */
    private static void optionImport() {
        new ImportMenu();
    }

    /**
     * export menu
     */
    private static void optionExport() {
        new ExportMenu();
    }

    /**
     * edit current library
     */
    private static void optionInventory() {
        new CollectionMenu();
    }

    /**
     * save current library to local files
     */
    private static void optionSave() {
        Collection.load().save();
        Settings.load().save();
        if (settings.saveCredentials) {
            Credentials.load().save();
        }
        if (settings.useLibrary) {
            Library.load().save();
        }
    }

    /**
     * tools menu
     */
    private static void optionTools() {
        new Tools();
    }

    /**
     * change settings menu
     */
    private static void optionSettings() {
        try {
            settings.changeSettings();
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
