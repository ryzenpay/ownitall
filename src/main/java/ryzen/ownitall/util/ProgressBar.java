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
    private static String title;
    private static int maxStep;
    private static int step;
    private static String message;
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
    public ProgressBar(String newTitle, int newMaxStep) {
        title = newTitle;
        step = 0;
        // this.title = title;
        maxStep = newMaxStep;
        if (output) {
            this.pb = new ProgressBarBuilder()
                    .setTaskName(title)
                    .setInitialMax(maxStep)
                    .setStyle(ProgressBarStyle.ASCII)
                    .hideEta()
                    .build();
        }
    }

    /**
     * <p>
     * step.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     * @param by      a int
     */
    public void step(String newMessage, int by) {
        step = step + by;
        message = newMessage;
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

    public static String getTitle() {
        return title;
    }

    public static int getStep() {
        return step;
    }

    public static int getMaxStep() {
        return maxStep;
    }

    public static String getMessage() {
        return message;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (output) {
            pb.setExtraMessage("Done").stepTo(maxStep);
            pb.close();
        }
    }
}
