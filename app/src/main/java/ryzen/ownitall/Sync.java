package ryzen.ownitall;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Sync {
    private File dataFolder;

    public Sync(String dataPath) {
        this.setDataFolder(dataPath);
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
                new FileOutputStream(this.getDataFolder().getAbsolutePath() + "/albums.ser"))) {
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
        try (ObjectInputStream albumInput = new ObjectInputStream(
                new FileInputStream(this.getDataFolder().getAbsolutePath() + "/albums.ser"))) {
            albums = (LinkedHashMap<Album, ArrayList<Song>>) albumInput.readObject();
            System.out.println("Successfully imported " + albums.size() + " albums");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error Importing Albums: " + e);
            System.err.println("If this persists, delete: " + this.getDataFolder().getAbsolutePath());
            albums = new LinkedHashMap<>();
        }
        return albums;
    }

    public void exportPlaylists(LinkedHashMap<Playlist, ArrayList<Song>> playlists) {
        try (ObjectOutputStream playlistOutput = new ObjectOutputStream(
                new FileOutputStream(this.getDataFolder().getAbsolutePath() + "/playlists.ser"))) {
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
        try (ObjectInputStream playlistInput = new ObjectInputStream(
                new FileInputStream(this.getDataFolder().getAbsolutePath() + "/playlists.ser"))) {
            playlists = (LinkedHashMap<Playlist, ArrayList<Song>>) playlistInput.readObject();
            System.out.println("Successfully imported " + playlists.size() + " playlists");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error Importing Playlists: " + e);
            System.err.println("If this persists, delete: " + this.getDataFolder().getAbsolutePath());
            playlists = new LinkedHashMap<>();
        }
        return playlists;
    }
}
