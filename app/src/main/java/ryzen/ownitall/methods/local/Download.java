package ryzen.ownitall.methods.local;

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
    // TODO: multiple download sources
    // qobuz
    // deezer
    // youtube (already implemented)
    // tidal
    private static final Logger logger = LogManager.getLogger();
    private static final Settings settings = Settings.load();
    private static Collection collection = Collection.load();
    private ExecutorService executor;
    private static final ArrayList<String> whiteList = new ArrayList<>(
            Arrays.asList("m3u", "png", "nfo", settings.getString("downloadformat")));
    private File localLibrary;

    /**
     * default download constructor
     * setting all settings / credentials
     * 
     * @throws InterruptedException - when user interrupts
     */
    public Download(File localLibrary) throws InterruptedException {
        if (settings.isEmpty("youtubedlpath")) {
            this.setYoutubedlPath();
        }
        if (settings.isEmpty("ffmpegpath")) {
            this.setFfmpegPath();
        }
        this.localLibrary = localLibrary;
    }

    private void setYoutubedlPath() throws InterruptedException {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Local Youtube DL executable path: ");
            settings.change("youtubedlpath", Input.request().getFile(true).getAbsolutePath());
        } catch (InterruptedException e) {
            logger.debug("Interrutped while setting youtubedl path");
            throw e;
        }
    }

    private void setFfmpegPath() throws InterruptedException {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Local FFMPEG executable path: ");
            settings.change("ffmpegpath", Input.request().getFile(true).getAbsolutePath());
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting FFMPEG executable path");
            throw e;
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
        int downloadThreads = settings.getInt("downloadthreads");
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
        command.add(settings.getFile("youtubedlpath").toString());
        command.add("--ffmpeg-location");
        command.add(settings.getFile("ffmpegpath").toString());
        // command.add("--concurrent-fragments");
        // command.add(String.valueOf(settings.getDownloadThreads()));
        // set up youtube searching and only 1 result
        command.add("--default-search");
        command.add("ytsearch1");
        // exclude any found playlists or shorts
        command.add("--no-playlist"); // Prevent downloading playlists
        command.add("--break-match-filter");
        command.add("duration>=45"); // exclude shorts
        // metadata and formatting
        command.add("--extract-audio");
        // command.add("--embed-thumbnail");
        command.add("--format");
        command.add("bestaudio/best");
        command.add("--audio-format");
        command.add(settings.getString("downloadformat"));
        command.add("--audio-quality");
        command.add(settings.getInt("downloadquality").toString());
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
                if (retries == 1) {
                    // cookies for age restriction (do not default to them)
                    if (!settings.isEmpty("downloadcookiesfile")) {
                        command.add(1, "--cookies");
                        command.add(2, settings.getFile("downloadcookiesfile").toString());
                    } else if (!settings.isEmpty("downloadcookiesbrowser")) {
                        command.add(1, "--cookies-from-browser");
                        command.add(2, settings.getString("downloadcookiesbrowser"));
                    }
                }
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
    public void syncLikedSongs() throws InterruptedException {
        logger.debug("Getting local liked songs to remove mismatches");
        Upload upload = new Upload(this.localLibrary);
        LikedSongs likedSongs = upload.getLikedSongs();
        File songFolder = this.localLibrary;
        if (settings.getBool("downloadhierachy")) {
            songFolder = new File(this.localLibrary, settings.getString("likedsongsname"));
        }
        if (likedSongs != null && !likedSongs.isEmpty()) {
            likedSongs.removeSongs(collection.getLikedSongs().getSongs());
            for (Song song : likedSongs.getSongs()) {
                if (!settings.getBool("downloadhierachy")) {
                    // skip if in a playlist
                    if (collection.getSongPlaylist(song) != null) {
                        continue;
                    }
                }
                File songFile = new File(songFolder, song.getFileName());
                if (songFile.exists()) {
                    if (songFolder.delete()) {
                        logger.info("Deleted liked song '" + songFile.getAbsolutePath());
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
        if (settings.getBool("downloadhierachy")) {
            songs = collection.getLikedSongs().getSongs();
            likedSongsFolder = new File(this.localLibrary, settings.getString("likedsongsname"));
            likedSongsFolder.mkdirs();
        } else {
            songs = collection.getStandaloneLikedSongs();
            likedSongsFolder = this.localLibrary;
            if (settings.getBool("downloadlikedsongsplaylist")) {
                Playlist likedSongsPlaylist = new Playlist(settings.getString("likedsongsname"));
                likedSongsPlaylist.addSongs(collection.getLikedSongs().getSongs());
                this.writePlaylistData(likedSongsPlaylist, this.localLibrary);
            }
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
    public void syncPlaylists() throws InterruptedException {
        logger.debug("Getting local playlists to remove mismatches");
        Upload upload = new Upload(this.localLibrary);
        ArrayList<Playlist> playlists = upload.getPlaylists();
        if (playlists != null && !playlists.isEmpty()) {
            playlists.removeAll(collection.getPlaylists());
            for (Playlist playlist : playlists) {
                if (settings.getBool("downloadhierachy")) {
                    File playlistFolder = new File(this.localLibrary, playlist.getFolderName());
                    if (MusicTools.deleteFolder(playlistFolder)) {
                        logger.info("Deleted playlist '" + playlist.getName() + "' folder: "
                                + playlistFolder.getAbsolutePath());
                    } else {
                        logger.error("Could not delete playlist '" + playlist.getName() + "' folder:"
                                + playlistFolder.getAbsolutePath());
                    }
                } else {
                    // deletes all playlists songs
                    this.syncPlaylist(new Playlist(playlist.getName()));
                    File m3uFile = new File(this.localLibrary, playlist.getFolderName() + ".m3u");
                    if (m3uFile.delete()) {
                        logger.info(
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
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in playlistSync");
            return;
        }
        logger.debug("Getting local playlist '" + playlist.getName() + "' to remove mismatches");
        File playlistFolder = this.localLibrary;
        Playlist localPlaylist = null;
        if (settings.getBool("downloadhierachy")) {
            playlistFolder = new File(this.localLibrary, playlist.getFolderName());
            localPlaylist = Upload.getPlaylist(playlistFolder);
        } else {
            File m3uFile = new File(this.localLibrary, playlist.getFolderName() + ".m3u");
            if (m3uFile.exists()) {
                localPlaylist = Upload.getM3UPlaylist(m3uFile);
            }
        }
        if (localPlaylist != null && !localPlaylist.isEmpty()) {
            localPlaylist.removeSongs(playlist.getSongs());
            for (Song song : localPlaylist.getSongs()) {
                File songFile = new File(playlistFolder, song.getFileName());
                if (songFile.exists()) {
                    if (!settings.getBool("downloadhierachy")) {
                        if (collection.isLiked(song)) {
                            continue;
                        }
                    }
                    if (songFile.delete()) {
                        logger.info("Deleted playlist '" + playlist.getName() + "' song: "
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
        if (settings.getBool("downloadhierachy")) {
            songs = playlist.getSongs();
            playlistFolder = new File(this.localLibrary, playlist.getFolderName());
            playlistFolder.mkdirs();
        } else {
            songs = collection.getStandalonePlaylistSongs(playlist);
            playlistFolder = this.localLibrary;
            this.writePlaylistData(playlist, playlistFolder);
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
    public void syncAlbums() throws InterruptedException {
        logger.debug("Getting local albums to remove mismatches");
        Upload upload = new Upload(this.localLibrary);
        ArrayList<Album> albums = upload.getAlbums();
        if (albums != null && !albums.isEmpty()) {
            albums.removeAll(collection.getAlbums());
            for (Album album : albums) {
                File albumFolder = new File(this.localLibrary, album.getFolderName());
                if (albumFolder.exists()) {
                    if (MusicTools.deleteFolder(albumFolder)) {
                        logger.info(
                                "Deleted album '" + album.getName() + "'' folder: " + albumFolder.getAbsolutePath());
                    } else {
                        logger.error("Failed to delete album '" + album.getName() + "' folder: "
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
        ArrayList<Album> albums = collection.getAlbums();
        try (ProgressBar pb = Progressbar.progressBar("Album Downloads", albums.size())) {
            for (Album album : albums) {
                this.downloadAlbum(album);
                pb.setExtraMessage(album.getName()).step();
            }
            pb.setExtraMessage("Done").step();
        }
    }

    public void syncAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided in albumSync");
            return;
        }
        logger.debug("Getting local album '" + album.getName() + "' to remove mismatches");
        File albumFolder = new File(this.localLibrary, album.getFolderName());
        Album localAlbum = Upload.getAlbum(albumFolder);
        if (localAlbum != null && !localAlbum.isEmpty()) {
            localAlbum.removeSongs(album.getSongs());
            for (Song song : localAlbum.getSongs()) {
                File songFile = new File(albumFolder, song.getFileName());
                if (songFile.exists()) {
                    if (songFile.delete()) {
                        logger.info("Deleted album '" + album.getName() + "' song: "
                                + songFile.getAbsolutePath());
                    } else {
                        logger.debug("could not delete album '" + album.getName() + "' song: "
                                + songFile.getAbsolutePath());
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
        File albumFolder = new File(this.localLibrary, album.getFolderName());
        albumFolder.mkdirs();
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
            MusicTools.writeData(m3uFile, collection.getPlaylistM3U(playlist));
        } catch (Exception e) {
            logger.error("Exception writing playlist '" + playlist.toString() + "' m3u", e);
        }
        try {
            if (playlist.getCoverImage() != null) {
                MusicTools.downloadImage(playlist.getCoverImage(),
                        new File(folder, playlist.getFolderName() + ".png"));
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
            MusicTools.writeData(nfoFile, collection.getAlbumNFO(album));
        } catch (Exception e) {
            logger.error("Exception writing album '" + album.toString() + "' nfo", e);
        }
        try {
            if (album.getCoverImage() != null) {
                MusicTools.downloadImage(album.getCoverImage(),
                        new File(folder, album.getFolderName() + ".png"));
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
