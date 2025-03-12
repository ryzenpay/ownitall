package ryzen.ownitall.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

public class Menu {
    private static final Logger logger = LogManager.getLogger(Menu.class);

    /**
     * clears screen contents when not in debug mode
     */
    public static void clearScreen() {
        if (logger.getLevel() != Level.DEBUG) {
            System.out.print("\033[H\033[2J");
        }
        System.out.flush();
    }

    public static void printLogo() {
        String asciiLogo = "                        _  _          _  _ \n" +
                "                       (_)| |        | || |\n" +
                "  ___ __      __ _ __   _ | |_  __ _ | || |\n" +
                " / _  \\ \\ /\\ / /| '_ \\ | || __|/ _` || || |\n" +
                "| (_) |\\ V  V / | | | || || |_| (_| || || |\n" +
                " \\___/  \\_/\\_/  |_| |_||_| \\__|\\__,_||_||_|\n" +
                "                ";
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
     * @throws InterruptedException - when user interrupts
     */
    public static String optionMenu(Set<String> setOptions, String menuName) throws InterruptedException {
        if (setOptions == null) {
            logger.debug("null optionset provided in optionMenu");
            return null;
        }
        ArrayList<String> options = new ArrayList<>(setOptions);
        int i = 1;
        int choice;
        while (true) {
            clearScreen();
            printLogo();
            System.out.println("[" + menuName + "] Choose an option from the following: ");
            i = 1;
            for (String option : options) {
                System.out.println("[" + i + "] " + option);
                i++;
            }
            System.out.println("[0] Exit");
            System.out.print("Enter your choice: ");
            choice = Input.request().getInt();
            if (choice < 0 || choice > options.size()) {
                System.err.println("Incorrect option, try again");
                System.out.print("Enter your choice: ");
            } else {
                options.add(0, "Exit");
                return options.get(choice);
            }

        }
    }

    /**
     * print menu with options and set values
     * 
     * @param setOptions - (linkedhash)map of options with name:value
     * @param menuName   - desired menu name
     * @return - name of selected option
     * @throws InterruptedException - when user interrupts
     */
    public static String optionMenuWithValue(Map<String, ?> setOptions, String menuName) throws InterruptedException {
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
                System.out.println("[" + i + "] " + option + ": " + setOptions.get(option).toString());
                i++;
            }
            System.out.println("[0] Exit");
            System.out.print("Enter your choice: ");
            choice = Input.request().getInt();
            if (choice < 0 || choice > options.size()) {
                System.err.println("Incorrect option, try again");
                System.out.print("Enter your choice: ");
            } else {
                options.add(0, "Exit");
                return options.get(choice);
            }

        }
    }
}
