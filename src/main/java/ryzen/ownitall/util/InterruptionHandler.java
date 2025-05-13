package ryzen.ownitall.util;

import sun.misc.Signal;
import sun.misc.SignalHandler;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * InterruptionHandler class.
 * </p>
 *
 * @author ryzen
 */
public class InterruptionHandler implements AutoCloseable {
    private static final Logger logger = new Logger(InterruptionHandler.class);
    private SignalHandler signalHandler;
    private static AtomicBoolean interrupted = new AtomicBoolean(false);

    /**
     * initialize interruption handler
     * which is thread save
     */
    public InterruptionHandler() {
        signalHandler = Signal.handle(new Signal("INT"), signal -> {
            logger.debug("SIGINT received");
            interrupted.set(true);
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
    public void throwInterruption() throws InterruptedException {
        if (interrupted.get()) {
            interrupted.set(false);
            throw new InterruptedException("Interruption caught");
        }
    }

    public static void forceInterruption() {
        interrupted.set(true);
        logger.debug("Forcibly set interruption to true");
    }

    public static void resetInterruption() {
        interrupted.set(false);
        logger.debug("Reset interruption to false");
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        interrupted.set(false);
        Signal.handle(new Signal("INT"), signalHandler);
    }
}
