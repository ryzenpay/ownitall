package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logs;
import ryzen.ownitall.util.Menu;
import sun.misc.Signal;
import sun.misc.SignalHandler;

@RestController
@SpringBootApplication
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
            exit();
        }
    }

    private static void handleFlags(String[] args) {
        if (args == null || args.length == 0) {
            logger.debug("no flags provided");
            return;
        }
        // https://commons.apache.org/proper/commons-cli/usage.html
        Options options = new Options();
        options.addOption("h", "help", false, "see all flag options");
        options.addOption("l", "log", true, "logging level");
        options.addOption("i", "noninteractive", true, "Enable non interactive");
        options.addOption("w", "web", false, "enable web front (localhost:8080)");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                HelpFormatter help = new HelpFormatter();
                help.printHelp("ant", options);
                exit();
            }
            if (cmd.hasOption("l")) {
                String level = cmd.getOptionValue("l");
                logger.debug("log level provided: " + level);
                Logs.setLogLevel(level);
            }
            if (cmd.hasOption("i")) {
                String trace = cmd.getOptionValue("i");
                logger.debug("non interactive parameter provided: " + trace);
                Input.request(trace);
            }
            if (cmd.hasOption("w") && !cmd.hasOption("i")) {
                logger.debug("Web parameter provided");
                SpringApplication.run(Main.class, args);
            }
        } catch (ParseException e) {
            logger.error("Exception parsing input flags");
            exit();
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

    private static void exit() {
        optionSave();
        System.exit(0);
    }
}
