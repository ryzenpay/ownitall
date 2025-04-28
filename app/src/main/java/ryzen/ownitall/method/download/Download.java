package ryzen.ownitall.method.download;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.tag.FieldKey;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.util.MusicTools;

public class Download extends Method {
    // TODO: multiple download sources
    // qobuz
    // deezer
    // youtube (already implemented)
    // tidal
    // PRIORITY: soulseek
    private static final Logger logger = LogManager.getLogger();
    private ExecutorService executor;
    private static final ArrayList<String> whiteList = new ArrayList<>(
            Arrays.asList("m3u", "png", "nfo", Settings.downloadFormat));
    protected File localLibrary = Settings.localFolder;
    public static final LinkedHashMap<String, Class<? extends Download>> methods;
    static {
        methods = new LinkedHashMap<>();
        methods.put("yt-dl", YT_dl.class);
    }

    public void threadDownload(Song song, File path) throws InterruptedException {
        if (song == null || path == null) {
            logger.debug("null song or path provided in threadDownload");
            return;
        }
        if (this.executor == null || this.executor.isShutdown()) {
            this.threadInit();
        }
        while (true) {
            try {
                // Attempt to execute the task
                executor.execute(() -> {
                    this.downloadSong(song, path);
                });
                break;
            } catch (RejectedExecutionException e) {
                Thread.sleep(1000);
            }
        }
    }

    /**
     * setup threading
     */
    public void threadInit() {
        int downloadThreads = Settings.downloadThreads;
        this.executor = new ThreadPoolExecutor(
                downloadThreads,
                downloadThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(downloadThreads));
    }

    /**
     * shut down all threads
     * 
     * @throws InterruptedException - if user interrupts while waiting
     */
    public void threadShutdown() throws InterruptedException {
        if (this.executor == null || this.executor.isShutdown()) {
            return;
        }
        executor.shutdown();
        logger.debug("Awaiting current threads to shutdown (max 10 min)");
        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
            logger.debug("All threads shut down");
        } catch (InterruptedException e) {
            executor.shutdownNow();
            logger.debug("All threads forcibly shut down");
            throw e;
        }
    }

    public void downloadSong(Song song, File path) {
        logger.warn("Unsupported download method to downloadSong");
    }

    /**
     * write song metadata
     * wrapper for MusicTools metadata writer
     * 
     * @param song     - song to get metadata details from
     * @param songFile - song file to write metadata to
     */
    public static void writeMetaData(Song song, File songFile) {
        if (song == null) {
            logger.debug("null song provided in writeMetaData");
            return;
        }
        if (songFile == null || !songFile.exists()) {
            logger.debug("null or non existant songFile provided in writeMetaData");
            return;
        }
        LinkedHashMap<FieldKey, String> id3Data = new LinkedHashMap<>();
        id3Data.put(FieldKey.TITLE, song.getName());
        ArrayList<Artist> artists = song.getArtists();
        String artistList = "";
        // TODO: readme update for jellyfin, requires custom delimiter
        // dashboard -> libraries -> <select library> -> scroll to bottom -> custom
        // delimiter
        if (artists.size() == 1) {
            artistList = song.getMainArtist().getName();
        } else {
            for (Artist artist : artists) {
                artistList += artist.toString() + ";";
            }
        }
        if (!artistList.isEmpty()) {
            id3Data.put(FieldKey.ARTIST, artistList);
        }
        String albumName = song.getAlbumName();
        if (albumName != null) {
            id3Data.put(FieldKey.ALBUM, albumName);
        }
        String mbid = song.getId("mbid");
        if (mbid != null) {
            id3Data.put(FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID, mbid);
        }
        try {
            MusicTools.writeMetaData(id3Data, Collection.isLiked(song), song.getCoverImage(), songFile);
        } catch (Exception e) {
            logger.error("writing song metadata for '" + song.toString() + "'", e);
        }
    }

    /**
     * write playlist m3u data including coverimage
     * 
     * @param playlist - playlist to get data from
     * @param folder   - folder to place m3u file in
     */
    public void writePlaylistData(Playlist playlist, File folder) {
        if (playlist == null) {
            logger.debug("null playlist provided in writePlaylistData");
            return;
        }
        if (folder == null || !folder.exists()) {
            logger.debug("null or non existant folder provided in writePlaylistData");
            return;
        }
        try {
            File m3uFile = new File(folder, playlist.getFolderName() + ".m3u");
            MusicTools.writeData(m3uFile, Collection.getPlaylistM3U(playlist));
        } catch (Exception e) {
            logger.error("Exception writing playlist '" + playlist.toString() + "' m3u", e);
        }
        try {
            if (playlist.getCoverImage() != null) {
                MusicTools.downloadImage(playlist.getCoverImage(),
                        new File(folder, playlist.getCoverImageFileName()));
            }
        } catch (IOException e) {
            logger.error("Exception writing playlist '" + playlist.toString() + "' coverimage", e);
        }
    }

    /**
     * write album nfo data including coverimage
     * 
     * @param album  - album to get data from
     * @param folder - folder to place nfo file in
     */
    public void writeAlbumData(Album album, File folder) {
        if (album == null) {
            logger.debug("null Album provided in writeAlbumData");
            return;
        }
        if (folder == null || !folder.exists()) {
            logger.debug("null or non existant folder provided in writeAlbumData");
            return;
        }
        try {
            File nfoFile = new File(folder, "album.nfo");
            MusicTools.writeData(nfoFile, Collection.getAlbumNFO(album));
        } catch (Exception e) {
            logger.error("Exception writing album '" + album.toString() + "' nfo", e);
        }
        try {
            if (album.getCoverImage() != null) {
                MusicTools.downloadImage(album.getCoverImage(),
                        new File(folder, album.getCoverImageFileName()));
            }
        } catch (IOException e) {
            logger.error("Exception writing album '" + album.toString() + "' coverimage", e);
        }
    }

    /**
     * clean folder with unwanted files not ending in:
     * - m3u
     * - png
     * - nfo
     * these are specified in "whitelist"
     * 
     * @param folder - folder to clean up files from
     */
    public void cleanFolder(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            logger.debug("Folder is null, does not exist or is not a directorty in cleanFolder");
            return;
        }
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                String extension = MusicTools.getExtension(file);
                if (!whiteList.contains(extension)) {
                    if (file.delete()) {
                        logger.debug("Cleaned up file: '" + file.getAbsolutePath() + "'");
                    } else {
                        logger.error("Failed to clean up file: '" + file.getAbsolutePath() + "'");
                    }
                }
            }
        }
    }

}
