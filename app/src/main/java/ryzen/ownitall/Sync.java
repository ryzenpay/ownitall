package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Sync {
    private static final Logger logger = LogManager.getLogger(Sync.class);
    private static final Settings settings = Settings.load();
    private static Sync instance;
    private File dataFolder;
    private File cacheFolder;
    ObjectMapper objectMapper;

    /**
     * initialize all files for syncronization
     * 
     */
    public Sync() {
        this.dataFolder = new File(settings.dataFolderPath);
        this.cacheFolder = new File(settings.cacheFolderPath);
        this.setDataFolder();
        this.setCacheFolder();
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    /**
     * set instance
     * 
     * @return - new or existing instance
     */
    public static Sync load() {
        if (instance == null) {
            instance = new Sync();
            logger.debug("New instance created");
        }
        return instance;
    }

    /**
     * function which is called to check if datafolder exists and create if deleted
     * in middle of process
     */
    private void setDataFolder() {
        if (!this.dataFolder.exists()) { // create folder if it does not exist
            this.dataFolder.mkdirs();
            logger.debug("Created datafolder '" + this.dataFolder.getAbsolutePath() + "'");
        }
    }

    /**
     * set cache folder
     */
    private void setCacheFolder() {
        if (!this.cacheFolder.exists()) {
            this.cacheFolder.mkdirs();
            logger.debug("Created cache folder: '" + this.cacheFolder.getAbsolutePath() + "'");
        }
    }

    /**
     * create archive folder (current date) in dataPath and move all current files
     * to it with no user input
     * 
     * @param userInput - optional boolean to hardcode user input
     */
    public void archive(boolean userInput) {
        this.setDataFolder();
        LocalDate currentDate = LocalDate.now();
        String folderName = currentDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        File archiveFolder = new File(this.dataFolder, folderName);
        if (archiveFolder.exists() && userInput) {
            try {
                System.out.println(
                        "You are about to overwrite the contents of the folder: '" + archiveFolder.getAbsolutePath()
                                + "'");
                System.out.print("Are you sure y/N: ");
                if (!Input.request().getAgreement()) {
                    return;
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting archive overwrite agreement");
                return;
            }
        }
        archiveFolder.mkdir();
        for (File file : this.dataFolder.listFiles()) {
            if (file.isFile()) {
                file.renameTo(new File(archiveFolder, file.getName()));
                logger.debug("Renamed file: '" + file.getAbsolutePath() + "'");
            }
        }
        Collection.load().clear();
        logger.info("Successfully archived music library to: '" + archiveFolder.getAbsolutePath() + "'");
    }

    /**
     * display a menu of all archived files (folders and dates) and give an option
     * to unarchive
     * also archives current files with todays date to prevent data loss
     */
    public void unArchive() {
        this.setDataFolder();
        LinkedHashMap<String, File> archiveFolders = new LinkedHashMap<>();
        for (File file : this.dataFolder.listFiles()) {
            if (file.isDirectory()) {
                archiveFolders.put(file.getName(), file);
            }
        }
        String choice;
        try {
            choice = Menu.optionMenu(archiveFolders.keySet(), "UNARCHIVING");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting unarchive folder choice");
            return;
        }
        if (choice.equals("Exit")) {
            return;
        } else {
            archive(false);
            File unarchiveFolder = archiveFolders.get(choice);
            for (File file : unarchiveFolder.listFiles()) {
                if (file.isFile()) {
                    File destFile = new File(this.dataFolder, file.getName());
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
        }
        Collection collection = Collection.load();
        collection.clear();
        this.importCollection();
        logger.info("Successfully unarchived music library");
    }

    /**
     * clear cache files
     */
    public void clearCache() {
        this.setCacheFolder();
        for (File file : this.cacheFolder.listFiles()) {
            file.delete();
            logger.info("Deleted file: '" + file.getAbsolutePath() + "'");
        }
    }

    public void clearInventory() {
        this.setDataFolder();
        for (File file : this.dataFolder.listFiles()) {
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
     * @return - constructed collection
     */
    public void importCollection() {
        Collection collection = Collection.load();
        logger.debug("importing collection... ");
        try (ProgressBar pb = Progressbar.progressBar("Opening Saved Data", 3)) {
            pb.setExtraMessage("Albums");
            collection.addAlbums(importAlbums());
            pb.setExtraMessage("Playlists").step();
            collection.addPlaylists(importPlaylists());
            pb.setExtraMessage("Liked Songs").step();
            LikedSongs likedSongs = importLikedSongs();
            if (likedSongs != null) {
                collection.addLikedSongs(likedSongs);
            }
            pb.setExtraMessage("Done").step();
            logger.debug("Successfully imported collection");
        }
    }

    /**
     * save collection to local files
     * orchestrates export albums, playlists and liked songs
     * 
     */
    public void exportCollection() {
        Collection collection = Collection.load();
        logger.debug("Exporting music collection...");
        try (ProgressBar pb = Progressbar.progressBar("Saving Data", 3)) {
            pb.setExtraMessage("Albums");
            exportAlbums(collection.getAlbums());
            pb.setExtraMessage("Playlists").step();
            exportPlaylists(collection.getPlaylists());
            pb.setExtraMessage("Liked Songs").step();
            exportLikedSongs(collection.getLikedSongs());
            pb.setExtraMessage("Done").step();
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
        this.setDataFolder();
        File albumFile = new File(this.dataFolder, settings.albumFile + ".json");
        try {
            this.objectMapper.writeValue(albumFile, albums);
            logger.debug("Saved albums to: '" + albumFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception saving albums: " + e);
        }
    }

    /**
     * import saved albums
     * 
     * @return - linkedhashset of constructed Album
     */
    public ArrayList<Album> importAlbums() {
        this.setDataFolder();
        File albumFile = new File(this.dataFolder, settings.albumFile + ".json");
        if (!albumFile.exists()) {
            return null;
        }
        try {
            return this.objectMapper.readValue(albumFile,
                    new TypeReference<ArrayList<Album>>() {
                    });
        } catch (IOException e) {
            logger.error("exception importing albums: " + e);
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
        this.setDataFolder();
        File playlistFile = new File(this.dataFolder, settings.playlistFile + ".json");
        try {
            this.objectMapper.writeValue(playlistFile, playlists);
            logger.debug("Saved playlists to: '" + playlistFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception saving playlists: " + e);
        }
    }

    /**
     * import all saved playlists
     * 
     * @return - linkedhashset of constructed Playlist
     */
    public ArrayList<Playlist> importPlaylists() {
        this.setDataFolder();
        File playlistFile = new File(this.dataFolder, settings.playlistFile + ".json");
        if (!playlistFile.exists()) {
            return null;
        }
        try {
            return this.objectMapper.readValue(playlistFile,
                    new TypeReference<ArrayList<Playlist>>() {
                    });

        } catch (IOException e) {
            logger.error("exception importing playlists: " + e);
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
        if (likedSongs == null || likedSongs.size() == 0) {
            return;
        }
        this.setDataFolder();
        File likedSongFile = new File(this.dataFolder, settings.likedSongFile + ".json");
        try {
            this.objectMapper.writeValue(likedSongFile, likedSongs);
            logger.debug("Saved liked songs to: '" + likedSongFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception saving liked songs: " + e);
        }
    }

    /**
     * import all saved liked songs
     * 
     * @return - constructed LikedSongs
     */
    public LikedSongs importLikedSongs() {
        this.setDataFolder();
        File likedSongFile = new File(this.dataFolder, settings.likedSongFile + ".json");
        if (!likedSongFile.exists()) {
            return null;
        }
        try {
            return this.objectMapper.readValue(likedSongFile,
                    LikedSongs.class);
        } catch (IOException e) {
            logger.error("exception importing liked songs: " + e);
            logger.warn("If this persists, delete the file: '" + likedSongFile.getAbsolutePath() + "'");
            return null;
        }
    }

    public LinkedHashMap<String, Album> cacheAlbums(LinkedHashMap<String, Album> albums) {
        this.setCacheFolder();
        File albumFile = new File(this.cacheFolder, settings.albumFile + ".json");
        LinkedHashMap<String, Album> cachedAlbums = new LinkedHashMap<>();
        if (albumFile.exists()) {
            try {
                cachedAlbums = this.objectMapper.readValue(albumFile,
                        new TypeReference<LinkedHashMap<String, Album>>() {
                        });
                logger.debug("loaded cached albums from: '" + albumFile.getAbsolutePath() + "'");
            } catch (IOException e) {
                logger.error("exception importing cached albums: " + e);
                logger.warn("If this persists, delete the file: '" + albumFile.getAbsolutePath() + "'");
            }
        }
        cachedAlbums.putAll(albums);
        try {
            this.objectMapper.writeValue(albumFile, cachedAlbums);
            logger.debug("saved cached albums to: '" + albumFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception exporting cached albums: " + e);
        }
        return cachedAlbums;
    }

    public LinkedHashMap<String, Artist> cacheArtists(LinkedHashMap<String, Artist> artists) {
        this.setCacheFolder();
        File artistFile = new File(this.cacheFolder, settings.artistFile + ".json");
        LinkedHashMap<String, Artist> cachedArtists = new LinkedHashMap<>();
        if (artistFile.exists()) {
            try {
                cachedArtists = this.objectMapper.readValue(artistFile,
                        new TypeReference<LinkedHashMap<String, Artist>>() {
                        });
                logger.debug("loaded cached artists from: " + artistFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("exception importing cached artists: " + e);
                logger.warn("If this persists, delete the file: '" + artistFile.getAbsolutePath() + "'");
            }
        }
        cachedArtists.putAll(artists);
        try {
            this.objectMapper.writeValue(artistFile, cachedArtists);
            logger.debug("saved cached artists to: '" + artistFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception exporting cached artists: " + e);
        }
        return cachedArtists;
    }

    public LinkedHashMap<String, Song> cacheSongs(LinkedHashMap<String, Song> songs) {
        this.setCacheFolder();
        File songFile = new File(this.cacheFolder, settings.songFile + ".json");
        LinkedHashMap<String, Song> cachedSongs = new LinkedHashMap<>();
        if (songFile.exists()) {
            try {
                cachedSongs = this.objectMapper.readValue(songFile,
                        new TypeReference<LinkedHashMap<String, Song>>() {
                        });
                logger.debug("loaded cached songs from: '" + songFile.getAbsolutePath() + "'");
            } catch (IOException e) {
                logger.error("exception importing cached songs: " + e);
                logger.warn("If this persists, delete the file: '" + songFile.getAbsolutePath() + "'");
            }
        }
        cachedSongs.putAll(songs);
        try {
            this.objectMapper.writeValue(songFile, cachedSongs);
            logger.debug("saved cached songs to: '" + songFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception exporting cached songs: " + e);
        }
        return cachedSongs;
    }

    public LinkedHashMap<String, String> cacheIds(LinkedHashMap<String, String> ids) {
        this.setCacheFolder();
        File idFile = new File(this.cacheFolder, "ids.json");
        LinkedHashMap<String, String> cachedIds = new LinkedHashMap<>();
        if (idFile.exists()) {
            try {
                cachedIds = this.objectMapper.readValue(idFile,
                        new TypeReference<LinkedHashMap<String, String>>() {
                        });
                logger.debug("loaded cached ids from: '" + idFile.getAbsolutePath() + "'");
            } catch (IOException e) {
                logger.error("exception importing cached ids: " + e);
                logger.warn("If this persists, delete the file: '" + idFile.getAbsolutePath() + "'");
            }
        }
        cachedIds.putAll(ids);
        try {
            this.objectMapper.writeValue(idFile, cachedIds);
            logger.debug("saved cached ids to: '" + idFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            logger.error("exception exporting cached ids: " + e);
        }
        return ids;
    }
}
