package ryzen.ownitall.methods;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Library;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.Progressbar;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.tag.FieldKey;

public class Upload {
    private static final Logger logger = LogManager.getLogger(Upload.class);
    private static final Settings settings = Settings.load();
    private static final LinkedHashSet<String> extensions = new LinkedHashSet<>(Arrays.asList("mp3", "flac", "wav"));
    private static Library library = Library.load();
    private static Collection collection = Collection.load();
    private File localLibrary;

    /**
     * default local constructor asking for library path
     */
    public Upload() throws InterruptedException {
        if (settings.getUploadFolder().isEmpty() || this.localLibrary == null) {
            this.setLocalLibrary();
        }
    }

    private void setLocalLibrary() throws InterruptedException {
        while (this.localLibrary == null || !this.localLibrary.exists()) {
            System.out.print("Provide absolute path to local music library (folder): ");
            this.localLibrary = Input.request().getFile(true);

        }
    }

    /**
     * Get local liked songs and put them in collection
     * current criteria:
     * - exclude all below and only look at metadata rating if download hierachy set
     * to false
     * - songs in root folder (library path)
     * - folder named "liked songs" (changeable in settings)
     * 
     */
    public void getLikedSongs() throws InterruptedException {
        try (ProgressBar pb = Progressbar.progressBar("Liked Songs", -1)) {
            if (settings.isDownloadHierachy()) {
                for (File file : this.localLibrary.listFiles()) {
                    if (file.isFile() && extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
                        Song song = getSong(file);
                        if (song != null) {
                            pb.setExtraMessage(song.getName()).step();
                            collection.addLikedSong(song);
                        }
                    }
                    if (file.isDirectory() && file.getName().equalsIgnoreCase(settings.getLikedSongsName())) {
                        collection.addLikedSongs(getSongs(file));
                    }
                }
            } else {
                // automatically adds them to liked by their metadata
                pb.setExtraMessage("Download Hierachy").step();
                getSongs(this.localLibrary);
            }
        }
    }

    public void processFolders() throws InterruptedException {
        try (ProgressBar pb = Progressbar.progressBar("Folders", -1)) {
            // get all albums
            for (File file : this.localLibrary.listFiles()) {
                if (file.isDirectory() && !file.getName().equalsIgnoreCase(settings.getLikedSongsName())) {
                    if (isAlbum(file)) {
                        Album album = getAlbum(file);
                        if (album != null) {
                            pb.setExtraMessage(album.getName()).step();
                            collection.addAlbum(album);
                        }
                    } else if (settings.isDownloadHierachy()) {
                        Playlist playlist = getPlaylist(file);
                        if (playlist != null) {
                            pb.setExtraMessage(playlist.getName()).step();
                            if (playlist.size() == 1) { // filter out singles
                                collection.addLikedSongs(playlist.getSongs());
                            } else {
                                collection.addPlaylist(playlist);
                            }
                        }
                    }
                } else if (MusicTools.getExtension(file).equalsIgnoreCase("m3u")) {
                    Playlist playlist = processM3U(file);
                    if (playlist != null) {
                        pb.setExtraMessage(playlist.getName()).step();
                        collection.addPlaylist(playlist);
                    }
                }
            }
        }
    }

    public static Playlist processM3U(File file) throws InterruptedException {
        if (file == null || file.isDirectory()) {
            logger.debug("folder is null or non file in processM3u");
            return null;
        }
        if (!MusicTools.getExtension(file).equalsIgnoreCase("m3u")) {
            logger.debug("provided file '" + file.getAbsolutePath() + "' does not end with .m3u in processM3u");
            return null;
        }
        Playlist playlist = new Playlist(file.getName().substring(0, file.getName().lastIndexOf('.')));
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String currSongLine = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#EXTM3U")) {
                    continue;
                } else if (line.startsWith("#PLAYLIST:")) {
                    playlist.setName(line.substring(10).trim());
                } else if (line.startsWith("#EXTIMG:")) {
                    File coverFile = new File(file.getParent(), line.substring(8).trim());
                    if (coverFile.exists()) {
                        playlist.setCoverImage(coverFile.toURI());
                    } else {
                        logger.debug("coverimage referenced in m3u '" + file.getAbsolutePath() + "' not found: "
                                + coverFile.getAbsolutePath());
                    }
                } else if (line.startsWith("#EXTINF:")) {
                    currSongLine = line.substring(8).trim();
                } else if (!line.isEmpty() && !line.startsWith("#") && currSongLine != null) {
                    File songFile = new File(file.getParent(), line);
                    if (songFile.exists()) {
                        Song song = getSong(songFile);
                        if (song != null) {
                            playlist.addSong(song);
                        }
                    } else {
                        logger.debug("Song referenced in m3u '" + file.getAbsoluteFile() + "' not found: "
                                + songFile.getAbsolutePath());
                    }
                    currSongLine = null;
                }
            }
        } catch (IOException e) {
            logger.error("Exception reading m3u file '" + file.getAbsolutePath() + "': " + e);
            return null;
        }
        return playlist;
    }

    public static Playlist getPlaylist(File folder) throws InterruptedException {
        if (folder == null || !folder.exists()) {
            logger.debug("null folder or non existing folder provided");
            return null;
        }
        Playlist playlist = new Playlist(folder.getName());
        LinkedHashSet<Song> songs = getSongs(folder);
        if (songs == null || songs.isEmpty()) {
            logger.debug("no songs found in playlist: '" + folder.getAbsolutePath() + "'");
            return null;
        }
        playlist.addSongs(songs);
        File coverFile = new File(folder, playlist.getFolderName() + ".png");
        if (coverFile.exists()) {
            playlist.setCoverImage(coverFile.toURI());
        }
        return playlist;
    }

    /**
     * construct Album class from an album folder
     * 
     * @param folder - folder to get files from
     * @return - constructed Album without songs
     */
    public static Album getAlbum(File folder) throws InterruptedException {
        // TODO: parse nfo file
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            logger.debug("null folder or non existant or non directory folder provided in construct Album");
            return null;
        }
        Album album = new Album(folder.getName());
        LinkedHashSet<Song> songs = getSongs(folder);
        for (Song song : songs) {
            if (song.getAlbumName() != null) {
                album.setName(song.getAlbumName());
            }
            album.addSong(song);
        }
        File albumCover = new File(folder, "cover.png");
        if (albumCover.exists()) {
            album.setCoverImage(albumCover.toURI());
        }
        if (library != null) {
            Album foundAlbum = library.getAlbum(album);
            if (foundAlbum != null) {
                album = foundAlbum;
            } else if (settings.isLibraryVerified()) {
                album = null;
            }
        }
        return album;
    }

    /**
     * function to check if folder is an album
     * current criteria: all mp3's with metadata say the same album
     * 
     * @param folder - folder of the playlist/album
     * @return - true if album, false if playlist
     */
    public static boolean isAlbum(File folder) throws InterruptedException {
        if (folder == null || !folder.exists() || !folder.isDirectory() || folder.list().length <= 1) {
            logger.debug("empty folder, non directory, non existant or directory with less than 1 files provided: "
                    + folder);
            return false;
        }
        File nfoFile = new File(folder, "album.nfo");
        if (nfoFile.exists()) {
            return true;
        }
        LinkedHashSet<Song> songs = getSongs(folder);
        if (songs == null) {
            return false;
        }
        String albumName = null;
        boolean foundAnyAlbum = false;
        for (Song song : songs) {
            if (song.getAlbumName() != null) {
                foundAnyAlbum = true;
                if (albumName == null) {
                    albumName = song.getAlbumName();
                } else if (!albumName.equals(song.getAlbumName())) {
                    return false;
                }
            } else if (foundAnyAlbum) {
                return false;
            }
        }
        return foundAnyAlbum;
    }

    /**
     * get all songs in a folder
     * 
     * @param folder - folder to get all songs from
     * @return - linkedhashset of constructed songs
     */
    public static LinkedHashSet<Song> getSongs(File folder) throws InterruptedException {
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
                    try {
                        if (MusicTools.isSongLiked(file)) {
                            collection.addLikedSong(song);
                        }
                    } catch (Exception e) {
                        logger.error("Error checking if song '" + song.getName() + "' is liked");
                    }
                }
            }
        }
        return songs;
    }

    /**
     * getting metadata from music file
     * 
     * @param file - file to get metadata from
     * @return - constructed Song
     */
    public static Song getSong(File file) throws InterruptedException {
        if (file == null || !file.exists()) {
            logger.debug("null or non existant file provided in getSong");
            return null;
        }
        if (!extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
            logger.debug("provided file is not in extensions: '" + file.getAbsolutePath() + "'");
            return null;
        }
        Song song = new Song(file.getName().substring(0, file.getName().lastIndexOf('.')));
        try {
            LinkedHashMap<FieldKey, String> songData = MusicTools.readMetaData(file);
            if (songData != null) {
                if (songData.get(FieldKey.TITLE) != null) {
                    song.setName(songData.get(FieldKey.TITLE));
                }
                if (songData.get(FieldKey.ARTIST) != null) {
                    song.setArtist(new Artist(songData.get(FieldKey.ARTIST)));
                }
                if (songData.get(FieldKey.COVER_ART) != null) {
                    song.setCoverImage(songData.get(FieldKey.COVER_ART));
                }
                if (songData.get(FieldKey.MUSICBRAINZ_RELEASEID) != null) {
                    song.addId("mbid", songData.get(FieldKey.MUSICBRAINZ_RELEASEID));
                }
                if (songData.get(FieldKey.ALBUM) != null) {
                    song.setAlbumName(songData.get(FieldKey.ALBUM));
                }
                Duration duration = MusicTools.getSongDuration(file);
                if (!duration.isZero()) {
                    song.setDuration(duration);
                }
            }
        } catch (Exception e) {
            logger.error("Unable to read file '" + file.getAbsolutePath() + "' metadata: " + e);
        }
        if (library != null) {
            Song foundSong = library.getSong(song);
            if (foundSong != null) {
                song = foundSong;
            } else if (settings.isLibraryVerified()) {
                song = null;
            }
        }
        return song;
    }
}