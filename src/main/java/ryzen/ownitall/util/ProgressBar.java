package ryzen.ownitall.util;

//http://tongfei.me/progressbar/
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

/**
 * <p>
 * ProgressBar class.
 * </p>
 *
 * @author ryzen
 */
public class ProgressBar implements AutoCloseable {
    private static ProgressBar instance;
    private String title;
    private int maxStep;
    private int step;
    private String message;
    private me.tongfei.progressbar.ProgressBar pb;

    public static boolean output = true;

    /**
     * <p>
     * Constructor for ProgressBar.
     * </p>
     *
     * @param title   a {@link java.lang.String} object
     * @param maxStep a int
     */
    private ProgressBar(String title, int maxStep) {
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
    }

    public static ProgressBar getInstance(String title, int maxStep) {
        if (instance == null) {
            instance = new ProgressBar(title, maxStep);
        }
        return instance;
    }

    public static ProgressBar getInstance() {
        return instance;
    }

    public String getTitle() {
        return this.title;
    }

    public int getStep() {
        return this.step;
    }

    public int getMaxStep() {
        return this.maxStep;
    }

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

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (output) {
            pb.setExtraMessage("Done").stepTo(maxStep);
            pb.close();
        }
        instance = null;
    }
}
