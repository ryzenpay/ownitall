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
import ryzen.ownitall.output.cli.ProgressBar;
import ryzen.ownitall.util.Input;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Storage {
    private static final Logger logger = LogManager.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    /**
     * function which is called to check if datafolder exists and create if deleted
     * in middle of process
     */
    private static void setDataFolder() {
        if (!Settings.dataFolder.exists()) { // create folder if it does not exist
            Settings.dataFolder.mkdirs();
            logger.debug("Created datafolder '" + Settings.dataFolder.getAbsolutePath() + "'");
        }
    }

    /**
     * set cache folder
     */
    private static void setCacheFolder() {
        if (!Settings.cacheFolder.exists()) {
            Settings.cacheFolder.mkdirs();
            logger.debug("Created cache folder: '" + Settings.cacheFolder.getAbsolutePath() + "'");
        }
    }

    /**
     * create archive folder (current date) in dataPath and move all current files
     * to it with no user input
     */
    public static void archive() {
        setDataFolder();
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
        logger.info("Successfully archived music library to: '" + archiveFolder.getAbsolutePath() + "'");
    }

    public static LinkedHashSet<File> getArchiveFolders() {
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
     */
    public static void unArchive(File unarchiveFolder) {
        if (unarchiveFolder == null || !unarchiveFolder.exists()) {
            logger.debug("null or non existant unarchive folder provided in unarchive");
            return;
        }
        setDataFolder();
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
        logger.info("Deleted old archive folder: '" + unarchiveFolder.getAbsolutePath() + "'");
        Collection.clear();
        importCollection();
        logger.info("Successfully unarchived music library");
    }

    /**
     * clear cache files
     */
    public static void clearCacheFiles() {
        setCacheFolder();
        for (File file : Settings.cacheFolder.listFiles()) {
            file.delete();
            logger.info("Deleted file: '" + file.getAbsolutePath() + "'");
        }
    }

    public static void clearInventoryFiles() {
        setDataFolder();
        for (File file : Settings.dataFolder.listFiles()) {
            if (file.isFile()) {
                file.delete();
                logger.info("Deleted file: '" + file.getAbsolutePath() + "'");
            }
        }
    }

    /**
     * import collection from files
     * orchestrates import albums, playlists and liked songs
     * 
     */
    public static void importCollection() {
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
     * 
     */
    public static void exportCollection() {
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
    public static void exportAlbums(ArrayList<Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return;
        }
        setDataFolder();
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
    public static ArrayList<Album> importAlbums() {
        setDataFolder();
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
    public static void exportPlaylists(ArrayList<Playlist> playlists) {
        if (playlists == null || playlists.isEmpty()) {
            return;
        }
        setDataFolder();
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
    public static ArrayList<Playlist> importPlaylists() {
        setDataFolder();
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
    public static void exportLikedSongs(LikedSongs likedSongs) {
        if (likedSongs == null || likedSongs.isEmpty()) {
            return;
        }
        setDataFolder();
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
    public static LikedSongs importLikedSongs() {
        setDataFolder();
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

    public static LinkedHashMap<String, Album> cacheAlbums(LinkedHashMap<String, Album> albums) {
        setCacheFolder();
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

    public static LinkedHashMap<String, Artist> cacheArtists(LinkedHashMap<String, Artist> artists) {
        setCacheFolder();
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

    public static LinkedHashMap<String, Song> cacheSongs(LinkedHashMap<String, Song> songs) {
        setCacheFolder();
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

    public static LinkedHashMap<String, String> cacheIds(LinkedHashMap<String, String> ids) {
        setCacheFolder();
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
