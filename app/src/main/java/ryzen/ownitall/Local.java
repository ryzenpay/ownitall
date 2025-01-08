package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.util.ArrayList;
import java.time.temporal.ChronoUnit;

public class Local {
    private File localLibrary;
    private LinkedHashSet<String> extensions = new LinkedHashSet<>(Arrays.asList("mp3", "flac", "mp4", "wav")); // https://bitbucket.org/ijabz/jaudiotagger/src/master/
    // formats (have to
    // be lower case)

    public Local() {
        System.out.println("Provide absolute path to local music library (folder): ");
        this.localLibrary = Input.getInstance().getFile();
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    public Local(String localFolderPath) {
        this.localLibrary = new File(localFolderPath);
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    public LinkedHashSet<Song> getLikedSongs() {
        LinkedHashSet<Song> likedSongs = new LinkedHashSet<>();
        for (File file : this.localLibrary.listFiles()) {
            if (file.isFile() && extensions.contains(getExtension(file))) {
                Song song = this.getSong(file);
                if (song != null) {
                    likedSongs.add(song);
                }
            }
            if (file.isDirectory() && file.toString().equalsIgnoreCase("liked songs")) {
                likedSongs.addAll(new LinkedHashSet<>(this.getSongs(file)));
            }
        }
        return likedSongs;
    }

    public LinkedHashSet<Playlist> getPlaylists() {
        LinkedHashSet<Playlist> playlists = new LinkedHashSet<>();
        for (File file : this.localLibrary.listFiles()) { // TODO: only scans root of folder, recursion?
            if (file.isDirectory()) {
                if (!isAlbum(file)) {
                    ArrayList<Song> songs = this.getSongs(file);
                    if (songs.size() > 1) {
                        Playlist playlist = new Playlist(file.toString());
                        playlist.addSongs(songs);
                        playlists.add(playlist);
                    }
                }
            }
        }
        return playlists;
    }

    public LinkedHashSet<Album> getAlbums() {
        LinkedHashSet<Album> albums = new LinkedHashSet<>();
        for (File file : this.localLibrary.listFiles()) { // TODO: only scans root of folder, recursion?
            if (file.isDirectory()) {
                if (isAlbum(file)) {
                    Album album = this.getAlbum(file);
                    album.addSongs(this.getSongs(file));
                    albums.add(album);
                }
            }
        }
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
    public Song getSong(File file) {
        Song song = new Song(file.toString());
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            AudioHeader audioHeader = audioFile.getAudioHeader();
            song.setName(tag.getFirst(FieldKey.TITLE));
            song.setDuration(audioHeader.getTrackLength(), ChronoUnit.SECONDS);
            List<String> artistList = tag.getAll(FieldKey.ARTIST);
            for (String artistName : artistList) {
                song.addArtist(new Artist(artistName));
            }
        } catch (InvalidAudioFrameException | TagException e) {
            System.err.println("File " + file.getAbsolutePath() + " is not an audio file or has incorrect metadata");
            return null;
        } catch (IOException | CannotReadException | ReadOnlyFileException e) {
            System.err.println("Error processing file: " + file.getAbsolutePath() + " error: " + e);
            return null;
        }
        return song;
    }

    public ArrayList<Song> getSongs(File folder) {
        ArrayList<Song> songs = new ArrayList<>();
        if (folder.isFile() || !folder.exists()) {
            return songs;
        }
        if (folder.list().length == 0) {
            return songs;
        }
        for (File file : folder.listFiles()) {
            if (file.isFile() && extensions.contains(getExtension(file))) {
                Song song = getSong(file);
                if (song != null) {
                    songs.add(song);
                }
            }
        }
        return songs;
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
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();
                foundAlbum = tag.getFirst(FieldKey.ALBUM);
            } catch (Exception e) {
                break;
            }
            if (foundAlbum != null && album != null) {
                if (!album.equals(foundAlbum)) {
                    return false;
                }
            } else if (album == null && foundAlbum != "") {
                album = foundAlbum;
            }
        }
        if (album != null) { // to prevent it defaulting to album if none of the songs had metadata
            return true;
        } else {
            return false;
        }
    }

    public Album getAlbum(File folder) {
        Album album = new Album(folder.toString());// default album name incase of error
        for (File file : folder.listFiles()) {
            try {
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();
                album.setName(tag.getFirst(FieldKey.ALBUM));
                List<String> artistList = tag.getAll(FieldKey.ARTIST);
                for (String artistName : artistList) {
                    album.addArtist(new Artist(artistName));
                }
            } catch (Exception e) {
                break;
            }
        }
        return album;
    }
}