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
    private static Settings settings = Settings.load();
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
        }
    }

    /**
     * set cache folder
     */
    private void setCacheFolder() {
        if (!this.cacheFolder.exists()) {
            this.cacheFolder.mkdirs();
        }
    }

    /**
     * check if existing data files exist
     * 
     * @return - true if exist, false if not
     */
    public boolean checkDataFolder() {
        File dataFolder = new File(settings.dataFolderPath);
        if (dataFolder.exists() && dataFolder.isDirectory()) {
            return true;
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
        Collection.load().clear();
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
            archive(true);
            File unarchiveFolder = archiveFolders.get(choice);
            for (File file : unarchiveFolder.listFiles()) {
                if (file.isFile()) {
                    File destFile = new File(this.dataFolder, file.getName());
                    destFile.delete();
                    new File(this.dataFolder, file.getName()).delete(); // delete the file about to be overwritten
                    file.renameTo(destFile);
                }
            }
            unarchiveFolder.delete();
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
        }
    }

    /**
     * import collection from files
     * orchestrates import albums, playlists and liked songs
     * 
     * @return - constructed collection
     */
    public Collection importCollection() {
        ProgressBar pb = Progressbar.progressBar("Opening Saved Data", 3);
        Collection collection = new Collection();
        pb.setExtraMessage("Albums");
        collection.addAlbums(this.importAlbums());
        pb.setExtraMessage("Playlists").step();
        collection.addPlaylists(this.importPlaylists());
        pb.setExtraMessage("Liked Songs").step();
        collection.addLikedSongs(this.importLikedSongs().getSongs());
        pb.setExtraMessage("Done").step();
        pb.close();
        return collection;
    }

    /**
     * save collection to local files
     * orchestrates export albums, playlists and liked songs
     * 
     * @param collection - constructed collection to save
     */
    public void exportCollection(Collection collection) {
        ProgressBar pb = Progressbar.progressBar("Saving Data", 3);
        pb.setExtraMessage("Albums");
        this.exportAlbums(collection.getAlbums());
        pb.setExtraMessage("Playlists").step();
        this.exportPlaylists(collection.getPlaylists());
        pb.setExtraMessage("Liked Songs").step();
        this.exportLikedSongs(collection.getLikedSongs());
        pb.setExtraMessage("Done").step();
        pb.close();
    }

    /**
     * save all albums
     * 
     * @param albums - linkedhashset of constructed Album
     */
    public void exportAlbums(LinkedHashSet<Album> albums) {
        this.setDataFolder();
        File albumFile = new File(this.dataFolder, settings.albumFile + ".json");
        try {
            this.objectMapper.writeValue(albumFile, albums);
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
        LinkedHashSet<Album> albums = new LinkedHashSet<>();
        if (!albumFile.exists()) {
            return albums;
        }
        try {
            albums = this.objectMapper.readValue(albumFile,
                    new TypeReference<LinkedHashSet<Album>>() {
                    });

        } catch (IOException e) {
            logger.error("Error importing albums: " + e);
            logger.info("If this persists, delete the file:" + albumFile.getAbsolutePath());
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
        File playlistFile = new File(this.dataFolder, settings.playlistFile + ".json");
        try {
            this.objectMapper.writeValue(playlistFile, playlists);
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
        LinkedHashSet<Playlist> playlists = new LinkedHashSet<>();
        if (!playlistFile.exists()) {
            return playlists;
        }
        try {
            playlists = this.objectMapper.readValue(playlistFile,
                    new TypeReference<LinkedHashSet<Playlist>>() {
                    });

        } catch (IOException e) {
            logger.error("Error importing playlists: " + e);
            logger.info("If this persists, delete the file:" + playlistFile.getAbsolutePath());
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
        File likedSongFile = new File(this.dataFolder, settings.likedSongFile + ".json");
        try {
            this.objectMapper.writeValue(likedSongFile, likedSongs);
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
        LikedSongs likedSongs = new LikedSongs();
        if (!likedSongFile.exists()) {
            return likedSongs;
        }
        try {
            likedSongs = this.objectMapper.readValue(likedSongFile,
                    LikedSongs.class);

        } catch (IOException e) {
            logger.error("Error importing liked songs: " + e);
            logger.info("If this persists, delete the file:" + likedSongFile.getAbsolutePath());
            return null;
        }
        return likedSongs;
    }

    /**
     * cache artists locally
     * ^ syncs with local files
     * 
     * @param artists - linkedhashset to cache
     * @return - updated linkedhashset of artists
     */
    public LinkedHashSet<Artist> cacheArtists(LinkedHashSet<Artist> artists) {
        this.setCacheFolder();
        File artistFile = new File(this.cacheFolder, settings.artistFile + ".json");
        LinkedHashSet<Artist> cachedArtists = new LinkedHashSet<>();
        if (artistFile.exists()) {
            try {
                cachedArtists = this.objectMapper.readValue(artistFile, new TypeReference<LinkedHashSet<Artist>>() {
                });
            } catch (IOException e) {
                logger.error("Error importing Library Artists: " + e);
                logger.info("If this persists, delete the file: " + artistFile.getAbsolutePath());
            }
        }
        cachedArtists.addAll(artists);
        try {
            this.objectMapper.writeValue(artistFile, cachedArtists);
        } catch (IOException e) {
            logger.error("Error exporting Library Artists: " + e);
        }
        return cachedArtists;
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
            } catch (IOException e) {
                logger.error("Error importing Library Songs: " + e);
                logger.info("If this persists, delete the file: " + songFile.getAbsolutePath());
            }
        }
        cachedSongs.addAll(songs);
        try {
            this.objectMapper.writeValue(songFile, cachedSongs);
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
            } catch (IOException e) {
                logger.error("Error importing Library Albums: " + e);
                logger.info("If this persists, delete the file: " + albumFile.getAbsolutePath());
            }
        }
        cachedAlbums.addAll(albums);
        try {
            this.objectMapper.writeValue(albumFile, cachedAlbums);
        } catch (IOException e) {
            logger.error("Error exporting Library Albums: " + e);
        }
        return cachedAlbums;
    }
}
