package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.Progressbar;

public class CollectionMenu {
    private static final Logger logger = LogManager.getLogger(CollectionMenu.class);
    private static Collection collection = Collection.load();
    private static Library library = Library.load();

    /**
     * default collectionmenu constructor initializing the menu
     */
    public CollectionMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Print Inventory", this::printInventory);
        options.put("Update Inventory", this::optionUpdateInventory);
        options.put("Edit Inventory", this::editMenu);
        options.put("Clear Inventory", this::optionClearInventory);
        try {
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
        } catch (InterruptedException e) {
            logger.debug("Interruption caught while getting collection menu choice");
        }
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
        try {
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
            for (Playlist playlist : collection.getPlaylists()) {
                options.put(playlist.toString(), playlist);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "PLAYLIST DELETION MENU");
                if (choice != null) {
                    if (choice.equals("Exit")) {
                        return;
                    } else {
                        collection.removePlaylist(options.get(choice));
                        logger.info("Successfully removed playlist: '" + choice + "'");
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting collection delete playlist choice");
                return;
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
        try {
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
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting collection merge playlist choice");
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
            try {
                String choice = Menu.optionMenu(options.keySet(), "ALBUM DELETION MENU");
                if (choice != null) {
                    if (choice.equals("Exit")) {
                        return;
                    } else {
                        collection.removeAlbum(options.get(choice));
                        logger.info("Successfully removed album: '" + choice + "'");
                    }
                }
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
            for (Song song : collection.getLikedSongs().getSongs()) {
                options.put(song.toString(), song);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "SONG DELETION MENU");
                if (choice != null) {
                    if (choice.equals("Exit")) {
                        return;
                    } else {
                        collection.removeLikedSong(options.get(choice));
                        logger.info("Successfully removed liked song: '" + choice + "'");
                    }
                }
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

    /**
     * library verify all of inventory
     */
    private void optionUpdateInventory() {
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
        switch (recursion) {
            case 1:
                this.printInventoryR1();
                break;
            case 2:
                this.printInventoryR2();
                break;
            case 3:
                this.printInventoryR3();
                break;
            default:
                System.err.println("Invalid recursion option.");
                break;
        }
        try {
            System.out.print("Press enter to continue: ");
            Input.request().getEnter();
        } catch (InterruptedException e) {
        }
    }

    public void printInventoryR1() {
        System.out
                .println("Total playlists: " + collection.getPlaylistCount() + "  ("
                        + collection.getPlaylistsTrackCount()
                        + " songs)");
        System.out.println(
                "Total albums: " + collection.getAlbumCount() + "  (" + collection.getAlbumsTrackCount()
                        + " songs)");
        System.out.println("Total liked songs: " + collection.getLikedSongs().size());
        System.out.println("With a total of " + collection.getTotalTrackCount() + " songs");
    }

    public void printInventoryR2() {
        System.out.println("Liked Songs (" + collection.getLikedSongs().size() + ")");
        System.out.println(
                "Playlists (" + collection.getPlaylistCount() + "): (" + collection.getPlaylistsTrackCount()
                        + " songs)");
        int i = 1;
        for (Playlist playlist : collection.getPlaylists()) {
            System.out
                    .println(
                            i + "/" + collection.getPlaylistCount() + " - " + playlist.getName() + " | "
                                    + playlist.size()
                                    + " - "
                                    + MusicTools.musicTime(playlist.getTotalDuration()));
            i++;
        }
        i = 1;
        System.out.println(
                "Albums (" + collection.getAlbumCount() + "): (" + collection.getAlbumsTrackCount() + " songs)");
        for (Album album : collection.getAlbums()) {
            System.out
                    .println(i + "/" + collection.getAlbumCount() + " - " + album.getName() + " | " + album.size()
                            + " - " + MusicTools.musicTime(album.getTotalDuration()));
            if (album.getArtists() != null) {
                System.out.println("    - Artist: " + album.getArtists().toString());
            }
            i++;
        }
    }

    public void printInventoryR3() {
        System.out.println("Liked Songs (" + collection.getLikedSongs().size() + "): ");
        int i = 1;
        for (Song likedSong : collection.getStandaloneLikedSongs()) {
            System.out
                    .println("    " + i + "/" + collection.getLikedSongs().size() + " = " + likedSong.getName() + " | "
                            + MusicTools.musicTime(likedSong.getDuration()));
            if (likedSong.getArtist() != null) {
                System.out.println("        - Artist: " + likedSong.getArtist().toString());
            }
            i++;
        }
        System.out.println("Playlists (" + collection.getPlaylistCount() + "): (" + collection.getPlaylistsTrackCount()
                + " songs)");
        i = 1;
        for (Playlist playlist : collection.getPlaylists()) {
            int y = 1;
            System.out
                    .println(
                            i + "/" + collection.getPlaylistCount() + " - " + playlist.getName() + " | "
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
        System.out.println(
                "Albums (" + collection.getAlbumCount() + "): (" + collection.getAlbumsTrackCount() + " songs)");
        for (Album album : collection.getAlbums()) {
            int y = 1;
            System.out
                    .println(i + "/" + collection.getAlbumCount() + " - " + album.getName() + " | " + album.size()
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
    }
}
