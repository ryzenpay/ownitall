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
    private static InterruptionHandler instance;
    private SignalHandler signalHandler;
    private AtomicBoolean interrupted = new AtomicBoolean(false);

    /**
     * initialize interruption handler
     * which is thread save
     */
    private InterruptionHandler() {
        signalHandler = Signal.handle(new Signal("INT"), signal -> {
            logger.debug("SIGINT received");
            interrupted.set(true);
        });
    }

    public static InterruptionHandler getInstance() {
        if (instance == null) {
            instance = new InterruptionHandler();
        }
        return instance;
    }

    public static InterruptionHandler getExistingInstance() {
        return instance;
    }

    /**
     * throw if interruption was caught
     * needs to be triggered for interruption to be thrown
     *
     * @throws java.lang.InterruptedException - when interruption caught
     */
    public void throwInterruption() throws InterruptedException {
        if (interrupted.get()) {
            interrupted.set(false);
            throw new InterruptedException("Interruption caught");
        }
    }

    /**
     * force trigger an interruption and reset interruption
     *
     * @throws java.lang.InterruptedException - forced interruption
     */
    public void triggerInterruption() throws InterruptedException {
        interrupted.set(false);
        throw new InterruptedException("Interruption manually triggered");
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        interrupted.set(false);
        Signal.handle(new Signal("INT"), signalHandler);
    }
}
