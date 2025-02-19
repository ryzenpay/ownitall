package ryzen.ownitall.library;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Signal;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.Progressbar;

public class Download {
    private static final Logger logger = LogManager.getLogger(Download.class);
    private static final Settings settings = Settings.load();
    private static Collection collection = Collection.load();
    private ExecutorService executor;
    private String downloadPath;
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.SEVERE);
    }
    private volatile AtomicBoolean interrupted = new AtomicBoolean(false);

    /**
     * default download constructor
     * setting all settings / credentials
     */
    public Download() {
        if (settings.getYoutubedlPath().isEmpty()) {
            settings.setYoutubedlPath();
        }
        if (settings.getFfmpegPath().isEmpty()) {
            settings.setFfmpegPath();
        }
        if (settings.getDownloadFolder().isEmpty()) {
            this.setDownloadPath();
        } else {
            this.downloadPath = settings.getDownloadFolder();
        }
        System.out.println("This is where i reccomend you to connect to VPN / use proxies");
        System.out.print("Enter y to continue: ");
        try {
            Input.request().getAgreement();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting vpn agreement");
        }
    }

    public String getDownloadPath() {
        return this.downloadPath;
    }

    /**
     * prompt of where to save downloaded music
     */
    private void setDownloadPath() {
        try {
            System.out.print("Please provide path to save music: ");
            this.downloadPath = Input.request().getFile(false).getAbsolutePath();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting download path");
            return;
        }
    }

    public void threadDownload(Song song, File path) {
        if (song == null || path == null) {
            logger.debug("Empty song or path provided in threadDownload");
            return;
        }
        if (interrupted.get()) {
            return;
        }
        if (this.executor == null || this.executor.isShutdown()) {
            this.threadInit();
        }
        // Set up a signal handler for SIGINT (Ctrl+C)
        Signal.handle(new Signal("INT"), signal -> {
            logger.info("Download interruption caught, finishing any in queue");
            interrupted.set(true);
        });
        while (!interrupted.get()) {
            try {
                // Attempt to execute the task
                executor.execute(() -> {
                    this.downloadSong(song, path);
                });
                break;
            } catch (RejectedExecutionException e) {
                // If the queue is full, wait for a thread to become free
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    interrupted.set(true);
                    logger.error("Awaiting for free thread was interrupted" + ie);
                }
            }
        }
        if (interrupted.get()) {
            threadShutdown();
        }
    }

    public void threadInit() {
        this.executor = new ThreadPoolExecutor(
                settings.getDownloadThreads(),
                settings.getDownloadThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(settings.getDownloadThreads()));
    }

    public void threadShutdown() {
        if (this.executor == null || this.executor.isShutdown()) {
            return;
        }
        try {
            executor.shutdown();
            logger.debug("Awaiting current threads to shutdown (max 10 min)");
            executor.awaitTermination(10, TimeUnit.MINUTES);
            logger.debug("All threads shut down");
        } catch (InterruptedException e) {
            logger.error("Awaiting for threads to finish was interrupted, shutting down now: " + e);
            executor.shutdownNow();
        }
    }

    /**
     * download a specified song
     * 
     * @param song - constructed song
     * @param path - folder of where to place
     */
    public void downloadSong(Song song, File path) {
        if (song == null || path == null) {
            logger.debug("Empty song or Path provided in downloadSong");
            return;
        }
        List<String> command = new ArrayList<>();
        // executables
        command.add(settings.getYoutubedlPath());
        command.add("--ffmpeg-location");
        command.add(settings.getFfmpegPath());
        // command.add("--concurrent-fragments");
        // command.add(String.valueOf(settings.getDownloadThreads()));
        // set up youtube searching and only 1 result
        command.add("--default-search");
        command.add("ytsearch1");
        // exclude any found playlists or shorts
        command.add("--no-playlist"); // Prevent downloading playlists
        command.add("--break-match-filter");
        command.add("duration>=45"); // exclude shorts
        if (!settings.getDownloadCookiesFile().isEmpty()) {
            command.add("--cookies");
            command.add(settings.getDownloadCookiesFile());
        } else if (!settings.getDownloadCookiesBrowser().isEmpty()) {
            command.add("--cookies-from-browser");
            command.add(settings.getDownloadCookiesBrowser());
        }
        // metadata and formatting
        command.add("--extract-audio");
        command.add("--embed-thumbnail");
        command.add("--format");
        command.add("bestaudio/best");
        command.add("--audio-format");
        command.add(settings.getDownloadFormat());
        command.add("--audio-quality");
        command.add(String.valueOf(settings.getDownloadQuality()));
        command.add("--embed-metadata"); // metadata we have overwrites this
        command.add("--no-write-comments");
        // download location
        command.add("--paths");
        command.add(path.getAbsolutePath());
        command.add("--output");
        command.add(song.getFileName() + ".%(ext)s");
        /**
         * search for video using the query / use url
         * ^ keep this at the end, incase of fucked up syntax making the other flags
         * drop
         */
        String searchQuery;
        if (song.getId("youtube") != null) {
            searchQuery = "https://youtube.com/watch?v=" + song.getId("youtube");
        } else {
            // search query filters
            searchQuery = song.toString() + " (official audio)"; // youtube search criteria
            // prevent any search impacting triggers + pipeline starters
            searchQuery = searchQuery.replaceAll("[\\\\/<>|:]", "");
        }
        command.add(searchQuery);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            int retries = 0;
            File songFile = new File(path, song.getFileName() + "." + settings.getDownloadFormat());
            StringBuilder completeLog = new StringBuilder();
            while (!songFile.exists() && retries < 3) {
                Process process = processBuilder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    // Capture output for logging
                    while ((line = reader.readLine()) != null) {
                        completeLog.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    if (exitCode == 2) {
                        logger.debug("Error with user provided options: " + command.toString());
                        break;
                    } else if (exitCode == 100) {
                        logger.error("Your yt-dlp needs to update");
                        break;
                    } else if (exitCode == 101) {
                        logger.debug("Download cancelled due to boundary criteria: " + searchQuery);
                        break;
                    } else {
                        logger.error("Unkown error while downloading song: " + song + "with code: " + exitCode);
                        logger.error(command.toString());
                        logger.error(completeLog.toString());
                    }
                    logger.error("Attempt: " + retries);
                }
                retries++;
            }
            if (songFile.exists()) {
                writeMetaData(song, songFile);
            } else {
                logger.info("the song " + song.toString() + " failed to download, check log for why");
            }
        } catch (Exception e) {
            logger.error("Error preparing yt-dlp: ", e);
        }
    }

    public static void writeMetaData(Song song, File songFile) {
        Album foundAlbum = collection.getSongAlbum(song);
        String albumName = null;
        if (foundAlbum != null) {
            albumName = foundAlbum.getName();
        }
        try {
            MusicTools.writeMetaData(song.getName(), song.getArtist().getName(), song.getCoverImage(),
                    collection.isLiked(song), albumName, songFile);
        } catch (Exception e) {
            logger.error("Error song metadata for " + song.toString() + ": " + e);
        }
    }

    /**
     * orchestrator of DownloadSong for all standalone liked songs
     * 
     */
    public void downloadLikedSongs() {
        LinkedHashSet<Song> likedSongs;
        File likedSongsFolder;
        if (settings.isDownloadHierachy()) {
            likedSongs = collection.getStandaloneLikedSongs();
            likedSongsFolder = new File(this.downloadPath, settings.getLikedSongName());
            likedSongsFolder.mkdirs();
        } else {
            likedSongs = collection.getLikedSongs().getSongs();
            likedSongsFolder = new File(this.downloadPath);
        }
        ProgressBar pb = Progressbar.progressBar("Downloading Liked songs", likedSongs.size() + 1);
        for (Song song : likedSongs) {
            pb.setExtraMessage(song.getName()).step();
            // TODO: if song previously not in album, but then in album, delete from main
            // folder
            if (settings.isDownloadHierachy() || collection.getSongAlbum(song) == null) {
                this.threadDownload(song, likedSongsFolder);
            }
        }
        pb.setExtraMessage("cleaning up").step();
        this.threadShutdown();
        logger.info("Clearing absess files");
        this.cleanFolder(likedSongsFolder);
        pb.setExtraMessage("Done").close();
    }

    /**
     * orchestrator of downloadPlaylist
     */
    public void downloadPlaylists() {
        LinkedHashSet<Playlist> playlists = collection.getPlaylists();
        ProgressBar pbPlaylist = Progressbar.progressBar("Playlist Downloads", playlists.size());
        for (Playlist playlist : playlists) {
            pbPlaylist.setExtraMessage(playlist.getName());
            this.downloadPlaylist(playlist);
            pbPlaylist.step();
        }
        pbPlaylist.setExtraMessage("Done").step().close();
    }

    /**
     * orchestrator for downloading a playlist
     * 
     * @param playlist - constructed playlist to download
     */
    public void downloadPlaylist(Playlist playlist) {
        if (playlist == null) {
            logger.debug("Empty playlist provided in downloadPlaylist");
            return;
        }
        File playlistFolder;
        if (settings.isDownloadHierachy()) {
            playlistFolder = new File(this.downloadPath, playlist.getFolderName());
            playlistFolder.mkdirs();
        } else {
            playlistFolder = new File(downloadPath);
        }
        ProgressBar pb = Progressbar.progressBar("Downloading Playlists: " + playlist.getName(), playlist.size() + 1);
        this.writePlaylistData(playlist, playlistFolder);
        for (Song song : playlist.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            if (settings.isDownloadHierachy() || collection.getSongAlbum(song) == null) {
                this.threadDownload(song, playlistFolder);
            }
        }
        pb.setExtraMessage("cleaning up").step();
        this.threadShutdown();
        logger.info("Clearing absess files");
        this.cleanFolder(playlistFolder);
        pb.setExtraMessage("Done").close();
    }

    public void downloadAlbums() {
        LinkedHashSet<Album> albums = collection.getAlbums();
        ProgressBar pbAlbum = Progressbar.progressBar("Album Downloads", albums.size());
        for (Album album : albums) {
            pbAlbum.setExtraMessage(album.getName());
            this.downloadAlbum(album);
            pbAlbum.step();
        }
        pbAlbum.setExtraMessage("Done").step().close();
    }

    public void downloadAlbum(Album album) {
        if (album == null) {
            logger.debug("Empty album provided in downloadAlbum");
            return;
        }
        ProgressBar pb = Progressbar.progressBar("Download Album: " + album.getName(), album.size() + 1);
        // albums are always in a folder
        File albumFolder = new File(this.downloadPath, album.getFolderName());
        this.writeAlbumData(album, albumFolder);
        for (Song song : album.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            this.threadDownload(song, albumFolder);
        }
        pb.setExtraMessage("cleaning up").step();
        this.threadShutdown();
        logger.info("Clearing absess files");
        this.cleanFolder(albumFolder);
        pb.setExtraMessage("Done").close();
    }

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
            MusicTools.writeData(playlist.getFolderName(), "m3u", collection.getPlaylistM3U(playlist), folder);
        } catch (Exception e) {
            logger.error("Error writing playlist (" + folder.getAbsolutePath() + ") m3u: " + e);
        }
        try {
            if (playlist.getCoverImage() != null) {
                MusicTools.downloadImage(playlist.getCoverImage(),
                        new File(folder, playlist.getFolderName() + ".png"));
            }
        } catch (Exception e) {
            logger.error("Error writing playlist (" + folder.getAbsolutePath() + ") coverimage: " + e);
        }
    }

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
            MusicTools.writeData("album", "nfo", album.getNFO(), folder);
        } catch (Exception e) {
            logger.error("Error writing album (" + folder.getAbsolutePath() + ") nfo: " + e);
        }
        try {
            if (album.getCoverImage() != null) {
                MusicTools.downloadImage(album.getCoverImage(),
                        new File(folder, "cover.png"));
            }
        } catch (Exception e) {
            logger.error("Error writing album (" + folder.getAbsolutePath() + ") coverimage: " + e);
        }
    }

    public void cleanFolder(File folder) {
        ArrayList<String> whiteList = new ArrayList<>(Arrays.asList("m3u", "png", "nfo", settings.getDownloadFormat()));
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            logger.debug("Folder is null, does not exist or is not a directorty in cleanFolder");
            return;
        }
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                String extension = MusicTools.getExtension(file);
                if (!whiteList.contains(extension)) {
                    if (file.delete()) {
                        logger.debug("Cleaned up file: " + file.getAbsolutePath());
                    } else {
                        logger.error("Failed to clean up file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }
}
