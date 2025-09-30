package ryzen.ownitall.ui.cli;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Storage;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.LastFM;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.ProgressBar;
import ryzen.ownitall.util.exceptions.MenuClosed;

/**
 * <p>
 * ModifyMenu class.
 * </p>
 *
 * @author ryzen
 */
public class ModifyMenu {
    private static final Logger logger = new Logger(ModifyMenu.class);

    /**
     * <p>
     * Constructor for ModifyMenu.
     * </p>
     */
    public ModifyMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Add", this::addMenu);
        options.put("Merge", this::mergeMenu);
        options.put("Update", this::optionUpdateInventory);
        options.put("Delete", this::deleteMenu);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "MODIFY MENU");
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    private void addMenu() {
        if (Settings.libraryType.isEmpty()) {
            logger.warn("Manually adding requires library to be enabled in settings");
            return;
        }
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Add Album", this::optionAddAlbum);
        options.put("Add Playlist", this::optionAddPlaylist);
        options.put("Add Song", this::optionAddSong);
        options.put("Add Artist", this::optionAddArtist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "ADD MENU");
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    private void optionAddAlbum() {
        String albumName = null;
        String artistName = null;
        try {
            while (albumName == null || albumName.isEmpty()) {
                System.out.print("*Enter Album Name: ");
                albumName = Input.request().getString();
            }
            System.out.print("Enter Album Main Artist: ");
            artistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Album details");
            return;
        }
        Album album = new Album(albumName);
        if (artistName != null) {
            album.addArtist(new Artist(artistName));
        }
        Library library = Library.load();
        if (library != null) {
            try {
                Album foundAlbum = library.getAlbum(album);
                if (foundAlbum != null) {
                    album = foundAlbum;
                } else if (Settings.libraryVerified) {
                    logger.warn(
                            "Album was not found in library and `LibraryVerified` is set to true, not adding Album");
                    return;
                }
            } catch (InterruptedException e) {
                logger.debug("Interruption caught while getting album");
                return;
            }
        }

        Collection.addAlbum(album);
        logger.info("Successfully added album '" + album.toString() + "' to collection");
    }

    private void optionAddPlaylist() {
        String playlistName = null;
        URI coverImage = null;
        try {
            while (playlistName == null || playlistName.isEmpty()) {
                System.out.print("*Enter Playlist Name: ");
                playlistName = Input.request().getString();
            }
            System.out.print("Enter Cover image URL: ");
            coverImage = Input.request().getURL();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Playlist details");
            return;
        }
        Playlist playlist = new Playlist(playlistName);
        if (coverImage != null) {
            playlist.setCoverImage(coverImage);
        }
        Collection.addPlaylist(playlist);
        logger.info("Successfully added playlist '" + playlist.toString() + "' to collection");
    }

    private void optionAddSong() {
        while (true) {
            LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
            options.put("Liked Songs", Collection.getLikedSongs());
            for (Playlist playlist : Collection.getPlaylists()) {
                options.put(playlist.toString(), playlist);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "PLAYLIST SELECTION MENU");
                Song song = interactiveCreateSong();
                if (song != null) {
                    options.get(choice).addSong(song);
                    logger.info("Succesfully added '" + song.getName() + "' to: '" + choice + "'");
                }
            } catch (MenuClosed e) {
            }
        }
    }

    private Song interactiveCreateSong() {
        String songName = null;
        String artistName = null;

        try {
            while (songName == null || songName.isEmpty()) {
                System.out.print("*Enter Song Name (without artists): ");
                songName = Input.request().getString();
            }
            System.out.print("Enter main artist name: ");
            artistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting song details");
            return null;
        }
        Song song = new Song(songName);
        if (artistName != null) {
            song.addArtist(new Artist(artistName));
        }
        Library library = Library.load();
        if (library != null) {
            try {
                Song foundSong = library.getSong(song);
                if (foundSong != null) {
                    song = foundSong;
                } else if (Settings.libraryVerified) {
                    logger.warn(
                            "Song was not found in library and `LibraryVerified` is set to true, not adding song");
                    return null;
                }
            } catch (InterruptedException e) {
                logger.debug("Interruption caugth while adding song");
                return null;
            }
        }
        return song;
    }

    private void optionAddArtist() {
        if (!(Library.load() instanceof LastFM)) {
            logger.warn("LastFM library is required to add artist albums");
            return;
        }
        String artistName = null;
        try {
            while (artistName == null || artistName.isEmpty()) {
                System.out.print("*Enter Artist Name: ");
                artistName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Artist details");
            return;
        }
        Library library = Library.load();
        try {
            Artist artist = library.getArtist(new Artist(artistName));
            if (artist == null) {
                return;
            }
            ArrayList<Album> albums = library.getArtistAlbums(artist);
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Successfully added " + albums.size() + " albums from '" + artist.toString()
                        + "' to collection");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while adding artist");
        }
    }

    private void deleteMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Delete Playlist(s)", this::optionDeletePlaylist);
        options.put("Delete Album(s)", this::optionDeleteAlbum);
        options.put("Delete Liked Song(s)", this::optionDeleteLikedSong);
        options.put("Clear Collection", this::optionClearInventory);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "EDIT INVENTORY MENU");
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    /**
     * option to delete playlist
     * lists all playlists with numbers and asks for an int input
     */
    private void optionDeletePlaylist() {
        while (true) {
            LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
            options.put("All", null);
            for (Playlist playlist : Collection.getPlaylists()) {
                options.put(playlist.toString(), playlist);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "PLAYLIST DELETION MENU");
                if (choice.equals("All")) {
                    Collection.clearPlaylists();
                } else {
                    Collection.removePlaylist(options.get(choice));
                }
                logger.info("Successfully removed playlist: '" + choice + "'");
            } catch (MenuClosed e) {
                break;
            }
        }
    }

    /**
     * option to delete album
     */
    private void optionDeleteAlbum() {
        while (true) {
            LinkedHashMap<String, Album> options = new LinkedHashMap<>();
            options.put("All", null);
            for (Album album : Collection.getAlbums()) {
                options.put(album.toString(), album);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "ALBUM DELETION MENU");
                if (choice.equals("All")) {
                    Collection.clearAlbums();
                } else {
                    Collection.removeAlbum(options.get(choice));
                }
                logger.info("Successfully removed album: '" + choice + "'");
            } catch (MenuClosed e) {
            }
        }
    }

    /**
     * option to delete liked song
     */
    private void optionDeleteLikedSong() {
        while (true) {
            LinkedHashMap<String, Song> options = new LinkedHashMap<>();
            options.put("All", null);
            for (Song song : Collection.getLikedSongs().getSongs()) {
                options.put(song.toString(), song);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "SONG DELETION MENU");
                if (choice.equals("All")) {
                    Collection.clearLikedSongs();
                } else {
                    Collection.removeLikedSong(options.get(choice));
                }
                logger.info("Successfully removed liked song: '" + choice + "'");
            } catch (MenuClosed e) {
            }
        }
    }

    /**
     * option to clear current collection
     */
    private void optionClearInventory() {
        try {
            System.out.print("Are you sure you want to clear the current inventory (y/N): ");
            if (Input.request().getAgreement()) {
                logger.info("Clearing inventory...");
                Collection.clear();
                new Storage().clearInventoryFiles();
                logger.info("Successfully cleared inventory");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting clear inventory agreement");
        }
    }

    private void mergeMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Playlists", this::optionMergePlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "MERGE MENU");
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    /**
     * prompts lists of playlists twice to merge them
     */
    private void optionMergePlaylist() {
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        for (Playlist playlist : Collection.getPlaylists()) {
            options.put(playlist.toString(), playlist);
        }
        try {
            String choice = Menu.optionMenu(options.keySet(), "PLAYLIST MERGE INTO");
            Playlist playlist = options.get(choice);
            options.remove(choice);
            String choice2 = Menu.optionMenu(options.keySet(), "PLAYLIST MERGE FROM");
            playlist.merge(options.get(choice2));
            Collection.removePlaylist(options.get(choice2));
            logger.info("Successfully merged playlist: '" + choice2 + "' into: '" + choice + "'");
        } catch (MenuClosed e) {
        }
    }

    /**
     * library verify all of inventory
     */
    private void optionUpdateInventory() {
        Library library = Library.load();
        if (library == null) {
            logger.warn("This requires library to be enabled");
            return;
        }
        logger.debug("updating current collection with library...");
        try (ProgressBar pb = new ProgressBar("Updating Collection", Collection.getTotalSongCount())) {
            for (Song song : Collection.getLikedSongs().getSongs()) {
                Song foundSong = library.getSong(song);
                if (foundSong != null) {
                    song.merge(foundSong);
                }
                pb.step(song.getName());
            }
            for (Playlist playlist : Collection.getPlaylists()) {
                for (Song song : playlist.getSongs()) {
                    Song foundSong = library.getSong(song);
                    if (foundSong != null) {
                        song.merge(foundSong);
                    }
                    pb.step(song.getName());
                }
            }
            for (Album album : Collection.getAlbums()) {
                Album foundAlbum = library.getAlbum(album);
                if (foundAlbum != null) {
                    album.merge(foundAlbum);
                }
                pb.step(album.getName(), album.size());
            }
        } catch (InterruptedException e) {
            logger.debug("Interruption caught while verifying inventory");
            return;
        }
        logger.info("done updating collection content");
    }
}
