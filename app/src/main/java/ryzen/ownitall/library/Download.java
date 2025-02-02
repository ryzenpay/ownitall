package ryzen.ownitall.library;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Collection collection = Collection.load();
    private ExecutorService executor;
    private String downloadPath;
    private LinkedHashSet<Song> failedSongs;
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.SEVERE);
    }

    /**
     * default download constructor
     * setting all settings / credentials
     */
    public Download() {
        this.failedSongs = new LinkedHashSet<>();
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
        Input.request().getAgreement();
    }

    public String getDownloadPath() {
        return this.downloadPath;
    }

    /**
     * prompt of where to save downloaded music
     */
    private void setDownloadPath() {
        System.out.print("Please provide path to save music: ");
        this.downloadPath = Input.request().getFile(false).getAbsolutePath();
    }

    public void threadDownload(Song song, File path) {
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
                // If the queue is full, wait for a thread to become free
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Awaiting for free thread was interrupted" + ie);
                }
            }
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
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error("Awaiting for threads to finish was interrupted: " + e);
        }
    }

    /**
     * download a specified song
     * 
     * @param song - constructed song
     * @param path - folder of where to place
     */
    public void downloadSong(Song song, File path) {
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
        String searchQuery = song.getLink("youtube");
        if (searchQuery == null) {
            // search query filters
            searchQuery = song.toString() + " (official audio)"; // youtube search criteria
            // prevent any search impacting triggers + pipeline starters
            searchQuery = searchQuery.replaceAll("[\\\\/<>|:]", "");
            command.add(searchQuery);
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
                        // TODO: search library again?
                    } else {
                        logger.error("Unkown error while downloading song: " + song + "with code: " + exitCode);
                        logger.error(command.toString());
                        logger.error(completeLog.toString());
                    }
                    logger.error("Attempt: " + retries);
                }
                retries++;
            }
            if (!songFile.exists()) {
                this.failedSongs.add(song);
            }
        } catch (Exception e) {
            logger.error("Error preparing yt-dlp: ", e);
        }
    }

    /**
     * orchestrator of DownloadSong for all standalone liked songs
     * 
     * @param likedSongs - constructed liked songs
     */
    public void downloadLikedSongs() {
        LinkedHashSet<Song> likedSongs;
        if (settings.isDownloadAllLikedSongs()) {
            likedSongs = collection.getLikedSongs().getSongs();
        } else {
            likedSongs = collection.getStandaloneLikedSongs();
        }
        File likedSongsFolder = new File(this.downloadPath);
        if (settings.isDownloadHierachy()) {
            likedSongsFolder = new File(this.downloadPath, settings.getLikedSongName());
            likedSongsFolder.mkdirs();
        }
        ProgressBar pb = Progressbar.progressBar("Downloading Liked songs", likedSongs.size());
        for (Song song : likedSongs) {
            pb.setExtraMessage(song.getName()).step();
            this.threadDownload(song, likedSongsFolder);
        }
        this.threadShutdown();
        writeSongsMetaData(likedSongs, likedSongsFolder, null);
        this.cleanFolder(likedSongsFolder);
        pb.setExtraMessage("Done").close();
    }

    /**
     * orchestrator of downloadPlaylist
     * 
     * @param playlists - linkedhashset of playlists to download
     */
    public void downloadPlaylists(LinkedHashSet<Playlist> playlists) {
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
        File playlistFolder = new File(this.downloadPath, playlist.getFolderName());
        playlistFolder.mkdirs();
        ProgressBar pb = Progressbar.progressBar("Downloading Playlists: " + playlist.getName(), playlist.size());
        try {
            MusicTools.writeM3U(playlist.getFolderName(), playlist.getM3U(), playlistFolder);
        } catch (Exception e) {
            logger.error("Error writing playlist (" + playlistFolder.getAbsolutePath() + ") m3u: " + e);
        }
        try {
            if (playlist.getCoverImage() != null) {
                MusicTools.downloadImage(playlist.getCoverImage(), playlistFolder);
            }
        } catch (Exception e) {
            logger.error("Error writing playlist (" + playlistFolder.getAbsolutePath() + ") coverimage: " + e);
        }
        for (Song song : playlist.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            this.threadDownload(song, playlistFolder);
        }
        this.threadShutdown();
        writeSongsMetaData(playlist.getSongs(), playlistFolder, null);
        this.cleanFolder(playlistFolder);
        pb.setExtraMessage("Done").close();
    }

    public void downloadAlbums(LinkedHashSet<Album> albums) {
        ProgressBar pbAlbum = Progressbar.progressBar("Album Downloads", albums.size());
        for (Album album : albums) {
            pbAlbum.setExtraMessage(album.getName());
            this.downloadAlbum(album);
            pbAlbum.step();
        }
        pbAlbum.setExtraMessage("Done").step().close();
    }

    public void downloadAlbum(Album album) {
        ProgressBar pb = Progressbar.progressBar("Download Album: " + album.getName(), album.size());
        File albumFolder = new File(this.downloadPath, album.getFolderName());
        albumFolder.mkdirs();
        try {
            MusicTools.writeM3U(album.getFolderName(), album.getM3U(), albumFolder);
        } catch (Exception e) {
            logger.error("Error writing album (" + albumFolder.getAbsolutePath() + ") m3u: " + e);
        }
        try {
            MusicTools.downloadImage(album.getCoverImage(), albumFolder);
        } catch (Exception e) {
            logger.error("Error writing album (" + albumFolder.getAbsolutePath() + ") coverimage: " + e);
        }
        for (Song song : album.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            this.threadDownload(song, albumFolder);
        }
        this.threadShutdown();
        writeSongsMetaData(album.getSongs(), albumFolder, album.getName());
        this.cleanFolder(albumFolder);
        pb.setExtraMessage("Done").close();
    }

    public static void writeSongsMetaData(LinkedHashSet<Song> songs, File folder, String albumName) {
        if (!folder.exists()) {
            return;
        }
        for (Song song : songs) {
            File songFile = new File(folder, song.getFileName() + "." + settings.getDownloadFormat());
            try {
                MusicTools.writeMetaData(song.getName(), song.getArtist().getName(), song.getCoverImage(),
                        collection.isLiked(song), albumName, songFile);
            } catch (Exception e) {
                logger.error("Error song metadata for " + song.toString() + ": " + e);
            }
        }
    }

    public void cleanFolder(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return;
        }
        for (File file : folder.listFiles()) {
            if (file.isFile() && !file.getName().equals("cover.png")) {
                String extension = MusicTools.getExtension(file);
                if (!extension.equals(settings.getDownloadFormat()) && !extension.equals("m3u")) {
                    if (file.delete()) {
                        logger.debug("Cleaned up file: " + file.getAbsolutePath());
                    } else {
                        logger.error("Failed to clean up file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    public void getFailedSongsReport() {
        if (!failedSongs.isEmpty()) {
            logger.error("Failed songs: ");
            logger.error(this.failedSongs.toString());
            this.failedSongs.clear();
        }
    }
}
