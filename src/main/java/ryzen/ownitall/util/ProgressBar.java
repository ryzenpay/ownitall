package ryzen.ownitall.util;

//http://tongfei.me/progressbar/
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import java.util.Stack;

/**
 * <p>
 * ProgressBar class.
 * </p>
 *
 * @author ryzen
 */
public class ProgressBar implements AutoCloseable {
    private String title;
    private int maxStep;
    private int step;
    private String message;
    private me.tongfei.progressbar.ProgressBar pb;

    /** Constant <code>output=true</code> */
    public static boolean output = true;
    private static final Stack<ProgressBar> instances = new Stack<>();

    /**
     * <p>
     * Constructor for ProgressBar.
     * </p>
     *
     * @param title   a {@link java.lang.String} object
     * @param maxStep a int
     */
    public ProgressBar(
            String title,
            int maxStep) {
        this.title = title;
        step = 0;
        // this.title = title;
        this.maxStep = maxStep;
        if (output) {
            this.pb = new ProgressBarBuilder()
                    .setTaskName(title)
                    .setInitialMax(maxStep)
                    .setStyle(ProgressBarStyle.ASCII)
                    .hideEta()
                    .build();
        }
        instances.push(this);
    }

    /**
     * <p>Getter for the field <code>title</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * <p>Getter for the field <code>step</code>.</p>
     *
     * @return a int
     */
    public int getStep() {
        return this.step;
    }

    /**
     * <p>Getter for the field <code>maxStep</code>.</p>
     *
     * @return a int
     */
    public int getMaxStep() {
        return this.maxStep;
    }

    /**
     * <p>Getter for the field <code>message</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getMessage() {
        return this.message;
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
        step = step + by;
        this.message = message;
        if (output) {
            pb.setExtraMessage(message).stepTo(step);
        }
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
     * <p>getCurrentInstance.</p>
     *
     * @return a {@link ryzen.ownitall.util.ProgressBar} object
     */
    public static ProgressBar getCurrentInstance() {
        if (instances.isEmpty()) {
            return null;
        }
        return instances.peek();
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (output) {
            pb.setExtraMessage("Done").stepTo(maxStep);
            pb.close();
        }
        // needed to trigger web gui completion
        maxStep = 1;
        if (!instances.remove(this)) {
            instances.pop();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof ProgressBar)) {
            return false;
        }
        ProgressBar pb = (ProgressBar) object;
        if (this.hashCode() == pb.hashCode()) {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return instances.size();
    }
}
