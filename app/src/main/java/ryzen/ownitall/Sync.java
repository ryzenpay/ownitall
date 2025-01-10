package ryzen.ownitall;

import java.io.File;

import java.io.IOException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Sync {
    private static final Logger logger = LogManager.getLogger(Sync.class);
    private static final Settings settings = Settings.load();
    private File dataFolder;
    private File albumFile;
    private File playlistFile;
    private File likedSongsFile;
    private File spotifyFile;
    private File youtubeFile;
    ObjectMapper objectMapper;

    /**
     * initialize all files for syncronization
     * 
     */
    public Sync() {
        this.dataFolder = new File(settings.dataFolderPath);
        this.setDataFolder();
        this.albumFile = new File(this.dataFolder, settings.albumFile + ".json");
        this.playlistFile = new File(this.dataFolder, settings.playlistFile + ".json");
        this.likedSongsFile = new File(this.dataFolder, settings.likedSongFile + ".json");
        this.spotifyFile = new File(this.dataFolder, settings.spotifyCredentialsFile + ".json");
        this.youtubeFile = new File(this.dataFolder, settings.youtubeCredentialsFile + ".json");
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public static Sync load() {
        return new Sync();
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
    public static boolean checkDataFolder() { // TODO: move to sync (after settings setup as it needs to be static)
        Settings settings = Settings.load();
        File dataFolder = new File(settings.dataFolderPath);
        if (dataFolder.exists() && dataFolder.isDirectory()) {
            File albumFile = new File(settings.dataFolderPath, settings.albumFile + ".json");
            File playlistFile = new File(settings.dataFolderPath, settings.playlistFile + ".json");
            File likedSongsFile = new File(settings.dataFolderPath, settings.likedSongFile + ".json");
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
            if (!Input.getInstance().getBool()) {
                return;
            }
        }
        archiveFolder.mkdir();
        for (File file : this.dataFolder.listFiles()) {
            if (file.isFile()) {
                file.renameTo(new File(archiveFolder, file.getName()));
            }
        }
    }

    /**
     * create archive folder (current date) in dataPath and move all current files
     * to it with no user input
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
        int choice = -1;
        while (true) {
            System.out.println("Available archive folders: ");
            int i = 1;
            for (String fileName : archiveFolders.keySet()) {
                System.out.println("[" + i + "] " + fileName);
                i++;
            }
            System.out.println("[0] Cancel");
            System.out.print("Enter your choice: ");
            choice = Input.getInstance().getInt();
            if (choice < 0 || choice > archiveFolders.size()) {
                System.out.println("Incorrect option, try again");
            } else {
                break;
            }
        }
        ArrayList<String> options = new ArrayList<>(archiveFolders.keySet());
        options.add(0, "Cancel");
        String chosen = options.get(choice);
        if (chosen == "Cancel") {
            return;
        } else {
            archive(true);
            File unarchiveFolder = archiveFolders.get(chosen);
            for (File file : unarchiveFolder.listFiles()) {
                file.renameTo(new File(this.dataFolder, file.getName())); // TODO: this doesnt work, needs
                                                                          // java.nio.File.move() (cant overwrite)
            }
        }
        System.out.println("Restart the program to see the unarchived data");
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

    /**
     * import saved spotify credentials
     * 
     * @return - constructed SpotifyCredentials (use isNull to check)
     */
    public SpotifyCredentials importSpotifyCredentials() {
        this.setDataFolder();
        SpotifyCredentials spotifyCredentials;
        if (!spotifyFile.exists()) {
            return null;
        }
        try {
            spotifyCredentials = this.objectMapper.readValue(this.spotifyFile,
                    SpotifyCredentials.class);

        } catch (IOException e) {
            logger.error("Error importing Spotify Credentials: " + e);
            logger.info("If this persists, delete the file:" + this.spotifyFile.getAbsolutePath());
            return null;
        }
        return spotifyCredentials;
    }

    /**
     * save spotify credentials
     * 
     * @param spotifyCredentials - constructed SpotifyCredentials
     */
    public void exportSpotifyCredentials(SpotifyCredentials spotifyCredentials) {
        this.setDataFolder();
        try {
            this.objectMapper.writeValue(this.spotifyFile, spotifyCredentials);
        } catch (IOException e) {
            logger.error("Error saving spotify credentials: " + e);
        }
    }

    /**
     * import saved youtube credentials (use isNull to check)
     * 
     * @return - constructed YoutubeCredentials
     */
    public YoutubeCredentials importYoutubeCredentials() {
        this.setDataFolder();
        YoutubeCredentials youtubeCredentials;
        if (!youtubeFile.exists()) {
            return null;
        }
        try {
            youtubeCredentials = this.objectMapper.readValue(this.youtubeFile,
                    YoutubeCredentials.class);
        } catch (IOException e) {
            logger.error("Error importing Youtube Credentials: " + e);
            logger.info("If this persists, delete the file:" + this.youtubeFile.getAbsolutePath());
            return null;
        }
        return youtubeCredentials;
    }

    /**
     * save youtube credentials
     * 
     * @param youtubeCredentials - constructed YoutubeCredentials
     */
    public void exportYoutubeCredentials(YoutubeCredentials youtubeCredentials) {
        this.setDataFolder();
        try {
            this.objectMapper.writeValue(this.youtubeFile, youtubeCredentials);
        } catch (IOException e) {
            logger.error("Error saving youtube credentials: " + e);
        }
    }
}
