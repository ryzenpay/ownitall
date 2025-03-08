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

    public InterruptionHandler() {
        signalHandler = Signal.handle(new Signal("INT"), signal -> {
            logger.debug("SIGINT received");
            interrupted.set(true);
        });
    }

    public boolean throwInterruption() throws InterruptedException {
        if (interrupted.get()) {
            interrupted.set(false);
            throw new InterruptedException();
        }
        return false;
    }

    public void triggerInterruption() throws InterruptedException {
        interrupted.set(false);
        throw new InterruptedException();
    }

    @Override
    public void close() {
        interrupted.set(false);
        Signal.handle(new Signal("INT"), signalHandler);
    }
}
