package ryzen.ownitall.method;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.output.cli.ProgressBar;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.MusicTools;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.tag.FieldKey;

@Method.Import
public class Upload extends Method {
    private static final Logger logger = LogManager.getLogger(Upload.class);
    private static final Library library = Library.load();
    private static final ArrayList<String> extensions = new ArrayList<>() {
        {
            add("mp3");
            add("flac");
            add("wav");
        }
    };

    public Upload() throws InterruptedException {
        if (Method.isCredentialsEmpty(Upload.class)) {
            logger.debug("Empty upload credentials");
            throw new InterruptedException("empty upload credentials");
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
     * @return - constructed likedsongs
     * @throws InterruptedException - when user interrupts
     */
    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        try (ProgressBar pb = new ProgressBar("Liked Songs", Settings.localFolder.listFiles().length);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            if (Settings.downloadHierachy) {
                File likedSongsFolder = new File(Settings.localFolder, Settings.likedSongName);
                if (likedSongsFolder.exists()) {
                    pb.step(likedSongsFolder.getName());
                    ArrayList<Song> songs = getSongs(likedSongsFolder);
                    if (songs != null) {
                        likedSongs.addSongs(songs);
                    }
                }
            } else {
                pb.step(Settings.localFolder.getName());
                LikedSongs rootLikedSongs = getLikedSongs(Settings.localFolder);
                if (rootLikedSongs != null) {
                    likedSongs.addSongs(rootLikedSongs.getSongs());
                }
                for (File folder : Settings.localFolder.listFiles()) {
                    interruptionHandler.throwInterruption();
                    if (folder.isDirectory()) {
                        pb.step(folder.getName());
                        LikedSongs folderLikedSongs = getLikedSongs(folder);
                        if (folderLikedSongs != null) {
                            likedSongs.addSongs(folderLikedSongs.getSongs());
                        }
                    }
                }
            }
        }
        return likedSongs;
    }

    public static LikedSongs getLikedSongs(File folder) throws InterruptedException {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            logger.debug("null, non existant or non folder passed in getLikedSongs");
            return null;
        }
        LikedSongs likedSongs = new LikedSongs();
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (File file : folder.listFiles()) {
                interruptionHandler.throwInterruption();
                if (file.isFile() && extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
                    Song song = getSong(file);
                    if (song != null) {
                        if (Settings.downloadHierachy) {
                            likedSongs.addSong(song);
                        } else {
                            try {
                                if (MusicTools.isSongLiked(file)) {
                                    likedSongs.addSong(song);
                                }
                            } catch (Exception e) {
                                logger.error(
                                        "Exception checking if song '" + file.getAbsolutePath() + "' is liked", e);
                            }
                        }
                    }
                }
            }
        }
        return likedSongs;
    }

    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = new ArrayList<>();
        try (ProgressBar pb = new ProgressBar("Playlists", Settings.localFolder.listFiles().length);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (File file : Settings.localFolder.listFiles()) {
                interruptionHandler.throwInterruption();
                if (Settings.downloadHierachy) {
                    if (file.isDirectory() && !file.getName().equalsIgnoreCase(Settings.likedSongName)) {
                        if (!isAlbum(file)) {
                            pb.step(file.getName());
                            Playlist playlist = this.getPlaylist(file.getAbsolutePath(), null);
                            if (playlist != null) {
                                playlists.add(playlist);
                            }
                        }
                    }
                } else if (file.isFile()) {
                    if (MusicTools.getExtension(file).equalsIgnoreCase("m3u")) {
                        if (file.getName().equalsIgnoreCase(Settings.likedSongName + ".m3u")) {
                            continue;
                        }
                        pb.step(file.getName());
                        Playlist playlist = getM3UPlaylist(file);
                        if (playlist != null) {
                            playlists.add(playlist);
                        }
                    }
                }
            }
        }
        return playlists;
    }

    public static Playlist getM3UPlaylist(File file) throws InterruptedException {
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
            logger.error("Exception reading m3u file '" + file.getAbsolutePath() + "'", e);
            return null;
        }
        return playlist;
    }

    @Override
    public Playlist getPlaylist(String path, String name) throws InterruptedException {
        if (path == null) {
            logger.debug("null folder, non existing or non folder provided in getPlaylist");
            return null;
        }
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            logger.debug("non existing or non folder provided in getPlaylist");
            return null;
        }
        if (name == null) {
            name = folder.getName();
        }
        Playlist playlist = new Playlist(name);
        ArrayList<Song> songs = getSongs(folder);
        if (songs == null || songs.isEmpty()) {
            logger.debug("no songs found in playlist: '" + folder.getAbsolutePath() + "'");
            return null;
        }
        playlist.addSongs(songs);
        // TODO: doesnt work cuz of variable extension
        // also fix in getAlbum()
        // WRAAAAAA
        // File coverFile = new File(folder, playlist.getCoverImageFileName());
        // if (coverFile.exists()) {
        // playlist.setCoverImage(coverFile.toURI());
        // }
        return playlist;
    }

    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        ArrayList<Album> albums = new ArrayList<>();
        try (ProgressBar pb = new ProgressBar("Albums", Settings.localFolder.listFiles().length);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (File file : Settings.localFolder.listFiles()) {
                interruptionHandler.throwInterruption();
                if (file.isDirectory() && !file.getName().equalsIgnoreCase(Settings.likedSongName)) {
                    if (isAlbum(file)) {
                        pb.step(file.getName());
                        Album album = this.getAlbum(file.getAbsolutePath(), null, null);
                        if (album != null) {
                            albums.add(album);
                        }
                    }
                }
            }
        }
        return albums;
    }

    /**
     * construct Album class from an album folder
     * 
     * @param folder - folder to get files from
     * @return - constructed Album without songs
     * @throws InterruptedException - when user interrupts
     */
    @Override
    public Album getAlbum(String path, String name, String albumArtistName) throws InterruptedException {
        if (path == null) {
            logger.debug("null path provided in getAlbum");
            return null;
        }
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            logger.debug("non existant or non directory provided in getAlbum");
            return null;
        }
        if (name == null) {
            name = folder.getName();
        }
        Album album = new Album(folder.getName());
        ArrayList<Song> songs = getSongs(folder);
        if (songs != null && !songs.isEmpty()) {
            album.addSongs(songs);
            File songFile = new File(folder, songs.get(0).getFileName());
            // get albumName from first song in album
            try {
                LinkedHashMap<FieldKey, String> songData = MusicTools
                        .readMetaData(new File(folder, songs.get(0).getFileName()));
                if (songData.get(FieldKey.ALBUM) != null) {
                    album.setName(songData.get(FieldKey.ALBUM));
                }
            } catch (Exception e) {
                logger.error("Exception reading albumName from song: " + songFile.getAbsolutePath(), e);
            }
        }
        // check getPlaylist()
        // File albumCover = new File(folder, album.getCoverImageFileName());
        // if (albumCover.exists()) {
        // album.setCoverImage(albumCover.toURI());
        // }
        if (library != null) {
            Album foundAlbum = library.getAlbum(album);
            if (foundAlbum != null) {
                album = foundAlbum;
            } else if (Settings.libraryVerified) {
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
     * @throws InterruptedException - when user interrupts
     */
    public static boolean isAlbum(File folder) throws InterruptedException {
        if (folder == null || !folder.exists() || !folder.isDirectory() || folder.list().length <= 1) {
            logger.debug("null/empty folder, non directory, non existant or directory with less than 1 files provided: "
                    + folder);
            return false;
        }
        File nfoFile = new File(folder, "album.nfo");
        if (nfoFile.exists()) {
            return true;
        }
        ArrayList<Song> songs = getSongs(folder);
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
     * @throws InterruptedException - when user interrupts
     */
    public static ArrayList<Song> getSongs(File folder) throws InterruptedException {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            logger.debug("null or non directory or non existant folder passed in getSongs");
            return null;
        }
        ArrayList<Song> songs = new ArrayList<>();
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (File file : folder.listFiles()) {
                interruptionHandler.throwInterruption();
                if (file.isFile() && extensions.contains(MusicTools.getExtension(file).toLowerCase())) {
                    Song song = getSong(file);
                    if (song != null) {
                        songs.add(song);
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
     * @throws InterruptedException - when user interrupts
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
                    String artistList = songData.get(FieldKey.ARTIST);
                    String[] artists = artistList.split(";");
                    for (String artist : artists) {
                        song.addArtist(new Artist(artist.strip()));
                    }
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
            logger.error("Unable to read file '" + file.getAbsolutePath() + "' metadata", e);
        }
        if (library != null) {
            Song foundSong = library.getSong(song);
            if (foundSong != null) {
                song = foundSong;
            } else if (Settings.libraryVerified) {
                song = null;
            }
        }
        return song;
    }
}