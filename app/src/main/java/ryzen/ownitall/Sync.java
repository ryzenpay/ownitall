package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.classes.Album;
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
            logger.debug("Created datafolder " + this.dataFolder.getAbsolutePath());
        }
    }

    /**
     * set cache folder
     */
    private void setCacheFolder() {
        if (!this.cacheFolder.exists()) {
            this.cacheFolder.mkdirs();
            logger.debug("Created cache folder: " + this.cacheFolder.getAbsolutePath());
        }
    }

    /**
     * create archive folder (current date) in dataPath and move all current files
     * to it with no user input
     * 
     * @param noUserInput - optional boolean to silence userinput
     */
    public void archive(boolean userInput) {
        this.setDataFolder();
        LocalDate currentDate = LocalDate.now();
        String folderName = currentDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        File archiveFolder = new File(this.dataFolder, folderName);
        if (archiveFolder.exists() && !userInput) {
            System.out.println(
                    "You are about to overwrite the contents of the folder: " + archiveFolder.getAbsolutePath());
            System.out.print("Are you sure y/N: ");
            if (!Input.request().getAgreement()) {
                return;
            }
        }
        archiveFolder.mkdir();
        for (File file : this.dataFolder.listFiles()) {
            if (file.isFile()) {
                file.renameTo(new File(archiveFolder, file.getName()));
                logger.debug("Renamed file: " + file.getAbsolutePath());
            }
        }
        Collection.load().clear();
        logger.info("Successfully archived music library to: " + archiveFolder.getAbsolutePath());
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
        String choice = Menu.optionMenu(archiveFolders.keySet(), "UNARCHIVING");
        if (choice.equals("Exit")) {
            return;
        } else {
            archive(false);
            File unarchiveFolder = archiveFolders.get(choice);
            for (File file : unarchiveFolder.listFiles()) {
                if (file.isFile()) {
                    File destFile = new File(this.dataFolder, file.getName());
                    if (destFile.exists()) {
                        destFile.delete();
                        logger.debug("deleted old file: " + destFile.getAbsolutePath());
                    }
                    file.renameTo(destFile);
                }
            }
            unarchiveFolder.delete();
            logger.debug("Deleted old archive folder: " + unarchiveFolder.getAbsolutePath());
        }
        Collection collection = Collection.load();
        collection.clear();
        collection.mergeCollection(this.importCollection());
        logger.info("Successfully unarchived music library");
    }

    /**
     * clear cache files
     */
    public void clearCache() {
        this.setCacheFolder();
        for (File file : this.cacheFolder.listFiles()) {
            file.delete();
            logger.debug("Deleted file: " + file.getAbsolutePath());
        }
    }

    /**
     * import collection from files
     * orchestrates import albums, playlists and liked songs
     * 
     * @return - constructed collection
     */
    public Collection importCollection() {
        Collection collection = new Collection();
        try (ProgressBar pb = Progressbar.progressBar("Opening Saved Data", 3)) {
            pb.setExtraMessage("Albums");
            collection.addAlbums(importAlbums());
            pb.setExtraMessage("Playlists").step();
            collection.addPlaylists(importPlaylists());
            pb.setExtraMessage("Liked Songs").step();
            LikedSongs likedSongs = importLikedSongs();
            if (likedSongs != null) {
                collection.addLikedSongs(likedSongs.getSongs());
            }
            pb.setExtraMessage("Done").step();
        }
        return collection;
    }

    /**
     * save collection to local files
     * orchestrates export albums, playlists and liked songs
     * 
     * @param collection - constructed collection to save
     */
    public void exportCollection(Collection collection) {
        try (ProgressBar pb = Progressbar.progressBar("Saving Data", 3)) {
            pb.setExtraMessage("Albums");
            exportAlbums(collection.getAlbums());
            pb.setExtraMessage("Playlists").step();
            exportPlaylists(collection.getPlaylists());
            pb.setExtraMessage("Liked Songs").step();
            exportLikedSongs(collection.getLikedSongs());
            pb.setExtraMessage("Done").step();
        }
    }

    /**
     * save all albums
     * 
     * @param albums - linkedhashset of constructed Album
     */
    public void exportAlbums(LinkedHashSet<Album> albums) {
        if (albums == null) {
            return;
        }
        this.setDataFolder();
        File albumFile = new File(this.dataFolder, settings.albumFile + ".json");
        try {
            this.objectMapper.writeValue(albumFile, albums);
            logger.debug("Saved albums to: " + albumFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error saving albums: " + e);
        }
    }

    /**
     * import saved albums
     * 
     * @return - linkedhashset of constructed Album
     */
    public LinkedHashSet<Album> importAlbums() {
        this.setDataFolder();
        File albumFile = new File(this.dataFolder, settings.albumFile + ".json");
        if (!albumFile.exists()) {
            return null;
        }
        try {
            return this.objectMapper.readValue(albumFile,
                    new TypeReference<LinkedHashSet<Album>>() {
                    });
        } catch (IOException e) {
            logger.error("Error importing albums: " + e);
            logger.info("If this persists, delete the file:" + albumFile.getAbsolutePath());
            return null;
        }
    }

    /**
     * save all playlists
     * 
     * @param playlists - linkedhashset of constructed Playlist
     */
    public void exportPlaylists(LinkedHashSet<Playlist> playlists) {
        if (playlists == null) {
            return;
        }
        this.setDataFolder();
        File playlistFile = new File(this.dataFolder, settings.playlistFile + ".json");
        try {
            this.objectMapper.writeValue(playlistFile, playlists);
            logger.debug("Saved playlists to: " + playlistFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error saving playlists: " + e);
        }
    }

    /**
     * import all saved playlists
     * 
     * @return - linkedhashset of constructed Playlist
     */
    public LinkedHashSet<Playlist> importPlaylists() {
        this.setDataFolder();
        File playlistFile = new File(this.dataFolder, settings.playlistFile + ".json");
        if (!playlistFile.exists()) {
            return null;
        }
        try {
            return this.objectMapper.readValue(playlistFile,
                    new TypeReference<LinkedHashSet<Playlist>>() {
                    });

        } catch (IOException e) {
            logger.error("Error importing playlists: " + e);
            logger.info("If this persists, delete the file:" + playlistFile.getAbsolutePath());
            return null;
        }
    }

    /**
     * save all liked songs
     * 
     * @param likedSongs - constructed LikedSongs
     */
    public void exportLikedSongs(LikedSongs likedSongs) {
        if (likedSongs == null) {
            return;
        }
        this.setDataFolder();
        File likedSongFile = new File(this.dataFolder, settings.likedSongFile + ".json");
        try {
            this.objectMapper.writeValue(likedSongFile, likedSongs);
            logger.debug("Saved liked songs to: " + likedSongFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error saving liked songs: " + e);
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
            logger.error("Error importing liked songs: " + e);
            logger.info("If this persists, delete the file:" + likedSongFile.getAbsolutePath());
            return null;
        }
    }

    /**
     * cache songs
     * ^ syncs with local files
     * 
     * @param songs - linkedhashset to offload
     * @return - updated linkedhashset of songs
     */
    public LinkedHashSet<Song> cacheSongs(LinkedHashSet<Song> songs) {
        this.setCacheFolder();
        File songFile = new File(this.cacheFolder, settings.songFile + ".json");
        LinkedHashSet<Song> cachedSongs = new LinkedHashSet<>();
        if (songFile.exists()) {
            try {
                cachedSongs = this.objectMapper.readValue(songFile, new TypeReference<LinkedHashSet<Song>>() {
                });
                logger.debug("loaded cached songs from: " + songFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Error importing Library Songs: " + e);
                logger.info("If this persists, delete the file: " + songFile.getAbsolutePath());
            }
        }
        cachedSongs.addAll(songs);
        try {
            this.objectMapper.writeValue(songFile, cachedSongs);
            logger.debug("saved cached songs to: " + songFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error exporting Library Songs: " + e);
        }
        return cachedSongs;
    }

    /**
     * cache albums
     * ^ syncs with local files
     * 
     * @param albums - linkedhashset to offload
     * @return - updated linkedhashset of albums
     */
    public LinkedHashSet<Album> cacheAlbums(LinkedHashSet<Album> albums) {
        this.setCacheFolder();
        File albumFile = new File(this.cacheFolder, settings.albumFile + ".json");
        LinkedHashSet<Album> cachedAlbums = new LinkedHashSet<>();
        if (albumFile.exists()) {
            try {
                cachedAlbums = this.objectMapper.readValue(albumFile, new TypeReference<LinkedHashSet<Album>>() {
                });
                logger.debug("loaded cached albums from: " + albumFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Error importing Library Albums: " + e);
                logger.info("If this persists, delete the file: " + albumFile.getAbsolutePath());
            }
        }
        cachedAlbums.addAll(albums);
        try {
            this.objectMapper.writeValue(albumFile, cachedAlbums);
            logger.debug("saved cached albums to: " + albumFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error exporting Library Albums: " + e);
        }
        return cachedAlbums;
    }
}
