package ryzen.ownitall.tools;

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
     * @return - Input instance
     */
    public static Input request() {
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
        return getString().charAt(0);
    }

    /**
     * save user int input
     * 
     * @return - int of user input
     */
    public int getInt() {
        while (true) {
            try {
                int result = Integer.parseInt(getString());
                return result;
            } catch (NumberFormatException e) {
                System.err.print("Invalid input. Please enter a valid integer: ");
            }
        }
    }

    /**
     * save user input and force boundaries
     * 
     * @param lowerBound - lower int boundary
     * @param upperBound - upper int boundary
     * @return - int of user input
     */
    public int getInt(int lowerBound, int upperBound) {
        while (true) {
            try {
                int result = Integer.parseInt(getString());
                if (result >= lowerBound && result <= upperBound) {
                    return result;
                } else {
                    System.err.println("Invalid input. outside of bounds: (" + lowerBound + "," + upperBound + ")");
                }
            } catch (NumberFormatException e) {
                System.err.print("Invalid input. Please enter a valid integer: ");
            }
        }
    }

    /**
     * save user long input
     * 
     * @return - long of user input
     */
    public long getLong() {
        while (true) {
            try {
                return Long.parseLong(getString());
            } catch (NumberFormatException e) {
                System.err.print("Invalid input. Please enter a valid long: ");
            }
        }
    }

    /**
     * save user file path input
     * 
     * @return - constructed File to users path
     */
    public File getFile(boolean exists) {
        while (true) {
            String path = getString();
            File file = new File(path);
            if (exists) {
                if (file.exists()) {
                    return file;
                } else {
                    System.err.print("The specified file or folder does not exist. Try again: ");
                }
            } else {
                return file;
            }
        }
    }

    /**
     * take user y/n input
     * 
     * @return - true of y, false for n
     */
    public boolean getAgreement() {
        while (true) {
            char choice = getChar();
            if (Character.toLowerCase(choice) == 'y') {
                return true;
            }
            if (Character.toLowerCase(choice) == 'n') {
                return false;
            }
            System.err.print("Invalid input. Enter y/N: ");
        }
    }

    /**
     * take user true / false input
     * 
     * @return - true of y, false for n
     */
    public boolean getBool() {
        while (true) {
            String choice = getString();
            if (choice.toLowerCase().equals("true")) {
                return true;
            }
            if (choice.toLowerCase().equals("false")) {
                return false;
            }
            System.err.print("Invalid input. Enter true/false: ");
        }
    }
}
