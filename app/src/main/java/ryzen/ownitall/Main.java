package ryzen.ownitall;

import java.util.LinkedHashMap;

import ryzen.ownitall.tools.Input;
import ryzen.ownitall.tools.Menu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static Settings settings = Settings.load();
    private static Collection collection;

    public static void main(String[] args) {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        collection = new Collection();
        if (Sync.load().checkDataFolder()) {
            logger.info("Local data found, attempting to import...");
            collection.importData();
        } else {
            logger.info("No local data found");
        }
        // main menu
        options.put("Import", Main::optionImport);
        options.put("Export", Main::optionExport);
        options.put("Print Inventory", Main::optionInventory);
        options.put("Save", Main::optionSave);
        options.put("Tools", Main::optionTools);
        options.put("Settings", Main::optionSettings);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "MAIN MENU");
            if (choice == "Exit") {
                exit();
            } else {
                options.get(choice).run();
            }
        }
    }

    private static void optionImport() {
        Import dataImport = new Import();
        collection.mergeAlbums(dataImport.getAlbums());
        collection.mergePlaylists(dataImport.getPlaylists());
        collection.mergeLikedSongs(dataImport.getLikedSongs());
    }

    private static void optionExport() {
        System.out.println("Export currently not supported");
        // TODO: Implement export functionality
    }

    private static void optionInventory() {
        System.out.print("Select recursion (1-3): ");
        int recursion = Input.request().getInt(1, 3);
        collection.printInventory(recursion);
    }

    private static void optionSave() {
        collection.exportData();
        try {
            settings.saveSettings();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private static void optionTools() {
        new Tools();
    }

    private static void optionSettings() {
        try {
            settings.changeSettings();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private static void exit() {
        optionSave();
        System.out.println("Exiting program. Goodbye!");
        logger.info("Exiting program...");
        System.exit(0);
    }
}
