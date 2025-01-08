package ryzen.ownitall;

import java.io.File;
import java.util.Scanner;

public class Input {
    private static Input instance;
    private Scanner scanner;

    /**
     * default input constructor to set up scanner
     */
    private Input() {
        scanner = new Scanner(System.in);
    }

    /**
     * function to get the instance of input
     * 
     * @return
     */
    public static Input getInstance() {
        if (instance == null) {
            instance = new Input();
        }
        return instance;
    }

    /**
     * save user string input
     * 
     * @return - string of user input
     */
    public String getString() {
        return scanner.nextLine().trim();
    }

    /**
     * save user char input
     * 
     * @return - char of user input
     */
    public char getChar() {
        return scanner.next().charAt(0);
    }

    /**
     * save user int input
     * 
     * @return - int of user input
     */
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

    /**
     * save user file path input
     * 
     * @return - constructed File to users path
     */
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

    /**
     * take user y/n input
     * 
     * @return - true of y, false for n
     */
    public boolean getBool() {
        while (true) {
            char choice = getChar();
            if (Character.toLowerCase(choice) == 'y') {
                return true;
            }
            if (Character.toLowerCase(choice) == 'n') {
                return false;
            }
            System.out.println("Invalid input. Please enter y/N.");
        }
    }
}
