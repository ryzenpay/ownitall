
package ryzen.ownitall;

import java.io.File;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.time.Duration;

public class Main {
    private static String DATAFOLDER = "data"; // TODO: user choice?
    private static LinkedHashMap<Album, ArrayList<Song>> albums;
    private static LinkedHashMap<Playlist, ArrayList<Song>> playlists;
    private static LikedSongs likedSongs;

    public static void main(String[] args) {

        if (!checkDataFolder()) {
            albums = new LinkedHashMap<>();
            playlists = new LinkedHashMap<>();
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
            System.out.println("[0] exit");
            System.out.print("Enter your choice: ");
            try {
                int choice = Input.getInstance().getInt();
                switch (choice) {
                    case 1:
                        promptForImport();
                        break;
                    case 2:
                        System.out.println("This is currently not supported");
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

    private static boolean checkDataFolder() {
        File dataFolder = new File(DATAFOLDER);
        if (dataFolder.exists() && dataFolder.isDirectory()) {
            File albumFile = new File(DATAFOLDER + "/albums.ser");
            File playlistFile = new File(DATAFOLDER + "/playlists.ser");
            File likedSongsFile = new File(DATAFOLDER + "/likedsongs.ser");
            if (albumFile.exists() && playlistFile.exists() && likedSongsFile.exists()) {
                return true;
            }
        }
        return false;
    }

    private static void promptForImport() {
        while (true) {
            Sync sync = new Sync(DATAFOLDER);
            System.out.println("Choose an import option:");
            System.out.println("[1] YouTube");
            System.out.println("[2] Spotify");
            System.out.println("[3] Local");
            System.out.println("[0] Exit");
            System.out.print("Enter your choice: ");

            try {
                int importChoice = Input.getInstance().getInt();
                switch (importChoice) {
                    case 1:
                        YoutubeCredentials youtubeCredentials = sync.importYoutubeCredentials();
                        Youtube youtube;
                        if (youtubeCredentials == null) {
                            System.out.println("No saved youtube credential records");
                            youtube = new Youtube();
                        } else if (youtubeCredentials.isNull()) {
                            System.out.println("Incorrect youtube credentials found");
                            youtube = new Youtube();
                        } else {
                            youtube = new Youtube(youtubeCredentials);
                        }
                        sync.exportYoutubeCredentials(youtube.getYoutubeCredentials());
                        sync.close();
                        System.out.println(
                                "Getting all Youtube liked songs");
                        LikedSongs youtubeLikedSongs = youtube.getLikedSongs();
                        likedSongs.addSongs(youtubeLikedSongs.getSongs());
                        likedSongs.setYoutubePageToken(youtubeLikedSongs.getYoutubePageToken());
                        printInventory(1);
                        return;
                    case 2:
                        SpotifyCredentials spotifyCredentials = sync.importSpotifyCredentials();
                        Spotify spotify;
                        if (spotifyCredentials == null) {
                            System.out.println("No saved spotify credential records");
                            spotify = new Spotify();
                        } else if (spotifyCredentials.isNull()) {
                            System.out.println("Incorrect spotify credentials found");
                            spotify = new Spotify();
                        } else {
                            spotify = new Spotify(spotifyCredentials);
                        }
                        sync.exportSpotifyCredentials(spotify.getSpotifyCredentials());
                        sync.close();
                        System.out.println(
                                "Getting all spotify Playlists, Albums and liked songs: (This might take a minute)");
                        LikedSongs spotifyLikedSongs = spotify.getLikedSongs();
                        likedSongs.addSongs(spotifyLikedSongs.getSongs());
                        likedSongs.setSpotifyPageOffset(spotifyLikedSongs.getSpotifyPageOffset());
                        playlists.putAll(spotify.getPlaylists());
                        albums.putAll(spotify.getAlbums());
                        printInventory(1);
                        return;
                    case 3:
                        Local local = new Local();
                        System.out.println("Getting all music from your local library");
                        likedSongs.addSongs(local.getLikedSongs());
                        playlists.putAll(local.getPlaylists());
                        albums.putAll(local.getAlbums());
                        printInventory(1);
                        sync.close();
                        return;
                    case 0:
                        System.out.println("Exiting import.");
                        sync.close();
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

    private static void exportData() {
        Sync sync = new Sync(DATAFOLDER);
        System.out.println("Beginning to save all data");
        sync.exportAlbums(albums);
        sync.exportPlaylists(playlists);
        sync.exportLikedSongs(likedSongs);
        System.out.println("Successfully saved all data");
        sync.close();
    }

    private static void importData() {
        Sync sync = new Sync(DATAFOLDER);
        System.out.println("Beginning to import all data");
        albums = sync.importAlbums();
        if (albums == null) {
            albums = new LinkedHashMap<>();
        }
        playlists = sync.importPlaylists();
        if (playlists == null) {
            playlists = new LinkedHashMap<>();
        }
        likedSongs = sync.importLikedSongs();
        if (likedSongs == null) {
            likedSongs = new LikedSongs();
        }
        System.out.println("Succesfully imported all data");
        sync.close();
    }

    /**
     * print the inventory depending on its "depth"
     * 
     * @param recursion - 1 = number count, 2 = album and playlist names, 3 =
     *                  albums, playlist and song names
     */
    private static void printInventory(int recursion) {
        int trackCount = 0;
        for (ArrayList<Song> songs : playlists.values()) {
            trackCount += songs.size();
        }
        for (ArrayList<Song> songs : albums.values()) {
            trackCount += songs.size();
        }
        int i = 1;
        int y = 1;
        switch (recursion) {
            case 1:
                System.out.println("Total playlists: " + playlists.size());
                System.out.println("Total albums: " + albums.size());
                System.out.println("Total liked songs: " + likedSongs.getSize());
                System.out.println("With a total of " + trackCount + " songs");
                break;
            case 2:
                System.out.println("Liked Songs (" + likedSongs.getSize() + ")");
                System.out.println("Playlists (" + playlists.size() + "): ");
                i = 1;
                for (Playlist playlist : playlists.keySet()) {
                    ArrayList<Song> songs = playlists.get(playlist);
                    System.out
                            .println(i + "/" + playlists.size() + " - " + playlist.getName() + " | " + songs.size()
                                    + " - " + musicTime(totalDuration(songs)));
                    i++;
                }
                i = 1;
                System.out.println("Albums (" + albums.size() + "): ");
                for (Album album : albums.keySet()) {
                    ArrayList<Song> songs = albums.get(album);
                    System.out
                            .println(i + "/" + albums.size() + " - " + album.getName() + " | " + songs.size()
                                    + " - " + musicTime(totalDuration(songs)));
                    System.out.println("    - Artists: " + album.getArtists().toString());
                    i++;
                }
                break;
            case 3:
                System.out.println("Liked Songs (" + likedSongs.getSize() + "): ");
                i = 1;
                for (Song likedSong : likedSongs.getSongs()) {
                    System.out.println("    " + i + "/" + likedSongs.getSize() + " = " + likedSong.getName() + " | "
                            + musicTime(likedSong.getDuration()));
                    System.out.println("        - Artists: " + likedSong.getArtists().toString());
                    i++;
                }
                System.out.println("Playlists (" + playlists.size() + "): ");
                i = 1;
                for (Playlist playlist : playlists.keySet()) {
                    y = 1;
                    ArrayList<Song> songs = playlists.get(playlist);
                    System.out
                            .println(i + "/" + playlists.size() + " - " + playlist.getName() + " | " + songs.size()
                                    + " - " + musicTime(totalDuration(songs)));
                    i++;
                    for (Song song : songs) {
                        if (likedSongs.checkLiked(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + songs.size() + " = " + song.getName() + " | "
                                + musicTime(song.getDuration()));
                        System.out.println("        - Artists: " + song.getArtists().toString());
                        y++;
                    }
                }
                i = 1;
                System.out.println("Albums (" + albums.size() + "): ");
                for (Album album : albums.keySet()) {
                    y = 1;
                    ArrayList<Song> songs = albums.get(album);
                    System.out
                            .println(i + "/" + albums.size() + " - " + album.getName() + " | " + songs.size()
                                    + " - " + musicTime(totalDuration(songs)));
                    i++;
                    for (Song song : songs) {
                        if (likedSongs.checkLiked(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + songs.size() + " = " + song.getName() + " | "
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

    private static Duration totalDuration(ArrayList<Song> songs) {
        Duration totalDuration = Duration.ZERO;
        for (Song song : songs) {
            totalDuration = totalDuration.plus(song.getDuration());
        }
        return totalDuration;
    }

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
