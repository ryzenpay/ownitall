package ryzen.ownitall;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.time.Duration;
import ryzen.ownitall.tools.MusicTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final Settings settings = Settings.load();
    private static LinkedHashSet<Album> albums;
    private static LinkedHashSet<Playlist> playlists;
    private static LikedSongs likedSongs;
    private static LinkedHashMap<String, Runnable> options;

    public static void main(String[] args) {
        options = new LinkedHashMap<>();
        albums = new LinkedHashSet<>();
        playlists = new LinkedHashSet<>();
        likedSongs = new LikedSongs();
        if (Sync.checkDataFolder()) {
            importData();
        }
        // main menu
        options.put("Import", Main::optionImport);
        options.put("Export", Main::optionExport);
        options.put("Print Inventory", Main::optionInventory);
        options.put("Save", Main::optionSave);
        options.put("Tools", Main::optionTools);
        options.put("Settings", Main::optionSettings);
        while (true) {
            String choice = promptMenu();
            if (choice != null) {
                if (choice.equals("Exit")) {
                    exit();
                } else {
                    options.get(choice).run();
                }
            }
        }
    }

    private static String promptMenu() {
        System.out.println("Choose an option from the following: ");
        int i = 1;
        for (String option : options.keySet()) {
            System.out.println("[" + i + "] " + option);
            i++;
        }
        System.out.println("[0] Exit");
        System.out.print("Enter your choice: ");
        int choice = Input.getInstance().getInt();
        if (choice < 0 || choice > options.size()) {
            System.out.println("Incorrect option, try again");
            return null;
        }
        ArrayList<String> arrayOptions = new ArrayList<>(options.keySet());
        arrayOptions.add(0, "Exit");
        return arrayOptions.get(choice);
    }

    private static void optionImport() {
        Import dataImport = new Import(settings.dataFolderPath);
        likedSongs.addSongs(dataImport.getLikedSongs());
        mergeAlbums(dataImport.getAlbums()); // TODO: collection class to handle this
        mergePlaylists(dataImport.getPlaylists());
    }

    private static void optionExport() {
        System.out.println("Export currently not supported");
        // TODO: Implement export functionality
    }

    private static void optionInventory() {
        System.out.print("Select recursion (1-3): ");
        int recursion = Input.getInstance().getInt(1, 3);
        printInventory(recursion);
    }

    private static void optionSave() {
        exportData();
        settings.saveSettings();
    }

    private static void optionTools() {
        new Tools();
    }

    private static void optionSettings() {
        settings.changeSettings();
    }

    private static void exit() {
        System.out.println("Exiting program. Goodbye!");
        System.exit(0);
    }

    private static void mergeAlbums(LinkedHashSet<Album> mergeAlbums) {
        LinkedHashMap<Integer, Album> mappedAlbums = new LinkedHashMap<>();
        for (Album album : albums) {
            mappedAlbums.put(album.hashCode(), album);
        }
        for (Album album : mergeAlbums) {
            Album foundAlbum = mappedAlbums.get(album.hashCode());
            if (foundAlbum == null) {
                mappedAlbums.put(album.hashCode(), album);
            } else {
                foundAlbum.mergeAlbum(album);
            }
        }
        albums = new LinkedHashSet<>(mappedAlbums.values());
    }

    private static void mergePlaylists(LinkedHashSet<Playlist> mergePlaylists) {
        LinkedHashMap<Integer, Playlist> mappedPlaylists = new LinkedHashMap<>();
        for (Playlist playlist : playlists) {
            mappedPlaylists.put(playlist.hashCode(), playlist);
        }
        for (Playlist playlist : mergePlaylists) {
            Playlist foundPlaylist = mappedPlaylists.get(playlist.hashCode());
            if (foundPlaylist == null) {
                mappedPlaylists.put(playlist.hashCode(), playlist);
            } else {
                foundPlaylist.mergePlaylist(playlist);
            }
        }
        playlists = new LinkedHashSet<>(mappedPlaylists.values());
    }

    /**
     * export all current data
     */
    private static void exportData() {
        Sync sync = new Sync();
        logger.info("Saving all data...");
        sync.exportAlbums(albums);
        sync.exportPlaylists(playlists);
        sync.exportLikedSongs(likedSongs);
        logger.info("Successfully saved all data");
    }

    /**
     * import data from local files
     */
    public static void importData() {
        Sync sync = new Sync();
        logger.info("Importing all data...");
        mergeAlbums(sync.importAlbums());
        mergePlaylists(sync.importPlaylists());
        likedSongs.addSongs(sync.importLikedSongs().getSongs());
        logger.info("Succesfully imported all data");
    }

    /**
     * print the inventory depending on its "depth"
     * 
     * @param recursion - 1 = number count, 2 = album and playlist names, 3 =
     *                  albums, playlist and song names
     */
    private static void printInventory(int recursion) {
        int trackCount = 0;
        for (Playlist playlist : playlists) {
            trackCount += playlist.size();
        }
        for (Album album : albums) {
            trackCount += album.size();
        }
        int i = 1;
        int y = 1;
        switch (recursion) {
            case 1:
                System.out.println("Total playlists: " + playlists.size());
                System.out.println("Total albums: " + albums.size());
                System.out.println("Total liked songs: " + likedSongs.size());
                System.out.println("With a total of " + trackCount + " songs");
                break;
            case 2:
                System.out.println("Liked Songs (" + likedSongs.size() + ")");
                System.out.println("Playlists (" + playlists.size() + "): ");
                i = 1;
                for (Playlist playlist : playlists) {
                    System.out
                            .println(
                                    i + "/" + playlists.size() + " - " + playlist.getName() + " | " + playlist.size()
                                            + " - " + MusicTime.musicTime(totalDuration(playlist.getSongs())));
                    i++;
                }
                i = 1;
                System.out.println("Albums (" + albums.size() + "): ");
                for (Album album : albums) {
                    System.out
                            .println(i + "/" + albums.size() + " - " + album.getName() + " | " + album.size()
                                    + " - " + MusicTime.musicTime(totalDuration(album.getSongs())));
                    System.out.println("    - Artists: " + album.getArtists().toString());
                    i++;
                }
                break;
            case 3:
                System.out.println("Liked Songs (" + likedSongs.size() + "): ");
                i = 1;
                for (Song likedSong : likedSongs.getSongs()) {
                    System.out.println("    " + i + "/" + likedSongs.size() + " = " + likedSong.getName() + " | "
                            + MusicTime.musicTime(likedSong.getDuration()));
                    System.out.println("        - Artists: " + likedSong.getArtists().toString());
                    i++;
                }
                System.out.println("Playlists (" + playlists.size() + "): ");
                i = 1;
                for (Playlist playlist : playlists) {
                    y = 1;
                    System.out
                            .println(
                                    i + "/" + playlists.size() + " - " + playlist.getName() + " | " + playlist.size()
                                            + " - " + MusicTime.musicTime(totalDuration(playlist.getSongs())));
                    i++;
                    for (Song song : playlist.getSongs()) {
                        if (likedSongs.checkLiked(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + playlist.size() + " = " + song.getName() + " | "
                                + MusicTime.musicTime(song.getDuration()));
                        System.out.println("        - Artists: " + song.getArtists().toString());
                        y++;
                    }
                }
                i = 1;
                System.out.println("Albums (" + albums.size() + "): ");
                for (Album album : albums) {
                    y = 1;
                    System.out
                            .println(i + "/" + albums.size() + " - " + album.getName() + " | " + album.size()
                                    + " - " + MusicTime.musicTime(totalDuration(album.getSongs())));
                    i++;
                    for (Song song : album.getSongs()) {
                        if (likedSongs.checkLiked(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + album.size() + " = " + song.getName() + " | "
                                + MusicTime.musicTime(song.getDuration()));
                        System.out.println("        - Artists: " + song.getArtists().toString());
                        y++;
                    }
                }
                break;
            default:
                System.err.println("Invalid recursion option.");
                break;
        }
    }

    /**
     * get the total duration of an arraylist of songs
     * 
     * @param songs - arraylist of constructed Song
     * @return - constructed Duration representing total duration of arraylist of
     *         songs
     */
    private static Duration totalDuration(ArrayList<Song> songs) {
        Duration totalDuration = Duration.ZERO;
        for (Song song : songs) {
            totalDuration = totalDuration.plus(song.getDuration());
        }
        return totalDuration;
    }
}
