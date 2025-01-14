package ryzen.ownitall.tools;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class Menu { // TODO: shutdownhook handling

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