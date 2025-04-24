package ryzen.ownitall.method.local;

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

import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.method.Local;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.ProgressBar;

public class Download {
    // TODO: multiple download sources
    // qobuz
    // deezer
    // youtube (already implemented)
    // tidal
    // PRIORITY: soularr (https://github.com/mrusse/soularr)
    private static final Logger logger = LogManager.getLogger();
    private ExecutorService executor;
    private static final ArrayList<String> whiteList = new ArrayList<>(
            Arrays.asList("m3u", "png", "nfo", Settings.downloadFormat));
    private File localLibrary;

    /**
     * default download constructor
     * setting all settings / credentials
     * 
     * @throws InterruptedException - when user interrupts
     */
    public Download(File localLibrary) throws InterruptedException {
        if (Method.isCredentialsEmpty(Local.class)) {
            throw new InterruptedException("empty jellyfin credentials");
        }
        this.localLibrary = localLibrary;
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
        int downloadThreads = Settings.downloadThreads;
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
        command.add(Credentials.youtubeDLFile.getAbsolutePath());
        command.add("--ffmpeg-location");
        command.add(Credentials.ffmpegFile.getAbsolutePath());
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
        command.add(Settings.downloadFormat);
        command.add("--audio-quality");
        command.add(Integer.toString(Settings.downloadQuality));
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
                    if (Settings.downloadCookieFile != null && Settings.downloadCookieFile.exists()) {
                        command.add(1, "--cookies");
                        command.add(2, Settings.downloadCookieFile.getAbsolutePath());
                    } else if (!Settings.downloadCookieBrowser.isEmpty()) {
                        command.add(1, "--cookies-from-browser");
                        command.add(2, Settings.downloadCookieBrowser);
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
        if (Settings.downloadHierachy) {
            songFolder = new File(this.localLibrary, Settings.likedSongName);
        }
        if (likedSongs != null && !likedSongs.isEmpty()) {
            likedSongs.removeSongs(Collection.getLikedSongs().getSongs());
            for (Song song : likedSongs.getSongs()) {
                if (!Settings.downloadHierachy) {
                    // skip if in a playlist
                    if (Collection.getSongPlaylist(song) != null) {
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
        if (Settings.downloadHierachy) {
            songs = Collection.getLikedSongs().getSongs();
            likedSongsFolder = new File(this.localLibrary, Settings.likedSongName);
            likedSongsFolder.mkdirs();
        } else {
            songs = Collection.getStandaloneLikedSongs();
            likedSongsFolder = this.localLibrary;
            if (Settings.downloadLikedsongPlaylist) {
                Playlist likedSongsPlaylist = new Playlist(Settings.likedSongName);
                likedSongsPlaylist.addSongs(Collection.getLikedSongs().getSongs());
                this.writePlaylistData(likedSongsPlaylist, this.localLibrary);
            }
        }
        try (ProgressBar pb = new ProgressBar("Downloading Liked songs", songs.size() + 1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (Song song : songs) {
                interruptionHandler.throwInterruption();
                pb.step(song.getName());
                this.threadDownload(song, likedSongsFolder);
            }
        }
        this.threadShutdown();
        this.cleanFolder(likedSongsFolder);
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
            playlists.removeAll(Collection.getPlaylists());
            for (Playlist playlist : playlists) {
                if (Settings.downloadHierachy) {
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
        ArrayList<Playlist> playlists = Collection.getPlaylists();
        try (ProgressBar pb = new ProgressBar("Playlist Downloads", playlists.size())) {
            for (Playlist playlist : playlists) {
                this.downloadPlaylist(playlist);
                pb.step(playlist.getName());
            }
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
        if (Settings.downloadHierachy) {
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
                    if (!Settings.downloadHierachy) {
                        if (Collection.isLiked(song)) {
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
        if (Settings.downloadHierachy) {
            songs = playlist.getSongs();
            playlistFolder = new File(this.localLibrary, playlist.getFolderName());
            playlistFolder.mkdirs();
        } else {
            songs = Collection.getStandalonePlaylistSongs(playlist);
            playlistFolder = this.localLibrary;
            this.writePlaylistData(playlist, playlistFolder);
        }
        try (ProgressBar pb = new ProgressBar("Downloading Playlists: " + playlist.getName(),
                playlist.size() + 1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (Song song : songs) {
                interruptionHandler.throwInterruption();
                pb.step(song.getName());
                this.threadDownload(song, playlistFolder);
            }
        }
        this.threadShutdown();
        this.cleanFolder(playlistFolder);
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
            albums.removeAll(Collection.getAlbums());
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
        ArrayList<Album> albums = Collection.getAlbums();
        try (ProgressBar pb = new ProgressBar("Album Downloads", albums.size())) {
            for (Album album : albums) {
                this.downloadAlbum(album);
                pb.step(album.getName());
            }
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
        try (ProgressBar pb = new ProgressBar("Download Album: " + album.getName(), album.size() + 1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            this.writeAlbumData(album, albumFolder);
            for (Song song : album.getSongs()) {
                interruptionHandler.throwInterruption();
                pb.step(song.getName());
                this.threadDownload(song, albumFolder);
            }
        }
        this.threadShutdown();
        this.cleanFolder(albumFolder);
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
        ArrayList<Artist> artists = song.getArtists();
        String artistList = "";
        // TODO: readme update for jellyfin, requires custom delimiter
        // dashboard -> libraries -> <select library> -> scroll to bottom -> custom
        // delimiter
        if (artists.size() == 1) {
            artistList = song.getMainArtist().getName();
        } else {
            for (Artist artist : artists) {
                artistList += artist.toString() + ";";
            }
        }
        if (!artistList.isEmpty()) {
            id3Data.put(FieldKey.ARTIST, artistList);
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
            MusicTools.writeMetaData(id3Data, Collection.isLiked(song), song.getCoverImage(), songFile);
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
            MusicTools.writeData(m3uFile, Collection.getPlaylistM3U(playlist));
        } catch (Exception e) {
            logger.error("Exception writing playlist '" + playlist.toString() + "' m3u", e);
        }
        try {
            if (playlist.getCoverImage() != null) {
                MusicTools.downloadImage(playlist.getCoverImage(),
                        new File(folder, playlist.getCoverImageFileName()));
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
            MusicTools.writeData(nfoFile, Collection.getAlbumNFO(album));
        } catch (Exception e) {
            logger.error("Exception writing album '" + album.toString() + "' nfo", e);
        }
        try {
            if (album.getCoverImage() != null) {
                MusicTools.downloadImage(album.getCoverImage(),
                        new File(folder, album.getCoverImageFileName()));
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
