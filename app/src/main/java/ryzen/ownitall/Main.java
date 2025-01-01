
package ryzen.ownitall;

import java.util.Scanner;
import java.io.File;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.ArrayList;

public class Main {
    private static String DATAFOLDER = "data"; // TODO: user choice?
    private static LinkedHashMap<Album, ArrayList<Song>> albums;
    private static LinkedHashMap<Playlist, ArrayList<Song>> playlists;

    public static void main(String[] args) {
        Sync sync = new Sync(DATAFOLDER);
        Scanner scanner = new Scanner(System.in);

        if (!checkDataFolder()) {
            System.out.println("First time import");
            albums = new LinkedHashMap<>();
            playlists = new LinkedHashMap<>();
            promptForImport(scanner);
        } else {
            albums = new LinkedHashMap<>(sync.importAlbums());
            playlists = new LinkedHashMap<>(sync.importPlaylists());
        }
        while (true) {
            System.out.println("Choose an option: ");
            System.out.println("[1] import");
            System.out.println("[2] export");
            System.out.println("[3] print inventory");
            System.out.println("[4] save");
            System.out.println("[0] exit");
            System.out.print("Enter your choice: ");
            try {
                int choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:
                        promptForImport(scanner);
                    case 2:
                        // TODO: Implement export functionality
                        break;
                    case 3:
                        printInventory();
                    case 4:
                        saveData(sync);
                        break;
                    case 0:
                        saveData(sync);
                        System.out.println("Exiting program. Goodbye!");
                        scanner.close();
                        System.exit(0);
                    default:
                        System.err.println("Invalid option. Please try again.");
                }
            } catch (Exception e) {
                System.err.println("Invalid input. or an error occured:\n" + e);
            }
        }

    }

    private static boolean checkDataFolder() {
        File dataFolder = new File(DATAFOLDER);
        if (dataFolder.exists() && dataFolder.isDirectory()) {
            File albumFile = new File(DATAFOLDER + "/albums.ser");
            File playlistFile = new File(DATAFOLDER + "/playlists.ser");
            if (albumFile.exists() && playlistFile.exists()) {
                return true;
            }
        }
        return false;
    }

    private static void promptForImport(Scanner scanner) {
        while (true) {
            System.out.println("Choose an import option:");
            System.out.println("[1] YouTube");
            System.out.println("[2] Spotify");
            System.out.println("[3] Local");
            System.out.println("[0] Exit");
            System.out.print("Enter your choice: ");

            try {
                int importChoice = scanner.nextInt();
                scanner.nextLine();
                switch (importChoice) {
                    case 1:
                        // TODO: import from youtube
                        System.out.println("This function is yet to be added");
                        break;
                    case 2:
                        Spotify spotify = new Spotify(scanner); // TODO: check for existing credentials
                        playlists = new LinkedHashMap<>(spotify.getPlaylists()); // TODO: this overwrites, should it
                                                                                 // append?
                        playlists.put(new Playlist("liked songs"), spotify.getLikedSongs());
                        albums = new LinkedHashMap<>(spotify.getAlbums());
                        printInventory();
                        return;
                    case 3:
                        // TODO: import from local
                        System.out.println("This function is yet to be added");
                        break;
                    case 0:
                        System.out.println("Exiting import. Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid option. Please enter a number between 1-4.");
                        break;
                }
            } catch (Exception e) {
                System.err.println("Invalid input. or an error occured:\n" + e);
            }
        }
    }

    private static void saveData(Sync sync) {
        System.out.println("Beginning to save all data");
        sync.exportAlbums(new LinkedHashMap<>(albums));
        sync.exportPlaylists(new LinkedHashMap<>(playlists));
        System.out.println("Succesfully saved all data");
    }

    private static void printInventory() {
        int trackCount = 0;
        for (ArrayList<Song> songs : playlists.values()) {
            trackCount += songs.size();
        }
        for (ArrayList<Song> songs : albums.values()) {
            trackCount += songs.size();
        }
        System.out.println("Total playlists fetched: " + playlists.size());
        System.out.println("Total albums fetched: " + albums.size());
        System.out.println("With a total of " + trackCount + " songs");
    }
}
