package ryzen.ownitall;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Tools {
    private LinkedHashMap<String, Runnable> options;

    private boolean status;

    public Tools() {
        this.options = new LinkedHashMap<>();
        this.status = false;
        options.put("Archive", this::optionArchive);
        options.put("UnArchive", this::optionUnArchive);
        while (!this.status) {
            String choice = promptMenu();
            if (choice != null) {
                if (choice.equals("Exit")) {
                    this.exit();
                } else {
                    this.options.get(choice).run();
                }
            }
        }
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
        int choice = Input.getInstance().getInt();
        if (choice < 0 || choice > options.size()) {
            System.out.println("Incorrect option, try again");
            return null;
        }
        ArrayList<String> arrayOptions = new ArrayList<>(options.keySet());
        arrayOptions.add(0, "Exit");
        return arrayOptions.get(choice);
    }

    private void optionArchive() {
        Sync sync = new Sync();
        sync.archive();
        this.status = true;
    }

    private void optionUnArchive() {
        Sync sync = new Sync();
        sync.unArchive();
        this.status = true;
    }

    private void exit() {
        this.status = true;
    }
}
