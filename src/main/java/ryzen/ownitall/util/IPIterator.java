package ryzen.ownitall.util;

import java.util.Iterator;

/**
 * Interruptable - Progress - Iterator
 */
public class IPIterator<T> implements Iterator<T>, Iterable<T> {
    private Iterator<T> iterated;
    private ProgressBar pb;
    private InterruptionHandler interruptionHandler;

    // TODO: make this class replace ProgressBarWrappedIterator.java
    // https://github.com/ctongfei/progressbar/blob/main/src/main/java/me/tongfei/progressbar/wrapped/ProgressBarWrappedIterator.java
    public IPIterator(Iterator<T> iterated, String title, int maxStep) {
        this.iterated = iterated;
        pb = new ProgressBar(title, maxStep);
        interruptionHandler = new InterruptionHandler();
    }

    @Override
    public boolean hasNext() {
        boolean r = iterated.hasNext();
        // TODO: would still require a throw interruption at the end
        // throw a custom runtime exception
        // how to enforce catching runtime exception?
        if (!r || interruptionHandler.isInterrupted()) {
            pb.close();
            interruptionHandler.close();
            return false;
        }
        return r;
    }

    @Override
    public T next() {
        T r = iterated.next();
        // this might get funky
        pb.step(r.toString());
        return r;
    }

    @Override
    public void remove() {
        iterated.remove();
    }

    // Implement the iterator method from Iterable<T>
    @Override
    public Iterator<T> iterator() {
        return this;
    }
}
