package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logs;
import ryzen.ownitall.util.Menu;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final Settings settings = Settings.load();
    private static final Storage sync = Storage.load();

    /**
     * main function launching the main ownitall menu
     * 
     * @param args - possible arguments to pass (not defined)
     */
    public static void main(String[] args) {
        handleFlags(args);
        Menu.clearScreen();
        // do nothing with SIGINT as menu should catch it
        Signal.handle(new Signal("INT"), SignalHandler.SIG_IGN);
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        sync.importCollection();
        // main menu
        options.put("Collection", Main::optionCollection);
        options.put("Save", Main::optionSave);
        options.put("Tools", Main::optionTools);
        options.put("Settings", Main::optionSettings);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "MAIN MENU");
                if (choice.equals("Exit")) {
                    logger.info("Exiting program...");
                    optionSave();
                    System.exit(0);
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.info("Interruption caught in main menu, gracefully closing program");
            optionSave();
        }
    }

    // TODO: log level flags
    // this includes putting all log types back into 1
    // modify the vscode config to attach the flag
    private static void handleFlags(String[] args) {
        if (args == null || args.length == 0) {
            logger.debug("no flags provided");
            return;
        }
        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            if (arg != null) {
                if (arg.startsWith("-")) {
                    i++;
                    String param = args[i];
                    if (arg.toLowerCase().contains("-i")) {
                        logger.debug("non interactive parameter provided: " + param);
                        Input.request(param);
                        continue;
                    }
                    if (arg.toLowerCase().contains("-l")) {
                        logger.debug("log level provided: " + param);
                        Logs.setLogLevel(param);
                        continue;
                    }
                }
            }
        }
    }

    /**
     * collection menu
     */
    private static void optionCollection() {
        new CollectionMenu();
    }

    /**
     * save current library to local files
     */
    private static void optionSave() {
        Collection.load().save();
        Settings.load().save();
        if (settings.saveCredentials) {
            Credentials.load().save();
        }
        if (Library.checkInstance()) {
            Library.load(); // caches in the load
        }
    }

    /**
     * tools menu
     */
    private static void optionTools() {
        new Tools();
    }

    /**
     * change settings menu
     */
    private static void optionSettings() {
        settings.changeSettings();
    }
}
