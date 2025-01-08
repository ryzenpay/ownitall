
package ryzen.ownitall;

import java.io.File;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.time.Duration;

public class Main {
    // TODO: initialize logger and remove all system.out.println &&
    // system.err.println
    private static String DATAFOLDER = "data"; // TODO: user choice?
    private static LinkedHashSet<Album> albums;
    private static LinkedHashSet<Playlist> playlists;
    private static LikedSongs likedSongs;

    public static void main(String[] args) {

        if (!checkDataFolder()) {
            albums = new LinkedHashSet<>();
            playlists = new LinkedHashSet<>();
            likedSongs = new LikedSongs();
        } else {
            importData();
        }
        while (true) {
            System.out.println("Choose an option: ");
            System.out.println("[1] import");
            System.out.println("[2] export");
            System.out.println("[3] print inventory");
            System.out.println("[4] save");
            System.out.println("[5] Settings"); // TODO: settings class (save in .appdata/settings.json)
            System.out.println("[0] exit");
            System.out.print("Enter your choice: ");
            try {
                int choice = Input.getInstance().getInt();
                switch (choice) {
                    case 1:
                        Import dataImport = new Import(DATAFOLDER);
                        likedSongs.addSongs(dataImport.getLikedSongs()); // TODO: this currently overwrites, but needs
                                                                         // to merge their song arrays
                        albums.addAll(dataImport.getAlbums());
                        playlists.addAll(dataImport.getPlaylists());
                        dataImport.printOverview();
                        break;
                    case 2:
                        System.out.println("Export currently not supported");
                        // TODO: Implement export functionality
                        break;
                    case 3:
                        printInventory(3);
                        break;
                    case 4:
                        exportData();
                        break;
                    case 0:
                        exportData();
                        System.out.println("Exiting program. Goodbye!");
                        System.exit(0);
                    default:
                        System.err.println("Invalid option. Please try again.");
                        break;
                }
            } catch (Exception e) {
                System.err.println("Invalid input. or an error occured:\n" + e);
            }
        }

    }

    /**
     * check if existing data files exist
     * 
     * @return - true if exist, false if not
     */
    private static boolean checkDataFolder() {
        File dataFolder = new File(DATAFOLDER);
        if (dataFolder.exists() && dataFolder.isDirectory()) {
            File albumFile = new File(DATAFOLDER + "/albums.json");
            File playlistFile = new File(DATAFOLDER + "/playlists.json");
            File likedSongsFile = new File(DATAFOLDER + "/likedsongs.json");
            if (albumFile.exists() && playlistFile.exists() && likedSongsFile.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * export all current data
     */
    private static void exportData() {
        Sync sync = new Sync(DATAFOLDER);
        System.out.println("Beginning to save all data");
        sync.exportAlbums(albums);
        sync.exportPlaylists(playlists);
        sync.exportLikedSongs(likedSongs);
        System.out.println("Successfully saved all data");
    }

    /**
     * import data from local files
     */
    private static void importData() {
        Sync sync = new Sync(DATAFOLDER);
        System.out.println("Beginning to import all data");
        albums = sync.importAlbums();
        if (albums == null) {
            albums = new LinkedHashSet<>();
        }
        playlists = sync.importPlaylists();
        if (playlists == null) {
            playlists = new LinkedHashSet<>();
        }
        likedSongs = sync.importLikedSongs();
        if (likedSongs == null) {
            likedSongs = new LikedSongs();
        }
        System.out.println("Succesfully imported all data");
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
                                            + " - " + musicTime(totalDuration(playlist.getSongs())));
                    i++;
                }
                i = 1;
                System.out.println("Albums (" + albums.size() + "): ");
                for (Album album : albums) {
                    System.out
                            .println(i + "/" + albums.size() + " - " + album.getName() + " | " + album.size()
                                    + " - " + musicTime(totalDuration(album.getSongs())));
                    System.out.println("    - Artists: " + album.getArtists().toString());
                    i++;
                }
                break;
            case 3:
                System.out.println("Liked Songs (" + likedSongs.size() + "): ");
                i = 1;
                for (Song likedSong : likedSongs.getSongs()) {
                    System.out.println("    " + i + "/" + likedSongs.size() + " = " + likedSong.getName() + " | "
                            + musicTime(likedSong.getDuration()));
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
                                            + " - " + musicTime(totalDuration(playlist.getSongs())));
                    i++;
                    for (Song song : playlist.getSongs()) {
                        if (likedSongs.checkLiked(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + playlist.size() + " = " + song.getName() + " | "
                                + musicTime(song.getDuration()));
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
                                    + " - " + musicTime(totalDuration(album.getSongs())));
                    i++;
                    for (Song song : album.getSongs()) {
                        if (likedSongs.checkLiked(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + album.size() + " = " + song.getName() + " | "
                                + musicTime(song.getDuration()));
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

    /**
     * convert duration into music time (mm:ss)
     * 
     * @param duration - constructed Duration
     * @return - string in format ((hh:)mm:ss)
     */
    private static String musicTime(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
