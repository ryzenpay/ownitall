package ryzen.ownitall.util;

import sun.misc.Signal;
import sun.misc.SignalHandler;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InterruptionHandler implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(InterruptionHandler.class);
    private SignalHandler signalHandler;
    private AtomicBoolean interrupted = new AtomicBoolean(false);

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
     * 
     * @throws InterruptedException - when interruption caught
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
     * @throws InterruptedException - forced interruption
     */
    public void triggerInterruption() throws InterruptedException {
        interrupted.set(false);
        throw new InterruptedException("Interruption manually triggered");
    }

    @Override
    public void close() {
        interrupted.set(false);
        Signal.handle(new Signal("INT"), signalHandler);
    }
}
