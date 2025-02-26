package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.MusicTools;

public class CollectionMenu {
    private static final Logger logger = LogManager.getLogger(CollectionMenu.class);
    private static final Settings settings = Settings.load();
    private static Collection collection = Collection.load();
    private static Library library = Library.load();

    /**
     * default collectionmenu constructor initializing the menu
     */
    public CollectionMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Print Inventory", this::printInventory);
        options.put("Add to Inventory", this::addMenu);
        options.put("Update Inventory", this::optionUpdateInventory);
        options.put("Edit Inventory", this::editMenu);
        options.put("Clear Inventory", this::optionClearInventory);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "INVENTORY MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        }
    }

    private void addMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Add Album", this::optionAddAlbum);
        options.put("Add Playlist", this::optionAddPlaylist);
        options.put("Add Song", this::optionAddSong);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "ADD INVENTORY MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        }
    }

    private void optionAddAlbum() {
        String mbid = null;
        String albumName = null;
        String artistName = null;
        if (settings.isUseLibrary()) {
            try {
                System.out.print("Enter Album release MBID: ");
                mbid = Input.request().getString();
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting album MBID");
            }
        } else if (mbid == null) {
            try {
                while (albumName == null || albumName.isEmpty()) {
                    System.out.print("*Enter Album Name: ");
                    albumName = Input.request().getString();
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting Album Name");
                return;
            }
            try {
                System.out.print("Enter Album Main Artist: ");
                artistName = Input.request().getString();
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting Album Artist Name");
            }
        }
        Album album;
        if (mbid != null) {
            album = library.getAlbum(mbid);
            if (album == null) {
                return;
            }
        } else {
            album = new Album(albumName);
            if (artistName != null) {
                album.addArtist(new Artist(artistName));
            }
            if (settings.useLibrary) {
                try {
                    Album foundAlbum = library.getAlbum(album);
                    if (foundAlbum != null) {
                        album = foundAlbum;
                    } else if (settings.isLibraryVerified()) {
                        logger.info(
                                "Album was not found in library and `LibraryVerified` is set to true, not adding Album");
                        return;
                    }
                } catch (InterruptedException e) {
                    logger.debug("Interruption caught while getting album");
                    return;
                }
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
            String choice = Menu.optionMenu(options.keySet(), "PLAYLIST SELECTION MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    return;
                } else {
                    Song song = interactiveCreateSong();
                    if (song != null) {
                        options.get(choice).addSong(song);
                        logger.info("Succesfully added '" + song.getName() + "' to: '" + choice + "'");
                    }
                }
            }
        }
    }

    private Song interactiveCreateSong() {
        String mbid = null;
        String songName = null;
        String artistName = null;
        if (settings.isUseLibrary()) {
            try {
                System.out.print("Enter song MBID: ");
                mbid = Input.request().getString();
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting song mbid");
            }
        } else if (mbid == null) {
            try {
                while (songName == null || songName.isEmpty()) {
                    System.out.print("*Enter Song Name (without artists): ");
                    songName = Input.request().getString();
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while setting songname");
                return null;
            }
            try {
                System.out.print("Enter main artist name: ");
                artistName = Input.request().getString();
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting song's artist");
            }
        }
        Song song = null;
        if (mbid != null) {
            song = library.getSong(mbid);
        } else {
            song = new Song(songName);
            if (artistName != null) {
                song.setArtist(new Artist(artistName));
            }
            if (settings.isUseLibrary()) {
                try {
                    Song foundSong = library.getSong(song);
                    if (foundSong != null) {
                        song = foundSong;
                    } else if (settings.isLibraryVerified()) {
                        logger.info(
                                "Song was not found in library and `LibraryVerified` is set to true, not adding song");
                        return null;
                    }
                } catch (InterruptedException e) {
                    logger.debug("Interruption caugth while adding song");
                    return null;
                }
            }
        }
        return song;
    }

    /**
     * initializes edit menu for more options
     */
    public void editMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Delete Playlist", this::optionDeletePlaylist);
        options.put("Merge Playlists", this::optionMergePlaylist);
        options.put("Delete Album", this::optionDeleteAlbum);
        options.put("Delete Liked Song", this::optionDeleteLikedSong);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "EDIT INVENTORY MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        }
    }

    /**
     * option to delete playlist
     * lists all playlists with numbers and asks for an int input
     */
    private void optionDeletePlaylist() {
        while (true) {
            LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
            for (Playlist playlist : collection.getPlaylists()) {
                options.put(playlist.toString(), playlist);
            }
            String choice = Menu.optionMenu(options.keySet(), "PLAYLIST DELETION MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    return;
                } else {
                    collection.removePlaylist(options.get(choice));
                    logger.info("Successfully removed playlist: '" + choice + "'");
                }
            }
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
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "PLAYLIST MERGE INTO");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    return;
                } else {
                    Playlist playlist = options.get(choice);
                    options.remove(choice);
                    String choice2 = Menu.optionMenu(options.keySet(), "PLAYLIST MERGE FROM");
                    if (choice2 != null) {
                        if (choice2.equals("Exit")) {
                            return;
                        } else {
                            playlist.merge(options.get(choice2));
                            collection.removePlaylist(options.get(choice2));
                            logger.info("Successfully merged playlist: '" + choice2 + "' into: '" + choice + "'");
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * option to delete album
     */
    private void optionDeleteAlbum() {
        while (true) {
            LinkedHashMap<String, Album> options = new LinkedHashMap<>();
            for (Album album : collection.getAlbums()) {
                options.put(album.toString(), album);
            }
            String choice = Menu.optionMenu(options.keySet(), "ALBUM DELETION MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    return;
                } else {
                    collection.removeAlbum(options.get(choice));
                    logger.info("Successfully removed album: '" + choice + "'");
                }
            }
        }
    }

    /**
     * option to delete liked song
     */
    private void optionDeleteLikedSong() {
        while (true) {
            LinkedHashMap<String, Song> options = new LinkedHashMap<>();
            for (Song song : collection.getLikedSongs().getSongs()) {
                options.put(song.toString(), song);
            }
            String choice = Menu.optionMenu(options.keySet(), "SONG DELETION MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    return;
                } else {
                    collection.removeLikedSong(options.get(choice));
                    logger.info("Successfully removed liked song: '" + choice + "'");
                }
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
                Sync.load().clearInventory();
                logger.info("Successfully cleared inventory");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting clear inventory agreement");
        }
    }

    /**
     * library verify all of inventory
     */
    private void optionUpdateInventory() {
        if (!settings.isUseLibrary()) {
            logger.info("This requires library to be enabled");
            return;
        }
        logger.info("updating current collection with library...");
        try {
            for (Song song : collection.getLikedSongs().getSongs()) {
                Song foundSong = library.getSong(song);
                if (foundSong != null) {
                    song.setName(foundSong.getName());
                    song.setArtist(foundSong.getArtist());
                    if (foundSong.getCoverImage() != null) {
                        song.setCoverImage(foundSong.getCoverImage());
                    }
                    song.addIds(foundSong.getIds());
                }
            }
            for (Playlist playlist : collection.getPlaylists()) {
                for (Song song : playlist.getSongs()) {
                    Song foundSong = library.getSong(song);
                    if (foundSong != null) {
                        song.setName(foundSong.getName());
                        song.setArtist(foundSong.getArtist());
                        if (foundSong.getCoverImage() != null) {
                            song.setCoverImage(foundSong.getCoverImage());
                        }
                        song.addIds(foundSong.getIds());
                    }
                }
            }
            for (Album album : collection.getAlbums()) {
                Album foundAlbum = library.getAlbum(album);
                if (foundAlbum != null) {
                    album.setName(foundAlbum.getName());
                    album.addArtists(foundAlbum.getArtists());
                    if (foundAlbum.getCoverImage() != null) {
                        album.setCoverImage(foundAlbum.getCoverImage());
                    }
                    album.addIds(foundAlbum.getIds());
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interruption caught while verifying inventory");
            return;
        }
        logger.info("done updating collection content");
    }

    /**
     * print the inventory depending on its "depth"
     * 
     */
    public void printInventory() {
        int recursion;
        try {
            System.out.print("Select recursion (1-3): ");
            recursion = Input.request().getInt(1, 3);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting inventory recursion");
            return;
        }
        int playlistCount = collection.getPlaylists().size();
        int playlistTrackCount = 0;
        int albumCount = collection.getAlbums().size();
        int albumTrackCount = 0;
        for (Playlist playlist : collection.getPlaylists()) {
            playlistTrackCount += playlist.size();
        }
        for (Album album : collection.getAlbums()) {
            albumTrackCount += album.size();
        }
        int likedSongsTrackCount = collection.getLikedSongs().size();
        int trackCount = collection.getStandaloneLikedSongs().size() + playlistTrackCount + albumTrackCount;
        int i = 1;
        int y = 1;
        switch (recursion) {
            case 1:
                System.out
                        .println("Total playlists: " + playlistCount + "  (" + playlistTrackCount
                                + " songs)");
                System.out.println(
                        "Total albums: " + albumCount + "  (" + albumTrackCount + " songs)");
                System.out.println("Total liked songs: " + likedSongsTrackCount);
                System.out.println("With a total of " + trackCount + " songs");
                break;
            case 2:
                System.out.println("Liked Songs (" + collection.getLikedSongs().size() + ")");
                System.out.println(
                        "Playlists (" + playlistCount + "): (" + playlistTrackCount + " songs)");
                i = 1;
                for (Playlist playlist : collection.getPlaylists()) {
                    System.out
                            .println(
                                    i + "/" + playlistCount + " - " + playlist.getName() + " | "
                                            + playlist.size()
                                            + " - "
                                            + MusicTools.musicTime(playlist.getTotalDuration()));
                    i++;
                }
                i = 1;
                System.out.println("Albums (" + albumCount + "): (" + albumTrackCount + " songs)");
                for (Album album : collection.getAlbums()) {
                    System.out
                            .println(i + "/" + albumCount + " - " + album.getName() + " | " + album.size()
                                    + " - " + MusicTools.musicTime(album.getTotalDuration()));
                    if (album.getArtists() != null) {
                        System.out.println("    - Artist: " + album.getArtists().toString());
                    }
                    i++;
                }
                break;
            case 3:
                System.out.println("Liked Songs (" + likedSongsTrackCount + "): ");
                i = 1;
                for (Song likedSong : collection.getStandaloneLikedSongs()) {
                    System.out.println("    " + i + "/" + likedSongsTrackCount + " = " + likedSong.getName() + " | "
                            + MusicTools.musicTime(likedSong.getDuration()));
                    if (likedSong.getArtist() != null) {
                        System.out.println("        - Artist: " + likedSong.getArtist().toString());
                    }
                    i++;
                }
                System.out.println("Playlists (" + playlistCount + "): (" + playlistTrackCount + " songs)");
                i = 1;
                for (Playlist playlist : collection.getPlaylists()) {
                    y = 1;
                    System.out
                            .println(
                                    i + "/" + playlistCount + " - " + playlist.getName() + " | "
                                            + playlist.size()
                                            + " - " + MusicTools.musicTime(playlist.getTotalDuration()));
                    i++;
                    for (Song song : playlist.getSongs()) {
                        if (collection.isLiked(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + playlist.size() + " = " + song.getName() + " | "
                                + MusicTools.musicTime(song.getDuration()));
                        if (song.getArtist() != null) {
                            System.out.println("        - Artist: " + song.getArtist().toString());
                        }
                        y++;
                    }
                }
                i = 1;
                System.out.println("Albums (" + albumCount + "): (" + albumTrackCount + " songs)");
                for (Album album : collection.getAlbums()) {
                    y = 1;
                    System.out
                            .println(i + "/" + albumCount + " - " + album.getName() + " | " + album.size()
                                    + " - " + MusicTools.musicTime(album.getTotalDuration()));
                    i++;
                    for (Song song : album.getSongs()) {
                        if (collection.isLiked(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + album.size() + " = " + song.getName() + " | "
                                + MusicTools.musicTime(song.getDuration()));
                        if (song.getArtist() != null) {
                            System.out.println("        - Artist: " + song.getArtist().toString());
                        }
                        y++;
                    }
                }
                break;
            default:
                System.err.println("Invalid recursion option.");
                break;
        }
        System.out.print("Press enter to continue: ");
        Input.request().getEnter();
    }
}
