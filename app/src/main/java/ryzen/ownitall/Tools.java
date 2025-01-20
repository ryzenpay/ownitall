package ryzen.ownitall;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import ryzen.ownitall.tools.Input;

public class Tools {
    private static Tools instance;
    private LinkedHashMap<String, Runnable> options;

    public Tools() {
        this.options = new LinkedHashMap<>();
        options.put("Archive", this::optionArchive);
        options.put("UnArchive", this::optionUnArchive);
        options.put("Clear Cache", this::optionClearCache);
        options.put("Clear Saved Logins", this::optionClearCredentials);
        options.put("Reset Settings", this::optionClearSettings);
        while (true) {
            String choice = promptMenu();
            if (choice != null) {
                if (choice.equals("Exit")) {
                    break;
                } else {
                    this.options.get(choice).run();
                }
            }
        }
    }

    public static Tools load() {
        if (instance == null) {
            instance = new Tools();
        }
        return instance;
    }

    private String promptMenu() {
        System.out.println("Choose an option from the following: ");
        int i = 1;
        for (String option : options.keySet()) {
            System.out.println("[" + i + "] " + option);
            i++;
        }
        System.out.println("[0] Exit");
        System.out.print("Enter your choice: ");
        int choice = Input.request().getInt();
        if (choice < 0 || choice > options.size()) {
            System.out.println("Incorrect option, try again");
            return null;
        }
        ArrayList<String> arrayOptions = new ArrayList<>(options.keySet());
        arrayOptions.add(0, "Exit");
        return arrayOptions.get(choice);
    }

    private void optionArchive() {
        Sync.load().archive();
    }

    private void optionUnArchive() {
        Sync.load().unArchive();
    }

    private void optionClearCache() {
        Sync.load().clearCache();
        Library.load().clear();
    }

    private void optionClearCredentials() {
        Credentials.load().clear();
    }

    private void optionClearSettings() {
        Settings.load().clear();
    }
}
