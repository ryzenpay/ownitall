package ryzen.ownitall;

import java.util.LinkedHashMap;

import ryzen.ownitall.tools.Menu;

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
            String choice = Menu.optionMenu(options.keySet(), "TOOL MENU");
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
