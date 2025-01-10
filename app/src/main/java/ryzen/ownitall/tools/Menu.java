package ryzen.ownitall.tools;

import java.util.ArrayList;
import java.util.Set;

public class Menu {

    /**
     * standard option menu with little to no customizability
     * 
     * @param options  - arraylist of options which need to reflect to a
     * @param menuName - parameter to give the menu a name and help the user
     *                 remember where they are
     *                 linkedhashmap
     * @return - string choice
     * 
     *         Example usage of this function:
     *         private LinkedHashMap<String, Runnable> options;
     *         options.put("Youtube", this::importYoutube);
     *         options.put("Spotify", this::importSpotify);
     *         options.put("Local", this::importLocal);
     *         while (true) {
     *         String choice = Menu.optionMenu(options.keySet(), "IMPORT");
     *         if (choice == "Exit") {
     *         break;
     *         } else {
     *         options.get(choice).run();
     *         }
     *         }
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
            choice = Input.getInstance().getInt();
            if (choice < 0 || choice > options.size()) {
                System.err.println("Incorrect option, try again");
            } else {
                options.add(0, "Exit");
                return options.get(choice);
            }
        }
    }
}
