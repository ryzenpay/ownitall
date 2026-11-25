package ryzen.ownitall.util;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * <p>
 * InterruptionHandler class.
 * </p>
 *
 * @author ryzen
 */
public class InterruptionHandler implements AutoCloseable {
    private static final Logger logger = new Logger(InterruptionHandler.class);
    private static volatile boolean interrupted;
    // number of nested interruption handlers
    private static int scope;
    private SignalHandler signalHandler;
    private boolean closed;

    /**
     * initialize interruption handler
     * which is thread save
     */
    public InterruptionHandler() {
        this.signalHandler = Signal.handle(new Signal("INT"), signal -> {
            logger.debug("SIGINT received");
            interrupted = true;
        });
        synchronized (InterruptionHandler.class) {
            if (++scope == 1) {
                resetInterruption();
            }
        }
    }

    /**
     * throw if interruption was caught
     * needs to be triggered for interruption to be thrown
     * needs to be non static to ensure InterruptionHandler was initialized at that
     * point, triggering close upon finish / error and resetting the interruption
     *
     * @throws java.lang.InterruptedException - when interruption caught
     */
    public void checkInterruption() throws InterruptedException {
        if (isInterrupted()) {
            throw new InterruptedException("Interruption caught");
        }
    }

    public static void checkGlobalInterruption() throws InterruptedException {
        if (isInterrupted()) {
            throw new InterruptedException("Global Interruption caught");
        }
    }

    /**
     * <p>
     * isInterrupted.
     * </p>
     *
     * @return a boolean
     */
    public static boolean isInterrupted() {
        return interrupted;
    }

    /**
     * <p>
     * forceInterruption.
     * </p>
     */
    public static void forceInterruption() {
        interrupted = true;
        logger.debug("Forcibly set interruption to true");
    }

    /**
     * <p>
     * resetInterruption.
     * </p>
     */
    public static void resetInterruption() {
        // clears interrupted flag
        interrupted = false;
        logger.debug("Reset interruption to false");
    }

    /**
     * {@inheritDoc}
     * outermost ensures last person leaving turns off the lights
     */
    @Override
    public void close() {
        boolean outermost;
        synchronized (InterruptionHandler.class) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            outermost = --scope <= 0;
        }
        if (outermost) {
            resetInterruption();
        }
        Signal.handle(new Signal("INT"), this.signalHandler);
    }
}
