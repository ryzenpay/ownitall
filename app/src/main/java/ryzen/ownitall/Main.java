package ryzen.ownitall;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;

import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logs;
import ryzen.ownitall.util.CLIMenu;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final Storage sync = Storage.load();

    /**
     * main function launching the main ownitall menu
     * 
     * @param args - possible arguments to pass (not defined)
     */
    public static void main(String[] args) {
        CLIMenu.clearScreen();
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
                Logs.setLogLevel(level);
            }
            sync.importCollection();
            if (cmd.hasOption("i")) {
                String trace = cmd.getOptionValue("i");
                logger.debug("non interactive parameter provided: " + trace);
                Input.request(trace);
            }
            if (cmd.hasOption("w") && !cmd.hasOption("i")) {
                logger.debug("Web parameter provided");
                ryzen.ownitall.output.web.MainMenu.load(args);
            } else {
                Signal.handle(new Signal("INT"), SignalHandler.SIG_IGN);
                new ryzen.ownitall.output.cli.MainMenu();
            }
        } catch (ParseException e) {
            logger.error("Exception parsing input flags");
        }
    }
}
