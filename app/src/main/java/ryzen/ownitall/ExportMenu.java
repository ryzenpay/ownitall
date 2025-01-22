package ryzen.ownitall;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.tools.Menu;

public class ExportMenu {
    private static final Logger logger = LogManager.getLogger(ExportMenu.class);
    private LinkedHashMap<String, Runnable> options;
    private Collection collection;

    public ExportMenu(Collection collection) {
        this.collection = collection;
        this.options = new LinkedHashMap<>();
        options.put("Download (YoutubeDL)", this::optionDownload);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "EXPORT");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    private void optionDownload() {
        logger.info("Downloading music...");
        Download download = new Download();
        ProgressBar pb = Main.progressBar("Download music", 3);
        pb.setExtraMessage("Liked songs");
        // TODO: if song is liked + playlist/album it will download twice
        download.downloadLikedSongs(this.collection.getLikedSongs());
        pb.setExtraMessage("Playlists").step();
        LinkedHashSet<Playlist> playlists = this.collection.getPlaylists();
        ProgressBar pbPlaylist = Main.progressBar("Playlist Downloads", playlists.size());
        for (Playlist playlist : playlists) {
            pbPlaylist.setExtraMessage(playlist.getName());
            download.downloadPlaylist(playlist);
            pbPlaylist.step();
        }
        pbPlaylist.setExtraMessage("Done").step();
        pbPlaylist.close();
        pb.setExtraMessage("Albums").step();
        LinkedHashSet<Album> albums = this.collection.getAlbums();
        ProgressBar pbAlbum = Main.progressBar("Album Downloads", albums.size());
        for (Album album : albums) {
            pbAlbum.setExtraMessage(album.getName());
            download.downloadAlbum(album);
            pbAlbum.step();
        }
        pbAlbum.setExtraMessage("Done").step();
        pbAlbum.close();
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("Done downloading music");
    }
}
