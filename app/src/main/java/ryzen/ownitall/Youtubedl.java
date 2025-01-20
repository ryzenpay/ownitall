package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.tools.Input;

public class Youtubedl {
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
    }
    private static final Logger logger = LogManager.getLogger(Youtubedl.class);
    private static Settings settings = Settings.load();

    public static Youtubedl instance;

    public Youtubedl() {
        if (settings.youtubedlPath.isEmpty()) {
            setYoutubedlPath();
        }
        if (settings.downloadPath.isEmpty()) {
            setDownloadPath();
        }
        logger.info("This is where i reccomend you to connect to VPN / use proxies");
        System.out.print("Enter y to continue: ");
        Input.request().getAgreement();
    }

    public static Youtubedl load() {
        if (instance == null) {
            instance = new Youtubedl();
        }
        return instance;
    }

    public static void setYoutubedlPath() {
        logger.info("A guide to obtaining the following variables is in the readme");
        System.out.print("Please provide local Youtube DL executable path: ");
        settings.setYoutubedlPath(Input.request().getFile().getAbsolutePath());
    }

    public static void setDownloadPath() {
        System.out.print("Please provide path to save music: ");
        settings.setDownloadPath(Input.request().getFile().getAbsolutePath());
    }

    public void downloadSong(Song song, File path) {
        String searchQuery = "";
        String downloadPath = path.getAbsolutePath() + "/" + song.getName();
        if (song.getArtist() != null) {
            searchQuery = song.getArtist().toString() + " - ";
        }
        searchQuery += song.getName();
        List<String> command = new ArrayList<>();
        command.add(settings.youtubedlPath);
        command.add("ytsearch1:" + searchQuery); // Limit to 1 result
        command.add("-x"); // Extract audio
        command.add("--audio-format");
        command.add(settings.downloadFormat);
        command.add("--add-metadata");
        command.add("-o");
        command.add(downloadPath);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("[download]") && line.contains("%")) {
                    logger.info("Downloading: " + line.trim());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Error downloading song " + song.toString() + " with error: " + exitCode);
                return;
            }

            logger.info("Download completed for: " + song.toString());
        } catch (Exception e) {
            logger.error("Error handling youtubeDL: " + e);
            return;
        }
    }

    public void downloadLikedSongs(LikedSongs likedSongs) {
        File likedSongsFolder = new File(settings.downloadPath, settings.likedSongFile);
        likedSongsFolder.mkdirs();
        for (Song song : likedSongs.getSongs()) {
            this.downloadSong(song, likedSongsFolder);
        }
    }

    public void downloadPlaylist(Playlist playlist) {
        File playlistFolder = new File(settings.downloadPath, playlist.getName());
        playlistFolder.mkdirs();
        for (Song song : playlist.getSongs()) {
            this.downloadSong(song, playlistFolder);
        }
    }

    public void downloadAlbum(Album album) {
        File albumFolder = new File(settings.downloadPath, album.getName());
        albumFolder.mkdirs();
        for (Song song : album.getSongs()) {
            this.downloadSong(song, albumFolder);
        }
    }
}
