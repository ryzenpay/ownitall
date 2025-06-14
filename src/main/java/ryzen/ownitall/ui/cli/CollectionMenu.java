package ryzen.ownitall.ui.cli;

import java.util.LinkedHashMap;

import ryzen.ownitall.Collection;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.method.interfaces.Export;
import ryzen.ownitall.method.interfaces.Import;
import ryzen.ownitall.method.interfaces.Sync;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.exceptions.MenuClosed;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * CollectionMenu class.
 * </p>
 *
 * @author ryzen
 */
public class CollectionMenu {
    private static final Logger logger = new Logger(CollectionMenu.class);

    /**
     * default collectionmenu constructor initializing the menu
     */
    public CollectionMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import", this::optionImport);
        options.put("Export", this::optionExport);
        options.put("Sync", this::optionSync);
        options.put("Modify", this::optionModify);
        options.put("Browse", this::printInventory);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "INVENTORY MENU");
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    private void optionImport() {
        try {
            new MethodMenu(Import.class).importMenu();
        } catch (InterruptedException | MissingSettingException e) {
            logger.debug("Interrupted while setting up import menu");
        }
    }

    private void optionExport() {
        try {
            new MethodMenu(Export.class).exportMenu();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting up export menu");
        } catch (MissingSettingException e) {
            logger.warn("Unable to set up selected method due to missing settings: "
                    + e.getMessage());
        }
    }

    private void optionSync() {
        try {
            new MethodMenu(Sync.class).syncMenu();
        } catch (InterruptedException | MissingSettingException e) {
            logger.debug("Interrupted while setting up sync menu");
        }
    }

    private void optionModify() {
        new ModifyMenu();
    }

    /**
     * print the inventory depending on its "depth"
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
    }

    /**
     * <p>
     * printInventoryR1.
     * </p>
     */
    public void printInventoryR1() {
        System.out
                .println("Total playlists: " + Collection.getPlaylistCount() + "  ("
                        + Collection.getPlaylistsTrackCount()
                        + " songs)");
        System.out.println(
                "Total albums: " + Collection.getAlbumCount() + "  (" + Collection.getAlbumsTrackCount()
                        + " songs)");
        System.out.println("Total liked songs: " + Collection.getLikedSongCount());
        System.out.println("With a total of " + Collection.getTotalTrackCount() + " songs");
    }

    /**
     * <p>
     * printInventoryR2.
     * </p>
     */
    public void printInventoryR2() {
        System.out.println("Liked Songs (" + Collection.getTotalTrackCount() + ")");
        System.out.println(
                "Playlists (" + Collection.getPlaylistCount() + "): (" + Collection.getPlaylistsTrackCount()
                        + " songs)");
        int i = 1;
        for (Playlist playlist : Collection.getPlaylists()) {
            System.out
                    .println(
                            i + "/" + Collection.getPlaylistCount() + " - " + playlist.getName() + " | "
                                    + playlist.size()
                                    + " - "
                                    + MusicTools.musicTime(playlist.getTotalDuration()));
            i++;
        }
        i = 1;
        System.out.println(
                "Albums (" + Collection.getAlbumCount() + "): (" + Collection.getAlbumsTrackCount() + " songs)");
        for (Album album : Collection.getAlbums()) {
            System.out
                    .println(i + "/" + Collection.getAlbumCount() + " - " + album.getName() + " | " + album.size()
                            + " - " + MusicTools.musicTime(album.getTotalDuration()));
            if (album.getArtists() != null) {
                System.out.println("    - Artist: " + album.getArtists().toString());
            }
            i++;
        }
    }

    /**
     * <p>
     * printInventoryR3.
     * </p>
     */
    public void printInventoryR3() {
        System.out.println("Liked Songs (" + Collection.getTotalTrackCount() + "): ");
        int i = 1;
        for (Song likedSong : Collection.getStandaloneLikedSongs()) {
            System.out
                    .println("    " + i + "/" + Collection.getTotalTrackCount() + " = " + likedSong.getName() + " | "
                            + MusicTools.musicTime(likedSong.getDuration()));
            if (likedSong.getMainArtist() != null) {
                System.out.println("        - Artist: " + likedSong.getMainArtist().toString());
            }
            i++;
        }
        System.out.println("Playlists (" + Collection.getPlaylistCount() + "): (" + Collection.getPlaylistsTrackCount()
                + " songs)");
        i = 1;
        for (Playlist playlist : Collection.getPlaylists()) {
            int y = 1;
            System.out
                    .println(
                            i + "/" + Collection.getPlaylistCount() + " - " + playlist.getName() + " | "
                                    + playlist.size()
                                    + " - " + MusicTools.musicTime(playlist.getTotalDuration()));
            i++;
            for (Song song : playlist.getSongs()) {
                if (Collection.isLiked(song)) {
                    System.out.print("*");
                } else {
                    System.out.print(" ");
                }
                System.out.println("   " + y + "/" + playlist.size() + " = " + song.getName() + " | "
                        + MusicTools.musicTime(song.getDuration()));
                if (song.getMainArtist() != null) {
                    System.out.println("        - Artist: " + song.getMainArtist().toString());
                }
                y++;
            }
        }
        i = 1;
        System.out.println(
                "Albums (" + Collection.getAlbumCount() + "): (" + Collection.getAlbumsTrackCount() + " songs)");
        for (Album album : Collection.getAlbums()) {
            int y = 1;
            System.out
                    .println(i + "/" + Collection.getAlbumCount() + " - " + album.getName() + " | " + album.size()
                            + " - " + MusicTools.musicTime(album.getTotalDuration()));
            i++;
            for (Song song : album.getSongs()) {
                if (Collection.isLiked(song)) {
                    System.out.print("*");
                } else {
                    System.out.print(" ");
                }
                System.out.println("   " + y + "/" + album.size() + " = " + song.getName() + " | "
                        + MusicTools.musicTime(song.getDuration()));
                if (song.getMainArtist() != null) {
                    System.out.println("        - Artist: " + song.getMainArtist().toString());
                }
                y++;
            }
        }
    }
}
