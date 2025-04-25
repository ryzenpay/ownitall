package ryzen.ownitall.output.cli;

//http://tongfei.me/progressbar/
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class ProgressBar implements AutoCloseable {
    // private String title;
    private int maxStep;
    private int step;
    private me.tongfei.progressbar.ProgressBar pb;

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

    public void step(String message, int by) {
        this.step = this.step + by;
        pb.setExtraMessage(message).stepTo(step);
    }

    public void step(String message) {
        this.step(message, 1);
    }

    public void step() {
        this.step(null, 1);
    }

    public void step(int by) {
        this.step(null, by);
    }

    @Override
    public void close() {
        pb.setExtraMessage("Done").stepTo(this.maxStep);
        pb.close();
    }
}
