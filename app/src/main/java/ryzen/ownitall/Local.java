package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.util.ArrayList;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Local {
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
    }
    private static final Logger logger = LogManager.getLogger(Local.class);
    private Settings settings;
    private File localLibrary;
    private LinkedHashSet<String> extensions = new LinkedHashSet<>(Arrays.asList("mp3", "flac", "wav")); // https://bitbucket.org/ijabz/jaudiotagger/src/master/
    // formats have to be lower case

    /**
     * default local constructor asking for library path
     */
    public Local() {
        this.settings = Settings.load();
        System.out.println("Provide absolute path to local music library (folder): ");
        this.localLibrary = Input.getInstance().getFile();
    }

    /**
     * default local constructor with a known library path
     * 
     * @param localFolderPath - String with path location to local music library
     */
    public Local(String localFolderPath) {
        this.settings = Settings.load();
        this.localLibrary = new File(localFolderPath);
    }

    /**
     * get all sub folders of the library folder
     * 
     * @return - arraylist of constructed File
     */
    private ArrayList<File> getLibraryFolders() {
        ArrayList<File> folders = new ArrayList<>();
        for (File file : this.localLibrary.listFiles()) {
            if (file.isDirectory()) {
                folders.add(file);
                addSubFolders(file, folders);
            }
        }
        return folders;
    }

    /**
     * recursive function used in getLibraryFolders to traverse through sub
     * directories
     * 
     * @param directory - constructed File of the directory to traverse
     * @param folders   - arraylist of current folders to add to
     */
    private void addSubFolders(File directory, ArrayList<File> folders) {
        File[] subFiles = directory.listFiles();
        if (subFiles != null) {
            for (File subFile : subFiles) {
                if (subFile.isDirectory()) {
                    folders.add(subFile);
                    addSubFolders(subFile, folders);
                }
            }
        }
    }

    /**
     * Get local liked songs
     * current criteria:
     * - songs in root folder (library path)
     * - folder named "liked songs" (changeable in settings)
     * 
     * @return - constructed LikedSongs
     */
    public LikedSongs getLikedSongs() {
        LikedSongs likedSongs = new LikedSongs();
        for (File file : this.localLibrary.listFiles()) {
            if (file.isFile() && extensions.contains(getExtension(file))) {
                Song song = this.getSong(file);
                if (song != null) {
                    likedSongs.addSong(song);
                }
            }
            if (file.isDirectory() && file.toString().equalsIgnoreCase(this.settings.likedSongName)) {
                likedSongs.addSongs(this.getSongs(file));
            }
        }
        return likedSongs;
    }

    /**
     * get local playlists
     * criteria for playlist is determined by isAlbum() = false
     * 
     * @return - LinkedHashSet with constructed Playlist
     */
    public LinkedHashSet<Playlist> getPlaylists() {
        LinkedHashSet<Playlist> playlists = new LinkedHashSet<>();
        for (File file : this.getLibraryFolders()) {
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

    /**
     * get local albums
     * criteria for album is determined by isAlbum() = true
     * 
     * @return - linkedhashset with constructed Album
     */
    public LinkedHashSet<Album> getAlbums() {
        LinkedHashSet<Album> albums = new LinkedHashSet<>();
        for (File file : this.getLibraryFolders()) {
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

    /**
     * get file extension of a file
     * 
     * @param file - constructed File to get extension from
     * @return - String of file extension
     */
    public String getExtension(File file) {
        String fileName = file.toString();
        int extensionIndex = fileName.lastIndexOf('.');
        return fileName.substring(extensionIndex + 1).toLowerCase();
    }

    /**
     * getting metadata from music file: https://github.com/mpatric/mp3agic
     * 
     * @param file - file to get metadata from
     * @return - constructed Song
     */
    public Song getSong(File file) {
        Song song = new Song(file.toString());
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader audioHeader = audioFile.getAudioHeader();
            song.setDuration(audioHeader.getTrackLength(), ChronoUnit.SECONDS);
            Tag tag = audioFile.getTag();
            if (tag != null) {
                if (!tag.getFirst(FieldKey.TITLE).isEmpty()) {
                    song.setName(tag.getFirst(FieldKey.TITLE));
                }
                List<String> artistList = tag.getAll(FieldKey.ARTIST);
                for (String artistName : artistList) {
                    song.addArtist(new Artist(artistName));
                }
            }
            // song.setCoverImage(tag.getFirst(FieldKey.COVER_ART)); //TODO: need to add
            // support / convert to url
        } catch (InvalidAudioFrameException | TagException e) {
            logger.error("File " + file.getAbsolutePath() + " is not an audio file or has incorrect metadata");
            return null;
        } catch (IOException | CannotReadException | ReadOnlyFileException e) {
            logger.error("Error processing file: " + file.getAbsolutePath() + " error: " + e);
            return null;
        }
        return song;
    }

    /**
     * parsing through library sub folder and construct Song for each file
     * 
     * @param folder - library sub folder to check
     * @return - arraylist of constructed Song
     */
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

    /**
     * construct Album class from an album folder
     * 
     * @param folder - folder to get files from
     * @return - constructed Album without songs
     */
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