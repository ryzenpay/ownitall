package ryzen.ownitall.ui.cli;

import java.util.LinkedHashMap;

import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.exceptions.AuthenticationException;
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
    public LibraryMenu() throws InterruptedException {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        // main menu
        options.put("Change Provider", this::optionChange);
        options.put("Clear Cache", this::optionClearCache);
        options.put("Cache Size", this::optionCacheSize);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "MAIN MENU");
            if (choice.equals("Exit")) {
                return;
            } else {
                options.get(choice).run();
            }
        }
    }

    private void optionChange() {
        try {
            String choice = Menu.optionMenu(Library.libraries.keySet(), "LIBRARIES");
            if (choice.equals("Exit")) {
                throw new InterruptedException("Exited");
            }
            Class<? extends Library> libraryClass = Library.libraries.get(choice);
            while (true) {
                try {
                    Library.initLibrary(libraryClass);
                    break;
                } catch (MissingSettingException e) {
                    logger.info("Missing settings to set up library '" + libraryClass.getSimpleName() + "'");
                    setCredentials(libraryClass);
                } catch (AuthenticationException e) {
                    logger.info("Authentication exception setting up library '" + libraryClass.getSimpleName()
                            + "', retrying...");
                    Library.clearCredentials(libraryClass);
                    setCredentials(libraryClass);
                } catch (NoSuchMethodException e) {
                    logger.error("library '" + libraryClass.getSimpleName() + "' does not exist", e);
                    break;
                }
            }
            Settings.load().set("libraryType", Library.libraries.get(choice));
            logger.info("Successfully changed library type to '" + choice + "'");
        } catch (InterruptedException | MissingSettingException e) {
            logger.debug("Interrupted while getting library change option");
        }
    }

    private static void setCredentials(Class<? extends Library> type)
            throws MissingSettingException, InterruptedException {
        if (type == null) {
            logger.debug("null type provided in setCredentials");
            return;
        }
        Settings credentials = Settings.load();
        LinkedHashMap<String, String> classCredentials = credentials.getGroup(type);
        if (classCredentials != null) {
            for (String name : classCredentials.keySet()) {
                System.out.print("Enter '" + name + "': ");
                String value = Input.request().getString();
                if (!credentials.set(classCredentials.get(name), value)) {
                    throw new MissingSettingException(
                            "Unable to set credential '" + name + "' for '" + type.getSimpleName() + "'");
                }
            }
        }
        if (credentials.isGroupEmpty(type)) {
            throw new MissingSettingException("Unable to set credentials for '" + type.getSimpleName() + "'");
        }
    }

    private void optionClearCache() {
        try {
            System.out.print("Are you sure you wan to clear cache (y/N): ");
            if (Input.request().getAgreement()) {
                logger.info("Clearing cache...");
                Library.clear();
                logger.info("Done clearing cache");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting clear cache agreement");
        }
    }

    private void optionCacheSize() {
        Library.load();
        int size = Library.getCacheSize();
        logger.info("The library cache has '" + size + "' entries");
    }
}
