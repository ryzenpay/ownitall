package ryzen.ownitall.methods;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.tag.FieldKey;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.classes.Artist;
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
        }
    }

    public void threadDownload(Song song, File path) throws InterruptedException {
        if (song == null || path == null) {
            logger.debug("Empty song or path provided in threadDownload");
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

    public void threadInit() {
        this.executor = new ThreadPoolExecutor(
                settings.getDownloadThreads(),
                settings.getDownloadThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(settings.getDownloadThreads()));
    }

    public void threadShutdown() throws InterruptedException {
        if (this.executor == null || this.executor.isShutdown()) {
            return;
        }
        executor.shutdown();
        logger.debug("Awaiting current threads to shutdown (max 10 min)");
        executor.awaitTermination(10, TimeUnit.MINUTES);
        logger.debug("All threads shut down");

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
        // TODO: max video length?
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
        // command.add("--embed-thumbnail");
        command.add("--format");
        command.add("bestaudio/best");
        command.add("--audio-format");
        command.add(settings.getDownloadFormat());
        command.add("--audio-quality");
        command.add(String.valueOf(settings.getDownloadQuality()));
        // command.add("--embed-metadata"); // metadata we have overwrites this
        // command.add("--no-write-comments");
        // download location
        command.add("--paths");
        command.add(path.getAbsolutePath());
        command.add("--output");
        command.add(song.getFileName());
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
            File songFile = new File(path, song.getFileName());
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
                        logger.debug("Download cancelled due to boundary criteria: '" + searchQuery + "'");
                        break;
                    } else {
                        logger.error("Unkown error while downloading song: '" + song + "' with code: " + exitCode);
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
                logger.warn("song '" + song.toString() + "' failed to download, check logs");
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Exception preparing yt-dlp: ", e);
        }
    }

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
        Artist artist = song.getArtist();
        if (artist != null) {
            id3Data.put(FieldKey.ARTIST, artist.getName());
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
            MusicTools.writeMetaData(id3Data, collection.isLiked(song), song.getCoverImage(), songFile);
        } catch (Exception e) {
            logger.error("writing song metadata for '" + song.toString() + "': " + e);
        }
    }

    /**
     * orchestrator of DownloadSong for all standalone liked songs
     * 
     */
    public void downloadLikedSongs() throws InterruptedException {
        LinkedHashSet<Song> songs;
        File likedSongsFolder;
        if (settings.isDownloadHierachy()) {
            songs = collection.getLikedSongs().getSongs();
            likedSongsFolder = new File(this.downloadPath, settings.getLikedSongsName());
            likedSongsFolder.mkdirs();
        } else {
            songs = collection.getStandaloneLikedSongs();
            likedSongsFolder = new File(this.downloadPath);
        }
        if (settings.isDownloadDelete()) {
            logger.debug("Getting local liked songs collection version to remove mismatches");
            LinkedHashSet<Song> localSongs = Upload.getLikedSongs(likedSongsFolder);
            localSongs.removeAll(songs);
            for (Song song : localSongs) {
                File songFile = new File(likedSongsFolder, song.getFileName());
                if (songFile.delete()) {
                    logger.debug("Deleted liked song '" + songFile.getAbsolutePath());
                } else {
                    logger.error("Failed to delete liked song: " + songFile.getAbsolutePath());
                }
            }
        }
        try (ProgressBar pb = Progressbar.progressBar("Downloading Liked songs", songs.size() + 1)) {
            for (Song song : songs) {
                pb.setExtraMessage(song.getName()).step();
                this.threadDownload(song, likedSongsFolder);
            }
            pb.setExtraMessage("cleaning up").step();
            this.threadShutdown();
            this.cleanFolder(likedSongsFolder);
            pb.setExtraMessage("Done");
        }
    }

    /**
     * orchestrator of downloadPlaylist
     */
    public void downloadPlaylists() throws InterruptedException {
        LinkedHashSet<Playlist> playlists = collection.getPlaylists();
        try (ProgressBar pb = Progressbar.progressBar("Playlist Downloads", playlists.size())) {
            for (Playlist playlist : playlists) {
                this.downloadPlaylist(playlist);
                pb.setExtraMessage(playlist.getName()).step();
            }
            pb.setExtraMessage("Done").step();
        }
    }

    /**
     * orchestrator for downloading a playlist
     * 
     * @param playlist - constructed playlist to download
     */
    public void downloadPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("Empty playlist provided in downloadPlaylist");
            return;
        }
        LinkedHashSet<Song> songs;
        File playlistFolder;
        if (settings.isDownloadHierachy()) {
            songs = playlist.getSongs();
            playlistFolder = new File(this.downloadPath, playlist.getFolderName());
            playlistFolder.mkdirs();
        } else {
            songs = collection.getStandalonePlaylistSongs(playlist);
            playlistFolder = new File(this.downloadPath);
        }
        if (settings.isDownloadDelete()) {
            logger.debug("Getting local playlist collection version to remove mismatches");
            Playlist localPlaylist = null;
            if (settings.isDownloadHierachy()) {
                localPlaylist = Upload.getPlaylist(playlistFolder);
            } else {
                File m3uFile = new File(this.downloadPath, playlist.getFolderName() + ".m3u");
                if (m3uFile.exists()) {
                    localPlaylist = Upload.getM3UPlaylist(m3uFile);
                }
            }
            if (localPlaylist != null) {
                LinkedHashSet<Song> localSongs = localPlaylist.getSongs();
                localSongs.removeAll(songs);
                for (Song song : localSongs) {
                    File songFile = new File(playlistFolder, song.getFileName());
                    if (songFile.delete()) {
                        logger.debug(
                                "Deleted playlist '" + playlist.getName() + "' song '" + songFile.getAbsolutePath());
                    } else {
                        logger.error("Failed to delete playlist '" + playlist.getName() + "' song: "
                                + songFile.getAbsolutePath());
                    }
                }
            }
        }
        try (ProgressBar pb = Progressbar.progressBar("Downloading Playlists: " + playlist.getName(),
                playlist.size() + 1)) {
            this.writePlaylistData(playlist, playlistFolder);
            for (Song song : songs) {
                pb.setExtraMessage(song.getName()).step();
                this.threadDownload(song, playlistFolder);
            }
            pb.setExtraMessage("cleaning up").step();
            this.threadShutdown();
            this.cleanFolder(playlistFolder);
            pb.setExtraMessage("Done");
        }
    }

    public void downloadAlbums() throws InterruptedException {
        LinkedHashSet<Album> albums = collection.getAlbums();
        try (ProgressBar pb = Progressbar.progressBar("Album Downloads", albums.size())) {
            for (Album album : albums) {
                this.downloadAlbum(album);
                pb.setExtraMessage(album.getName()).step();
            }
            pb.setExtraMessage("Done").step();
        }
    }

    public void downloadAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("Empty album provided in downloadAlbum");
            return;
        }
        // albums are always in a folder
        File albumFolder = new File(this.downloadPath, album.getFolderName());
        if (!settings.isDownloadHierachy()) {
            for (Song song : album.getSongs()) {
                File songFile = new File(this.downloadPath, song.getFileName());
                if (songFile.exists()) {
                    if (songFile.delete()) {
                        logger.debug(
                                "Deleted non-album '" + album.getFolderName() + "' song: "
                                        + songFile.getAbsolutePath());
                    } else {
                        logger.error(
                                "Failed to delete non-album '" + album.getName() + "' song: "
                                        + songFile.getAbsolutePath());
                    }
                }
            }
        }
        if (settings.isDownloadDelete() && albumFolder.exists()) {
            logger.debug("Getting local album collection version to remove mismatches");
            Album localAlbum = Upload.getAlbum(new File(this.downloadPath, album.getFolderName()));
            if (localAlbum != null) {
                LinkedHashSet<Song> localSongs = localAlbum.getSongs();
                localSongs.removeAll(album.getSongs());
                for (Song song : localSongs) {
                    File songFile = new File(albumFolder, song.getFileName());
                    if (songFile.delete()) {

                        logger.debug("Deleted album '" + album.getName() + "' song '" + songFile.getAbsolutePath()
                                + "' as it was not in collection and downloaddelete is enabled");
                    } else {
                        logger.error(
                                "Failed to delete album '" + album.getName() + "' song: " + songFile.getAbsolutePath());
                    }
                }
            }
        }
        try (ProgressBar pb = Progressbar.progressBar("Download Album: " + album.getName(), album.size() + 1)) {
            albumFolder.mkdirs();
            this.writeAlbumData(album, albumFolder);
            for (Song song : album.getSongs()) {
                pb.setExtraMessage(song.getName()).step();
                this.threadDownload(song, albumFolder);
            }
            pb.setExtraMessage("cleaning up").step();
            this.threadShutdown();
            this.cleanFolder(albumFolder);
            pb.setExtraMessage("Done");
        }
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
            logger.error("Exception writing playlist '" + playlist.toString() + "' m3u: " + e);
        }
        try {
            if (playlist.getCoverImage() != null) {
                MusicTools.downloadImage(playlist.getCoverImage(),
                        new File(folder, playlist.getFolderName() + ".png"));
            }
        } catch (IOException e) {
            logger.error("Exception writing playlist '" + playlist.toString() + "' coverimage: " + e);
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
            MusicTools.writeData("album", "nfo", collection.getAlbumNFO(album), folder);
        } catch (Exception e) {
            logger.error("Exception writing album '" + album.toString() + "' nfo: " + e);
        }
        try {
            if (album.getCoverImage() != null) {
                MusicTools.downloadImage(album.getCoverImage(),
                        new File(folder, "cover.png"));
            }
        } catch (IOException e) {
            logger.error("Exception writing album '" + album.toString() + "' coverimage: " + e);
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
                        logger.debug("Cleaned up file: '" + file.getAbsolutePath() + "'");
                    } else {
                        logger.error("Failed to clean up file: '" + file.getAbsolutePath() + "'");
                    }
                }
            }
        }
    }
}
