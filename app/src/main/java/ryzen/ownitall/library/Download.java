package ryzen.ownitall.library;

import java.io.InputStreamReader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.Progressbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Download {
    private static final Logger logger = LogManager.getLogger(Download.class);
    private static Settings settings = Settings.load();
    private final ExecutorService executorService;
    private final Semaphore semaphore;
    private String downloadPath;
    private LinkedHashMap<File, Song> failedSongs;

    /**
     * default download constructor
     * setting all settings / credentials
     */
    public Download() {
        this.failedSongs = new LinkedHashMap<>();
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
        logger.info("This is where i reccomend you to connect to VPN / use proxies");
        System.out.print("Enter y to continue: ");
        Input.request().getAgreement();
        this.executorService = Executors.newFixedThreadPool(settings.getDownloadThreads());
        this.semaphore = new Semaphore(settings.getDownloadThreads());
    }

    /**
     * prompt of where to save downloaded music
     */
    private void setDownloadPath() {
        System.out.print("Please provide path to save music: ");
        this.downloadPath = Input.request().getFile(false).getAbsolutePath();
    }

    public void threadDownload(Song song, File path) {
        // Attempt to acquire a permit from the semaphore
        try {
            semaphore.acquire(); // Block until a permit is available
            this.downloadSong(song, path); // Perform the download
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            logger.error("Thread was interrupted while waiting to acquire semaphore.", e);
        } finally {
            semaphore.release(); // Ensure permit is released after download completes
        }
    }

    /**
     * download a specified song
     * 
     * @param song - constructed song
     * @param path - folder of where to place
     */
    public void downloadSong(Song song, File path) {
        File songFile = new File(path, song.getFileName() + "." + settings.getDownloadFormat());
        if (songFile.exists()) { // dont download twice
            return;
        }
        File likedSongFile;
        if (settings.isDownloadHierachy()) {
            File likedSongFolder = new File(this.downloadPath, settings.getLikedSongFile());
            likedSongFile = new File(likedSongFolder, song.getFileName() + "." + settings.getDownloadFormat());
        } else {
            likedSongFile = new File(this.downloadPath, song.getFileName() + "." + settings.getDownloadFormat());
        }
        if (likedSongFile.exists()) { // to prevent overwriting from its own folder
            try {
                Files.copy(likedSongFile.toPath(), songFile.toPath());
                logger.debug("Already found liked song downloaded: " + likedSongFile.getAbsolutePath());
                return;
            } catch (IOException e) {
                logger.error("Error moving found music file: " + likedSongFile.getAbsolutePath() + " to: "
                        + songFile.getAbsolutePath() + " error: " + e);
            }
        }
        String searchQuery = song.toString() + " #official #audio"; // youtube search criteria
        List<String> command = new ArrayList<>();
        // executables
        command.add(settings.getYoutubedlPath());
        command.add("--ffmpeg-location");
        command.add(settings.getFfmpegPath());
        command.add("--concurrent-fragments");
        command.add(String.valueOf(settings.getDownloadThreads()));
        // command.add("--quiet");
        // search for video using the query
        command.add("\"ytsearch1:" + searchQuery + "\""); // TODO: cookies for age restriction
        // exclude any found playlists or shorts
        command.add("--no-playlist"); // Prevent downloading playlists
        command.add("--break-match-filter");
        command.add("duration>=45"); // exclude shorts
        // metadata and formatting
        command.add("--extract-audio");
        command.add("--embed-thumbnail");
        command.add("--audio-format");
        command.add(settings.getDownloadFormat());
        command.add("--audio-quality");
        command.add(String.valueOf(settings.getDownloadQuality()));
        command.add("--embed-metadata");
        // download location
        command.add("--paths");
        command.add(path.getAbsolutePath());
        command.add("--output");
        command.add(song.getFileName() + ".%(ext)s");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            Process process = processBuilder.start();

            // Capture output for logging
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String completeLog = "";
            while ((line = reader.readLine()) != null) {
                completeLog += line + "\n";
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Error downloading song " + song.toString() + " with error: " + exitCode);
                logger.debug(completeLog); // Log last line of output
                this.failedSongs.put(songFile, song);
            } else if (!songFile.exists()) {
                logger.error("Everything passed but the file " + songFile.getAbsolutePath() + " is still missing");
                logger.debug("Complete log: " + completeLog);
                this.failedSongs.put(songFile, song);
            }
        } catch (Exception e) {
            logger.error("Error handling youtubeDL: " + e);
            return;
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * orchestrator of DownloadSong for all liked songs
     * 
     * @param likedSongs - constructed liked songs
     */
    public void downloadLikedSongs(LikedSongs likedSongs) {
        File likedSongsFolder = new File(this.downloadPath);
        if (settings.isDownloadHierachy()) {
            likedSongsFolder = new File(this.downloadPath, settings.getLikedSongName());
            likedSongsFolder.mkdirs();
        }
        ProgressBar pb = Progressbar.progressBar("Downloading Liked songs", likedSongs.size());
        try {
            MusicTools.writeM3U(likedSongs.getFileName(), likedSongs.getM3U(), likedSongsFolder);
        } catch (Exception e) {
            logger.error(
                    "Error writing Liked Songs (" + likedSongsFolder.getAbsolutePath() + ") m3u +/ coverimage: " + e);
        }
        for (Song song : likedSongs.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            this.threadDownload(song, likedSongsFolder);
            // this.downloadSong(song, likedSongsFolder);
        }
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error("Threads were interrupted while awaiting termination" + e);
        }
        this.cleanFolder(likedSongsFolder);
        pb.setExtraMessage("Done");
        pb.close();
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
        pbPlaylist.setExtraMessage("Done").step();
        pbPlaylist.close();
    }

    /**
     * orchestrator for downloading a playlist
     * 
     * @param playlist - constructed playlist to download
     */
    public void downloadPlaylist(Playlist playlist) {
        File playlistFolder = new File(this.downloadPath);
        if (settings.isDownloadHierachy()) {
            playlistFolder = new File(this.downloadPath, playlist.getFileName());
            playlistFolder.mkdirs();
        }
        ProgressBar pb = Progressbar.progressBar("Downloading Playlists: " + playlist.getName(), playlist.size());
        try {
            MusicTools.writeM3U(playlist.getFileName(), playlist.getM3U(), playlistFolder);
        } catch (Exception e) {
            logger.error("Error writing playlist (" + playlistFolder.getAbsolutePath() + ") m3u +/ coverimage: " + e);
        }
        for (Song song : playlist.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            this.threadDownload(song, playlistFolder);
            // this.downloadSong(song, playlistFolder);
        }
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error("Threads were interrupted while awaiting termination" + e);
        }
        this.cleanFolder(playlistFolder);
        pb.setExtraMessage("Done");
        pb.close();
    }

    public void downloadAlbums(LinkedHashSet<Album> albums) {
        ProgressBar pbAlbum = Progressbar.progressBar("Album Downloads", albums.size());
        for (Album album : albums) {
            pbAlbum.setExtraMessage(album.getName());
            this.downloadAlbum(album);
            pbAlbum.step();
        }
        pbAlbum.setExtraMessage("Done").step();
        pbAlbum.close();
    }

    public void downloadAlbum(Album album) {
        ProgressBar pb = Progressbar.progressBar("Download Album: " + album.getName(), album.size());
        File albumFolder = new File(this.downloadPath, album.getFileName());
        albumFolder.mkdirs();
        try {
            MusicTools.writeM3U(album.getFileName(), album.getM3U(), albumFolder);
            if (album.getCoverImage() != null) {
                MusicTools.downloadImage(album.getCoverImage(), albumFolder);
            }
        } catch (FileAlreadyExistsException e) {
        } catch (Exception e) {
            logger.error("Error writing album (" + albumFolder.getAbsolutePath() + ") m3u +/ coverimage: " + e);
        }
        for (Song song : album.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            this.threadDownload(song, albumFolder);
            // this.downloadSong(song, albumFolder);
        }
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error("Threads were interrupted while awaiting termination" + e);
        }
        this.cleanFolder(albumFolder);
        pb.setExtraMessage("Done");
        pb.close();
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
                        logger.info("Cleaned up file: " + file.getAbsolutePath());
                    } else {
                        logger.error("Failed to clean up file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    public LinkedHashMap<File, Song> getFailedSongs() {
        return this.failedSongs;
    }
}
