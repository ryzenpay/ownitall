package ryzen.ownitall.library.menu;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Download;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class DownloadMenu {
    private static final Logger logger = LogManager.getLogger(DownloadMenu.class);
    private static final Settings settings = Settings.load();
    private static final Credentials credentials = Credentials.load();
    private static Collection collection = Collection.load();
    private Download download;

    public DownloadMenu() {
        this.download = new Download();
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Download Library", this::optionDownloadCollection);
        options.put("Download Playlist", this::optionDownloadPlaylist);
        options.put("Download Album", this::optionDownloadAlbum);
        options.put("Download Liked Songs", this::optionDownloadLikedSongs);
        options.put("Write Collection Data", this::optionCollectionData);
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
        download.downloadPlaylists();
        pb.setExtraMessage("Albums").step();
        download.downloadAlbums();
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("Done downloading music");
    }

    private void optionDownloadPlaylist() {
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        options.put("All", null);
        for (Playlist playlist : collection.getPlaylists()) {
            options.put(playlist.toString(), playlist);
        }
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD PLAYLIST");
            if (choice.equals("Exit")) {
                break;
            } else if (choice.equals("All")) {
                logger.info("Downloading all playlists...");
                download.downloadPlaylists();
            } else {
                logger.info("Downloading playlist " + choice + "...");
                download.downloadPlaylist(options.get(choice));
                break;
            }
        }
        logger.info("Done downloading playlist");
    }

    private void optionDownloadAlbum() {
        LinkedHashMap<String, Album> options = new LinkedHashMap<>();
        options.put("All", null);
        for (Album album : collection.getAlbums()) {
            options.put(album.toString(), album);
        }
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD ALBUM");
            if (choice.equals("Exit")) {
                break;
            } else if (choice.equals("All")) {
                logger.info("Downloading all albums");
                download.downloadAlbums();
            } else {
                logger.info("Downloading album " + choice + "...");
                download.downloadAlbum(options.get(choice));
                break;
            }
        }
        logger.info("Done donwloading album");
    }

    private void optionDownloadLikedSongs() {
        logger.info("Downloading liked songs...");
        download.downloadLikedSongs();
        logger.info("Done downloading liked songs");
    }

    private void optionCollectionData() {
        logger.info("Writing collection data (M3U, NFO, coverimages)...");
        ProgressBar pb = Progressbar.progressBar("Music Metadata", 3);
        pb.setExtraMessage("Albums");
        String downloadPath = download.getDownloadPath();
        for (Album album : collection.getAlbums()) {
            File albumFolder = new File(downloadPath, album.getFolderName());
            download.writeAlbumData(album, albumFolder);
            for (Song song : album.getSongs()) {
                File songFile = new File(albumFolder, song.getFileName());
                if (songFile.exists()) {
                    Download.writeMetaData(song, songFile);
                }
            }
        }
        pb.setExtraMessage("Playlists").step();
        for (Playlist playlist : collection.getPlaylists()) {
            File playlistFolder;
            LinkedHashSet<Song> songs;
            if (settings.isDownloadHierachy()) {
                songs = playlist.getSongs();
                playlistFolder = new File(download.getDownloadPath(), playlist.getFolderName());
                playlistFolder.mkdirs();
            } else {
                songs = collection.getStandalonePlaylistSongs(playlist);
                playlistFolder = new File(downloadPath);
            }
            download.writePlaylistData(playlist, playlistFolder);
            for (Song song : songs) {
                File songFile = new File(playlistFolder, song.getFileName());
                if (songFile.exists()) {
                    Download.writeMetaData(song, songFile);
                }
            }
        }
        pb.setExtraMessage("Liked Songs").step();
        LinkedHashSet<Song> songs;
        File likedSongsFolder;
        if (settings.isDownloadHierachy()) {
            songs = collection.getLikedSongs().getSongs();
            likedSongsFolder = new File(download.getDownloadPath(), settings.getLikedSongName());
        } else {
            songs = collection.getStandaloneLikedSongs();
            likedSongsFolder = new File(download.getDownloadPath());
        }
        for (Song song : songs) {
            File songFile = new File(likedSongsFolder, song.getFileName());
            if (songFile.exists()) {
                Download.writeMetaData(song, songFile);
            }
        }
        pb.setExtraMessage("Done").step().close();
        logger.info("Done writing collection data");
    }

    // TODO: jellyfin api
    // mark favorites, remove favorites, scrape favorites, ...
    // https://api.jellyfin.org
    private void optionJellyFinFavorites() {
        if (credentials.jellyFinisEmpty()) {
            credentials.setJellyFinCredentials();
        }
        try {
            System.out.print("Enter JellyFin username: ");
            String username = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting JellyFin username");
            return;
        }
    }
}
