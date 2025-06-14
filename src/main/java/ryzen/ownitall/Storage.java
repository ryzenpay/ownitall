package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.ProgressBar;

/**
 * <p>
 * Storage class.
 * </p>
 *
 * @author ryzen
 */
public class Storage {
    private static final Logger logger = new Logger(Storage.class);
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    /**
     * <p>
     * Constructor for Storage.
     * </p>
     */
    public Storage() {
        if (!Settings.dataFolder.exists()) { // create folder if it does not exist
            Settings.dataFolder.mkdirs();
            logger.debug("Created datafolder '" + Settings.dataFolder.getAbsolutePath() + "'");
        }
        if (!Settings.cacheFolder.exists()) {
            Settings.cacheFolder.mkdirs();
            logger.debug("Created cache folder: '" + Settings.cacheFolder.getAbsolutePath() + "'");
        }
    }

    /**
     * create archive folder (current date) in dataPath and move all current files
     * to it with no user input
     */
    public void archive() {
        LocalDate currentDate = LocalDate.now();
        String folderName = currentDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        File archiveFolder = new File(Settings.dataFolder, folderName);
        archiveFolder.mkdir();
        for (File file : Settings.dataFolder.listFiles()) {
            if (file.isFile()) {
                file.renameTo(new File(archiveFolder, file.getName()));
                logger.debug("Renamed file: '" + file.getAbsolutePath() + "'");
            }
        }
        Collection.clear();
        logger.debug("Successfully archived music library to: '" + archiveFolder.getAbsolutePath() + "'");
    }

    /**
     * <p>
     * getArchiveFolders.
     * </p>
     *
     * @return a {@link java.util.LinkedHashSet} object
     */
    public LinkedHashSet<File> getArchiveFolders() {
        LinkedHashSet<File> archiveFolders = new LinkedHashSet<>();
        for (File file : Settings.dataFolder.listFiles()) {
            if (file.isDirectory()) {
                archiveFolders.add(file);
            }
        }
        return archiveFolders;
    }

    /**
     * display a menu of all archived files (folders and dates) and give an option
     * to unarchive
     * also archives current files with todays date to prevent data loss
     *
     * @param unarchiveFolder a {@link java.io.File} object
     */
    public void unArchive(File unarchiveFolder) {
        if (unarchiveFolder == null || !unarchiveFolder.exists()) {
            logger.debug("null or non existant unarchive folder provided in unarchive");
            return;
        }
        archive();
        for (File file : unarchiveFolder.listFiles()) {
            if (file.isFile()) {
                File destFile = new File(Settings.dataFolder, file.getName());
                if (destFile.exists()) {
                    System.out
                            .print("This will delete file '" + destFile.getAbsolutePath() + "' are you sure y/N: ");
                    try {
                        if (Input.request().getAgreement()) {
                            destFile.delete();
                            logger.debug("deleted old file: '" + destFile.getAbsolutePath() + "'");
                        } else {
                            continue;
                        }
                    } catch (InterruptedException e) {
                        logger.debug("Interrupted while getting unarchive overwrite agreement");
                        continue;
                    }
                }
                file.renameTo(destFile);
            }
        }
        unarchiveFolder.delete();
        logger.debug("Deleted old archive folder: '" + unarchiveFolder.getAbsolutePath() + "'");
        Collection.clear();
        importCollection();
        logger.debug("Successfully unarchived music library");
    }

    /**
     * clear cache files
     */
    public void clearCacheFiles() {
        for (File file : Settings.cacheFolder.listFiles()) {
            file.delete();
            logger.debug("Deleted file: '" + file.getAbsolutePath() + "'");
        }
    }

    /**
     * <p>
     * clearInventoryFiles.
     * </p>
     */
    public void clearInventoryFiles() {
        for (File file : Settings.dataFolder.listFiles()) {
            if (file.isFile()) {
                file.delete();
                logger.debug("Deleted file: '" + file.getAbsolutePath() + "'");
            }
        }
    }

    /**
     * import collection from files
     * orchestrates import albums, playlists and liked songs
     */
    public void importCollection() {
        logger.debug("importing collection... ");
        try (ProgressBar pb = new ProgressBar("Loading data", 3)) {
            pb.step("Albums");
            Collection.addAlbums(importAlbums());
            pb.step("Playlists");
            Collection.addPlaylists(importPlaylists());
            pb.step("Liked Songs");
            Collection.addLikedSongs(importLikedSongs());
            logger.debug("Successfully imported collection");
        }
    }

    /**
     * save collection to local files
     * orchestrates export albums, playlists and liked songs
     */
    public void exportCollection() {
        logger.debug("Exporting music collection...");
        try (ProgressBar pb = new ProgressBar("Saving data", 3)) {
            pb.step("Albums");
            exportAlbums(Collection.getAlbums());
            pb.step("Playlists");
            exportPlaylists(Collection.getPlaylists());
            pb.step("Liked Songs");
            exportLikedSongs(Collection.getLikedSongs());
            logger.debug("Successfully exported music collection");
        }
    }

    /**
     * save all albums
     *
     * @param albums - linkedhashset of constructed Album
     */
    public void exportAlbums(ArrayList<Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return;
        }
        File albumFile = new File(Settings.dataFolder, Settings.albumFile + ".json");
        try {
            objectMapper.writeValue(albumFile, albums);
            logger.debug("Saved albums to: '" + albumFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception saving albums", e);
        }
    }

    /**
     * import saved albums
     *
     * @return - linkedhashset of constructed Album
     */
    public ArrayList<Album> importAlbums() {
        File albumFile = new File(Settings.dataFolder, Settings.albumFile + ".json");
        if (!albumFile.exists()) {
            return null;
        }
        try {
            return objectMapper.readValue(albumFile,
                    new TypeReference<ArrayList<Album>>() {
                    });
        } catch (IOException e) {
            logger.error("exception importing albums", e);
            logger.warn("If this persists, delete the file: '" + albumFile.getAbsolutePath() + "'");
            return null;
        }
    }

    /**
     * save all playlists
     *
     * @param playlists - linkedhashset of constructed Playlist
     */
    public void exportPlaylists(ArrayList<Playlist> playlists) {
        if (playlists == null || playlists.isEmpty()) {
            return;
        }
        File playlistFile = new File(Settings.dataFolder, Settings.playlistFile + ".json");
        try {
            objectMapper.writeValue(playlistFile, playlists);
            logger.debug("Saved playlists to: '" + playlistFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception saving playlists", e);
        }
    }

    /**
     * import all saved playlists
     *
     * @return - linkedhashset of constructed Playlist
     */
    public ArrayList<Playlist> importPlaylists() {
        File playlistFile = new File(Settings.dataFolder, Settings.playlistFile + ".json");
        if (!playlistFile.exists()) {
            return null;
        }
        try {
            return objectMapper.readValue(playlistFile,
                    new TypeReference<ArrayList<Playlist>>() {
                    });

        } catch (IOException e) {
            logger.error("exception importing playlists", e);
            logger.warn("If this persists, delete the file: '" + playlistFile.getAbsolutePath() + "'");
            return null;
        }
    }

    /**
     * save all liked songs
     *
     * @param likedSongs - constructed LikedSongs
     */
    public void exportLikedSongs(LikedSongs likedSongs) {
        if (likedSongs == null || likedSongs.isEmpty()) {
            return;
        }
        File likedSongFile = new File(Settings.dataFolder, Settings.likedSongFile + ".json");
        try {
            objectMapper.writeValue(likedSongFile, likedSongs);
            logger.debug("Saved liked songs to: '" + likedSongFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception saving liked songs", e);
        }
    }

    /**
     * import all saved liked songs
     *
     * @return - constructed LikedSongs
     */
    public LikedSongs importLikedSongs() {
        File likedSongFile = new File(Settings.dataFolder, Settings.likedSongFile + ".json");
        if (!likedSongFile.exists()) {
            return null;
        }
        try {
            return objectMapper.readValue(likedSongFile,
                    LikedSongs.class);
        } catch (IOException e) {
            logger.error("exception importing liked songs", e);
            logger.warn("If this persists, delete the file: '" + likedSongFile.getAbsolutePath() + "'");
            return null;
        }
    }

    /**
     * <p>
     * cacheAlbums.
     * </p>
     *
     * @param albums a {@link java.util.LinkedHashMap} object
     * @return a {@link java.util.LinkedHashMap} object
     */
    public LinkedHashMap<String, Album> cacheAlbums(LinkedHashMap<String, Album> albums) {
        File albumFile = new File(Settings.cacheFolder, Settings.albumFile + ".json");
        LinkedHashMap<String, Album> cachedAlbums = new LinkedHashMap<>();
        if (albumFile.exists()) {
            try {
                cachedAlbums = objectMapper.readValue(albumFile,
                        new TypeReference<LinkedHashMap<String, Album>>() {
                        });
                logger.debug("loaded cached albums from: '" + albumFile.getAbsolutePath() + "'");
            } catch (IOException e) {
                logger.error("exception importing cached albums", e);
                logger.warn("If this persists, delete the file: '" + albumFile.getAbsolutePath() + "'");
            }
        }
        cachedAlbums.putAll(albums);
        try {
            objectMapper.writeValue(albumFile, cachedAlbums);
            logger.debug("saved cached albums to: '" + albumFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception exporting cached albums", e);
        }
        return cachedAlbums;
    }

    /**
     * <p>
     * cacheArtists.
     * </p>
     *
     * @param artists a {@link java.util.LinkedHashMap} object
     * @return a {@link java.util.LinkedHashMap} object
     */
    public LinkedHashMap<String, Artist> cacheArtists(LinkedHashMap<String, Artist> artists) {
        File artistFile = new File(Settings.cacheFolder, Settings.artistFile + ".json");
        LinkedHashMap<String, Artist> cachedArtists = new LinkedHashMap<>();
        if (artistFile.exists()) {
            try {
                cachedArtists = objectMapper.readValue(artistFile,
                        new TypeReference<LinkedHashMap<String, Artist>>() {
                        });
                logger.debug("loaded cached artists from: " + artistFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("exception importing cached artists", e);
                logger.warn("If this persists, delete the file: '" + artistFile.getAbsolutePath() + "'");
            }
        }
        cachedArtists.putAll(artists);
        try {
            objectMapper.writeValue(artistFile, cachedArtists);
            logger.debug("saved cached artists to: '" + artistFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception exporting cached artists", e);
        }
        return cachedArtists;
    }

    /**
     * <p>
     * cacheSongs.
     * </p>
     *
     * @param songs a {@link java.util.LinkedHashMap} object
     * @return a {@link java.util.LinkedHashMap} object
     */
    public LinkedHashMap<String, Song> cacheSongs(LinkedHashMap<String, Song> songs) {
        File songFile = new File(Settings.cacheFolder, Settings.songFile + ".json");
        LinkedHashMap<String, Song> cachedSongs = new LinkedHashMap<>();
        if (songFile.exists()) {
            try {
                cachedSongs = objectMapper.readValue(songFile,
                        new TypeReference<LinkedHashMap<String, Song>>() {
                        });
                logger.debug("loaded cached songs from: '" + songFile.getAbsolutePath() + "'");
            } catch (IOException e) {
                logger.error("exception importing cached songs", e);
                logger.warn("If this persists, delete the file: '" + songFile.getAbsolutePath() + "'");
            }
        }
        cachedSongs.putAll(songs);
        try {
            objectMapper.writeValue(songFile, cachedSongs);
            logger.debug("saved cached songs to: '" + songFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception exporting cached songs", e);
        }
        return cachedSongs;
    }

    /**
     * <p>
     * cacheIds.
     * </p>
     *
     * @param ids a {@link java.util.LinkedHashMap} object
     * @return a {@link java.util.LinkedHashMap} object
     */
    public LinkedHashMap<String, String> cacheIds(LinkedHashMap<String, String> ids) {
        File idFile = new File(Settings.cacheFolder, "ids.json");
        LinkedHashMap<String, String> cachedIds = new LinkedHashMap<>();
        if (idFile.exists()) {
            try {
                cachedIds = objectMapper.readValue(idFile,
                        new TypeReference<LinkedHashMap<String, String>>() {
                        });
                logger.debug("loaded cached ids from: '" + idFile.getAbsolutePath() + "'");
            } catch (IOException e) {
                logger.error("exception importing cached ids", e);
                logger.warn("If this persists, delete the file: '" + idFile.getAbsolutePath() + "'");
            }
        }
        cachedIds.putAll(ids);
        try {
            objectMapper.writeValue(idFile, cachedIds);
            logger.debug("saved cached ids to: '" + idFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception exporting cached ids", e);
        }
        return ids;
    }
}
