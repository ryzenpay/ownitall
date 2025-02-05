package ryzen.ownitall;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.MusicTools;

public class CollectionMenu {
    private static final Logger logger = LogManager.getLogger(CollectionMenu.class);
    private static final Collection collection = Collection.load();

    /**
     * default collectionmenu constructor initializing the menu
     */
    public CollectionMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Print Inventory", this::printInventory);
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
                    logger.info("Successfully removed playlist: " + choice);
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
                            logger.info("Successfully merged playlist: " + choice2 + " into: " + choice);
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
                    logger.info("Successfully removed album: " + choice);
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
                    logger.info("Successfully removed liked song: " + choice);
                }
            }
        }
    }

    /**
     * option to clear current collection
     */
    private void optionClearInventory() {
        System.out.print("Are you sure you want to clear the current inventory (y/N): ");
        if (Input.request().getAgreement()) {
            logger.info("Clearing inventory...");
            collection.clear();
            logger.info("Successfully cleared inventory");
        }
    }

    /**
     * print the inventory depending on its "depth"
     * 
     */
    public void printInventory() {
        System.out.print("Select recursion (1-3): ");
        int recursion = Input.request().getInt(1, 3);
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
