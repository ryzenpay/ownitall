package ryzen.ownitall.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class Input {
    private static final Logger logger = LogManager.getLogger(Input.class);
    private static Input instance;
    private static Scanner scanner;
    private AtomicBoolean interrupted = new AtomicBoolean(false);

    private Input() {
        scanner = new Scanner(System.in);
    }

    public static Input request() {
        if (instance == null) {
            instance = new Input();
        }
        return instance;
    }

    public String getString() throws InterruptedException {
        SignalHandler signalHandler = Signal.handle(new Signal("INT"), signal -> {
            logger.debug("SIGINT received");
            interrupted.set(true);
        });
        try {
            if (scanner.hasNextLine()) {
                return scanner.nextLine().trim();
            } else {
                interrupted.set(true);
                throw new InterruptedException("Input Stream Ended");
            }
        } catch (Exception e) {
            if (interrupted.get()) {
                throw new InterruptedException("SIGINT received");
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            // reset signal handler
            Signal.handle(new Signal("INT"), signalHandler);
            if (interrupted.get()) {
                scanner = new Scanner(System.in);
            }
            interrupted.set(false);
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

    public URI getURL() throws InterruptedException {
        while (true) {
            String link = getString();
            try {
                return new URI(link);
            } catch (URISyntaxException e) {
                System.err.print("Invalid URL provided, enter URL: ");
            }
        }
    }

    public void getEnter() {
        try {
            this.getString();
        } catch (InterruptedException e) {
        }
    }
}
