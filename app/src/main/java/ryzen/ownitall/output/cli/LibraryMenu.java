package ryzen.ownitall.output.cli;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;

public class LibraryMenu {
    private static final Logger logger = LogManager.getLogger();
    private static final Credentials credentials = Credentials.load();
    private static final Settings settings = Settings.load();

    // TODO: statistics?
    public LibraryMenu() throws InterruptedException {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        // main menu
        options.put("Change", this::optionChange);
        options.put("Clear Cache", this::optionClearCache);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "MAIN MENU");
            if (choice.equals("Exit")) {
                return;
            } else {
                options.get(choice).run();
            }
        }
    }

    public static void initializeLibrary() throws InterruptedException {
        if (settings.isEmpty("librarytype")) {
            return;
        }
        Class<? extends Library> libraryClass = Library.libraries.get(settings.getString("librarytype"));
        if (!Library.isCredentialsEmpty(libraryClass)) {
            return;
        }
        setCredentials(libraryClass);
        if (Library.isCredentialsEmpty(libraryClass)) {
            throw new InterruptedException("Unable to set credentials for '" + libraryClass.getSimpleName() + "'");
        }
    }

    private void optionChange() {
        try {
            String choice = Menu.optionMenu(Library.libraries.keySet(), "LIBRARIES");
            if (choice.equals("Exit")) {
                throw new InterruptedException("Exited");
            }
            Class<? extends Library> libraryClass = Library.libraries.get(choice);
            settings.change("librarytype", choice.toLowerCase());
            setCredentials(libraryClass);
            logger.info("Successfully changed library type to '" + choice + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting library change option");
        }
    }

    private static void setCredentials(Class<?> type) throws InterruptedException {
        if (type == null) {
            logger.debug("null type provided in setCredentials");
            return;
        }
        LinkedHashMap<String, String> classCredentials = Library.credentialGroups.get(type);
        if (classCredentials != null) {
            for (String name : classCredentials.keySet()) {
                System.out.print("Enter '" + name + "': ");
                String value = Input.request().getString();
                if (!credentials.change(classCredentials.get(name), value)) {
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
}
