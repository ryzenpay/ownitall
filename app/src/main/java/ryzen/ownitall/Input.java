package ryzen.ownitall;

import java.io.File;
import java.util.Scanner;

public class Input {
    private static Input instance;
    private Scanner scanner;

    private Input() {
        scanner = new Scanner(System.in);
    }

    public static Input getInstance() {
        if (instance == null) {
            instance = new Input();
        }
        return instance;
    }

    public String getString() {
        return scanner.nextLine().trim();
    }

    public int getInt() {
        while (true) {
            try {
                int result = Integer.parseInt(scanner.nextLine().trim());
                return result;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
            }
        }
    }

    public File getFile() {
        while (true) {
            String path = getString();
            File file = new File(path);
            if (file.exists()) {
                return file;
            } else {
                System.out.println("The specified file or folder does not exist. Please try again.");
            }
        }
    }
}
