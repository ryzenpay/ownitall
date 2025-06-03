package ryzen.ownitall.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import ryzen.ownitall.util.exceptions.MenuClosed;

/**
 * <p>
 * Menu class.
 * </p>
 *
 * @author ryzen
 */
// TODO: create custom exception ClosedMenu
// enforces catching the "exit" option
public class Menu {
    private static final Logger logger = new Logger(Menu.class);
    /**
     * Constant
     * <code>asciiLogo="                        _  _          _"{trunked}</code>
     */
    private static String asciiLogo = "";

    /**
     * <p>
     * setLogo.
     * </p>
     *
     * @param logo a {@link java.lang.String} object
     */
    public static void setLogo(String logo) {
        asciiLogo = logo;
    }

    /**
     * clears screen contents when not in debug mode
     */
    public static void clearScreen() {
        if (!Logger.is(Level.DEBUG)) {
            System.out.print("\033[H\033[2J");
        }
        System.out.flush();
        if (Logger.is(Level.DEBUG)) {
            return;
        }
        printLogo();
        Logger.printLogs(Level.INFO);
        Logger.clearLogs();
    }

    /**
     * <p>
     * printLogo.
     * </p>
     */
    public static void printLogo() {
        System.out.println(asciiLogo);
    }

    /**
     * standard option menu with little to no customizability
     *
     * @param setOptions - arraylist of options which need to reflect to a
     * @param menuName   - parameter to give the menu a name and help the user
     *                   remember where they are
     *                   linkedhashmap
     * @return - string choice
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public static String optionMenu(Set<String> setOptions, String menuName) throws MenuClosed {
        if (setOptions == null) {
            logger.debug("null optionset provided in optionMenu");
            return null;
        }
        ArrayList<String> options = new ArrayList<>(setOptions);
        int i = 1;
        int choice;
        while (true) {
            clearScreen();
            System.out.println("[" + menuName + "] Choose an option from the following: ");
            i = 1;
            for (String option : options) {
                System.out.println("[" + i + "] " + option);
                i++;
            }
            System.out.println("[0] Exit");
            System.out.print("Enter your choice: ");
            try {
                choice = Input.request().getInt();
                if (choice < 0 || choice > options.size()) {
                    System.err.println("Incorrect option, try again");
                    System.out.print("Enter your choice: ");
                } else {
                    if (choice == 0) {
                        throw new MenuClosed();
                    }
                    return options.get(choice);
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting menu '" + menuName + "' option");
                throw new MenuClosed(e);
            }
        }
    }

    /**
     * print menu with options and set values
     *
     * @param setOptions - (linkedhash)map of options with name:value
     * @param menuName   - desired menu name
     * @return - name of selected option
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public static String optionMenuWithValue(Map<String, ?> setOptions, String menuName)
            throws MenuClosed {
        if (setOptions == null) {
            logger.debug("null optionset provided in optionMenuWithValue");
            return null;
        }
        ArrayList<String> options = new ArrayList<>(setOptions.keySet());
        int i = 1;
        int choice;
        while (true) {
            clearScreen();
            System.out.println("[" + menuName + "] Choose an option from the following: ");
            i = 1;
            for (String option : options) {
                Object object = setOptions.get(option);
                if (object == null) {
                    object = "";
                }
                System.out.println("[" + i + "] " + option + ": " + object.toString());
                i++;
            }
            System.out.println("[0] Exit");
            System.out.print("Enter your choice: ");
            try {
                choice = Input.request().getInt();
                if (choice < 0 || choice > options.size()) {
                    System.err.println("Incorrect option, try again");
                    System.out.print("Enter your choice: ");
                } else {
                    if (choice == 0) {
                        throw new MenuClosed();
                    }
                    return options.get(choice);
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting menu '" + menuName + "' option");
                throw new MenuClosed(e);
            }
        }
    }
}
