package ryzen.ownitall;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import ryzen.ownitall.tools.Input;

import java.util.ArrayList;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Local {
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
    }
    private static final Logger logger = LogManager.getLogger(Local.class);
    private static Settings settings = Settings.load();
    private File localLibrary;
    private LinkedHashSet<String> extensions = new LinkedHashSet<>(Arrays.asList("mp3", "flac", "wav")); // https://bitbucket.org/ijabz/jaudiotagger/src/master/
    // formats have to be lower case

    /**
     * default local constructor asking for library path
     */
    public Local() {
        System.out.println("Provide absolute path to local music library (folder): ");
        this.localLibrary = Input.request().getFile();
    }

    /**
     * default local constructor with a known library path
     * 
     * @param localFolderPath - String with path location to local music library
     */
    public Local(String localFolderPath) {
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
            if (file.isDirectory() && file.getName().equalsIgnoreCase(settings.getLikedSongName())) {
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
                    Playlist playlist = new Playlist(file.getName());
                    playlist.addSongs(songs);
                    playlists.add(playlist);
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
        String fileName = file.getName();
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
        Song song;
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader audioHeader = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();
            if (tag != null && !tag.getFirst(FieldKey.TITLE).isEmpty()) {
                String songName = tag.getFirst(FieldKey.TITLE);
                String artistName = tag.getFirst(FieldKey.ARTIST);
                song = Library.load().getSong(songName, artistName);
                if (song == null) {
                    song = new Song(tag.getFirst(FieldKey.TITLE));
                }
            } else {
                song = new Song(file.getName());
            }
            song.setDuration(audioHeader.getTrackLength(), ChronoUnit.SECONDS);
            // song.setCoverImage(tag.getFirst(FieldKey.COVER_ART)); //TODO: need to add
            // support / convert to url
        } catch (InvalidAudioFrameException | TagException e) {
            logger.error("File " + file.getAbsolutePath() + " is not an audio file or has incorrect metadata");
            song = new Song(file.getName());
        } catch (IOException | CannotReadException | ReadOnlyFileException e) {
            logger.error("Error processing file: " + file.getAbsolutePath() + " error: " + e);
            song = new Song(file.getName());
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
        for (File file : folder.listFiles()) {
            if (file.isFile() && extensions.contains(getExtension(file))) {
                songs.add(getSong(file));
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
        if (folder.list().length <= 1) { // empty or a single
            return false;
        }
        for (File file : folder.listFiles()) {
            String foundAlbum = "";
            try {
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();
                foundAlbum = tag.getFirst(FieldKey.ALBUM);
            } catch (Exception e) {
                break;
            }
            if (!foundAlbum.isEmpty()) {
                if (album == null) {
                    album = foundAlbum;
                } else {
                    if (!album.equals(foundAlbum)) {
                        return false;
                    }
                }
            }
        }
        if (album == null) {
            return false;
        }
        return true;
    }

    /**
     * construct Album class from an album folder
     * 
     * @param folder - folder to get files from
     * @return - constructed Album without songs
     */
    public Album getAlbum(File folder) {
        Album album;
        for (File file : folder.listFiles()) {
            try {
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();
                if (tag != null && !tag.getFirst(FieldKey.ALBUM).isEmpty()) {
                    String albumName = tag.getFirst(FieldKey.ALBUM);
                    String artistName = tag.getFirst(FieldKey.ALBUM);
                    album = Library.load().getAlbum(albumName, artistName);
                    return album;
                }
            } catch (Exception e) {
                break;
            }
        }
        album = new Album(folder.getName()); // default to foldername
        return album;
    }
}