package ryzen.ownitall.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//http://tongfei.me/progressbar/
import me.tongfei.progressbar.DelegatingProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class Progressbar {
    private static final Logger logger = LogManager.getLogger(Progressbar.class);

    /**
     * standardized progressbar
     * 
     * @param title   - title of progress bar
     * @param maxStep - max steps till completion
     * @return - constructed ProgressBar
     */
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
