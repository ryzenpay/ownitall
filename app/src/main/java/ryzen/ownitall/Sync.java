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
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Sync implements AutoCloseable {
    private File dataFolder;
    private File albumFile;
    private File playlistFile;
    private File likedSongsFile;
    private File spotifyFile;

    public Sync(String dataPath) { // TODO: archiving and unarchiving
        this.setDataFolder(dataPath);
        this.albumFile = new File(this.dataFolder, "albums.ser");
        this.albumFile = new File(this.dataFolder, "albums.ser");
        this.playlistFile = new File(this.dataFolder, "playlists.ser");
        this.likedSongsFile = new File(this.dataFolder, "likedsongs.ser");
        this.spotifyFile = new File(this.dataFolder, "spotifyCredentials.txt");
    }

    @Override
    public void close() {
        // Close any open resources
        closeFile(albumFile);
        closeFile(playlistFile);
        closeFile(likedSongsFile);
        closeFile(spotifyFile);

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
            return new LinkedHashMap<>();
        }
        try (ObjectInputStream albumInput = new ObjectInputStream(
                new FileInputStream(this.albumFile))) {
            albums = (LinkedHashMap<Album, ArrayList<Song>>) albumInput.readObject();
            System.out.println("Successfully imported " + albums.size() + " albums");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error Importing Albums: " + e);
            System.err.println("If this persists, delete: " + this.albumFile.getAbsolutePath());
            albums = new LinkedHashMap<>();
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
            return new LinkedHashMap<>();
        }
        try (ObjectInputStream playlistInput = new ObjectInputStream(
                new FileInputStream(this.playlistFile))) {
            playlists = (LinkedHashMap<Playlist, ArrayList<Song>>) playlistInput.readObject();
            System.out.println("Successfully imported " + playlists.size() + " playlists");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error Importing Playlists: " + e);
            System.err
                    .println("If this persists, delete: " + this.playlistFile.getAbsolutePath());
            playlists = new LinkedHashMap<>();
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
            return new LikedSongs();
        }
        try (ObjectInputStream likedInput = new ObjectInputStream(new FileInputStream(this.likedSongsFile))) {
            likedSongs = (LikedSongs) likedInput.readObject();
            System.out.println("Successfully imported " + likedSongs.getSize() + " liked songs");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error importing liked songs: " + e);
            System.err.println("If this persists, delete: " + this.likedSongsFile.getAbsolutePath());
            likedSongs = new LikedSongs();
        }
        return likedSongs;
    }

    public SpotifyCredentials importSpotifyCredentials() {
        SpotifyCredentials spotifyCredentials;
        if (!spotifyFile.exists()) {
            return new SpotifyCredentials();
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
            spotifyCredentials = new SpotifyCredentials();
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
}
