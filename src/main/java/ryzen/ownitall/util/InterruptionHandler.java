package ryzen.ownitall.util;

import java.util.concurrent.atomic.AtomicBoolean;

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
    private static AtomicBoolean isInterrupted = new AtomicBoolean();
    private SignalHandler signalHandler;

    /**
     * initialize interruption handler
     * which is thread save
     */
    public InterruptionHandler() {
        resetInterruption();
        signalHandler = Signal.handle(new Signal("INT"), signal -> {
            logger.debug("SIGINT received");
            isInterrupted.set(true);
        });
    }

    public InterruptionHandler(boolean resetInterruption) {
        if (resetInterruption) {
            resetInterruption();
        }
        signalHandler = Signal.handle(new Signal("INT"), signal -> {
            logger.debug("SIGINT received");
            isInterrupted.set(true);
        });
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

    public static boolean isInterrupted() {
        return isInterrupted.get();
    }

    /**
     * <p>
     * forceInterruption.
     * </p>
     */
    public static void forceInterruption() {
        isInterrupted.set(true);
        logger.debug("Forcibly set interruption to true");
    }

    /**
     * <p>
     * resetInterruption.
     * </p>
     */
    public static void resetInterruption() {
        // clears interrupted flag
        isInterrupted.set(false);
        logger.debug("Reset interruption to false");
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        Signal.handle(new Signal("INT"), signalHandler);
    }
}
