package ryzen.ownitall.integrations.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jaudiotagger.tag.FieldKey;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.FileTools;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.WebTools.AuthenticationException;

/**
 * Base of every download integration, a download integration only has to provide
 * the binary command to download a single song and how to read its exit codes,
 * everything else (threading, metadata, cleanup, sync) is shared here.
 * <p>
 * Implementations are their own method (ex: YTdlp, SoulSeek), so this class
 * deliberately implements no interface to keep it out of the method menus.
 * </p>
 * <p>
 * Local class.
 * </p>
 *
 * @author ryzen
 */
public abstract class Local {
    // more download sources
    // qobuz
    // deezer
    // tidal
    private static final Logger logger = new Logger(Local.class);
    private ExecutorService executor;
    private static final ArrayList<String> whiteList = new ArrayList<>(
            Arrays.asList("m3u", "png", "nfo"));
    static {
        // so when user changes it doesnt delete their old
        whiteList.addAll(Arrays.asList(Settings.load().getOptions("downloadFormat")));
    }
    /** Constant <code>downloadThreads=Settings.downloadThreads</code> */
    protected static int downloadThreads = Settings.downloadThreads;
    private static final int retries = 3;

    /**
     * shared setup for every download method, download methods validate their own
     * settings and binaries in their own constructors
     */
    protected Local() {
    }

    /**
     * build the command of the download binary for a single song
     *
     * @param song         - song to download
     * @param downloadFile - temporary file the binary must write to, it gets moved
     *                     to its collection path once done
     * @param attempt      - which download attempt this is (0 based), implementations that can
     *                     widen their search use it to loosen their criteria
     * @return             the command to run, null if the song cannot be downloaded
     * @throws java.lang.InterruptedException if any.
     */
    public abstract ArrayList<String> createCommand(Song song, File downloadFile, int attempt)
            throws InterruptedException;

    /**
     * interpret a non zero exit code of the download binary
     *
     * @param exitCode - exit code of the download binary
     * @param log      - everything the binary printed during every attempt so far, implementations
     *                 matching on error text rather than exit codes read it here
     * @throws ryzen.ownitall.integrations.local.Local.DownloadException if the exit
     *                                                                   code makes
     *                                                                   retrying
     *                                                                   pointless
     */
    public abstract void handleError(int exitCode, String log) throws DownloadException;

    /**
     * the file the binary ended up writing, defaults to the one it was told to write,
     * implementations letting the binary decide the name (ex: yt-dlp templates) return the path
     * it printed instead
     *
     * @param expected - file the binary was told to write
     * @param log      - everything the binary printed during every attempt so far
     * @return the file to move into the collection
     */
    protected File getDownloadedFile(File expected, String log) {
        return expected;
    }

    /**
     * called once a download landed on disk but before it is moved into the collection,
     * implementations use it to remember what they actually matched
     *
     * @param song - song being downloaded
     * @param log  - everything the binary printed during every attempt so far
     */
    protected void onDownloaded(Song song, String log) {
    }

    /**
     * one line describing what the binary matched (or last tried), appended to the warning of a
     * failed download so a wrong or missing song can be diagnosed from the logs
     *
     * @param log - everything the binary printed during every attempt so far
     * @return a {@link java.lang.String} object, null if the implementation has nothing to report
     */
    protected String getMatchDetails(String log) {
        return null;
    }

    /**
     * <p>
     * threadDownload.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    public void threadDownload(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("null song or path provided in threadDownload");
            return;
        }
        if (this.executor == null || this.executor.isShutdown()) {
            this.threadInit();
        }
        while (true) {
            try {
                InterruptionHandler.checkGlobalInterruption();
                // Attempt to execute the task
                this.executor.execute(() -> {
                    this.exportSong(song);
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
                downloadThreads,
                downloadThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(downloadThreads));
    }

    /**
     * shut down all threads
     *
     * @throws java.lang.InterruptedException - if user interrupts while waiting
     */
    public void threadShutdown() throws InterruptedException {
        if (this.executor == null || this.executor.isShutdown()) {
            return;
        }
        this.executor.shutdown();
        logger.debug("Awaiting current threads to shutdown (max 10 min)");
        try {
            this.executor.awaitTermination(10, TimeUnit.MINUTES);
            logger.debug("All threads shut down");
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            logger.debug("All threads forcibly shut down");
            throw e;
        }
    }

    /**
     * <p>
     * downloadSong.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     */
    public void exportSong(Song song) {
        try {
            File downloadFile = new File(Settings.localFolder,
                    String.valueOf(song.hashCode()) + "." + Settings.downloadFormat);
            File songFile = new File(Settings.localFolder, Collection.getRelativeSongPath(song).toString());
            StringBuilder completeLog = new StringBuilder();
            String failure = null;
            for (int attempt = 0; attempt < retries; attempt++) {
                if (songFile.exists()) {
                    logger.debug("Skipping song '" + song.getName() + "', already exists");
                    break;
                }
                ArrayList<String> command = this.createCommand(song, downloadFile, attempt);
                if (command == null) {
                    return;
                }
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true); // Merge stdout and stderr
                Process process = processBuilder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    // Capture output for logging
                    while ((line = reader.readLine()) != null) {
                        completeLog.append(line).append("\n");
                    }
                }
                int exitCode = process.waitFor();
                File downloadedFile = this.getDownloadedFile(downloadFile, completeLog.toString());
                if (downloadedFile.exists()) {
                    this.onDownloaded(song, completeLog.toString());
                    songFile.getParentFile().mkdirs();
                    if (!downloadedFile.renameTo(songFile)) {
                        logger.warn("Unable to move downloaded song '" + song.getName() + "' from '"
                                + downloadedFile.getAbsolutePath() + "'");
                    }
                    break;
                }
                if (exitCode != 0) {
                    logger.debug("Attempt: " + attempt + " exited with code " + exitCode);
                    logger.debug("Command: " + command.toString() + "\n Complete log \n: " + completeLog.toString());
                    try {
                        this.handleError(exitCode, completeLog.toString());
                    } catch (DownloadException e) {
                        // the binary told us another attempt cannot help, do not waste them
                        failure = e.getMessage();
                        break;
                    }
                }
            }
            if (songFile.exists()) {
                writeMetaData(song, songFile);
            } else {
                String matched = this.getMatchDetails(completeLog.toString());
                logger.warn("song '" + song.toString() + "' failed to download ("
                        + (failure != null ? failure : "no file was written")
                        + (matched == null ? "" : ", last match '" + matched + "'") + ", check logs)");
                logger.debug("Complete download log: " + completeLog.toString());
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while downloading song");
            InterruptionHandler.forceInterruption();
        } catch (IOException e) {
            logger.error("Exception preparing " + this.getClass().getSimpleName() + ": ", e);
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
        ArrayList<Artist> artists = song.getArtists();
        String artistList = "";
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
     */
    public void writePlaylistData(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in writePlaylistData");
            return;
        }
        try {
            File m3uFile = new File(Settings.localFolder, FileTools.sanitizeFileName(playlist.getName()) + ".m3u");
            FileTools.writeData(m3uFile, Collection.getPlaylistM3U(playlist));
        } catch (Exception e) {
            logger.error("Exception writing playlist '" + playlist.toString() + "' m3u", e);
        }
        try {
            if (playlist.getCoverImage() != null) {
                WebTools.downloadImage(playlist.getCoverImage(),
                        new File(Settings.localFolder, Collection.getCoverFileName(playlist)));
            }
        } catch (IOException e) {
            logger.error("Exception writing playlist '" + playlist.toString() + "' coverimage", e);
        }
    }

    /**
     * write album nfo data including coverimage
     *
     * @param album - album to get data from
     */
    public void writeAlbumData(Album album) {
        if (album == null) {
            logger.debug("null Album provided in writeAlbumData");
            return;
        }
        File folder = new File(Settings.localFolder, FileTools.sanitizeFileName(album.getName()));
        if (folder == null || !folder.exists()) {
            logger.debug("null or non existant folder provided in writeAlbumData");
            return;
        }
        try {
            File nfoFile = new File(folder, "album.nfo");
            ArrayList<String> artistNames = new ArrayList<>();
            for (Artist artist : album.getArtists()) {
                artistNames.add(artist.getName());
            }
            ArrayList<String> songNames = new ArrayList<>();
            for (Song song : album.getSongs()) {
                songNames.add(song.getName());
            }
            String albumNFO = MusicTools.getAlbumNFO(album.getName(), artistNames, songNames,
                    Collection.getCoverFileName(album));
            if (albumNFO != null) {
                FileTools.writeData(nfoFile, albumNFO);
            }
        } catch (Exception e) {
            logger.error("Exception writing album '" + album.toString() + "' nfo", e);
        }
        try {
            if (album.getCoverImage() != null) {
                WebTools.downloadImage(album.getCoverImage(),
                        new File(folder, Collection.getCoverFileName(album)));
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
     */
    public void cleanFolder() {
        for (File file : Settings.localFolder.listFiles()) {
            if (file.isFile()) {
                String extension = FileTools.getExtension(file);
                if (!whiteList.contains(extension)) {
                    if (file.delete()) {
                        logger.debug("Cleaned up file: '" + file.getAbsolutePath() + "'");
                    } else {
                        logger.warn("Failed to clean up file: '" + file.getAbsolutePath() + "'");
                    }
                }
            }
        }
    }

    /**
     * clean up local liked songs before downloading
     *
     * @throws java.lang.InterruptedException                       if any.
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public void syncLikedSongs() throws InterruptedException, AuthenticationException, MissingSettingException {
        logger.debug("Getting local liked songs to remove mismatches");
        LikedSongs likedSongs = new Upload().getLikedSongs();
        if (likedSongs != null && !likedSongs.isEmpty()) {
            likedSongs.removeSongs(Collection.getLikedSongs().getSongs());
            for (Song song : IPIterator.wrap(likedSongs.getSongs(), "Liked Songs", likedSongs.size())) {
                // skip if in a playlist
                if (Collection.getSongPlaylist(song) != null) {
                    continue;
                }
                File songFile = new File(Settings.localFolder, Collection.getRelativeSongPath(song).toString());
                if (songFile.exists()) {
                    if (songFile.delete()) {
                        logger.info("Deleted liked song '" + songFile.getAbsolutePath());
                    } else {
                        logger.warn("Failed to delete liked song: " + songFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * orchestrator of DownloadSong for all standalone liked songs
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadLikedSongs() throws InterruptedException {
        ArrayList<Song> songs = Collection.getStandaloneLikedSongs();
        Playlist likedSongsPlaylist = new Playlist(Settings.likedSongName);
        likedSongsPlaylist.addSongs(Collection.getLikedSongs().getSongs());
        this.writePlaylistData(likedSongsPlaylist);
        try {
            for (Song song : IPIterator.wrap(songs, "Liked Songs", songs.size())) {
                this.threadDownload(song);
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            throw e;
        }
        this.threadShutdown();
        this.cleanFolder();
    }

    /**
     * deletes entire playlists which are not in collection
     *
     * @throws java.lang.InterruptedException                       if any.
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public void syncPlaylists() throws InterruptedException, MissingSettingException, AuthenticationException {
        logger.debug("Getting local playlists to remove mismatches");
        ArrayList<Playlist> playlists = new Upload().getPlaylists();
        if (playlists != null && !playlists.isEmpty()) {
            playlists.removeAll(Collection.getPlaylists());
            for (Playlist playlist : IPIterator.wrap(playlists, "Playlists", playlists.size())) {
                // deletes all playlists songs
                this.syncPlaylist(new Playlist(playlist.getName()));
                File m3uFile = new File(Settings.localFolder, FileTools.sanitizeFileName(playlist.getName()) + ".m3u");
                if (m3uFile.delete()) {
                    logger.info(
                            "Cleaned up playlist '" + playlist.getName() + "' m3u file: "
                                    + m3uFile.getAbsolutePath());
                } else {
                    logger.warn("Could not delete playlist '" + playlist.getName() + "' m3u file: "
                            + m3uFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * orchestrator of downloadPlaylist
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = Collection.getPlaylists();
        for (Playlist playlist : IPIterator.wrap(playlists, "Playlists", playlists.size())) {
            this.uploadPlaylist(playlist);
        }
    }

    /**
     * cleans up individual songs in a playlist
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     * @throws java.lang.InterruptedException                       if any.
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public void syncPlaylist(Playlist playlist) throws InterruptedException, MissingSettingException,
            AuthenticationException {
        if (playlist == null) {
            logger.debug("null playlist provided in playlistSync");
            return;
        }
        logger.debug("Getting local playlist '" + playlist.getName() + "' to remove mismatches");
        File m3uFile = new File(Settings.localFolder, FileTools.sanitizeFileName(playlist.getName()) + ".m3u");
        if (m3uFile.exists()) {
            Playlist localPlaylist = Upload.getM3UPlaylist(m3uFile);
            if (localPlaylist != null && !localPlaylist.isEmpty()) {
                localPlaylist.removeSongs(playlist.getSongs());
                for (Song song : IPIterator.wrap(localPlaylist.getSongs(), localPlaylist.getName(),
                        localPlaylist.size())) {
                    if (Collection.isLiked(song)) {
                        continue;
                    }
                    File songFile = new File(Settings.localFolder, Collection.getRelativeSongPath(song).toString());
                    if (songFile.exists()) {
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
    }

    /**
     * orchestrator for downloading a playlist
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in downloadPlaylist");
            return;
        }
        ArrayList<Song> songs = Collection.getStandalonePlaylistSongs(playlist);
        this.writePlaylistData(playlist);
        try {
            for (Song song : IPIterator.wrap(songs, playlist.getName(), playlist.size())) {
                this.threadDownload(song);
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            throw e;
        }
        this.threadShutdown();
        this.cleanFolder();
    }

    /**
     * deletes entire albums which are not in collection
     *
     * @throws java.lang.InterruptedException                       if any.
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public void syncAlbums() throws InterruptedException, MissingSettingException, AuthenticationException {
        logger.debug("Getting local albums to remove mismatches");
        ArrayList<Album> albums = new Upload().getAlbums();
        if (albums != null && !albums.isEmpty()) {
            albums.removeAll(Collection.getAlbums());
            for (Album album : IPIterator.wrap(albums.iterator(), "Albums", albums.size())) {
                File albumFolder = new File(Settings.localFolder, FileTools.sanitizeFileName(album.getName()));
                if (albumFolder.exists()) {
                    if (FileTools.deleteFolder(albumFolder)) {
                        logger.info(
                                "Deleted album '" + album.getName() + "'' folder: " + albumFolder.getAbsolutePath());
                    } else {
                        logger.warn("Failed to delete album '" + album.getName() + "' folder: "
                                + albumFolder.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * orchestrator for downloadAlbum
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadAlbums() throws InterruptedException {
        ArrayList<Album> albums = Collection.getAlbums();
        for (Album album : IPIterator.wrap(albums, "Albums", albums.size())) {
            this.uploadAlbum(album);
        }
    }

    /**
     * cleans up individual songs in an album
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException                       if any.
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public void syncAlbum(Album album) throws InterruptedException, MissingSettingException, AuthenticationException {
        if (album == null) {
            logger.debug("null album provided in albumSync");
            return;
        }
        logger.debug("Getting local album '" + album.getName() + "' to remove mismatches");
        File albumFolder = new File(Settings.localFolder, FileTools.sanitizeFileName(album.getName()));
        Album localAlbum = new Upload().getAlbum(albumFolder.getAbsolutePath(), album.getName(),
                album.getMainArtist().getName());
        if (localAlbum != null && !localAlbum.isEmpty()) {
            localAlbum.removeSongs(album.getSongs());
            for (Song song : IPIterator.wrap(localAlbum.getSongs().iterator(), album.getName(), localAlbum.size())) {
                File songFile = new File(Settings.localFolder, Collection.getRelativeSongPath(song).toString());
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
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public void uploadAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided in downloadAlbum");
            return;
        }
        // albums are always in a folder
        try {
            this.writeAlbumData(album);
            for (Song song : IPIterator.wrap(album.getSongs(), album.getName(), album.size())) {
                this.threadDownload(song);
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            throw e;
        }
        this.threadShutdown();
        this.cleanFolder();
    }

    /**
     * <p>
     * DownloadException class.
     * </p>
     */
    public static class DownloadException extends Exception {
        /**
         * <p>
         * Constructor for DownloadException.
         * </p>
         *
         * @param message a {@link java.lang.String} object
         */
        public DownloadException(String message) {
            super(message);
        }

        /**
         * <p>
         * Constructor for DownloadException.
         * </p>
         */
        public DownloadException() {
            super();
        }
    }
}
