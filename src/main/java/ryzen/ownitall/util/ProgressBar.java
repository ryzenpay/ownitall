package ryzen.ownitall.util;

import java.time.Duration;
import java.util.Stack;

import me.tongfei.progressbar.DelegatingProgressBarConsumer;
//http://tongfei.me/progressbar/
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

/**
 * there is one root progressbar which is the first created progress bar,
 * every progressbar is its own "instance" but if they were not the first, will
 * append to the root instance
 * you can still get details of each individual progress bar with their own
 * instance, but the printed output is of the root progressbar
 *
 * @author ryzen
 */
// TODO: progressbar EVERYWHERE
// TODO: make logs not affect progressbar
public class ProgressBar implements AutoCloseable {
    Logger logger = new Logger(ProgressBar.class);
    public static boolean output = true;
    private static Stack<ProgressBar> instances;

    private me.tongfei.progressbar.ProgressBar pb;

    /**
     * <p>
     * Constructor for ProgressBar.
     * </p>
     *
     * @param title   a {@link java.lang.String} object
     * @param maxStep a int
     */
    public ProgressBar(String title, int maxStep) {
        ProgressBarBuilder pbInit = new ProgressBarBuilder()
                .setTaskName(title)
                .setInitialMax(maxStep)
                .setStyle(ProgressBarStyle.ASCII)
                .hideEta();
        if (!output) {
            pbInit.setConsumer(new DelegatingProgressBarConsumer(null));
        }
        this.pb = pbInit.build();
        if (instances == null) {
            instances = new Stack<>();
        }
        instances.add(this);
    }

    /**
     * <p>
     * Getter for the field <code>title</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getTitle() {
        return this.pb.getTaskName();
    }

    /**
     * <p>
     * Getter for the field <code>step</code>.
     * </p>
     *
     * @return a int
     */
    public long getStep() {
        return this.pb.getCurrent();
    }

    /**
     * <p>
     * Getter for the field <code>maxStep</code>.
     * </p>
     *
     * @return a int
     */
    public long getMaxStep() {
        // needed since we are using builder
        if (this.pb.isIndefinite()) {
            return -1;
        }
        return this.pb.getMax();
    }

    /**
     * <p>
     * Getter for the field <code>message</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getMessage() {
        return this.pb.getExtraMessage();
    }

    public Duration getElapsedTime() {
        return this.pb.getElapsedAfterStart();
    }

    /**
     * <p>
     * step.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     * @param by      a int
     */
    public void step(String message, int by) {
        pb.setExtraMessage(message).stepBy(by);
    }

    /**
     * <p>
     * step.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void step(String message) {
        this.step(message, 1);
    }

    /**
     * <p>
     * step.
     * </p>
     */
    public void step() {
        this.step(null, 1);
    }

    /**
     * <p>
     * step.
     * </p>
     *
     * @param by a int
     */
    public void step(int by) {
        this.step(null, by);
    }

    /**
     * <p>
     * getCurrentInstance.
     * </p>
     *
     * @return a {@link ryzen.ownitall.util.ProgressBar} object
     */
    public static Stack<ProgressBar> getInstances() {
        return instances;
    }

    @Override
    public void close() {
        this.pb.close();
        instances.remove(this);
    }
}
