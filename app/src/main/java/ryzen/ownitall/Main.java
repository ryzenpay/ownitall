package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sun.misc.Signal;
import sun.misc.SignalHandler;

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
        // do nothing with SIGINT as menu should catch it
        Signal.handle(new Signal("INT"), SignalHandler.SIG_IGN);
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        sync.importCollection();
        // main menu
        options.put("Import", Main::optionImport);
        options.put("Export", Main::optionExport);
        options.put("Inventory", Main::optionInventory);
        options.put("Save", Main::optionSave);
        options.put("Tools", Main::optionTools);
        options.put("Settings", Main::optionSettings);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "MAIN MENU");
                if (choice.equals("Exit")) {
                    logger.info("Exiting program...");
                    optionSave();
                    System.exit(0);
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.info("Interruption caught in main menu, gracefully closing program");
            optionSave();
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
        if (Library.checkInstance()) {
            Library.load().cache();
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
        settings.changeSettings();
    }
}
