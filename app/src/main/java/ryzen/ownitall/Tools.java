package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.CLIMenu;

public class Tools {
    private static final Logger logger = LogManager.getLogger(Tools.class);
    private static Tools instance;

    /**
     * default tools constructor prompting tools menu
     */
    public Tools() {
        // TODO: isolate menu
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Archive", this::optionArchive);
        options.put("UnArchive", this::optionUnArchive);
        options.put("Clear Cache", this::optionClearCache);
        options.put("Clear Saved Logins", this::optionClearCredentials);
        options.put("Reset Settings", this::optionClearSettings);
        try {
            while (true) {
                String choice = CLIMenu.optionMenu(options.keySet(), "TOOL MENU");
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting tools menu response");
        }
    }

    /**
     * tools instance
     * 
     * @return - constructed or new tools instance
     */
    public static Tools load() {
        if (instance == null) {
            instance = new Tools();
        }
        return instance;
    }

    /**
     * trigger sync archive
     */
    private void optionArchive() {
        try {
            Storage.load().archive(true);
            System.out.print("Press Enter to continue...");
            Input.request().getEnter();
        } catch (InterruptedException e) {

        }
    }

    /**
     * trigger sync unarchive
     */
    private void optionUnArchive() {
        try {
            Storage.load().unArchive();
            System.out.print("Press Enter to continue...");
            Input.request().getEnter();
        } catch (InterruptedException e) {

        }
    }

    /**
     * clear memory cache and local cache
     */
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

    /**
     * clear memory credentials and local credentials
     */
    private void optionClearCredentials() {
        Credentials.load().clear();
        logger.info("Cleared credentials");
    }

    /**
     * clear memory settings and local settings
     */
    private void optionClearSettings() {
        Settings.load().clear();
        logger.info("Cleared settings");
    }
}
