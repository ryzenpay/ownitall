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
import ryzen.ownitall.tools.Input;
import ryzen.ownitall.tools.Menu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Sync {
    private static final Logger logger = LogManager.getLogger(Sync.class);
    private static Settings settings = Settings.load();
    private static Sync instance;
    private File dataFolder;
    private File albumFile;
    private File playlistFile;
    private File likedSongsFile;
    ObjectMapper objectMapper;

    /**
     * initialize all files for syncronization
     * 
     */
    public Sync() {
        this.dataFolder = new File(settings.getDataFolderPath());
        this.setDataFolder();
        this.albumFile = new File(this.dataFolder, settings.getAlbumFile() + ".json");
        this.playlistFile = new File(this.dataFolder, settings.getPlaylistFile() + ".json");
        this.likedSongsFile = new File(this.dataFolder, settings.getLikedSongFile() + ".json");
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public static Sync load() {
        if (instance == null) {
            instance = new Sync();
        }
        return instance;
    }

    /**
     * function which is called to check if datafolder exists and create if deleted
     * in middle of process
     * for future improvements, use an interceptor but requires another class (bleh)
     * ^ or a dynamic proxy whatever
     */
    private void setDataFolder() {
        if (!this.dataFolder.exists()) { // create folder if it does not exist
            this.dataFolder.mkdirs();
        }
    }

    /**
     * check if existing data files exist
     * 
     * @return - true if exist, false if not
     */
    public boolean checkDataFolder() {
        File dataFolder = new File(settings.getDataFolderPath());
        if (dataFolder.exists() && dataFolder.isDirectory()) {
            File albumFile = new File(settings.getDataFolderPath(), settings.getAlbumFile() + ".json");
            File playlistFile = new File(settings.getDataFolderPath(), settings.getPlaylistFile() + ".json");
            File likedSongsFile = new File(settings.getDataFolderPath(), settings.getLikedSongFile() + ".json");
            if (albumFile.exists() && playlistFile.exists() && likedSongsFile.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * create archive folder (current date) in dataPath and move all current files
     * to it
     */
    public void archive() {
        this.setDataFolder();
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        File archiveFolder = new File(this.dataFolder, currentDate.format(formatter).toString());
        if (archiveFolder.exists()) {
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
            }
        }
        logger.info("Successfully archived music library to: " + archiveFolder.getAbsolutePath());
    }

    /**
     * create archive folder (current date) in dataPath and move all current files
     * to it with no user input
     * 
     * @param noUserInput - optional boolean to silence userinput
     */
    public void archive(boolean noUserInput) {
        this.setDataFolder();
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        File archiveFolder = new File(this.dataFolder, currentDate.format(formatter).toString());
        archiveFolder.mkdir();
        for (File file : this.dataFolder.listFiles()) {
            if (file.isFile()) {
                file.renameTo(new File(archiveFolder, file.getName()));
            }
        }
        logger.info("Successfully archived music library to: " + archiveFolder.getAbsolutePath());
        logger.info("Restart the program to refresh the library");
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
            archive(true);
            File unarchiveFolder = archiveFolders.get(choice);
            for (File file : unarchiveFolder.listFiles()) {
                file.renameTo(new File(this.dataFolder, file.getName())); // TODO: this doesnt work, needs
                                                                          // java.nio.File.move() (cant overwrite)
            }
        }
        logger.info("Successfully unarchived music library from"); // TODO: doesnt work because after closing program it
                                                                   // stores cached data
        logger.info("Restart the program to see the unarchived data");
    }

    public Collection importCollection() {
        ProgressBar pb = Main.progressBar("Opening Saved Data", 3);
        Collection collection = new Collection();
        pb.setExtraMessage("Albums");
        collection.mergeAlbums(this.importAlbums());
        pb.setExtraMessage("Playlists").step();
        collection.mergePlaylists(this.importPlaylists());
        pb.setExtraMessage("Liked Songs").step();
        collection.mergeLikedSongs(this.importLikedSongs());
        pb.setExtraMessage("Done");
        pb.close();
        return collection;
    }

    public void exportCollection(Collection collection) {
        ProgressBar pb = Main.progressBar("Saving Data", 3);
        pb.setExtraMessage("Albums");
        this.exportAlbums(collection.getAlbums());
        pb.setExtraMessage("Playlists").step();
        this.exportPlaylists(collection.getPlaylists());
        pb.setExtraMessage("Liked Songs").step();
        this.exportLikedSongs(collection.getLikedSongs());
        pb.setExtraMessage("Done");
        pb.close();
    }

    /**
     * save all albums
     * 
     * @param albums - linkedhashset of constructed Album
     */
    public void exportAlbums(LinkedHashSet<Album> albums) {
        this.setDataFolder();
        try {
            this.objectMapper.writeValue(this.albumFile, albums);
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
        LinkedHashSet<Album> albums;
        if (!albumFile.exists()) {
            return null;
        }
        try {
            albums = this.objectMapper.readValue(this.albumFile,
                    new TypeReference<LinkedHashSet<Album>>() {
                    });

        } catch (IOException e) {
            logger.error("Error importing albums: " + e);
            logger.info("If this persists, delete the file:" + this.albumFile.getAbsolutePath());
            return null;
        }
        return albums;
    }

    /**
     * save all playlists
     * 
     * @param playlists - linkedhashset of constructed Playlist
     */
    public void exportPlaylists(LinkedHashSet<Playlist> playlists) {
        this.setDataFolder();
        try {
            this.objectMapper.writeValue(this.playlistFile, playlists);
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
        LinkedHashSet<Playlist> playlists;
        if (!playlistFile.exists()) {
            return null;
        }
        try {
            playlists = this.objectMapper.readValue(this.playlistFile,
                    new TypeReference<LinkedHashSet<Playlist>>() {
                    });

        } catch (IOException e) {
            logger.error("Error importing playlists: " + e);
            logger.info("If this persists, delete the file:" + this.playlistFile.getAbsolutePath());
            return null;
        }
        return playlists;
    }

    /**
     * save all liked songs
     * 
     * @param likedSongs - constructed LikedSongs
     */
    public void exportLikedSongs(LikedSongs likedSongs) {
        this.setDataFolder();
        try {
            this.objectMapper.writeValue(this.likedSongsFile, likedSongs);
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
        LikedSongs likedSongs;
        if (!likedSongsFile.exists()) {
            return null;
        }
        try {
            likedSongs = this.objectMapper.readValue(this.likedSongsFile,
                    LikedSongs.class);

        } catch (IOException e) {
            logger.error("Error importing liked songs: " + e);
            logger.info("If this persists, delete the file:" + this.likedSongsFile.getAbsolutePath());
            return null;
        }
        return likedSongs;
    }
}
