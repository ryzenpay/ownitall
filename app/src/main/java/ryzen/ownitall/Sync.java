package ryzen.ownitall;

import java.io.File;

import java.io.IOException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Sync {
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
     * @param dataPath - datapath of where to store data
     */
    public Sync(String dataPath) {
        this.dataFolder = new File(dataPath);
        this.setDataFolder();
        this.albumFile = new File(this.dataFolder, "albums.json");
        this.albumFile = new File(this.dataFolder, "albums.json");
        this.playlistFile = new File(this.dataFolder, "playlists.json");
        this.likedSongsFile = new File(this.dataFolder, "likedsongs.json");
        this.spotifyFile = new File(this.dataFolder, "spotifyCredentials.json");
        this.youtubeFile = new File(this.dataFolder, "youtubeCredentials.json");
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
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
     * create archive folder (current date) in dataPath and move all current files
     * to it
     */
    public void archive() { // TODO: implement into a menu
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
                file.renameTo(new File(archiveFolder, file.toString()));
            }
        }
    }

    /**
     * display a menu of all archived files (folders and dates) and give an option
     * to unarchive
     * also archives current files with todays date to prevent data loss
     */
    public void unarchive() {
        this.setDataFolder();
        ArrayList<File> archiveFolders = new ArrayList<>();
        for (File file : this.dataFolder.listFiles()) {
            if (file.isDirectory()) {
                archiveFolders.add(file);
            }
        }
        System.out.println("Available archive folders: ");
        for (int i = 0; i < archiveFolders.size(); i++) {
            System.out.println("[" + i + "] " + archiveFolders.get(i));
        }
        int choice = Input.getInstance().getInt();
        do {
            System.out.print("Your choice: ");
            choice = Input.getInstance().getInt();
        } while (choice < 1 || choice > archiveFolders.size());
        File unarchiveFolder = archiveFolders.get(choice);
        archive();
        for (File file : unarchiveFolder.listFiles()) {
            file.renameTo(new File(unarchiveFolder, file.toString()));
        }
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
            System.err.println("Error saving albums: " + e);
            e.printStackTrace();
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
            System.err.println("Error importing albums: " + e);
            e.printStackTrace();
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
            System.err.println("Error saving playlists: " + e);
            e.printStackTrace();
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
            System.err.println("Error importing playlists: " + e);
            e.printStackTrace();
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
            System.err.println("Error saving liked songs: " + e);
            e.printStackTrace();
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
            System.err.println("Error importing liked songs: " + e);
            e.printStackTrace();
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
            System.err.println("Error importing Spotify Credentials: " + e);
            e.printStackTrace();
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
            System.err.println("Error saving spotify credentials: " + e);
            e.printStackTrace();
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
            System.err.println("Error importing Youtube Credentials: " + e);
            e.printStackTrace();
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
            System.err.println("Error saving youtube credentials: " + e);
            e.printStackTrace();
        }
    }
}
