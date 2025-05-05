package ryzen.ownitall.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Input class.
 * </p>
 *
 * @author ryzen
 */
public class Input {
    private static final Logger logger = LogManager.getLogger(Input.class);
    private static Input instance;
    private static Scanner scanner;
    private static Queue<String> nonInteractive = new LinkedList<>();

    /**
     * default input constructor creating system scanner
     */
    private Input() {
        scanner = new Scanner(System.in);
    }

    /**
     * instance loader for input
     *
     * @return - new or existing instance of input
     */
    public static Input request() {
        if (instance == null) {
            instance = new Input();
        }
        return instance;
    }

    /**
     * <p>
     * Setter for the field <code>nonInteractive</code>.
     * </p>
     *
     * @param params a {@link java.lang.String} object
     */
    public static void setNonInteractive(String params) {
        if (params == null) {
            logger.error("null params provided in input request");
            return;
        }
        String[] inputParams = params.split(",");
        for (String param : inputParams) {
            nonInteractive.add(param);
        }
    }

    /**
     * get string from user input
     *
     * @return - string of user input
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public String getString() throws InterruptedException {
        if (!nonInteractive.isEmpty()) {
            return nonInteractive.poll();
        }
        try (InterruptionHandler interruptionHandler = InterruptionHandler.getInstance()) {
            if (scanner.hasNextLine()) {
                return scanner.nextLine().trim();
            } else {
                interruptionHandler.triggerInterruption();
            }
        } catch (InterruptedException e) {
            scanner = new Scanner(System.in);
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Thread.dumpStack();
        throw new RuntimeException("Got out of getString loop");
    }

    /**
     * get string from user input with enforced length
     *
     * @param length - length the user input has to meet
     * @return - string of length
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public String getString(int length) throws InterruptedException {
        while (true) {
            String input = getString();
            if (input.length() != length) {
                logger.info("String needs to be of length: " + length);
                System.err.print("Invalid string provided, try again: ");
            } else {
                return input;
            }
        }
    }

    /**
     * get char from user
     *
     * @return - char of user input
     * @throws java.lang.InterruptedException - when user interrupts
     */
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

    /**
     * get int from user
     *
     * @return - int of user input
     * @throws java.lang.InterruptedException - when user interrupts
     */
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

    /**
     * get int from user between two boundaries
     *
     * @param lowerBound - lowest int boundary
     * @param upperBound - upper int boundary
     * @return - int between boundaries
     * @throws java.lang.InterruptedException - when user interrupts
     */
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
                System.err.print("Invalid input. Enter a valid integer: ");
            }
        }

    }

    /**
     * get long from user input
     *
     * @return - long from user input
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public long getLong() throws InterruptedException {
        while (true) {
            try {
                String inputLong = getString();
                if (inputLong == null) {
                    return 0L;
                }
                return Long.parseLong(inputLong);
            } catch (NumberFormatException e) {
                System.err.print("Invalid input. Enter a valid long: ");
            }
        }
    }

    /**
     * get file from user input
     *
     * @param exists - boolean if the file should exist
     * @return - constructed file from user input
     * @throws java.lang.InterruptedException - when user interrupts
     */
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

    /**
     * get user agreement (y or n)
     *
     * @return - y = true, n = false
     * @throws java.lang.InterruptedException - when user interrupts
     */
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

    /**
     * get bool from user input
     *
     * @return - true = true, false = false
     * @throws java.lang.InterruptedException - when user interrupts
     */
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

    /**
     * get url from user input
     *
     * @return - constructed URI of input url
     * @throws java.lang.InterruptedException - when user interrupts
     */
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

    /**
     * get user to press enter or interrupt
     *
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public void getEnter() throws InterruptedException {
        if (!nonInteractive.isEmpty()) {
            return;
        }
        this.getString();
    }

    /**
     * <p>
     * getClassStr.
     * </p>
     *
     * @return a {@link java.lang.Class} object
     * @throws java.lang.InterruptedException if any.
     */
    public Class<?> getClassStr() throws InterruptedException {
        while (true) {
            String className = getString();
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                System.err.print("Class not found, enter Full Class Name: ");
            }
        }
    }

    /**
     * <p>
     * getValue.
     * </p>
     *
     * @param type a {@link java.lang.Class} object
     * @return a {@link java.lang.Object} object
     * @throws java.lang.InterruptedException if any.
     */
    public Object getValue(Class<?> type) throws InterruptedException {
        if (type == boolean.class) {
            return this.getBool();
        } else if (type == String.class) {
            return this.getString();
        } else if (type == int.class) {
            return this.getInt();
        } else if (type == char.class) {
            return this.getChar();
        } else if (type == long.class) {
            return this.getLong();
        } else if (type == URI.class) {
            return this.getURL();
        } else if (type == File.class) {
            return this.getFile(false);
        } else {
            // TODO: this stinks
            return this.getClassStr();
            // logger.warn("Getting variables of the type '" + type.getSimpleName() + "' is
            // currently not supported");
            // return null;
        }
    }
}
