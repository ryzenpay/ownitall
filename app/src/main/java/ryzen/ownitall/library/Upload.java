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

import ryzen.ownitall.Collection;
import ryzen.ownitall.Library;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.MusicTools;

import java.util.ArrayList;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Upload {
    private static final Logger logger = LogManager.getLogger(Upload.class);
    private static final Settings settings = Settings.load();
    private static final LinkedHashSet<String> extensions = new LinkedHashSet<>(Arrays.asList("mp3", "flac", "wav"));
    private static Library library = Library.load();
    private static Collection collection = Collection.load();
    private File localLibrary;
    private ArrayList<File> localLibraryFolders;
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
    }

    /**
     * default local constructor asking for library path
     */
    public Upload() {
        if (settings.getUploadFolder().isEmpty() || this.localLibrary == null) {
            this.setLocalLibrary();
        }
        this.localLibraryFolders = this.getLibraryFolders();
    }

    private void setLocalLibrary() {
        while (this.localLibrary == null || !this.localLibrary.exists()) {
            System.out.print("Provide absolute path to local music library (folder): ");
            this.localLibrary = Input.request().getFile(true);
        }
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
    public void getLikedSongs() { // TODO: testing, re-import tagged songs as liked
        for (File file : this.localLibrary.listFiles()) {
            if (file.isFile()) {
                Song song = getSong(file);
                if (song != null) {
                    collection.addLikedSong(song);
                }
            }
            if (file.isDirectory() && file.getName().equalsIgnoreCase(settings.getLikedSongName())) {
                collection.addLikedSongs(getSongs(file));
            }
        }
    }

    public void processFolders() {
        for (File file : this.localLibraryFolders) {
            if (file.isDirectory()) {
                if (isAlbum(file)) {
                    Album album = getAlbum(file);
                    if (album != null && !album.isEmpty()) {
                        collection.addAlbum(album);
                    }
                } else {
                    Playlist playlist = getPlaylist(file);
                    if (playlist != null && !playlist.isEmpty()) {
                        if (playlist.size() <= 1) { // filter out singles
                            collection.addLikedSongs(playlist.getSongs());
                        } else {
                            collection.addPlaylist(playlist);
                        }
                    }
                }
            }
        }
    }

    public static Playlist getPlaylist(File folder) {
        Playlist playlist = new Playlist(folder.getName());
        LinkedHashSet<Song> songs = getSongs(folder);
        if (songs.size() == 0) {
            return null;
        }
        playlist.addSongs(songs);
        File coverFile = new File(folder, "cover.png");
        if (coverFile.exists()) {
            playlist.setCoverImage(coverFile.toURI());
        }
        return playlist;
    }

    public static Album getAlbum(File folder) {
        Album album = constructAlbum(folder);
        if (album == null && !settings.isLibraryVerified()) {
            album = new Album(folder.getName());
        } else if (album == null) {
            return null;
        }
        LinkedHashSet<Song> songs = getSongs(folder);
        if (songs.size() == 0) {
            return null;
        }
        File coverFile = new File(folder, "cover.png");
        if (coverFile.exists()) {
            album.setCoverImage(coverFile.toURI());
        }
        album.addSongs(songs);
        return album;
    }

    /**
     * getting metadata from music file
     * 
     * @param file - file to get metadata from
     * @return - constructed Song
     */
    public static Song getSong(File file) {
        if (!extensions.contains(MusicTools.getExtension(file))) {
            return null;
        }
        Song song = null;
        String songName = file.getName().substring(0, file.getName().length() - 4);
        String artistName = null;
        long duration = 0L;
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader audioHeader = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();
            if (tag != null && !tag.getFirst(FieldKey.TITLE).isEmpty()) {
                songName = tag.getFirst(FieldKey.TITLE);
                artistName = tag.getFirst(FieldKey.ARTIST);
            }
            duration = audioHeader.getTrackLength();
        } catch (InvalidAudioFrameException | TagException e) {
            logger.error("File " + file.getAbsolutePath() + " is not an audio file or has incorrect metadata");
        } catch (IOException | CannotReadException | ReadOnlyFileException e) {
            logger.error("Error processing file: " + file.getAbsolutePath() + " error: " + e);
        }
        if (settings.isUseLibrary()) {
            song = library.searchSong(songName, artistName);
        }
        if (song == null && !settings.isLibraryVerified()) {
            song = new Song(songName);
            song.setArtist(new Artist(artistName));
        }
        if (song != null) {
            song.setDuration(duration, ChronoUnit.SECONDS);
            return song;
        }
        return null;
    }

    /**
     * get all songs in a folder
     * 
     * @param folder - folder to get all songs from
     * @return - linkedhashset of constructed songs
     */
    public static LinkedHashSet<Song> getSongs(File folder) {
        LinkedHashSet<Song> songs = new LinkedHashSet<>();
        if (folder.isFile() || !folder.exists()) {
            return songs;
        }
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                Song song = getSong(file);
                if (song == null && !settings.isLibraryVerified()) {
                    song = new Song(file.getName());
                }
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
    public static boolean isAlbum(File folder) {
        if (!folder.isDirectory() || folder.list().length <= 1) {
            return false;
        }
        String album = null;
        boolean foundAnyAlbum = false;
        for (File file : folder.listFiles()) {
            if (file.isFile() && extensions.contains(MusicTools.getExtension(file))) {
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
        }

        return foundAnyAlbum;
    }

    /**
     * construct Album class from an album folder
     * 
     * @param folder - folder to get files from
     * @return - constructed Album without songs
     */
    public static Album constructAlbum(File folder) {
        Album album = null;
        String albumName = null;
        String artistName = null;
        File albumSongFile = null;
        for (File file : folder.listFiles()) {
            if (file.isFile() && extensions.contains(MusicTools.getExtension(file))) {
                albumSongFile = file;
                break;
            }
        }
        if (albumSongFile != null) {
            try {
                AudioFile audioFile = AudioFileIO.read(albumSongFile);
                Tag tag = audioFile.getTag();
                if (tag != null && !tag.getFirst(FieldKey.ALBUM).isEmpty()) {
                    albumName = tag.getFirst(FieldKey.ALBUM);
                    artistName = tag.getFirst(FieldKey.ALBUM);
                }
            } catch (Exception e) {
                logger.error("Error parsing album: " + e);
            }
        }
        if (settings.isUseLibrary()) {
            if (albumName != null) {
                album = library.searchAlbum(albumName, artistName);
            } else {
                album = library.searchAlbum(folder.getName(), artistName);
            }
        }
        if (album == null && !settings.isLibraryVerified()) {
            if (albumName != null) {
                album = new Album(albumName);
            } else {
                album = new Album(folder.getName());
            }
            if (artistName != null) {
                album.addArtist(new Artist(artistName));
            }
        }
        return album;
    }
}