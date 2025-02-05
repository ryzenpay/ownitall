package ryzen.ownitall.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Menu {
    private static final Logger logger = LogManager.getLogger(Menu.class);

    public static void clearScreen() {
        // System.out.print("\033[H\033[2J");
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
        System.out.flush();
    }

    /**
     * standard option menu with little to no customizability
     * 
     * @param setOptions - arraylist of options which need to reflect to a
     * @param menuName   - parameter to give the menu a name and help the user
     *                   remember where they are
     *                   linkedhashmap
     * @return - string choice
     * 
     */
    public static String optionMenu(Set<String> setOptions, String menuName) {
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

    public static String optionMenuWithValue(Map<String, ?> setOptions, String menuName) {
        ArrayList<String> options = new ArrayList<>(setOptions.keySet());
        int i = 1;
        int choice;
        while (true) {
            try {
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
            } catch (Exception e) {
                logger.info("Shutdown hook caught");
                return "Exit";
            }
        }
    }
}
