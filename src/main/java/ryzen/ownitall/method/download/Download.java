package ryzen.ownitall.method.download;

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
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.method.Upload;
import ryzen.ownitall.method.interfaces.Export;
import ryzen.ownitall.method.interfaces.Sync;
import ryzen.ownitall.util.ClassLoader;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * Download class.
 * </p>
 *
 * @author ryzen
 */
public class Download implements Sync, Export {
    // more download sources
    // qobuz
    // deezer
    // tidal
    private static final Logger logger = new Logger(Download.class);
    private ExecutorService executor;
    // currently needed to prevent infinite looping of constructors
    private static boolean initiation = false;
    private DownloadInterface downloadClass;
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
     * <p>
     * Constructor for Download.
     * </p>
     *
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public Download() throws MissingSettingException, AuthenticationException {
        if (initiation) {
            return;
        }
        if (Settings.downloadMethod.isEmpty()) {
            throw new MissingSettingException("Missing downloadMethod");
        }
        try {
            Class<? extends DownloadInterface> downloadClass = ClassLoader.load().getSubClass(DownloadInterface.class,
                    Settings.downloadMethod);
            try {
                logger.debug("Initializing '" + downloadClass.getSimpleName() + "' download class");
                initiation = true;
                this.downloadClass = downloadClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause instanceof MissingSettingException) {
                    throw new MissingSettingException(e);
                }
                if (cause instanceof AuthenticationException) {
                    throw new AuthenticationException(e);
                }
                logger.error("Exception while setting up download class '" + downloadClass.getSimpleName() + "'", e);
                throw new NoSuchMethodException(downloadClass.getName());
            }
        } catch (NoSuchMethodException e) {
            logger.error("Invalid or missing download class set in settings", e);
            throw new AuthenticationException(e);
        } finally {
            initiation = false;
        }
    }

    /**
     * <p>
     * threadDownload.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @param path a {@link java.io.File} object
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
     * @param path a {@link java.io.File} object
     */
    public void exportSong(Song song) {
        try {
            File downloadFile = new File(Settings.localFolder,
                    String.valueOf(song.hashCode()) + "." + Settings.downloadFormat);
            ArrayList<String> command = downloadClass.createCommand(song, downloadFile);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            File songFile = new File(Settings.localFolder, Collection.getRelativeSongPath(song).toString());
            StringBuilder completeLog = new StringBuilder();
            for (int i = 0; i < retries; i++) {
                if (songFile.exists()) {
                    break;
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
                    logger.debug("Attempt: " + i);
                    downloadClass.handleError(exitCode);
                    logger.debug(
                            "Command: " + command.toString() + "\n Complete log \n: " + completeLog.toString());
                }
                if (downloadFile.exists()) {
                    songFile.getParentFile().mkdirs();
                    if (!downloadFile.renameTo(songFile)) {
                        logger.warn("Unable to move downloaded song '" + song.getName() + "' from '"
                                + downloadFile.getAbsolutePath() + "'");
                    }
                    break;
                }
            }
            if (songFile.exists()) {
                writeMetaData(song, songFile);
            } else {
                logger.warn("song '" + song.toString() + "' failed to download, check logs");
                logger.debug("Complete download log: " + completeLog.toString());
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while downloading song");
            InterruptionHandler.forceInterruption();
        } catch (IOException e) {
            logger.error("Exception preparing " + downloadClass.getClass().getSimpleName() + ": ", e);
        } catch (DownloadException e) {
            logger.warn("Exception while downloading: " + e.getMessage());
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
     * @param folder   - folder to place m3u file in
     */
    public void writePlaylistData(Playlist playlist) {
        if (playlist == null) {
            logger.debug("null playlist provided in writePlaylistData");
            return;
        }
        try {
            File m3uFile = new File(Settings.localFolder, MusicTools.sanitizeFileName(playlist.getName()) + ".m3u");
            MusicTools.writeData(m3uFile, Collection.getPlaylistM3U(playlist));
        } catch (Exception e) {
            logger.error("Exception writing playlist '" + playlist.toString() + "' m3u", e);
        }
        try {
            if (playlist.getCoverImage() != null) {
                MusicTools.downloadImage(playlist.getCoverImage(),
                        new File(Settings.localFolder, Collection.getCollectionCoverFileName(playlist)));
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
    public void writeAlbumData(Album album) {
        if (album == null) {
            logger.debug("null Album provided in writeAlbumData");
            return;
        }
        File folder = new File(Settings.localFolder, MusicTools.sanitizeFileName(album.getName()));
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
                        new File(folder, Collection.getCollectionCoverFileName(album)));
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
    public void cleanFolder() {
        for (File file : Settings.localFolder.listFiles()) {
            if (file.isFile()) {
                String extension = MusicTools.getExtension(file);
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
     * {@inheritDoc}
     *
     * clean up local liked songs before downloading
     */
    @Override
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
     * {@inheritDoc}
     *
     * orchestrator of DownloadSong for all standalone liked songs
     */
    @Override
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
     * {@inheritDoc}
     *
     * deletes entire playlists which are not in collection
     */
    @Override
    public void syncPlaylists() throws InterruptedException, MissingSettingException, AuthenticationException {
        logger.debug("Getting local playlists to remove mismatches");
        ArrayList<Playlist> playlists = new Upload().getPlaylists();
        if (playlists != null && !playlists.isEmpty()) {
            playlists.removeAll(Collection.getPlaylists());
            for (Playlist playlist : IPIterator.wrap(playlists, "Playlists", playlists.size())) {
                // deletes all playlists songs
                this.syncPlaylist(new Playlist(playlist.getName()));
                File m3uFile = new File(Settings.localFolder, MusicTools.sanitizeFileName(playlist.getName()) + ".m3u");
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
     * {@inheritDoc}
     *
     * orchestrator of downloadPlaylist
     */
    @Override
    public void uploadPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = Collection.getPlaylists();
        for (Playlist playlist : IPIterator.wrap(playlists, "Playlists", playlists.size())) {
            this.uploadPlaylist(playlist);
        }
    }

    /**
     * {@inheritDoc}
     *
     * cleans up individual songs in a playlist
     */
    @Override
    public void syncPlaylist(Playlist playlist) throws InterruptedException, MissingSettingException,
            AuthenticationException {
        if (playlist == null) {
            logger.debug("null playlist provided in playlistSync");
            return;
        }
        logger.debug("Getting local playlist '" + playlist.getName() + "' to remove mismatches");
        Playlist localPlaylist = null;
        File m3uFile = new File(Settings.localFolder, MusicTools.sanitizeFileName(playlist.getName()) + ".m3u");
        if (m3uFile.exists()) {
            localPlaylist = Upload.getM3UPlaylist(m3uFile);
        }
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

    /**
     * {@inheritDoc}
     *
     * orchestrator for downloading a playlist
     */
    @Override
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
     * {@inheritDoc}
     *
     * deletes entire albums which are not in collection
     */
    @Override
    public void syncAlbums() throws InterruptedException, MissingSettingException, AuthenticationException {
        logger.debug("Getting local albums to remove mismatches");
        ArrayList<Album> albums = new Upload().getAlbums();
        if (albums != null && !albums.isEmpty()) {
            albums.removeAll(Collection.getAlbums());
            for (Album album : IPIterator.wrap(albums.iterator(), "Albums", albums.size())) {
                File albumFolder = new File(Settings.localFolder, MusicTools.sanitizeFileName(album.getName()));
                if (albumFolder.exists()) {
                    if (MusicTools.deleteFolder(albumFolder)) {
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
     * {@inheritDoc}
     *
     * orchestrator for downloadAlbum
     */
    @Override
    public void uploadAlbums() throws InterruptedException {
        ArrayList<Album> albums = Collection.getAlbums();
        for (Album album : IPIterator.wrap(albums, "Albums", albums.size())) {
            this.uploadAlbum(album);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbum(Album album) throws InterruptedException, MissingSettingException, AuthenticationException {
        if (album == null) {
            logger.debug("null album provided in albumSync");
            return;
        }
        logger.debug("Getting local album '" + album.getName() + "' to remove mismatches");
        File albumFolder = new File(Settings.localFolder, MusicTools.sanitizeFileName(album.getName()));
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
     * {@inheritDoc}
     *
     * download an album
     * has its own folder
     */
    @Override
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
}
