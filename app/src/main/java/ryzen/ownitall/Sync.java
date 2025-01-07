package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

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

    public Sync(String dataPath) {
        this.setDataFolder(dataPath);
        this.albumFile = new File(this.dataFolder, "albums.json");
        this.albumFile = new File(this.dataFolder, "albums.json");
        this.playlistFile = new File(this.dataFolder, "playlists.json");
        this.likedSongsFile = new File(this.dataFolder, "likedsongs.json");
        this.spotifyFile = new File(this.dataFolder, "spotifyCredentials.json");
        this.youtubeFile = new File(this.dataFolder, "youtubeCredentials.json");
        this.objectMapper = new ObjectMapper();
    }

    private void setDataFolder(String dataPath) {
        this.dataFolder = new File(dataPath);
        if (!this.dataFolder.exists()) { // create folder if it does not exist
            this.dataFolder.mkdirs();
        }
    }

    private void setDataFolder() {
        if (!this.dataFolder.exists()) { // create folder if it does not exist
            this.dataFolder.mkdirs();
        }
    }

    public File getDataFolder() {
        return this.dataFolder;
    }

    public void archive() { // TODO: implement into a menu
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

    public void unarchive() {
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

    public void exportAlbums(LinkedHashMap<Album, ArrayList<Song>> albums) {
        try {
            this.objectMapper.writeValue(this.albumFile, albums);
        } catch (IOException e) {
            System.err.println("Error saving albums: " + e);
        }
    }

    public LinkedHashMap<Album, ArrayList<Song>> importAlbums() {
        LinkedHashMap<Album, ArrayList<Song>> albums;
        if (!albumFile.exists()) {
            return null;
        }
        try {
            albums = this.objectMapper.readValue(this.albumFile,
                    new TypeReference<LinkedHashMap<Album, ArrayList<Song>>>() {
                    });

        } catch (IOException e) {
            System.err.println("Error importing albums: " + e);
            return null;
        }
        return albums;
    }

    public void exportPlaylists(LinkedHashMap<Playlist, ArrayList<Song>> playlists) {
        try {
            this.objectMapper.writeValue(this.playlistFile, playlists);
        } catch (IOException e) {
            System.err.println("Error saving playlists: " + e);
        }
    }

    public LinkedHashMap<Playlist, ArrayList<Song>> importPlaylists() {
        LinkedHashMap<Playlist, ArrayList<Song>> playlists;
        if (!playlistFile.exists()) {
            return null;
        }
        try {
            playlists = this.objectMapper.readValue(this.playlistFile,
                    new TypeReference<LinkedHashMap<Playlist, ArrayList<Song>>>() {
                    });

        } catch (IOException e) {
            System.err.println("Error importing playlists: " + e);
            return null;
        }
        return playlists;
    }

    public void exportLikedSongs(LikedSongs likedSongs) {
        try {
            this.objectMapper.writeValue(this.likedSongsFile, likedSongs);
        } catch (IOException e) {
            System.err.println("Error saving liked songs: " + e);
        }
    }

    public LikedSongs importLikedSongs() {
        LikedSongs likedSongs;
        if (!likedSongsFile.exists()) {
            return null;
        }
        try {
            likedSongs = this.objectMapper.readValue(this.likedSongsFile,
                    LikedSongs.class);

        } catch (IOException e) {
            System.err.println("Error importing liked songs: " + e);
            return null;
        }
        return likedSongs;
    }

    public SpotifyCredentials importSpotifyCredentials() {
        SpotifyCredentials spotifyCredentials;
        if (!spotifyFile.exists()) {
            return null;
        }
        try {
            spotifyCredentials = this.objectMapper.readValue(this.spotifyFile,
                    SpotifyCredentials.class);

        } catch (IOException e) {
            System.err.println("Error importing Spotify Credentials: " + e);
            return null;
        }
        return spotifyCredentials;
    }

    public void exportSpotifyCredentials(SpotifyCredentials spotifyCredentials) {
        try {
            this.objectMapper.writeValue(this.spotifyFile, spotifyCredentials);
        } catch (IOException e) {
            System.err.println("Error saving spotify credentials: " + e);
        }
    }

    public YoutubeCredentials importYoutubeCredentials() {
        YoutubeCredentials youtubeCredentials;
        if (!youtubeFile.exists()) {
            return null;
        }
        try {
            youtubeCredentials = this.objectMapper.readValue(this.youtubeFile,
                    YoutubeCredentials.class);
        } catch (IOException e) {
            System.err.println("Error importing Youtube Credentials: " + e);
            return null;
        }
        return youtubeCredentials;
    }

    public void exportYoutubeCredentials(YoutubeCredentials youtubeCredentials) {
        try {
            this.objectMapper.writeValue(this.youtubeFile, youtubeCredentials);
        } catch (IOException e) {
            System.err.println("Error saving youtube credentials: " + e);
        }
    }
}
