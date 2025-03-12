package ryzen.ownitall.methods;

import java.io.InputStreamReader;
import java.io.BufferedReader;
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

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.Progressbar;

public class Download {
    private static final Logger logger = LogManager.getLogger(Download.class);
    private static final Settings settings = Settings.load();
    private static Collection collection = Collection.load();
    private ExecutorService executor;
    private File downloadFolder;
    private static final ArrayList<String> whiteList = new ArrayList<>(
            Arrays.asList("m3u", "png", "nfo", settings.getDownloadFormat()));
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.SEVERE);
    }

    /**
     * default download constructor
     * setting all settings / credentials
     * 
     * @throws InterruptedException - when user interrupts
     */
    public Download() throws InterruptedException {
        if (settings.getYoutubedlPath().isEmpty()) {
            settings.setYoutubedlPath();
        }
        if (settings.getFfmpegPath().isEmpty()) {
            settings.setFfmpegPath();
        }
        if (settings.getDownloadFolder().isEmpty()) {
            this.setDownloadPath();
        } else {
            this.downloadFolder = new File(settings.getDownloadFolder());
        }
        this.downloadFolder.mkdirs();
        System.out.println("This is where i reccomend you to connect to VPN / use proxies");
        System.out.print("Enter enter to continue: ");
        Input.request().getEnter();
    }

    /**
     * get the set download folder
     * 
     * @return - constructed File of download folder
     */
    public File getDownloadFolder() {
        return this.downloadFolder;
    }

    /**
     * prompt of where to save downloaded music
     */
    private void setDownloadPath() {
        try {
            System.out.print("Path to save music: ");
            this.downloadFolder = Input.request().getFile(false);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting download path");
        }
    }

    /**
     * threading for downloadSong
     * uses the threading option set in settings for how many threads
     * 
     * @param song - song to download
     * @param path - path of where to download
     * @throws InterruptedException - when user interrupts
     */
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
        this.executor = new ThreadPoolExecutor(
                settings.getDownloadThreads(),
                settings.getDownloadThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(settings.getDownloadThreads()));
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
            throw e;
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
            logger.debug("null song or Path provided in downloadSong");
            return;
        }
        ArrayList<String> command = new ArrayList<>();
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
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            int retries = 0;
            File songFile = new File(path, song.getFileName());
            StringBuilder completeLog = new StringBuilder();
            while (!songFile.exists() && retries < 3) {
                interruptionHandler.throwInterruption();
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

    /**
     * clean up local liked songs before downloading
     * 
     * @throws InterruptedException - when user interrupts
     */
    public void likedSongsCleanUp() throws InterruptedException {
        logger.debug("Getting local liked songs to remove mismatches");
        Upload upload = new Upload(this.downloadFolder);
        LikedSongs likedSongs = upload.getLikedSongs();
        File songFolder = this.downloadFolder;
        if (settings.isDownloadHierachy()) {
            songFolder = new File(this.downloadFolder, settings.getLikedSongsName());
        }
        if (likedSongs != null && !likedSongs.isEmpty()) {
            likedSongs.removeSongs(collection.getLikedSongs().getSongs());
            for (Song song : likedSongs.getSongs()) {
                if (!settings.isDownloadHierachy()) {
                    // skip if in a playlist
                    if (collection.getSongPlaylist(song) != null) {
                        continue;
                    }
                }
                File songFile = new File(songFolder, song.getFileName());
                if (songFile.exists()) {
                    if (songFolder.delete()) {
                        logger.debug("Deleted liked song '" + songFile.getAbsolutePath());
                    } else {
                        logger.error("Failed to delete liked song: " + songFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * orchestrator of DownloadSong for all standalone liked songs
     * 
     * @throws InterruptedException - when user interrupts
     */
    public void downloadLikedSongs() throws InterruptedException {
        ArrayList<Song> songs;
        File likedSongsFolder;
        if (settings.isDownloadHierachy()) {
            songs = collection.getLikedSongs().getSongs();
            likedSongsFolder = new File(this.downloadFolder, settings.getLikedSongsName());
            likedSongsFolder.mkdirs();
        } else {
            songs = collection.getStandaloneLikedSongs();
            likedSongsFolder = this.downloadFolder;
            if (settings.isDownloadLikedSongsPlaylist()) {
                Playlist likedSongsPlaylist = new Playlist(settings.getLikedSongsName());
                likedSongsPlaylist.addSongs(collection.getLikedSongs().getSongs());
                this.writePlaylistData(likedSongsPlaylist, this.downloadFolder);
            }
        }
        if (settings.isDownloadDelete()) {
            this.likedSongsCleanUp();
        }
        try (ProgressBar pb = Progressbar.progressBar("Downloading Liked songs", songs.size() + 1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (Song song : songs) {
                interruptionHandler.throwInterruption();
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
     * deletes entire playlists which are not in collection
     * 
     * @throws InterruptedException - when user interrupts
     */
    public void playlistsCleanUp() throws InterruptedException {
        logger.debug("Getting local playlists to remove mismatches");
        Upload upload = new Upload(this.getDownloadFolder());
        ArrayList<Playlist> playlists = upload.getPlaylists();
        if (playlists != null && !playlists.isEmpty()) {
            playlists.removeAll(collection.getPlaylists());
            for (Playlist playlist : playlists) {
                if (settings.isDownloadHierachy()) {
                    File playlistFolder = new File(this.downloadFolder, playlist.getFolderName());
                    if (playlistFolder.delete()) {
                        logger.debug("Deleted playlist '" + playlist.getName() + "' folder: "
                                + playlistFolder.getAbsolutePath());
                    } else {
                        logger.error("Could not delete playlist '" + playlist.getName() + "' folder:"
                                + playlistFolder.getAbsolutePath());
                    }
                } else {
                    // deletes all playlists songs
                    this.playlistCleanUp(new Playlist(playlist.getName()));
                    File m3uFile = new File(this.downloadFolder, playlist.getFolderName() + ".m3u");
                    if (m3uFile.delete()) {
                        logger.debug(
                                "Cleaned up playlist '" + playlist.getName() + "' m3u file: "
                                        + m3uFile.getAbsolutePath());
                    } else {
                        logger.error("Could not delete playlist '" + playlist.getName() + "' m3u file: "
                                + m3uFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * orchestrator of downloadPlaylist
     * 
     * @throws InterruptedException - when user interrupts
     */
    public void downloadPlaylists() throws InterruptedException {
        if (settings.isDownloadDelete()) {
            this.playlistsCleanUp();
        }
        ArrayList<Playlist> playlists = collection.getPlaylists();
        try (ProgressBar pb = Progressbar.progressBar("Playlist Downloads", playlists.size())) {
            for (Playlist playlist : playlists) {
                this.downloadPlaylist(playlist);
                pb.setExtraMessage(playlist.getName()).step();
            }
            pb.setExtraMessage("Done").step();
        }
    }

    /**
     * cleans up individual songs in a playlist
     * 
     * @param playlist - playlist to clean up
     * @throws InterruptedException - when user interrupts
     */
    public void playlistCleanUp(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in playlistSync");
            return;
        }
        logger.debug("Getting local playlist '" + playlist.getName() + "' to remove mismatches");
        File playlistFolder = this.downloadFolder;
        Playlist localPlaylist = null;
        if (settings.isDownloadHierachy()) {
            playlistFolder = new File(this.downloadFolder, playlist.getFolderName());
            localPlaylist = Upload.getPlaylist(playlistFolder);
        } else {
            File m3uFile = new File(this.downloadFolder, playlist.getFolderName() + ".m3u");
            if (m3uFile.exists()) {
                localPlaylist = Upload.getM3UPlaylist(m3uFile);
            }
        }
        if (localPlaylist != null && !localPlaylist.isEmpty()) {
            localPlaylist.removeSongs(playlist.getSongs());
            for (Song song : localPlaylist.getSongs()) {
                File songFile = new File(playlistFolder, song.getFileName());
                if (songFile.exists()) {
                    if (!settings.isDownloadHierachy()) {
                        if (collection.isLiked(song)) {
                            continue;
                        }
                    }
                    if (songFile.delete()) {
                        logger.debug("Deleted playlist '" + playlist.getName() + "' song: "
                                + songFile.getAbsolutePath());
                    } else {
                        logger.debug("could not delete playlist '" + playlist.getName() + "' song: "
                                + songFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * orchestrator for downloading a playlist
     * 
     * @param playlist - constructed playlist to download
     * @throws InterruptedException - when user interrupts
     */
    public void downloadPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in downloadPlaylist");
            return;
        }
        ArrayList<Song> songs;
        File playlistFolder;
        if (settings.isDownloadHierachy()) {
            songs = playlist.getSongs();
            playlistFolder = new File(this.downloadFolder, playlist.getFolderName());
            playlistFolder.mkdirs();
        } else {
            songs = collection.getStandalonePlaylistSongs(playlist);
            playlistFolder = this.downloadFolder;
            this.writePlaylistData(playlist, playlistFolder);
        }
        if (settings.isDownloadDelete()) {
            this.playlistCleanUp(playlist);
        }
        try (ProgressBar pb = Progressbar.progressBar("Downloading Playlists: " + playlist.getName(),
                playlist.size() + 1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (Song song : songs) {
                interruptionHandler.throwInterruption();
                pb.setExtraMessage(song.getName()).step();
                this.threadDownload(song, playlistFolder);
            }
            pb.setExtraMessage("cleaning up").step();
            this.threadShutdown();
            this.cleanFolder(playlistFolder);
            pb.setExtraMessage("Done");
        }
    }

    /**
     * deletes entire albums which are not in collection
     * 
     * @throws InterruptedException - when user interrupts
     */
    public void albumsCleanUp() throws InterruptedException {
        logger.debug("Getting local albums to remove mismatches");
        Upload upload = new Upload(this.getDownloadFolder());
        ArrayList<Album> albums = upload.getAlbums();
        if (albums != null && !albums.isEmpty()) {
            albums.removeAll(collection.getAlbums());
            for (Album album : albums) {
                File albumFolder = new File(this.downloadFolder, album.getFolderName());
                if (albumFolder.exists()) {
                    for (File songFile : albumFolder.listFiles()) {
                        if (songFile.delete()) {
                            logger.debug(
                                    "Deleted out of sync album '" + album.getName() + "' song: "
                                            + songFile.getAbsolutePath());
                        } else {
                            logger.error("could not delete out of sync album '" + album.getName() + "' song: "
                                    + songFile.getAbsolutePath());
                        }
                    }
                    if (albumFolder.delete()) {
                        logger.debug(
                                "Deleted album '" + album.getName() + "' folder: " + albumFolder.getAbsolutePath());
                    } else {
                        logger.error(
                                "Could not delete album '" + album.getName() + "' folder: "
                                        + albumFolder.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * orchestrator for downloadAlbum
     * 
     * @throws InterruptedException - when user interrupts
     */
    public void downloadAlbums() throws InterruptedException {
        if (settings.isDownloadDelete()) {
            this.albumsCleanUp();
        }
        ArrayList<Album> albums = collection.getAlbums();
        try (ProgressBar pb = Progressbar.progressBar("Album Downloads", albums.size())) {
            for (Album album : albums) {
                this.downloadAlbum(album);
                pb.setExtraMessage(album.getName()).step();
            }
            pb.setExtraMessage("Done").step();
        }
    }

    /**
     * cleans up individual album songs for odd ones
     * 
     * @param album - album to clean up
     * @throws InterruptedException - when user interrupts
     */
    // TODO: tmp disable library to ensure it deletes non wanted?
    public void albumCleanUp(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided in albumSync");
            return;
        }
        logger.debug("Getting local album '" + album.getName() + "' to remove mismatches");
        File albumFolder = new File(this.downloadFolder, album.getFolderName());
        Album localAlbum = Upload.getAlbum(albumFolder);
        if (localAlbum != null && !album.isEmpty()) {
            localAlbum.removeSongs(album.getSongs());
            for (Song song : localAlbum.getSongs()) {
                File songFile = new File(albumFolder, song.getFileName());
                if (songFile.exists()) {
                    if (songFile.delete()) {
                        logger.debug("Deleted album '" + album.getName() + "' song: " + songFile.getAbsolutePath());
                    } else {
                        logger.error(
                                "could not delete album '" + album.getName() + "' song: " + songFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * download an album
     * has its own folder
     * 
     * @param album - album to download
     * @throws InterruptedException - when the user interrupts
     */
    public void downloadAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided in downloadAlbum");
            return;
        }
        // albums are always in a folder
        File albumFolder = new File(this.downloadFolder, album.getFolderName());
        albumFolder.mkdirs();
        if (settings.isDownloadDelete()) {
            this.albumCleanUp(album);
        }
        try (ProgressBar pb = Progressbar.progressBar("Download Album: " + album.getName(), album.size() + 1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            this.writeAlbumData(album, albumFolder);
            for (Song song : album.getSongs()) {
                interruptionHandler.throwInterruption();
                pb.setExtraMessage(song.getName()).step();
                this.threadDownload(song, albumFolder);
            }
            pb.setExtraMessage("cleaning up").step();
            this.threadShutdown();
            this.cleanFolder(albumFolder);
            pb.setExtraMessage("Done");
        }
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
            MusicTools.writeData(m3uFile, collection.getPlaylistM3U(playlist));
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
            MusicTools.writeData(nfoFile, collection.getAlbumNFO(album));
        } catch (Exception e) {
            logger.error("Exception writing album '" + album.toString() + "' nfo: " + e);
        }
        try {
            if (album.getCoverImage() != null) {
                MusicTools.downloadImage(album.getCoverImage(),
                        new File(folder, album.getFolderName() + ".png"));
            }
        } catch (IOException e) {
            logger.error("Exception writing album '" + album.toString() + "' coverimage: " + e);
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
