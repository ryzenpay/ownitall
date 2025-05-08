package ryzen.ownitall.method.download;

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

import org.jaudiotagger.tag.FieldKey;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.method.Upload;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.ProgressBar;

/**
 * <p>
 * Download class.
 * </p>
 *
 * @author ryzen
 */
@Method.Export
public class Download extends Method {
    // more download sources
    // qobuz
    // deezer
    // tidal
    private static final Logger logger = new Logger(Download.class);
    private ExecutorService executor;
    private static final ArrayList<String> whiteList = new ArrayList<>(
            Arrays.asList("m3u", "png", "nfo", Settings.downloadFormat));
    /** Constant <code>downloadThreads=Settings.downloadThreads</code> */
    protected static int downloadThreads = Settings.downloadThreads;

    /**
     * <p>
     * threadDownload.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @param path a {@link java.io.File} object
     * @throws java.lang.InterruptedException if any.
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
                this.executor.execute(() -> {
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
    public void downloadSong(Song song, File path) {
        logger.warn("Unsupported download method to downloadSong");
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
                        logger.error("Failed to clean up file: '" + file.getAbsolutePath() + "'", new Exception());
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
    public void syncLikedSongs() throws InterruptedException {
        logger.debug("Getting local liked songs to remove mismatches");
        Upload upload = new Upload();
        LikedSongs likedSongs = upload.getLikedSongs();
        File songFolder = Settings.localFolder;
        if (Settings.downloadHierachy) {
            songFolder = new File(Settings.localFolder, Settings.likedSongName);
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
                        logger.error("Failed to delete liked song: " + songFile.getAbsolutePath(), new Exception());
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
        ArrayList<Song> songs;
        File likedSongsFolder;
        if (Settings.downloadHierachy) {
            songs = Collection.getLikedSongs().getSongs();
            likedSongsFolder = new File(Settings.localFolder, Settings.likedSongName);
            likedSongsFolder.mkdirs();
        } else {
            songs = Collection.getStandaloneLikedSongs();
            likedSongsFolder = Settings.localFolder;
            if (Settings.downloadLikedsongPlaylist) {
                Playlist likedSongsPlaylist = new Playlist(Settings.likedSongName);
                likedSongsPlaylist.addSongs(Collection.getLikedSongs().getSongs());
                this.writePlaylistData(likedSongsPlaylist, Settings.localFolder);
            }
        }
        try (ProgressBar pb = new ProgressBar("Downloading Liked songs", songs.size() + 1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (Song song : songs) {
                interruptionHandler.throwInterruption();
                pb.step(song.getName());
                this.threadDownload(song, likedSongsFolder);
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            throw e;
        }
        this.threadShutdown();
        this.cleanFolder(likedSongsFolder);
    }

    /**
     * {@inheritDoc}
     *
     * deletes entire playlists which are not in collection
     */
    @Override
    public void syncPlaylists() throws InterruptedException {
        logger.debug("Getting local playlists to remove mismatches");
        Upload upload = new Upload();
        ArrayList<Playlist> playlists = upload.getPlaylists();
        if (playlists != null && !playlists.isEmpty()) {
            playlists.removeAll(Collection.getPlaylists());
            for (Playlist playlist : playlists) {
                if (Settings.downloadHierachy) {
                    File playlistFolder = new File(Settings.localFolder, playlist.getFolderName());
                    if (MusicTools.deleteFolder(playlistFolder)) {
                        logger.info("Deleted playlist '" + playlist.getName() + "' folder: "
                                + playlistFolder.getAbsolutePath());
                    } else {
                        logger.error("Could not delete playlist '" + playlist.getName() + "' folder:"
                                + playlistFolder.getAbsolutePath(), new Exception());
                    }
                } else {
                    // deletes all playlists songs
                    this.syncPlaylist(new Playlist(playlist.getName()));
                    File m3uFile = new File(Settings.localFolder, playlist.getFolderName() + ".m3u");
                    if (m3uFile.delete()) {
                        logger.info(
                                "Cleaned up playlist '" + playlist.getName() + "' m3u file: "
                                        + m3uFile.getAbsolutePath());
                    } else {
                        logger.error("Could not delete playlist '" + playlist.getName() + "' m3u file: "
                                + m3uFile.getAbsolutePath(), new Exception());
                    }
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
        try (ProgressBar pb = new ProgressBar("Playlist Downloads", playlists.size())) {
            for (Playlist playlist : playlists) {
                this.uploadPlaylist(playlist);
                pb.step(playlist.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * cleans up individual songs in a playlist
     */
    @Override
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in playlistSync");
            return;
        }
        logger.debug("Getting local playlist '" + playlist.getName() + "' to remove mismatches");
        File playlistFolder = Settings.localFolder;
        Playlist localPlaylist = null;
        Upload upload = new Upload();
        if (Settings.downloadHierachy) {
            playlistFolder = new File(Settings.localFolder, playlist.getFolderName());
            localPlaylist = upload.getPlaylist(playlistFolder.getAbsolutePath(), playlist.getName());
        } else {
            File m3uFile = new File(Settings.localFolder, playlist.getFolderName() + ".m3u");
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
        ArrayList<Song> songs;
        File playlistFolder;
        if (Settings.downloadHierachy) {
            songs = playlist.getSongs();
            playlistFolder = new File(Settings.localFolder, playlist.getFolderName());
            playlistFolder.mkdirs();
        } else {
            songs = Collection.getStandalonePlaylistSongs(playlist);
            playlistFolder = Settings.localFolder;
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
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            throw e;
        }
        this.threadShutdown();
        this.cleanFolder(playlistFolder);
    }

    /**
     * {@inheritDoc}
     *
     * deletes entire albums which are not in collection
     */
    @Override
    public void syncAlbums() throws InterruptedException {
        logger.debug("Getting local albums to remove mismatches");
        Upload upload = new Upload();
        ArrayList<Album> albums = upload.getAlbums();
        if (albums != null && !albums.isEmpty()) {
            albums.removeAll(Collection.getAlbums());
            for (Album album : albums) {
                File albumFolder = new File(Settings.localFolder, album.getFolderName());
                if (albumFolder.exists()) {
                    if (MusicTools.deleteFolder(albumFolder)) {
                        logger.info(
                                "Deleted album '" + album.getName() + "'' folder: " + albumFolder.getAbsolutePath());
                    } else {
                        logger.error("Failed to delete album '" + album.getName() + "' folder: "
                                + albumFolder.getAbsolutePath(), new Exception());
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
        try (ProgressBar pb = new ProgressBar("Album Downloads", albums.size())) {
            for (Album album : albums) {
                this.uploadAlbum(album);
                pb.step(album.getName());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided in albumSync");
            return;
        }
        logger.debug("Getting local album '" + album.getName() + "' to remove mismatches");
        File albumFolder = new File(Settings.localFolder, album.getFolderName());
        Upload upload = new Upload();
        Album localAlbum = upload.getAlbum(albumFolder.getAbsolutePath(), album.getName(),
                album.getMainArtist().getName());
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
        File albumFolder = new File(Settings.localFolder, album.getFolderName());
        albumFolder.mkdirs();
        try (ProgressBar pb = new ProgressBar("Download Album: " + album.getName(), album.size() + 1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            this.writeAlbumData(album, albumFolder);
            for (Song song : album.getSongs()) {
                interruptionHandler.throwInterruption();
                pb.step(song.getName());
                this.threadDownload(song, albumFolder);
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            throw e;
        }
        this.threadShutdown();
        this.cleanFolder(albumFolder);
    }
}
