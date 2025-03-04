package ryzen.ownitall.methods.menu;

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
import ryzen.ownitall.methods.Download;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class DownloadMenu {
    private static final Logger logger = LogManager.getLogger(DownloadMenu.class);
    private static final Settings settings = Settings.load();
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
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting download menu choice");
        }
    }

    /**
     * option to locally download entire collection
     */
    private void optionDownloadCollection() {
        logger.debug("Downloading music...");
        try (ProgressBar pb = Progressbar.progressBar("Download music", 3)) {
            pb.setExtraMessage("Liked songs");
            download.downloadLikedSongs();
            pb.setExtraMessage("Playlists").step();
            download.downloadPlaylists();
            pb.setExtraMessage("Albums").step();
            download.downloadAlbums();
            pb.setExtraMessage("Done").step();
            logger.debug("Done downloading music");
        } catch (InterruptedException e) {
            logger.debug("Interruption caught in download Collection");
        }
    }

    private void optionDownloadPlaylist() {
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        options.put("All", null);
        for (Playlist playlist : collection.getPlaylists()) {
            options.put(playlist.toString(), playlist);
        }
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD PLAYLIST");
                if (choice.equals("Exit")) {
                    break;
                } else if (choice.equals("All")) {
                    logger.debug("Downloading all playlists...");
                    download.downloadPlaylists();
                } else {
                    logger.debug("Downloading playlist " + choice + "...");
                    download.downloadPlaylist(options.get(choice));
                    break;
                }
                logger.debug("Done downloading playlist");
            }
        } catch (InterruptedException e) {
            logger.debug("Interruption caught downloading playlist");
        }
    }

    private void optionDownloadAlbum() {
        LinkedHashMap<String, Album> options = new LinkedHashMap<>();
        options.put("All", null);
        for (Album album : collection.getAlbums()) {
            options.put(album.toString(), album);
        }
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "DOWNLOAD ALBUM");
                if (choice.equals("Exit")) {
                    break;
                } else if (choice.equals("All")) {
                    logger.debug("Downloading all albums");
                    download.downloadAlbums();
                } else {
                    logger.debug("Downloading album " + choice + "...");
                    download.downloadAlbum(options.get(choice));
                    break;
                }
                logger.debug("Done downloading album");
            }
        } catch (InterruptedException e) {
            logger.debug("Interruption caught downloading Album");
        }
    }

    private void optionDownloadLikedSongs() {
        logger.info("Downloading liked songs...");
        try {
            download.downloadLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interruption caught downloading liked songs");
        }
        logger.info("Done downloading liked songs");
    }

    private void optionCollectionData() {
        logger.debug("Writing collection data (M3U, NFO, coverimages)...");
        try (ProgressBar pb = Progressbar.progressBar("Music Metadata",
                collection.getAlbumCount() + collection.getPlaylistCount() + collection.getLikedSongs().size())) {
            pb.setExtraMessage("Albums");
            String downloadPath = download.getDownloadPath();
            for (Album album : collection.getAlbums()) {
                pb.setExtraMessage(album.getName()).step();
                File albumFolder = new File(downloadPath, album.getFolderName());
                if (!albumFolder.exists()) {
                    continue;
                }
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
                pb.setExtraMessage(playlist.getName()).step();
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
                if (!playlistFolder.exists()) {
                    continue;
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
                likedSongsFolder = new File(download.getDownloadPath(), settings.getLikedSongsName());
            } else {
                songs = collection.getStandaloneLikedSongs();
                likedSongsFolder = new File(download.getDownloadPath());
            }
            for (Song song : songs) {
                pb.setExtraMessage(song.getName()).step();
                File songFile = new File(likedSongsFolder, song.getFileName());
                if (songFile.exists()) {
                    Download.writeMetaData(song, songFile);
                }
            }
            pb.setExtraMessage("Done");
            logger.debug("Done writing collection data");
        }
    }

    // TODO: jellyfin api
    // log in with get user id + api key
    // mark each liked song as favorite
}
