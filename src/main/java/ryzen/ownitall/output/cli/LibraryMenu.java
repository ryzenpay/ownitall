package ryzen.ownitall.output.cli;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;

public class LibraryMenu {
    private static final Logger logger = LogManager.getLogger(LibraryMenu.class);
    private static final Settings settings = Settings.load();

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
            Settings.load().set("libraryType", Library.libraries.get(choice));
            initializeLibrary();
            logger.info("Successfully changed library type to '" + choice + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting library change option");
        }
    }

    public static void initializeLibrary() throws InterruptedException {
        if (Settings.libraryType == null) {
            return;
        }
        if (Library.isCredentialsEmpty(Settings.libraryType)) {
            setCredentials(Settings.libraryType);
        }
    }

    private static void setCredentials(Class<? extends Library> type) throws InterruptedException {
        if (type == null) {
            logger.debug("null type provided in setCredentials");
            return;
        }
        LinkedHashMap<String, String> classCredentials = Library.credentialGroups.get(type);
        if (classCredentials != null) {
            for (String name : classCredentials.keySet()) {
                System.out.print("Enter '" + name + "': ");
                String value = Input.request().getString();
                if (!settings.set(classCredentials.get(name), value)) {
                    throw new InterruptedException(
                            "Unable to set credential '" + name + "' for '" + type.getSimpleName() + "'");
                }
            }
        }
        if (Library.isCredentialsEmpty(type)) {
            throw new InterruptedException("Unable to set credentials for '" + type.getSimpleName() + "'");
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
        int size = Library.getCacheSize();
        logger.info("The library cache has '" + size + "' entries");
    }
}
