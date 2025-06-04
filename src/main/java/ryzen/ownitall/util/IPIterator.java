package ryzen.ownitall.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

// source: https://github.com/ctongfei/progressbar/blob/main/src/main/java/me/tongfei/progressbar/wrapped/ProgressBarWrappedIterator.java
/**
 * Interruptable - Progress - Iterator
 *
 * @author ryzen
 */
// TODO: implement while loops
// an iterator with step which checks interruption & steps
// TODO: task lists?
public class IPIterator<T> implements Iterator<T>, Iterable<T>, AutoCloseable {
    private static final Logger logger = new Logger(IPIterator.class);
    private static IPIterator<?> rootInstance;

    private Iterator<T> iterated;
    private ProgressBar pb;
    private InterruptionHandler interruptionHandler;

    private IPIterator(Iterator<T> iterated, String title, int maxStep) throws InterruptedException {
        this.iterated = iterated;
        this.pb = new ProgressBar(title, maxStep);
        interruptionHandler = new InterruptionHandler(false);
        if (rootInstance == null) {
            InterruptionHandler.resetInterruption();
            rootInstance = this;
        } else {
            interruptionHandler.checkInterruption();
        }
    }

    /**
     * <p>
     * wrap.
     * </p>
     *
     * @param iterated a {@link java.util.Iterator} object
     * @param title    a {@link java.lang.String} object
     * @param maxStep  a int
     * @param <T>      a T class
     * @return a {@link ryzen.ownitall.util.IPIterator} object
     * @throws java.lang.InterruptedException if any.
     */
    public static <T> IPIterator<T> wrap(Iterator<T> iterated, String title, int maxStep) throws InterruptedException {
        if (iterated == null) {
            logger.debug("null iterated provided in wrap");
            return null;
        }
        return new IPIterator<>(iterated, title, maxStep);
    }

    /**
     * <p>
     * wrap.
     * </p>
     *
     * @param iterated a {@link java.util.ArrayList} object
     * @param title    a {@link java.lang.String} object
     * @param maxStep  a int
     * @param <T>      a T class
     * @return a {@link ryzen.ownitall.util.IPIterator} object
     * @throws java.lang.InterruptedException if any.
     */
    public static <T> IPIterator<T> wrap(ArrayList<T> iterated, String title, int maxStep) throws InterruptedException {
        if (iterated == null) {
            logger.debug("null iterated provided in wrap");
            return null;
        }
        return new IPIterator<>(iterated.iterator(), title, maxStep);
    }

    /**
     * <p>
     * wrap.
     * </p>
     *
     * @param iterated a {@link java.util.stream.Stream} object
     * @param title    a {@link java.lang.String} object
     * @param maxStep  a int
     * @param <T>      a T class
     * @return a {@link ryzen.ownitall.util.IPIterator} object
     * @throws java.lang.InterruptedException if any.
     */
    public static <T> IPIterator<T> wrap(T[] iterated, String title, int maxStep) throws InterruptedException {
        if (iterated == null) {
            logger.debug("null iterated provided in wrap");
            return null;
        }
        return new IPIterator<>(Arrays.stream(iterated).iterator(), title, maxStep);
    }

    public static IPIterator<?> manual(String title, int maxStep) throws InterruptedException {
        return new IPIterator<>(null, title, maxStep);
    }

    public void step(String message) throws InterruptedException {
        interruptionHandler.checkInterruption();
        if (iterated != null) {
            iterated.next();
        } else {
            this.pb.step(message);
        }
    }

    public void step(int by) throws InterruptedException {
        interruptionHandler.checkInterruption();
        if (iterated != null) {
            for (int i = 0; i < by; i++) {
                iterated.next();
            }
        } else {
            this.pb.step(by);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        boolean r = iterated.hasNext();
        if (!r || InterruptionHandler.isInterrupted()) {
            return false;
        }
        return r;
    }

    /** {@inheritDoc} */
    @Override
    public T next() {
        T r = iterated.next();
        // this might get funky
        pb.step(r.toString());
        return r;
    }

    /** {@inheritDoc} */
    @Override
    public void remove() {
        iterated.remove();
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<T> iterator() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        pb.close();
        interruptionHandler.close();
        if (this.equals(rootInstance)) {
            rootInstance = null;
        }
    }
}
