package ryzen.ownitall.util;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class Progressbar {
    /**
     * standardized progressbar
     * 
     * @param title   - title of progress bar
     * @param maxStep - max steps till completion
     * @return - constructed ProgressBar
     */
    // TODO: make this work with logger
    public static ProgressBar progressBar(String title, int maxStep) {
        return new ProgressBarBuilder()
                .setInitialMax(maxStep)
                .setTaskName(title)
                // .setConsumer(new DelegatingProgressBarConsumer(logger::info))
                .setStyle(ProgressBarStyle.ASCII)
                .hideEta()
                .build();
    }
}
