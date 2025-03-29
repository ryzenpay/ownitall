package ryzen.ownitall.output.cli;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Collection;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.CLIMenu;
import ryzen.ownitall.util.MusicTools;

public class CollectionMenu {
    private static final Logger logger = LogManager.getLogger(CollectionMenu.class);
    private static Collection collection = Collection.load();

    /**
     * default collectionmenu constructor initializing the menu
     */
    public CollectionMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import", this::optionImport);
        options.put("Export", this::optionExport);
        options.put("Sync", this::optionSync);
        options.put("Modify", this::optionModify);
        options.put("Print Inventory", this::printInventory);
        try {
            while (true) {
                String choice = CLIMenu.optionMenu(options.keySet(), "INVENTORY MENU");
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interruption caught while getting collection menu choice");
        }
    }

    private void optionImport() {
        try {
            new MethodMenu().importMenu();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting up import menu");
        }
    }

    private void optionExport() {
        try {
            new MethodMenu().exportMenu();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting up export menu");
        }
    }

    private void optionSync() {
        try {
            new MethodMenu().sync();
        } catch (InterruptedException e) {
            logger.debug("Interrutped while setting up sync menu");
        }
    }

    private void optionModify() {
        new ManualMenu();
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
