package ryzen.ownitall;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.tools.Input;

public class Youtubedl {
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
    }
    private static final Logger logger = LogManager.getLogger(Youtubedl.class);
    private static Settings settings = Settings.load();

    public static Youtubedl instance;

    public Youtubedl() {
        settings.setYoutubedlPath();
        settings.setFfmpegPath();
        settings.setDownloadPath();
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

    public void downloadSong(Song song, File path) {
        String baseFileName = path.getAbsolutePath() + "/" + song.getName();
        String searchQuery = song.getName();
        if (song.getArtist() != null) {
            searchQuery = song.getArtist().toString() + " - " + searchQuery;
        }
        List<String> command = new ArrayList<>();
        command.add(settings.youtubedlPath);
        command.add("--ffmpeg-location");
        command.add(settings.ffmpegPath);
        command.add("ytsearch1:" + searchQuery); // Limit to 1 result
        command.add("--no-playlist");
        command.add("--extract-audio");
        command.add("--audio-format");
        command.add(settings.downloadFormat);
        command.add("--audio-quality");
        command.add(String.valueOf(settings.downloadQuality));
        command.add("--embed-metadata");
        command.add("--output");
        command.add(baseFileName + ".%(ext)s");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            Process process = processBuilder.start();

            // Capture output for logging
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String lastLine = "";
            while ((line = reader.readLine()) != null) {
                lastLine = line; // Store the last line for logging
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
        File likedSongsFolder = new File(settings.downloadPath, settings.likedSongFile);
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
        File playlistFolder = new File(settings.downloadPath, playlist.getName());
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
        String albumFileName = album.getMainArtist() + " - " + album.getName();
        ProgressBar pb = Main.progressBar("Download Album: " + album.getName(), album.size());
        File albumFolder = new File(settings.downloadPath, albumFileName);
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
