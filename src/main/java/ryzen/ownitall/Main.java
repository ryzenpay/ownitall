package ryzen.ownitall;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.ProgressBar;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * <p>
 * Main class.
 * </p>
 *
 * @author ryzen
 */
public class Main {
    private static final Logger logger = new Logger(Main.class);

    /**
     * main function launching the main ownitall menu
     *
     * @param args - possible arguments to pass (not defined)
     */
    public static void main(String[] args) {
        Menu.clearScreen();
        if (args == null || args.length == 0) {
            logger.debug("no flags provided");
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
                return;
            }
            if (cmd.hasOption("l")) {
                String level = cmd.getOptionValue("l");
                logger.debug("log level provided: " + level);
                Logger.setLogLevel(level);
            }
            Settings.load();
            Storage.importCollection();
            if (cmd.hasOption("i")) {
                String trace = cmd.getOptionValue("i");
                logger.debug("non interactive parameter provided: " + trace);
                Settings.interactive = false;
                Input.setNonInteractive(trace);
            }
            if (cmd.hasOption("w") && !cmd.hasOption("i")) {
                logger.debug("Web parameter provided");
                ProgressBar.output = false;
                ryzen.ownitall.ui.web.MainMenu.main(args);
            } else {
                Signal.handle(new Signal("INT"), SignalHandler.SIG_IGN);
                ProgressBar.output = true;
                Menu.setLogo(Settings.logo);
                new ryzen.ownitall.ui.cli.MainMenu();
            }
        } catch (ParseException e) {
            logger.error("Exception parsing input flags", e);
        }
    }

    /**
     * <p>
     * save.
     * </p>
     */
    public static void save() {
        Collection.save();
        Settings.load().save();
        if (Library.checkInstance()) {
            Library.load();
        }
    }
}
