package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;

public class Tools {
    private static final Logger logger = LogManager.getLogger(Tools.class);
    private static Tools instance;

    /**
     * default tools constructor prompting tools menu
     */
    public Tools() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Archive", this::optionArchive);
        options.put("UnArchive", this::optionUnArchive);
        options.put("Clear Cache", this::optionClearCache);
        options.put("Clear Saved Logins", this::optionClearCredentials);
        options.put("Reset Settings", this::optionClearSettings);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "TOOL MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
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
        Sync.load().archive(true);
        Input.request().getEnter();
    }

    /**
     * trigger sync unarchive
     */
    private void optionUnArchive() {
        Sync.load().unArchive();
        Input.request().getEnter();
    }

    /**
     * clear memory cache and local cache
     */
    private void optionClearCache() {
        try {
            System.out.print("Are you sure you wan to clear cache (y/N): ");
            if (Input.request().getAgreement()) {
                logger.info("Clearing cache...");
                Sync.load().clearCache();
                if (Library.checkInstance()) { // to prevent logging in
                    Library.load().clear();
                }
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
