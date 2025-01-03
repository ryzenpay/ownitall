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
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;

public class Sync {
    private File dataFolder;
    private File albumFile;
    private File playlistFile;
    private File spotifyFile;

    public Sync(String dataPath) {
        this.setDataFolder(dataPath);
        this.albumFile = new File(this.dataFolder, "albums.ser");
        this.albumFile = new File(this.dataFolder, "albums.ser");
        this.playlistFile = new File(this.dataFolder, "playlists.ser");
        this.spotifyFile = new File(this.dataFolder, "spotifyCredentials.txt");
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
        } catch (NotSerializableException e) {
            System.out.println("Not saving albums, as they are empty");
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
        } catch (NotSerializableException e) {
            System.out.println("Not saving playlists, as they are empty");
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
            String code = reader.readLine();
            LocalDateTime codeExpiration = LocalDateTime.parse(reader.readLine());
            spotifyCredentials = new SpotifyCredentials(clientId, clientSecret, redirectUrl, code, codeExpiration);
        } catch (IOException e) {
            System.err.println("Error importing spotify credentials: " + e);
            System.err
                    .println("If this persists, delete: " + this.spotifyFile);
            spotifyCredentials = new SpotifyCredentials();
        }
        return spotifyCredentials;
    }

    public void exportSpotifyCredentials(SpotifyCredentials spotifyCredentials) {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(this.spotifyFile))) {
            writer.println(spotifyCredentials.getClientId());
            writer.println(spotifyCredentials.getClientSecret());
            writer.println(spotifyCredentials.getRedirectUrl());
            writer.println(spotifyCredentials.getCode());
            writer.println(spotifyCredentials.getCodeExpiration().toString());
            System.out.println("Successfully saved Spotify credentials");
        } catch (FileNotFoundException e) {
            this.setDataFolder(); // since they are being exported its gotta exist
        } catch (IOException e) {
            System.err.println("Error Saving Spotify Credentials: " + e);
        }
    }
}
