package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Sync implements AutoCloseable {
    private File dataFolder;
    private File albumFile;
    private File playlistFile;
    private File likedSongsFile;
    private File spotifyFile;
    private File youtubeFile;

    public Sync(String dataPath) {
        this.setDataFolder(dataPath);
        this.albumFile = new File(this.dataFolder, "albums.ser");
        this.albumFile = new File(this.dataFolder, "albums.ser");
        this.playlistFile = new File(this.dataFolder, "playlists.ser");
        this.likedSongsFile = new File(this.dataFolder, "likedsongs.ser");
        this.spotifyFile = new File(this.dataFolder, "spotifyCredentials.txt");
        this.youtubeFile = new File(this.dataFolder, "youtubeCredentials.txt");
    }

    @Override
    public void close() {
        // Close any open resources
        closeFile(this.albumFile);
        closeFile(this.playlistFile);
        closeFile(this.likedSongsFile);
        closeFile(this.spotifyFile);
        closeFile(this.youtubeFile);

        // Perform any additional cleanup
        System.out.println("Sync resources closed successfully.");
    }

    private void closeFile(File file) {
        if (file != null && file.exists()) {
            try {
                // Attempt to close the file if it's open
                // This is a simplified approach; in a real scenario, you might need to track
                // open file handles
                new FileInputStream(file).close();
            } catch (IOException e) {
                System.err.println("Error closing file: " + file.getName() + " - " + e.getMessage());
            }
        }
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
        try (ObjectOutputStream albumOutput = new ObjectOutputStream(
                new FileOutputStream(this.albumFile))) {
            albumOutput.writeObject(albums);
            System.out.println("Successfully saved " + albums.size() + " albums");
        } catch (FileNotFoundException e) {
            this.setDataFolder();
        } catch (IOException e) {
            System.err.println("Error Saving Albums: " + e);
        }
    }

    public LinkedHashMap<Album, ArrayList<Song>> importAlbums() {
        LinkedHashMap<Album, ArrayList<Song>> albums;
        if (!albumFile.exists()) {
            return null;
        }
        try (ObjectInputStream albumInput = new ObjectInputStream(
                new FileInputStream(this.albumFile))) {
            albums = (LinkedHashMap<Album, ArrayList<Song>>) albumInput.readObject();
            System.out.println("Successfully imported " + albums.size() + " albums");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error Importing Albums: " + e);
            System.err.println("If this persists, delete: " + this.albumFile.getAbsolutePath());
            return null;
        }
        return albums;
    }

    public void exportPlaylists(LinkedHashMap<Playlist, ArrayList<Song>> playlists) {
        try (ObjectOutputStream playlistOutput = new ObjectOutputStream(
                new FileOutputStream(this.playlistFile))) {
            playlistOutput.writeObject(playlists);
            System.out.println("Successfully saved " + playlists.size() + " playlists");

        } catch (FileNotFoundException e) {
            this.setDataFolder(); // since they are being exported its gotta exist
        } catch (IOException e) {
            System.err.println("Error Saving Playlists: " + e);
        }
    }

    public LinkedHashMap<Playlist, ArrayList<Song>> importPlaylists() {
        LinkedHashMap<Playlist, ArrayList<Song>> playlists;
        if (!playlistFile.exists()) {
            return null;
        }
        try (ObjectInputStream playlistInput = new ObjectInputStream(
                new FileInputStream(this.playlistFile))) {
            playlists = (LinkedHashMap<Playlist, ArrayList<Song>>) playlistInput.readObject();
            System.out.println("Successfully imported " + playlists.size() + " playlists");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error Importing Playlists: " + e);
            System.err
                    .println("If this persists, delete: " + this.playlistFile.getAbsolutePath());
            return null;
        }
        return playlists;
    }

    public void exportLikedSongs(LikedSongs likedSongs) {
        try (ObjectOutputStream likedOutput = new ObjectOutputStream(new FileOutputStream(this.likedSongsFile))) {
            likedOutput.writeObject(likedSongs);
            System.out.println("Successfully saved " + likedSongs.getSize() + " liked songs");
        } catch (FileNotFoundException e) {
            this.setDataFolder();
        } catch (IOException e) {
            System.err.println("Error saving liked songs: " + e);
        }
    }

    public LikedSongs importLikedSongs() {
        LikedSongs likedSongs;
        if (!this.likedSongsFile.exists()) {
            return null;
        }
        try (ObjectInputStream likedInput = new ObjectInputStream(new FileInputStream(this.likedSongsFile))) {
            likedSongs = (LikedSongs) likedInput.readObject();
            System.out.println("Successfully imported " + likedSongs.getSize() + " liked songs");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error importing liked songs: " + e);
            System.err.println("If this persists, delete: " + this.likedSongsFile.getAbsolutePath());
            return null;
        }
        return likedSongs;
    }

    public SpotifyCredentials importSpotifyCredentials() {
        SpotifyCredentials spotifyCredentials;
        if (!spotifyFile.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(
                new FileReader(this.spotifyFile))) {

            String clientId = reader.readLine();
            String clientSecret = reader.readLine();
            String redirectUrl = reader.readLine();
            spotifyCredentials = new SpotifyCredentials(clientId, clientSecret, redirectUrl);
        } catch (IOException e) {
            System.err.println("Error importing spotify credentials, creating new ones: " + e);
            System.err
                    .println("If this persists, delete: " + this.spotifyFile.getAbsolutePath());
            return null;
        }
        return spotifyCredentials;
    }

    public void exportSpotifyCredentials(SpotifyCredentials spotifyCredentials) {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(this.spotifyFile))) {
            writer.println(spotifyCredentials.getClientId());
            writer.println(spotifyCredentials.getClientSecret());
            writer.println(spotifyCredentials.getRedirectUrlString());
            System.out.println("Successfully saved Spotify credentials");
        } catch (FileNotFoundException e) {
            this.setDataFolder(); // since they are being exported its gotta exist
        } catch (IOException e) {
            System.err.println("Error Saving Spotify Credentials: " + e);
        }
    }

    public YoutubeCredentials importYoutubeCredentials() {
        YoutubeCredentials youtubeCredentials;
        if (!youtubeFile.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(
                new FileReader(this.youtubeFile))) {

            String applicationName = reader.readLine();
            String clientId = reader.readLine();
            String clientSecret = reader.readLine();
            youtubeCredentials = new YoutubeCredentials(applicationName, clientId, clientSecret);
        } catch (IOException e) {
            System.err.println("Error importing Youtube credentials, creating new ones: " + e);
            System.err
                    .println("If this persists, delete: " + this.youtubeFile.getAbsolutePath());
            return null;
        }
        return youtubeCredentials;
    }

    public void exportYoutubeCredentials(YoutubeCredentials youtubeCredentials) {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(this.youtubeFile))) {
            writer.println(youtubeCredentials.getApplicationName());
            writer.println(youtubeCredentials.getClientId());
            writer.println(youtubeCredentials.getClientSecret());
            System.out.println("Successfully saved Youtube credentials");
        } catch (FileNotFoundException e) {
            this.setDataFolder(); // since they are being exported its gotta exist
        } catch (IOException e) {
            System.err.println("Error Saving Youtube Credentials: " + e);
        }
    }
}
