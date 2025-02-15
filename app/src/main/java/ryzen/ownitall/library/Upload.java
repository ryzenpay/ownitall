package ryzen.ownitall.library;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

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
    }

    private void setLocalLibrary() {
        while (this.localLibrary == null || !this.localLibrary.exists()) {
            try {
                System.out.print("Provide absolute path to local music library (folder): ");
                this.localLibrary = Input.request().getFile(true);
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting music library path");
            }
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
     * Get local liked songs and put them in collection
     * current criteria:
     * - songs in root folder (library path)
     * - folder named "liked songs" (changeable in settings)
     * 
     */
    public void getLikedSongs() {
        if (settings.isDownloadHierachy()) {
            for (File file : this.localLibrary.listFiles()) {
                if (file.isFile() && extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
                    Song song = getSong(file);
                    if (song != null) {
                        collection.addLikedSong(song);
                    }
                }
                if (file.isDirectory() && file.getName().equalsIgnoreCase(settings.getLikedSongName())) {
                    // automatically adds them to liked
                    getSongs(file);
                }
            }
        } else {
            // automatically adds them to liked
            getSongs(this.localLibrary);
        }
    }

    public void processFolders() {
        for (File file : this.getLibraryFolders()) {
            if (file.isDirectory() && !file.getName().equalsIgnoreCase(settings.getLikedSongName())) {
                if (isAlbum(file)) {
                    Album album = getAlbum(file);
                    if (album != null) {
                        collection.addAlbum(album);
                    }
                } else {
                    if (settings.isDownloadHierachy()) {
                        Playlist playlist = getPlaylist(file);
                        if (playlist != null) {
                            if (playlist.size() == 1) { // filter out singles
                                collection.addLikedSongs(playlist.getSongs());
                            } else {
                                collection.addPlaylist(playlist);
                            }
                        }
                    } else {
                        logger.debug("Skipped folder " + file.getAbsolutePath()
                                + " as it is not an album and downloadHierachy is set to: "
                                + settings.isDownloadHierachy());
                    }
                }
            }
        }
        // check for m3u files in root directory
        if (!settings.isDownloadHierachy()) {
            for (File inFile : this.localLibrary.listFiles()) {
                if (MusicTools.getExtension(inFile).equalsIgnoreCase("m3u")) {
                    Playlist playlist = processM3U(inFile);
                    if (playlist != null) {
                        collection.addPlaylist(playlist);
                    }
                }
            }
        }
    }

    public static Playlist processM3U(File file) {
        if (file == null || file.isDirectory()) {
            logger.debug("folder is null or non file in processM3u");
            return null;
        }
        if (!MusicTools.getExtension(file).equalsIgnoreCase("m3u")) {
            logger.debug("provided file " + file.getAbsolutePath() + "does not end with .m3u in processM3u");
            return null;
        }
        String playlistName = null;
        URI coverImage = null;
        LinkedHashSet<Song> songs = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String currSongLine = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#EXTM3U")) {
                    continue;
                } else if (line.startsWith("#PLAYLIST:")) {
                    playlistName = line.substring(10).trim();
                } else if (line.startsWith("#EXTIMG:")) {
                    File coverFile = new File(file.getParent(), line.substring(8).trim());
                    if (coverFile.exists()) {
                        coverImage = coverFile.toURI();
                    } else {
                        logger.debug("coverimage referenced in m3u " + file.getAbsolutePath() + "not found: "
                                + coverFile.getAbsolutePath());
                    }
                } else if (line.startsWith("#EXTINF:")) {
                    currSongLine = line.substring(8).trim();
                } else if (!line.isEmpty() && !line.startsWith("#") && currSongLine != null) {
                    File songFile = new File(file.getParent(), line);
                    if (songFile.exists()) {
                        Song song = getSong(songFile);
                        if (song != null) {
                            songs.add(song);
                        }
                    } else {
                        logger.debug("Song referenced in m3u " + file.getAbsoluteFile() + " not found: "
                                + songFile.getAbsolutePath());
                    }
                    currSongLine = null;
                }
            }
        } catch (IOException e) {
            logger.error("Error reading m3u file " + file.getAbsolutePath() + ": " + e);
            return null;
        }
        if (playlistName == null) {
            playlistName = file.getName().replace(".m3u", "");
            logger.warn(
                    "Was unable to retrieve playlist name from m3u file " + file.getAbsolutePath() + ", defaulting to "
                            + playlistName);
        }
        if (songs.isEmpty()) {
            logger.warn("No songs found in m3u file " + file.getAbsolutePath() + " skipping...");
            return null;
        }
        Playlist playlist = new Playlist(playlistName);
        if (coverImage != null) {
            playlist.setCoverImage(coverImage);
        }
        playlist.addSongs(songs);
        return playlist;
    }

    public static Playlist getPlaylist(File folder) {
        if (folder == null || !folder.exists()) {
            logger.debug("null folder or non existing folder provided");
            return null;
        }
        Playlist playlist = new Playlist(folder.getName());
        LinkedHashSet<Song> songs = getSongs(folder);
        if (songs == null || songs.isEmpty()) {
            logger.debug("no songs found in " + folder.getAbsolutePath());
            return null;
        }
        playlist.addSongs(songs);
        File coverFile = new File(folder, playlist.getFolderName() + ".png");
        if (coverFile.exists()) {
            playlist.setCoverImage(coverFile.toURI());
        }
        return playlist;
    }

    public static Album getAlbum(File folder) {
        if (folder == null || !folder.exists()) {
            logger.debug("null folder or non existing folder passed in getAlbum");
            return null;
        }
        Album album = constructAlbum(folder);
        if (album == null) {
            logger.debug("Error creating constructed album for " + folder.getAbsolutePath());
            return null;
        }
        LinkedHashSet<Song> songs = getSongs(folder);
        if (songs == null || songs.isEmpty()) {
            logger.debug("no songs found in album " + folder.getAbsolutePath());
            return null;
        }
        album.addSongs(songs);
        File coverFile = new File(folder, album.getFolderName() + ".png");
        if (coverFile.exists()) {
            album.setCoverImage(coverFile.toURI());
        }
        return album;
    }

    /**
     * getting metadata from music file
     * 
     * @param file - file to get metadata from
     * @return - constructed Song
     */
    public static Song getSong(File file) {
        if (file == null || !file.exists()) {
            logger.debug("null or non existant file provided in getSong");
            return null;
        }
        if (!extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
            logger.debug("provided file is not in extensions:" + file.getAbsolutePath());
            return null;
        }
        Song song = null;
        String songName = file.getName().substring(0, file.getName().lastIndexOf('.'));
        String artistName = null;
        String coverImage = null;
        long duration = 0L;
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader audioHeader = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();
            if (tag != null) {
                if (!tag.getFirst(FieldKey.TITLE).isEmpty()) {
                    songName = tag.getFirst(FieldKey.TITLE);
                }
                if (!tag.getFirst(FieldKey.ARTIST).isEmpty()) {
                    artistName = tag.getFirst(FieldKey.ARTIST);
                }
                if (!tag.getFirst(FieldKey.COVER_ART).isEmpty()) {
                    coverImage = tag.getFirst(FieldKey.COVER_ART);
                }
            }
            duration = audioHeader.getTrackLength();
        } catch (Exception e) {
            logger.error("Error parsing metadata for file: " + file.getAbsolutePath() + " : ");
        }
        if (settings.isUseLibrary()) {
            song = library.getSong(songName, artistName);
        }
        if (song == null && !settings.isLibraryVerified()) {
            song = new Song(songName);
            if (artistName != null) {
                song.setArtist(new Artist(artistName));
            }
            if (coverImage != null) {
                song.setCoverImage(coverImage);
            }
        }
        if (song != null) {
            song.setDuration(duration, ChronoUnit.SECONDS);
        }
        return song;
    }

    /**
     * get all songs in a folder
     * 
     * @param folder - folder to get all songs from
     * @return - linkedhashset of constructed songs
     */
    public static LinkedHashSet<Song> getSongs(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            logger.debug("null or non directory or non existant folder passed in getSongs");
            return null;
        }
        LinkedHashSet<Song> songs = new LinkedHashSet<>();
        for (File file : folder.listFiles()) {
            if (file.isFile() && extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
                Song song = getSong(file);
                if (song != null) {
                    songs.add(song);
                    if (isLiked(file)) {
                        collection.addLikedSong(song);
                    }
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
        if (folder == null || !folder.exists() || !folder.isDirectory() || folder.list().length <= 1) {
            logger.debug("empty folder, non directory, non existant or directory with less than 1 files provided: "
                    + folder);
            return false;
        }
        String album = null;
        boolean foundAnyAlbum = false;
        for (File file : folder.listFiles()) {
            if (file.isFile() && extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
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

    public static boolean isLiked(File file) {
        if (file == null || !file.isFile()) {
            logger.debug("Empty file or non file provided: " + file);
            return false;
        }
        if (file.isFile() && extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
            try {
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();
                if (tag != null) {
                    String rating = tag.getFirst(FieldKey.RATING);
                    if (rating.equals("255")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.error("Error checking folder if album: " + e);
                return false;
            }
        } else {
            logger.debug("Unsupported format for: " + file.getAbsolutePath());
        }
        return false;
    }

    /**
     * construct Album class from an album folder
     * 
     * @param folder - folder to get files from
     * @return - constructed Album without songs
     */
    public static Album constructAlbum(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            logger.debug("null folder or non existant or non directory folder provided in construct Album");
            return null;
        }
        Album album = null;
        String albumName = folder.getName();
        String artistName = null;
        File albumSongFile = null;
        for (File file : folder.listFiles()) {
            if (file.isFile() && extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
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
                    artistName = tag.getFirst(FieldKey.ARTIST);
                }
            } catch (Exception e) {
                logger.error("Error parsing album: " + e);
            }
        }
        if (settings.isUseLibrary()) {
            album = library.getAlbum(albumName, artistName);

        }
        if (album == null && !settings.isLibraryVerified()) {
            if (albumName != null) {
                album = new Album(albumName);
            } else {
                album = new Album(folder.getName());
            }
            album.addArtist(new Artist(artistName));
        }
        return album;
    }
}