package ryzen.ownitall.util;

import java.io.File;
import java.util.Scanner;

public class Input {
    private static Input instance;
    private Scanner scanner;

    private Input() {
        scanner = new Scanner(System.in);
    }

    public static Input request() {
        if (instance == null) {
            instance = new Input();
        }
        return instance;
    }

    public String getString() {
        // TODO: sigtermhook currently does not work
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("SIGTERM caught");
        }));

        try {
            return scanner.nextLine().trim();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    public char getChar() {
        while (true) {
            try {
                String input = getString();
                if (input == null) {
                    return '\0';
                }
                return input.charAt(0);
            } catch (Exception e) {
                System.err.print("Invalid Input. Please enter a valid character: ");
            }
        }
    }

    public int getInt() {
        while (true) {
            try {
                String inputInteger = getString();
                if (inputInteger == null) {
                    return 0;
                }
                return Integer.parseInt(inputInteger);
            } catch (Exception e) {
                System.err.print("Invalid input. Please enter a valid integer: ");
            }
        }
    }

    public int getInt(int lowerBound, int upperBound) {
        while (true) {
            try {
                int result = getInt();
                if (result >= lowerBound && result <= upperBound) {
                    return result;
                } else {
                    System.err.println("Invalid input. outside of bounds: (" + lowerBound + "," + upperBound + ")");
                }
            } catch (Exception e) {
                System.err.print("Invalid input. Please enter a valid integer: ");
            }
        }
    }

    public long getLong() {
        while (true) {
            try {
                String inputLong = getString();
                if (inputLong == null) {
                    return 0L;
                }
                return Long.parseLong(inputLong);
            } catch (Exception e) {
                System.err.print("Invalid input. Please enter a valid long: ");
            }
        }
    }

    public File getFile(boolean exists) {
        while (true) {
            String path = getString();
            if (path == null) {
                return null;
            }
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

    public boolean getAgreement() {
        while (true) {
            char choice = getChar();
            if (choice == '\0')
                return false;
            if (Character.toLowerCase(choice) == 'y') {
                return true;
            }
            if (Character.toLowerCase(choice) == 'n') {
                return false;
            }
            System.err.print("Invalid input. Enter y/N: ");
        }
    }

    public boolean getBool() {
        while (true) {
            String choice = getString();
            if (choice == null) {
                return false;
            }
            if (choice.equalsIgnoreCase("true")) {
                return true;
            }
            if (choice.equalsIgnoreCase("false")) {
                return false;
            }
            System.err.print("Invalid input. Enter true/false: ");

        }
    }

    public void getEnter() {
        this.getString();
    }
}
