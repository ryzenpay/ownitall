package ryzen.ownitall.output.cli;

//http://tongfei.me/progressbar/
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

/**
 * <p>ProgressBar class.</p>
 *
 * @author ryzen
 */
public class ProgressBar implements AutoCloseable {
    // private String title;
    private int maxStep;
    private int step;
    private me.tongfei.progressbar.ProgressBar pb;

    /**
     * <p>Constructor for ProgressBar.</p>
     *
     * @param title a {@link java.lang.String} object
     * @param maxStep a int
     */
    public ProgressBar(String title, int maxStep) {
        this.step = 0;
        // this.title = title;
        this.maxStep = maxStep;
        this.pb = new ProgressBarBuilder()
                .setTaskName(title)
                .setInitialMax(maxStep)
                .setStyle(ProgressBarStyle.ASCII)
                .hideEta()
                .build();
    }

    /**
     * <p>step.</p>
     *
     * @param message a {@link java.lang.String} object
     * @param by a int
     */
    public void step(String message, int by) {
        this.step = this.step + by;
        pb.setExtraMessage(message).stepTo(step);
    }

    /**
     * <p>step.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void step(String message) {
        this.step(message, 1);
    }

    /**
     * <p>step.</p>
     */
    public void step() {
        this.step(null, 1);
    }

    /**
     * <p>step.</p>
     *
     * @param by a int
     */
    public void step(int by) {
        this.step(null, by);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        pb.setExtraMessage("Done").stepTo(this.maxStep);
        pb.close();
    }
}
