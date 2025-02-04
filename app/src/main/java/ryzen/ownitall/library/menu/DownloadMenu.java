package ryzen.ownitall.library.menu;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Download;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class DownloadMenu {
    private static final Logger logger = LogManager.getLogger(DownloadMenu.class);
    private static final Collection collection = Collection.load();
    private static final Settings settings = Settings.load();
    private Download download;

    public DownloadMenu() {
        this.download = new Download();
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Download Library", this::optionDownloadCollection);
        options.put("Download Playlist", this::optionDownloadPlaylist);
        options.put("Download Album", this::optionDownloadAlbum);
        options.put("Download Liked Songs", this::optionDownloadLikedSongs);
        options.put("Write Metadata (existing songs)", this::optionMetaData);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    /**
     * option to locally download entire collection
     */
    private void optionDownloadCollection() {
        logger.info("Downloading music...");
        ProgressBar pb = Progressbar.progressBar("Download music", 3);
        pb.setExtraMessage("Liked songs");
        download.downloadLikedSongs();
        pb.setExtraMessage("Playlists").step();
        download.downloadPlaylists(collection.getPlaylists());
        pb.setExtraMessage("Albums").step();
        download.downloadAlbums(collection.getAlbums());
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("Done downloading music");
        // TODO: error handling for failed songs
        download.getFailedSongsReport();
    }

    private void optionDownloadPlaylist() {
        logger.info("Download Playlist...");
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        for (Playlist playlist : collection.getPlaylists()) {
            options.put(playlist.toString(), playlist);
        }
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD PLAYLIST");
            if (choice.equals("Exit")) {
                break;
            } else {
                download.downloadPlaylist(options.get(choice));
                break;
            }
        }
        logger.info("Done downloading playlist");
        download.getFailedSongsReport();
    }

    private void optionDownloadAlbum() {
        logger.info("Downloading album...");
        LinkedHashMap<String, Album> options = new LinkedHashMap<>();
        for (Album album : collection.getAlbums()) {
            options.put(album.toString(), album);
        }
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD ALBUM");
            if (choice.equals("Exit")) {
                break;
            } else {
                download.downloadAlbum(options.get(choice));
                break;
            }
        }
        logger.info("Done donwloading album");
        download.getFailedSongsReport();
    }

    private void optionDownloadLikedSongs() {
        logger.info("Downloading liked songs...");
        download.downloadLikedSongs();
        logger.info("Done downloading liked songs");
        download.getFailedSongsReport();
    }

    private void optionMetaData() {
        logger.info("Writing Metadata...");
        ProgressBar pb = Progressbar.progressBar("Writing Metadata", 3);
        String downloadPath = download.getDownloadPath();
        // liked songs
        pb.setExtraMessage("Liked songs");
        File likedSongsFolder = new File(downloadPath);
        if (settings.isDownloadHierachy()) {
            likedSongsFolder = new File(downloadPath, settings.getLikedSongName());
            likedSongsFolder.mkdirs();
        }
        LinkedHashSet<Song> likedSongs;
        if (settings.isDownloadAllLikedSongs()) {
            likedSongs = collection.getLikedSongs().getSongs();
        } else {
            likedSongs = collection.getStandaloneLikedSongs();
        }
        Download.writeSongsMetaData(likedSongs, likedSongsFolder, null);
        // playlists
        pb.setExtraMessage("Playlists").step();
        for (Playlist playlist : collection.getPlaylists()) {
            File playlistFolder = new File(downloadPath, playlist.getFolderName());
            Download.writeSongsMetaData(playlist.getSongs(), playlistFolder, null);
        }
        // albums
        pb.setExtraMessage("Albums").step();
        for (Album album : collection.getAlbums()) {
            File albumFolder = new File(downloadPath, album.getFolderName());
            Download.writeSongsMetaData(album.getSongs(), albumFolder, album.getName());
        }
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("Done writing metadata");
    }
}
