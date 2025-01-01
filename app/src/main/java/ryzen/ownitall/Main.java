
package ryzen.ownitall;

import java.util.Scanner;
import java.io.File;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;

public class Main {
    private static String DATAFOLDER = "data"; // TODO: user choice?
    private static Set<Album> albums;
    private static Set<Playlist> playlists;

    public static void main(String[] args) {
        albums = new LinkedHashSet<>();
        playlists = new LinkedHashSet<>();
        Sync sync = new Sync(DATAFOLDER);
        Scanner scanner = new Scanner(System.in);

        if (!checkDataFolder()) {
            promptForImport(scanner);
        } else {
            while (true) {
                System.out.println("Choose an option: ");
                System.out.println("[1] import");
                System.out.println("[2] export");
                System.out.println("[3] save");
                System.out.println("[0] exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        promptForImport(scanner);
                        break;
                    case 2:
                        // TODO: Implement export functionality
                        break;
                    case 3:
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
            }
        }
    }

    private static boolean checkDataFolder() {
        File dataFolder = new File(DATAFOLDER);
        return dataFolder.exists() && dataFolder.isDirectory() && dataFolder.list().length > 0;
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
                        return;
                    case 2:
                        Spotify spotify = new Spotify(); // TODO: check for existing credentials
                        playlists = new LinkedHashSet<>(spotify.getPlaylists());
                        playlists.add(new Playlist("liked songs", spotify.getLikedSongs()));
                        albums = new LinkedHashSet<>(spotify.getAlbums());
                        scanner.nextLine(); // TODO: this is fucking stupid, why do i have to do this each time
                        return;
                    case 3:
                        // TODO: import from local
                        System.out.println("This function is yet to be added");
                        return;
                    case 0:
                        System.out.println("Exiting import. Goodbye!");
                        System.exit(0);
                    default:
                        System.out.println("Invalid option. Please enter a number between 1-4.");
                }
            } catch (Exception e) {
                System.err.println("Invalid input. Please enter a numerical value." + e);
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static void saveData(Sync sync) {
        System.out.println("Beginning to save all data");
        sync.exportAlbums(new ArrayList<>(albums));
        sync.exportPlaylists(new ArrayList<>(playlists));
        System.out.println("Succesfully saved all data");
    }
}
