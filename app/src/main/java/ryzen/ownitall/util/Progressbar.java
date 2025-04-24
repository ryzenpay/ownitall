package ryzen.ownitall.util;

//http://tongfei.me/progressbar/
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class ProgressBar implements AutoCloseable {
    private int step;
    private static boolean cli;
    private me.tongfei.progressbar.ProgressBar cliPb;
    private static boolean web;

    public ProgressBar(String title, int maxStep) {
        this.step = 0;
        if (cli) {
            this.cliPb = new ProgressBarBuilder()
                    .setTaskName(title)
                    .setInitialMax(maxStep)
                    .setStyle(ProgressBarStyle.ASCII)
                    .hideEta()
                    .build();
        }
    }

    public static void setCLI() {
        cli = true;
    }

    public static void setWeb() {
        web = true;
    }

    public void step(String message, int by) {
        this.step = this.step + by;
        if (cli) {
            cliPb.setExtraMessage(message).stepTo(step);
        }
        if (web) {
            // TODO: web progress bar
        }
    }

    public void step(String message) {
        this.step(message, 1);
    }

    @Override
    public void close() {
        if (cli) {
            cliPb.setExtraMessage("Done").step();
            cliPb.close();
        }
        if (web) {
            // TODO: web progress bar
        }
    }
}
