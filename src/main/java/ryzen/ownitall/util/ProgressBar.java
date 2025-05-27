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
    private String title;
    private int maxStep;
    private int step;
    private long startTime;
    private String message;
    private me.tongfei.progressbar.ProgressBar pb;
    /** Constant <code>output=true</code> */
    public static boolean output = true;
    private static long elapsedTime;
    private static ProgressBar instance;
    // cheat method of when to close pb
    private static int loadCounter = 0;

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
        this.step = 0;
        this.startTime = System.currentTimeMillis();
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

    public static ProgressBar load(String title, int maxStep) {
        if (instance == null) {
            instance = new ProgressBar(title, maxStep);
        } else {
            instance.title += ": " + title;
            if (maxStep < 0) {
                instance.maxStep = maxStep;
            } else {
                if (instance.maxStep < 0) {
                    instance.maxStep = maxStep;
                } else {
                    instance.maxStep += maxStep;
                }
            }
            if (output) {
                instance.pb = new ProgressBarBuilder()
                        .setTaskName(title)
                        .setInitialMax(instance.maxStep)
                        .setStyle(ProgressBarStyle.ASCII)
                        .hideEta()
                        .build();
                instance.pb.stepTo(instance.step);
            }
        }
        loadCounter++;
        return instance;
    }

    /**
     * <p>
     * Getter for the field <code>title</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * <p>
     * Getter for the field <code>step</code>.
     * </p>
     *
     * @return a int
     */
    public int getStep() {
        return this.step;
    }

    /**
     * <p>
     * Getter for the field <code>maxStep</code>.
     * </p>
     *
     * @return a int
     */
    public int getMaxStep() {
        return this.maxStep;
    }

    /**
     * <p>
     * Getter for the field <code>message</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getMessage() {
        return this.message;
    }

    public static long getElapsedTime() {
        if (instance == null) {
            return elapsedTime;
        }
        return System.currentTimeMillis() - getInstance().startTime;
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
     * <p>
     * getCurrentInstance.
     * </p>
     *
     * @return a {@link ryzen.ownitall.util.ProgressBar} object
     */
    public static ProgressBar getInstance() {
        if (instance == null) {
            return null;
        }
        return instance;
    }

    @Override
    public void close() {
        elapsedTime = getElapsedTime();
        // remove sub category of progress bar
        instance.title = instance.title.split(":")[0];
        loadCounter--;
        if (loadCounter == 0) {
            if (output) {
                pb.setExtraMessage("Done").stepTo(instance.maxStep);
                pb.close();
            }
            // needed to trigger web gui completion
            this.maxStep = 1;
            instance = null;
        }
    }
}
