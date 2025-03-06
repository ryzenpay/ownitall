package ryzen.ownitall.library;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Credentials;

public class Jellyfin extends Library {
    private static final Logger logger = LogManager.getLogger(LastFM.class);
    private static final Credentials credentials = Credentials.load();

    // https://api.jellyfin.org/
    public Jellyfin() throws InterruptedException {
        super();
        if (credentials.jellyfinIsEmpty()) {
            credentials.setJellyfinCredentials();
        }
        this.queryDiff = 1;
    }
}
