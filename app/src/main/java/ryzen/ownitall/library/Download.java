package ryzen.ownitall.library;

import java.io.InputStreamReader;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import ryzen.ownitall.util.MusicTools;

public class Download {
    private static final Logger logger = LogManager.getLogger(Download.class);
    private static Settings settings = Settings.load();
    private String downloadPath;

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
        this.setDownloadPath();
        logger.info("This is where i reccomend you to connect to VPN / use proxies");
        System.out.print("Enter y to continue: ");
        Input.request().getAgreement();
    }

    /**
     * prompt of where to save downloaded music
     */
    private void setDownloadPath() {
        System.out.print("Please provide path to save music: ");
        this.downloadPath = Input.request().getFile(false).getAbsolutePath();
    }

    /**
     * download a specified song
     * TODO: musicbee playlist / album / liked songs generation (M3U)
     * 
     * @param song - constructed song
     * @param path - folder of where to place
     */
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
                // TODO: clean up youtube dl files (song.getFileName() with different
                // extensions)
                return;
            }
        } catch (Exception e) {
            logger.error("Error handling youtubeDL: " + e);
            return;
        }
    }

    /**
     * orchestrator of DownloadSong for all liked songs
     * 
     * @param likedSongs - constructed liked songs
     */
    public void downloadLikedSongs(LikedSongs likedSongs) {
        File likedSongsFolder = new File(this.downloadPath, settings.getLikedSongName());
        ProgressBar pb = Main.progressBar("Downloading Liked songs", likedSongs.size());
        likedSongsFolder.mkdirs();
        for (Song song : likedSongs.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            this.downloadSong(song, likedSongsFolder);
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
        ProgressBar pbPlaylist = Main.progressBar("Playlist Downloads", playlists.size());
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
        File playlistFolder = new File(this.downloadPath, playlist.getFileName());
        ProgressBar pb = Main.progressBar("Downloading Playlists: " + playlist.getName(), playlist.size());
        playlistFolder.mkdirs();
        for (Song song : playlist.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            this.downloadSong(song, playlistFolder);
        }
        this.cleanFolder(playlistFolder);
        pb.setExtraMessage("Done");
        pb.close();
    }

    public void downloadAlbums(LinkedHashSet<Album> albums) {
        ProgressBar pbAlbum = Main.progressBar("Album Downloads", albums.size());
        for (Album album : albums) {
            pbAlbum.setExtraMessage(album.getName());
            this.downloadAlbum(album);
            pbAlbum.step();
        }
        pbAlbum.setExtraMessage("Done").step();
        pbAlbum.close();
    }

    public void downloadAlbum(Album album) {
        ProgressBar pb = Main.progressBar("Download Album: " + album.getName(), album.size());
        File albumFolder = new File(this.downloadPath, album.getFileName());
        albumFolder.mkdirs();
        for (Song song : album.getSongs()) {
            pb.setExtraMessage(song.getName()).step();
            this.downloadSong(song, albumFolder);
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
            if (file.isFile()) {
                if (!MusicTools.getExtension(file).equals(settings.getDownloadFormat())) {
                    if (file.delete()) {
                        logger.info("Cleaned up file: " + file.getAbsolutePath());
                    } else {
                        logger.error("Failed to clean up file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }
}
