package ryzen.ownitall.ui.cli;

import java.io.File;
import java.util.LinkedHashMap;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Storage;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.exceptions.MenuClosed;

/**
 * <p>
 * ToolsMenu class.
 * </p>
 *
 * @author ryzen
 */
public class ToolsMenu {
    private static final Logger logger = new Logger(ToolsMenu.class);

    /**
     * <p>
     * Constructor for ToolsMenu.
     * </p>
     */
    public ToolsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Archive", this::optionArchive);
        options.put("UnArchive", this::optionUnArchive);
        options.put("Library", this::optionLibrary);
        options.put("Clean Albums", this::optionCleanAlbums);
        options.put("Clear Saved Logins", this::optionClearCredentials);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "TOOL MENU");
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    private void optionArchive() {
        Storage.archive();
        logger.info("Successfully archived");
    }

    private void optionUnArchive() {
        LinkedHashMap<String, File> options = new LinkedHashMap<>();
        for (File file : Storage.getArchiveFolders()) {
            options.put(file.getName(), file);
        }
        try {
            String choice = Menu.optionMenu(options.keySet(), "UNARCHIVING");
            Storage.unArchive(options.get(choice));
        } catch (MenuClosed e) {
        }
    }

    private void optionLibrary() {
        new LibraryMenu();
    }

    private void optionCleanAlbums() {
        logger.info("Cleaning albums...");
        Collection.cleanAlbums();
        logger.info("Done cleaning albums");
    }

    private void optionClearCredentials() {
        try {
            System.out.print("Are you sure you wan to clear Credentials (y/N): ");
            if (Input.request().getAgreement()) {
                logger.info("Clearing Credentials...");
                for (Class<?> methodClass : Method.getMethods()) {
                    Method.clearCredentials(methodClass);
                }
                logger.info("Done clearing Credentials");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting clear Credentials agreement");
        }
    }
}
