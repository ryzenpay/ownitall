package ryzen.ownitall;

import java.util.LinkedHashMap;

import ryzen.ownitall.tools.Input;
import ryzen.ownitall.tools.Menu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static Settings settings = Settings.load();
    private static Sync sync = Sync.load();
    private static Collection collection;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::optionSave));
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        if (sync.checkDataFolder()) {
            logger.info("Local data found, attempting to import...");
            collection = sync.importCollection();
        } else {
            logger.info("No local data found");
            collection = new Collection();
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
            try {
                String choice = Menu.optionMenu(options.keySet(), "MAIN MENU");
                if (choice.equals("Exit")) {
                    System.out.println("Exiting program. Goodbye!");
                    logger.info("Exiting program...");
                    System.exit(0);
                } else {
                    options.get(choice).run();
                }
            } catch (Exception e) {
                System.out.println("Interrupted: " + e);
            }
        }
    }

    private static void optionImport() {
        // TODO: import soundcloud, apple music?
        ImportMenu dataImport = new ImportMenu();
        collection.mergeCollection(dataImport.getCollection());
    }

    private static void optionExport() {
        new ExportMenu(collection);
    }

    private static void optionPrintInventory() {
        System.out.print("Select recursion (1-3): ");
        int recursion = Input.request().getInt(1, 3);
        collection.printInventory(recursion);
    }

    private static void optionEditInventory() {
        collection.editMenu();
    }

    private static void optionSave() {
        sync.exportCollection(collection);
        try {
            settings.saveSettings();
            if (settings.saveCredentials) {
                Credentials.load().save();
            }
            if (settings.useLibrary) {
                Library.load().save();
            }
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

    public static ProgressBar progressBar(String title, int maxStep) {
        return new ProgressBarBuilder()
                .setInitialMax(maxStep)
                .setTaskName(title)
                // .setConsumer(new DelegatingProgressBarConsumer(logger::info))
                .setStyle(ProgressBarStyle.ASCII)
                .hideEta()
                .build();
    }
}
