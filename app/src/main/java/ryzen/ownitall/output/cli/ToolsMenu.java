package ryzen.ownitall.output.cli;

import java.io.File;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.Storage;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;

public class ToolsMenu {
    private static final Logger logger = LogManager.getLogger();

    public ToolsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Archive", this::optionArchive);
        options.put("UnArchive", this::optionUnArchive);
        options.put("Library", this::optionLibrary);
        options.put("Clear Saved Logins", this::optionClearCredentials);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "TOOL MENU");
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting tools menu choice");
        }
    }

    private void optionArchive() {
        Storage.load().archive();
        logger.info("Successfully archived");
    }

    private void optionUnArchive() {
        Storage storage = Storage.load();
        LinkedHashMap<String, File> options = new LinkedHashMap<>();
        for (File file : storage.getArchiveFolders()) {
            options.put(file.getName(), file);
        }
        try {
            String choice = Menu.optionMenu(options.keySet(), "UNARCHIVING");
            if (choice.equals("Exit")) {
                return;
            }
            storage.unArchive(options.get(choice));
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting unarchive folder choice");
            return;
        }
    }

    private void optionLibrary() {
        try {
            new LibraryMenu();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting library menu choice");
        }
    }

    private void optionClearCredentials() {
        try {
            System.out.print("Are you sure you wan to clear Credentials (y/N): ");
            if (Input.request().getAgreement()) {
                logger.info("Clearing Credentials...");
                Credentials.load().clear();
                logger.info("Done clearing Credentials");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting clear Credentials agreement");
        }
    }
}
