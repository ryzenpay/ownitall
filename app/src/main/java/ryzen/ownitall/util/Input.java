package ryzen.ownitall.util;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.Scanner;

import sun.misc.Signal;

public class Input {
    private volatile boolean interrupted = false;
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

    private void resetScanner() {
        if (scanner != null) {
            scanner.close();
        }
        scanner = new Scanner(System.in);
    }

    public String getString() throws InterruptedException {
        try {
            // Set up a signal handler for SIGINT (Ctrl+C)
            Signal.handle(new Signal("INT"), signal -> {
                // System.out.println("\nInput Interruption Caught");
                interrupted = true;
                resetScanner(); // Reset scanner on interruption
            });

            while (!interrupted) {
                try {
                    if (scanner.hasNextLine()) {
                        String input = scanner.nextLine().trim();
                        if (interrupted) {
                            throw new InterruptedException("SIGINT received");
                        }
                        return input;
                    }
                } catch (NoSuchElementException e) {
                    if (interrupted) {
                        throw new InterruptedException("SIGINT received");
                    }
                    // If not interrupted, reset the scanner and continue
                    resetScanner();
                }
            }
            throw new InterruptedException("SIGINT received");
        } catch (IllegalStateException e) {
            throw e;
        } finally {
            interrupted = false; // Reset the flag
        }
    }

    public char getChar() throws InterruptedException {
        while (true) {
            try {
                String input = getString();
                return input.charAt(0);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.print("Invalid char provided, try again: ");
            }
        }

    }

    public int getInt() throws InterruptedException {
        while (true) {
            try {
                String inputInteger = getString();
                return Integer.parseInt(inputInteger);
            } catch (NumberFormatException e) {
                System.err.print("Invalid int provided, try again: ");
            }
        }
    }

    public int getInt(int lowerBound, int upperBound) throws InterruptedException {
        while (true) {
            try {
                int result = getInt();
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

    public long getLong() throws InterruptedException {
        while (true) {
            try {
                String inputLong = getString();
                if (inputLong == null) {
                    return 0L;
                }
                return Long.parseLong(inputLong);
            } catch (NumberFormatException e) {
                System.err.print("Invalid input. Please enter a valid long: ");
            }
        }
    }

    public File getFile(boolean exists) throws InterruptedException {
        while (true) {
            String path = getString();
            File file = new File(path);
            if (exists) {
                if (file.exists()) {
                    return file;
                } else {
                    System.err.print("The specified file or folder does not exist, try again: ");
                }
            } else {
                return file;
            }
        }
    }

    public boolean getAgreement() throws InterruptedException {
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

    public boolean getBool() throws InterruptedException {
        while (true) {
            String choice = getString();
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
        try {
            this.getString();
        } catch (InterruptedException e) {
        }
    }
}
