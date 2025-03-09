package ryzen.ownitall.methods;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.LastFM;

//TODO: import from jellyfin
public class Jellyfin {
    private static final Logger logger = LogManager.getLogger(LastFM.class);
    private static final Credentials credentials = Credentials.load();

    // https://api.jellyfin.org/
    public Jellyfin() throws InterruptedException {
        super();
        if (credentials.jellyfinIsEmpty()) {
            credentials.setJellyfinCredentials();
        }
    }

    // https://api.jellyfin.org/#tag/ItemLookup/operation/GetMusicVideoRemoteSearchResults
    public void uploadLikedSongs(ArrayList<Song> songs) {
        if (songs == null) {
            logger.debug("null songs provided in uploadLikedSongs");
            return;
        }
        for (Song song : songs) {
            LinkedHashMap<String, String> searchInfo = new LinkedHashMap<>();
            searchInfo.put("Name", song.getName());
            if (song.getArtist() != null) {
                searchInfo.put("Artists", "[" + song.getArtist() + "]");
            }
        }
    }

    private void query(String method, LinkedHashMap<String, String> params) {

    }

}
