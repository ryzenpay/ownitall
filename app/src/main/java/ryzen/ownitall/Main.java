package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static Settings settings = Settings.load();
    private static Sync sync = Sync.load();
    private static Collection collection = Collection.load();

    /**
     * main function launching the main ownitall menu
     * 
     * @param args - possible arguments to pass (not defined)
     */
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::optionSave));
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        if (sync.checkDataFolder()) {
            logger.info("Local data found, attempting to import...");
            collection.mergeCollection(sync.importCollection());
        }
        // main menu
        options.put("Import", Main::optionImport);
        options.put("Export", Main::optionExport);
        options.put("Print Inventory", Main::optionPrintInventory);
        options.put("Edit Inventory", Main::optionEditInventory);
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
     * print library of current imported music
     */
    private static void optionPrintInventory() {
        System.out.print("Select recursion (1-3): ");
        int recursion = Input.request().getInt(1, 3);
        collection.printInventory(recursion);
    }

    /**
     * edit current library
     */
    private static void optionEditInventory() {
        collection.editMenu();
    }

    /**
     * save current library to local files
     */
    private static void optionSave() {
        sync.exportCollection(collection);
        settings.saveSettings();
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
