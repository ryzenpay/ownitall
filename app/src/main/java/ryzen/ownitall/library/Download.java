package ryzen.ownitall.library;

import java.io.InputStreamReader;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Main;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;

public class Download {
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
    }
    private static final Logger logger = LogManager.getLogger(Download.class);
    private static Settings settings = Settings.load();
    private String downloadPath;

    public Download() {
        if (settings.getYoutubedlPath().isEmpty()) {
            settings.setYoutubedlPath();
        }
        if (settings.getFfmpegPath().isEmpty()) {
            settings.setFfmpegPath();
        }
        this.setDownloadPath();
        logger.info("This is where i reccomend you to connect to VPN / use proxies");
        System.out.print("Enter y to continue: ");
        Input.request().getAgreement();
    }

    private void setDownloadPath() {
        System.out.print("Please provide path to save music: ");
        this.downloadPath = Input.request().getFile(false).getAbsolutePath();
    }

    // TODO: musicbee playlist / album / liked songs generation
    public void downloadSong(Song song, File path) {
        File songFile = new File(path, song.getFileName() + "." + settings.getDownloadFormat());
        if (songFile.exists()) { // dont download twice
            logger.info("Already found downloaded file: " + songFile.getAbsolutePath());
            return;
        }
        File likedSongsFolder = new File(this.downloadPath, settings.getLikedSongName());
        File likedSongFile = new File(likedSongsFolder, song.getFileName() + "." + settings.getDownloadFormat());
        if (likedSongFile.exists()) {
            try {
                Files.copy(likedSongFile.toPath(), songFile.toPath());
                logger.info("Already found liked song downloaded: " + likedSongFile);
                return;
            } catch (IOException e) {
                logger.error("Error moving found music file: " + likedSongFile.getAbsolutePath() + " to: "
                        + songFile.getAbsolutePath() + " error: " + e);
            }
        }
        List<String> command = new ArrayList<>();
        // executables
        command.add(settings.getYoutubedlPath());
        command.add("--ffmpeg-location");
        command.add(settings.getFfmpegPath());
        command.add("--quiet");
        // search for video using the query
        command.add("ytsearch1:" + song.toString()); // TODO: cookies for age restriction
        // exclude any found playlists
        command.add("--no-playlist");
        command.add("--break-match-filters");
        command.add("playlist");
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
            String lastLine = "";
            while ((line = reader.readLine()) != null) {
                lastLine = line;
                logger.debug(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Error downloading song " + song.toString() + " with error: " + exitCode);
                logger.error("Last output from youtube-dl: " + lastLine); // Log last line of output
                return;
            }
        } catch (Exception e) {
            logger.error("Error handling youtubeDL: " + e);
            return;
        }
    }

    public void downloadLikedSongs(LikedSongs likedSongs) {
        File likedSongsFolder = new File(this.downloadPath, settings.getLikedSongName());
        ProgressBar pb = Main.progressBar("Downloading Liked songs", likedSongs.size());
        likedSongsFolder.mkdirs();
        for (Song song : likedSongs.getSongs()) {
            pb.setExtraMessage(song.getName());
            this.downloadSong(song, likedSongsFolder);
            pb.step();
        }
        pb.setExtraMessage("Done").step();
        pb.close();
    }

    public void downloadPlaylist(Playlist playlist) {
        File playlistFolder = new File(this.downloadPath, playlist.getFileName());
        ProgressBar pb = Main.progressBar("Downloading Playlists: " + playlist.getName(), playlist.size());
        playlistFolder.mkdirs();
        for (Song song : playlist.getSongs()) {
            pb.setExtraMessage(song.getName());
            this.downloadSong(song, playlistFolder);
            pb.step();
        }
        pb.setExtraMessage("Done").step();
        pb.close();
    }

    public void downloadAlbum(Album album) {
        ProgressBar pb = Main.progressBar("Download Album: " + album.getName(), album.size());
        File albumFolder = new File(this.downloadPath, album.getFileName());
        albumFolder.mkdirs();
        for (Song song : album.getSongs()) {
            pb.setExtraMessage(song.getName());
            this.downloadSong(song, albumFolder);
            pb.step();
        }
        pb.setExtraMessage("Done").step();
        pb.close();
    }
}
