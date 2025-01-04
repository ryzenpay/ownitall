package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.time.Duration;

public class Local {
    private File localLibrary;
    private LinkedHashSet<String> extensions = new LinkedHashSet<>(Arrays.asList("mp3")); // TODO: support more
                                                                                          // formats (have to
                                                                                          // be lower case)

    public Local() {
        System.out.println("Provide absolute path to local music library (folder): ");
        this.localLibrary = Input.getInstance().getFile();
    }

    public Local(String localFolderPath) {
        this.localLibrary = new File(localFolderPath);
    }

    public LinkedHashSet<Song> getLikedSongs() {
        LinkedHashSet<Song> likedSongs = new LinkedHashSet<>();
        for (File file : this.localLibrary.listFiles()) {
            if (file.isFile() && extensions.contains(getExtension(file))) {
                Song song = this.getMP3Song(file);
                if (song != null) {
                    likedSongs.add(song);
                }
                break;
            }
            if (file.isDirectory() && file.toString().equalsIgnoreCase("liked songs")) {
                // TODO: go through each song and get their metadata
            }
        }
        return likedSongs;
    }

    public LinkedHashMap<Playlist, ArrayList<Song>> getPlaylists() {
        LinkedHashMap<Playlist, ArrayList<Song>> playlists = new LinkedHashMap<>();
        // TODO: go through each folder, ensure its not an album
        return playlists;
    }

    public LinkedHashMap<Album, ArrayList<Song>> getAlbums() {
        LinkedHashMap<Album, ArrayList<Song>> albums = new LinkedHashMap<>();
        // TODO: go through each folder, ensure its an album
        return albums;
    }

    public String getExtension(File file) {
        String fileName = file.toString();
        int extensionIndex = fileName.lastIndexOf('.');
        return fileName.substring(extensionIndex + 1).toLowerCase();
    }

    /**
     * getting metadata from mp3 file: https://github.com/mpatric/mp3agic
     * 
     * @param file
     * @return
     */
    public Song getMP3Song(File file) {
        String name;
        ArrayList<Artist> artists = new ArrayList<>();
        Duration duration;
        try {
            Mp3File mp3File = new Mp3File(file);
            duration = Duration.ofSeconds(mp3File.getLengthInSeconds());
            if (mp3File.hasId3v1Tag()) {
                ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                name = id3v1Tag.getTitle();
                artists.add(new Artist(id3v1Tag.getArtist())); // TODO: multiple artists
                return new Song(name, artists, duration);
            }
            if (mp3File.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                name = id3v2Tag.getTitle();
                artists.add(new Artist(id3v2Tag.getArtist()));
                return new Song(name, artists, duration);
            }
            return new Song(file.toString(), duration);
        } catch (UnsupportedTagException e) {
            System.err.println("The file: " + file.getAbsolutePath() + " is unsupported: " + e);
            return null;
        } catch (IOException | InvalidDataException e) {
            System.err.println("Error processing file: " + file.getAbsolutePath() + " error: " + e);
            return null;
        }
    }

    /**
     * function to check if folder is an album
     * current criteria: all mp3's with metadata say the same album
     * 
     * @param folder - folder of the playlist/album
     * @return - true if album, false if playlist
     */
    public boolean isAlbum(File folder) {
        String album = null;
        if (folder.isFile() || !folder.exists()) {
            return false;
        }
        if (folder.list().length == 0) {
            return false;
        }
        for (File file : folder.listFiles()) {
            String foundAlbum = null;
            try {
                Mp3File mp3File = new Mp3File(file);
                if (mp3File.hasId3v1Tag()) {
                    ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                    foundAlbum = id3v1Tag.getAlbum();
                }
                if (mp3File.hasId3v2Tag()) {
                    ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                    foundAlbum = id3v2Tag.getAlbum();
                }
            } catch (IOException | InvalidDataException | UnsupportedTagException e) {
                break;
            }
            if (foundAlbum != null && album != null) {
                if (!album.equals(foundAlbum)) {
                    return false;
                }
            } else if (album == null) {
                album = foundAlbum;
            }
        }
        if (album != null) { // to prevent it defaulting to album if none of the songs had metadata
            return true;
        } else {
            return false;
        }
    }
}
