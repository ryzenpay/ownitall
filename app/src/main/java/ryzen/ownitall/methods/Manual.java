package ryzen.ownitall.methods;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Storage;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class Manual {
    // TODO: search needs capitalization?
    // lastfm for some reason is triggered by it
    private static final Logger logger = LogManager.getLogger(Manual.class);
    private static final Settings settings = Settings.load();
    private static Collection collection = Collection.load();
    private static Library library = Library.load();

    public Manual() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Add", this::addMenu);
        options.put("Merge", this::mergeMenu);
        options.put("Update", this::optionUpdateInventory);
        options.put("Delete", this::deleteMenu);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "MANUAL MENU");
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting manual menu choice");
        }
    }

    private void addMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Add Album", this::optionAddAlbum);
        options.put("Add Playlist", this::optionAddPlaylist);
        options.put("Add Song", this::optionAddSong);
        options.put("Add Artist", this::optionAddArtist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "ADD MENU");
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting manual add menu choice");
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
        if (library != null) {
            try {
                Album foundAlbum = library.getAlbum(album);
                if (foundAlbum != null) {
                    album = foundAlbum;
                } else if (settings.isLibraryVerified()) {
                    logger.warn(
                            "Album was not found in library and `LibraryVerified` is set to true, not adding Album");
                    return;
                }
            } catch (InterruptedException e) {
                logger.debug("Interruption caught while getting album");
                return;
            }
        }

        collection.addAlbum(album);
        logger.info("Successfully added album '" + album.toString() + "' to collection");
    }

    private void optionAddPlaylist() {
        String playlistName = null;
        try {
            while (playlistName == null || playlistName.isEmpty()) {
                System.out.print("*Enter Playlist Name: ");
                playlistName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Playlist Name");
            return;
        }
        Playlist playlist = new Playlist(playlistName);
        collection.addPlaylist(playlist);
        logger.info("Successfully added playlist '" + playlist.toString() + "' to collection");
    }

    private void optionAddSong() {
        while (true) {
            LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
            options.put("Liked Songs", collection.getLikedSongs());
            for (Playlist playlist : collection.getPlaylists()) {
                options.put(playlist.toString(), playlist);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "PLAYLIST SELECTION MENU");
                if (choice.equals("Exit")) {
                    return;
                }
                Song song = interactiveCreateSong();
                if (song != null) {
                    options.get(choice).addSong(song);
                    logger.info("Succesfully added '" + song.getName() + "' to: '" + choice + "'");
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting manual add song playlist option");
                return;
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
            logger.debug("Interrupted while setting songname");
            return null;
        }
        Song song = new Song(songName);
        if (artistName != null) {
            song.setArtist(new Artist(artistName));
        }
        if (library != null) {
            try {
                Song foundSong = library.getSong(song);
                if (foundSong != null) {
                    song = foundSong;
                } else if (settings.isLibraryVerified()) {
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
        if (settings.getLibrayType() != 1) {
            logger.warn("LastFM library type is required for this");
            return;
        }
        String artistName = null;
        try {
            while (artistName == null || artistName.isEmpty()) {
                System.out.print("*Enter Artist Name: ");
                artistName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Artist Name");
            return;
        }
        try {
            Artist artist = library.getArtist(new Artist(artistName));
            if (artist == null) {
                return;
            }
            ArrayList<Album> albums = library.getArtistAlbums(artist);
            if (albums != null) {
                collection.addAlbums(albums);
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
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting collection edit menu choice");
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
            for (Playlist playlist : collection.getPlaylists()) {
                options.put(playlist.toString(), playlist);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "PLAYLIST DELETION MENU");
                if (choice.equals("Exit")) {
                    return;
                }
                if (choice.equals("All")) {
                    collection.clearPlaylists();
                } else {
                    collection.removePlaylist(options.get(choice));
                }
                logger.info("Successfully removed playlist: '" + choice + "'");
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting collection delete playlist choice");
                return;
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
            for (Album album : collection.getAlbums()) {
                options.put(album.toString(), album);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "ALBUM DELETION MENU");
                if (choice.equals("Exit")) {
                    return;
                }
                if (choice.equals("All")) {
                    collection.clearAlbums();
                } else {
                    collection.removeAlbum(options.get(choice));
                }
                logger.info("Successfully removed album: '" + choice + "'");
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting collection delete album option");
                return;
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
            for (Song song : collection.getLikedSongs().getSongs()) {
                options.put(song.toString(), song);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "SONG DELETION MENU");
                if (choice.equals("Exit")) {
                    return;
                }
                if (choice.equals("All")) {
                    collection.clearLikedSongs();
                } else {
                    collection.removeLikedSong(options.get(choice));
                }
                logger.info("Successfully removed liked song: '" + choice + "'");
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting collection delete liked song choice");
                return;
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
                collection.clear();
                Storage.load().clearInventory();
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
                if (choice.equals("Exit")) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting manual merge menu choice");
        }
    }

    /**
     * prompts lists of playlists twice to merge them
     */
    private void optionMergePlaylist() {
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        for (Playlist playlist : collection.getPlaylists()) {
            options.put(playlist.toString(), playlist);
        }
        try {
            String choice = Menu.optionMenu(options.keySet(), "PLAYLIST MERGE INTO");
            if (choice.equals("Exit")) {
                return;
            }
            Playlist playlist = options.get(choice);
            options.remove(choice);
            String choice2 = Menu.optionMenu(options.keySet(), "PLAYLIST MERGE FROM");
            if (choice2.equals("Exit")) {
                return;
            }
            playlist.merge(options.get(choice2));
            collection.removePlaylist(options.get(choice2));
            logger.info("Successfully merged playlist: '" + choice2 + "' into: '" + choice + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting collection merge playlist choice");
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
        try (ProgressBar pb = Progressbar.progressBar("Updating Collection", collection.getTotalTrackCount())) {
            for (Song song : collection.getLikedSongs().getSongs()) {
                Song foundSong = library.getSong(song);
                if (foundSong != null) {
                    song.merge(foundSong);
                }
                pb.setExtraMessage(song.getName()).step();
            }
            for (Playlist playlist : collection.getPlaylists()) {
                for (Song song : playlist.getSongs()) {
                    Song foundSong = library.getSong(song);
                    if (foundSong != null) {
                        song.merge(foundSong);
                    }
                    pb.setExtraMessage(song.getName()).step();
                }
            }
            for (Album album : collection.getAlbums()) {
                Album foundAlbum = library.getAlbum(album);
                if (foundAlbum != null) {
                    album.merge(foundAlbum);
                }
                pb.setExtraMessage(album.getName()).stepBy(album.size());
            }
        } catch (InterruptedException e) {
            logger.debug("Interruption caught while verifying inventory");
            return;
        }
        logger.debug("done updating collection content");
    }
}
