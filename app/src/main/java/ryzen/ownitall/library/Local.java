package ryzen.ownitall.library;

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

import ryzen.ownitall.Library;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.MusicTools;

import java.util.ArrayList;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Local {
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
    }
    private static final Logger logger = LogManager.getLogger(Local.class);
    private static final Settings settings = Settings.load();
    private static Library library = Library.load();
    private File localLibrary;
    private final LinkedHashSet<String> extensions = new LinkedHashSet<>(Arrays.asList("mp3", "flac", "wav")); // https://bitbucket.org/ijabz/jaudiotagger/src/master/
    // formats have to be lower case

    /**
     * default local constructor asking for library path
     */
    public Local() {
        System.out.print("Provide absolute path to local music library (folder): ");
        this.localLibrary = Input.request().getFile(true);
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
            if (file.isFile() && extensions.contains(MusicTools.getExtension(file))) {
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
                    Playlist playlist = new Playlist(file.getName());
                    playlist.addSongs(this.getSongs(file));
                    if (!playlist.isEmpty()) {
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
                    if (!album.isEmpty()) {
                        albums.add(album);
                    }
                }
            }
        }
        return albums;
    }

    /**
     * getting metadata from music file: https://github.com/mpatric/mp3agic
     * 
     * @param file - file to get metadata from
     * @return - constructed Song
     */
    public Song getSong(File file) {
        Song song;
        String fileName = file.getName().substring(0, file.getName().length() - 4);
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader audioHeader = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();
            if (tag != null && !tag.getFirst(FieldKey.TITLE).isEmpty()) {
                song = library.getSong(tag.getFirst(FieldKey.TITLE), tag.getFirst(FieldKey.ARTIST));
            } else {

                song = library.getSong(fileName, null);
                // song = new Song(file.getName());
            }
            song.setDuration(audioHeader.getTrackLength(), ChronoUnit.SECONDS);
        } catch (InvalidAudioFrameException | TagException e) {
            logger.error("File " + file.getAbsolutePath() + " is not an audio file or has incorrect metadata");
            song = new Song(fileName);
        } catch (IOException | CannotReadException | ReadOnlyFileException e) {
            logger.error("Error processing file: " + file.getAbsolutePath() + " error: " + e);
            song = new Song(fileName);
        }
        return song;
    }

    public LinkedHashSet<Song> getSongs(File folder) {
        LinkedHashSet<Song> songs = new LinkedHashSet<>();
        if (folder.isFile() || !folder.exists()) {
            return songs;
        }
        for (File file : folder.listFiles()) {
            if (file.isFile() && extensions.contains(MusicTools.getExtension(file))) {
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
        if (!folder.isDirectory() || folder.list().length <= 1) {
            return false;
        }
        String album = null;
        boolean foundAnyAlbum = false;
        File[] files = folder.listFiles((dir, name) -> extensions.contains(MusicTools.getExtension(new File(name))));
        if (files == null || files.length <= 1) {
            return false;
        }
        for (File file : files) {
            try {
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();
                if (tag != null) {
                    String foundAlbum = tag.getFirst(FieldKey.ALBUM);
                    if (!foundAlbum.isEmpty()) {
                        foundAnyAlbum = true;
                        if (album == null) {
                            album = foundAlbum;
                        } else if (!album.equals(foundAlbum)) {
                            return false;
                        }
                    } else if (foundAnyAlbum) {
                        return false;
                    }
                } else if (foundAnyAlbum) {
                    return false;
                }
            } catch (Exception e) {
                logger.error("Error checking folder if album: " + e);
                return false;
            }
        }
        return foundAnyAlbum;
    }

    /**
     * construct Album class from an album folder
     * 
     * @param folder - folder to get files from
     * @return - constructed Album without songs
     */
    public Album getAlbum(File folder) {
        Album album;
        String albumName = null;
        String artistName = null;
        File albumSong = folder.listFiles()[0];
        try {
            AudioFile audioFile = AudioFileIO.read(albumSong);
            Tag tag = audioFile.getTag();
            if (tag != null && !tag.getFirst(FieldKey.ALBUM).isEmpty()) {
                albumName = tag.getFirst(FieldKey.ALBUM);
                artistName = tag.getFirst(FieldKey.ALBUM);
            }
        } catch (Exception e) {
            logger.error("Error parsing album: " + e);
        }
        if (albumName != null) {
            album = library.getAlbum(albumName, artistName);
        } else {
            album = library.getAlbum(folder.getName(), artistName);
        }
        return album;
    }
}