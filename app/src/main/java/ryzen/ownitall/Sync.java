package ryzen.ownitall;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

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

    public File getDataFolder() {
        return this.dataFolder;
    }

    public void exportAlbums(ArrayList<Album> albums) {
        try (ObjectOutputStream albumOutput = new ObjectOutputStream(
                new FileOutputStream(this.getDataFolder().getAbsolutePath() + "/albums.ser"))) {
            albumOutput.writeObject(albums);
            System.out.println("Successfully saved " + albums.size() + " albums");
        } catch (IOException e) {
            System.err.println("Error Saving Albums: " + e);
        }
    }

    public ArrayList<Album> importAlbums() {
        ArrayList<Album> albums;
        try (ObjectInputStream albumInput = new ObjectInputStream(
                new FileInputStream(this.getDataFolder().getAbsolutePath() + "/albums.ser"))) {
            albums = (ArrayList<Album>) albumInput.readObject();
            System.out.println("Successfully imported " + albums.size() + " albums");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error Importing Albums: " + e);
            albums = new ArrayList<>();
        }
        return albums;
    }

    public void exportPlaylists(ArrayList<Playlist> playlists) {
        try (ObjectOutputStream playlistOutput = new ObjectOutputStream(
                new FileOutputStream(this.getDataFolder().getAbsolutePath() + "/playlists.ser"))) {
            playlistOutput.writeObject(playlists);
            System.out.println("Successfully saved " + playlists.size() + " playlists");
        } catch (IOException e) {
            System.err.println("Error Saving Playlists: " + e);
        }
    }

    public ArrayList<Playlist> importPlaylists() {
        ArrayList<Playlist> playlists;
        try (ObjectInputStream playlistInput = new ObjectInputStream(
                new FileInputStream(this.getDataFolder().getAbsolutePath() + "/playlists.ser"))) {
            playlists = (ArrayList<Playlist>) playlistInput.readObject();
            System.out.println("Successfully imported " + playlists.size() + " playlists");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error Importing Playlists: " + e);
            playlists = new ArrayList<>();
        }
        return playlists;
    }
}
