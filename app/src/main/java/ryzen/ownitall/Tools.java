package ryzen.ownitall;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;

public class Tools {
    private static final Logger logger = LogManager.getLogger(Tools.class);

    /**
     * trigger sync archive
     */
    public static void archive() {
        Storage.load().archive(true);
    }

    /**
     * trigger sync unarchive
     */
    public static void unArchive() {
        Storage.load().unArchive();
    }

    /**
     * clear memory cache and local cache
     */
    public static void clearCache() {
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
    public static void clearCredentials() {
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

    /**
     * clear memory settings and local settings
     */
    public static void clearSettings() {
        Settings.load().clear();
        logger.info("Cleared settings");
    }
}
