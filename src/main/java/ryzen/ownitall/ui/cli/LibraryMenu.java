package ryzen.ownitall.ui.cli;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.ClosedMenu;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * LibraryMenu class.
 * </p>
 *
 * @author ryzen
 */
public class LibraryMenu {
    private static final Logger logger = new Logger(LibraryMenu.class);

    /**
     * <p>
     * Constructor for LibraryMenu.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public LibraryMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        // main menu
        options.put("Change Provider", this::optionChange);
        options.put("Clear Cache", this::optionClearCache);
        options.put("Cache Size", this::optionCacheSize);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "MAIN MENU");
                options.get(choice).run();
            }
        } catch (ClosedMenu e) {
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting LibraryMenu choice");
        }
    }

    private void optionChange() {
        try {
            LinkedHashMap<String, Class<? extends Library>> options = new LinkedHashMap<>();
            for (Class<? extends Library> libraryClass : Library.getLibraries()) {
                options.put(libraryClass.getSimpleName(), libraryClass);
            }
            String choice = Menu.optionMenu(options.keySet(), "LIBRARIES");
            Class<? extends Library> libraryClass = options.get(choice);
            while (true) {
                try {
                    Library.initLibrary(libraryClass);
                    break;
                } catch (MissingSettingException e) {
                    logger.warn("Missing settings to set up library '" + libraryClass.getSimpleName() + "': "
                            + e.getMessage());
                    setCredentials(libraryClass);
                } catch (AuthenticationException e) {
                    logger.warn("Authentication exception setting up library '" + libraryClass.getSimpleName()
                            + "': " + e.getMessage());
                    Library.clearCredentials(libraryClass);
                    setCredentials(libraryClass);
                } catch (NoSuchMethodException e) {
                    logger.error("library '" + libraryClass.getSimpleName() + "' does not exist", e);
                    break;
                }
            }
            Settings.libraryType = options.get(choice).getSimpleName();
            logger.info("Successfully changed library type to '" + choice + "'");
        } catch (InterruptedException | MissingSettingException | ClosedMenu e) {
            logger.debug("Interrupted while getting library change option");
        }
    }

    private static void setCredentials(Class<? extends Library> type)
            throws MissingSettingException, InterruptedException {
        if (type == null) {
            logger.debug("null type provided in setCredentials");
            return;
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(type);
        if (credentials != null) {
            try {
                SettingsMenu.changeSettings(credentials);
            } catch (NoSuchFieldException e) {
                logger.error("Unable to find setting to change", e);
            }
        }
    }

    private void optionClearCache() {
        try {
            System.out.print("Are you sure you wan to clear cache (y/N): ");
            if (Input.request().getAgreement()) {
                logger.info("Clearing cache...");
                Library.load().clear();
                logger.info("Done clearing cache");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting clear cache agreement");
        }
    }

    private void optionCacheSize() {
        int size = Library.load().getCacheSize();
        logger.info("The library cache has '" + size + "' entries");
    }
}
