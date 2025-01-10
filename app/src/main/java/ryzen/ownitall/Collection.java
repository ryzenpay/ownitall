package ryzen.ownitall;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.tools.MusicTime;

public class Collection {
    private static final Logger logger = LogManager.getLogger(Collection.class);
    private LikedSongs likedSongs;
    private LinkedHashSet<Playlist> playlists;
    private LinkedHashSet<Album> albums;

    public Collection() {
        this.likedSongs = new LikedSongs();
        this.playlists = new LinkedHashSet<>();
        this.albums = new LinkedHashSet<>();
    }

    /**
     * function to either load fresh data or load and append to current data from
     * local files
     * 
     */
    public void importData() {
        Sync sync = new Sync();
        logger.info("Importing all data...");
        this.mergeAlbums(sync.importAlbums());
        this.mergePlaylists(sync.importPlaylists());
        this.mergeLikedSongs(sync.importLikedSongs());
        logger.info("Succesfully imported all data");
    }

    /**
     * export a collection to local files
     * 
     */
    public void exportData() {
        Sync sync = new Sync();
        logger.info("Saving all data...");
        sync.exportAlbums(this.albums);
        sync.exportPlaylists(this.playlists);
        sync.exportLikedSongs(this.likedSongs);
        logger.info("Successfully saved all data");
    }

    public void mergeAlbums(LinkedHashSet<Album> mergeAlbums) {
        LinkedHashMap<Integer, Album> mappedAlbums = new LinkedHashMap<>();
        for (Album album : this.albums) {
            mappedAlbums.put(album.hashCode(), album);
        }
        for (Album album : mergeAlbums) {
            if (mappedAlbums.containsKey(album.hashCode())) {
                mappedAlbums.get(album.hashCode()).mergeAlbum(album);
            } else {
                mappedAlbums.put(album.hashCode(), album);
            }
        }
        this.albums = new LinkedHashSet<Album>(mappedAlbums.values());
    }

    public void mergePlaylists(LinkedHashSet<Playlist> mergePlaylists) {
        LinkedHashMap<Integer, Playlist> mappedPlaylists = new LinkedHashMap<>();
        for (Playlist playlist : playlists) {
            mappedPlaylists.put(playlist.hashCode(), playlist);
        }
        for (Playlist playlist : mergePlaylists) {
            if (mappedPlaylists.containsKey(playlist.hashCode())) {
                mappedPlaylists.get(playlist.hashCode()).mergePlaylist(playlist);
            } else {
                mappedPlaylists.put(playlist.hashCode(), playlist);
            }
        }
        this.playlists = new LinkedHashSet<>(mappedPlaylists.values());
    }

    public void mergeLikedSongs(LikedSongs mergeLikedSongs) {
        this.likedSongs.addSongs(mergeLikedSongs.getSongs());
    }

    public LikedSongs getLikedSongs() {
        return this.likedSongs;
    }

    public LinkedHashSet<Album> getAlbums() {
        return this.albums;
    }

    public LinkedHashSet<Playlist> getPlaylists() {
        return this.playlists;
    }

    /**
     * print the inventory depending on its "depth"
     * 
     * @param recursion - 1 = number count, 2 = album and playlist names, 3 =
     *                  albums, playlist and song names
     */
    public void printInventory(int recursion) {
        int trackCount = 0;
        for (Playlist playlist : this.playlists) {
            for (Song song : playlist.getSongs()) { // to prevent duplicate liked songs and entries
                if (!this.likedSongs.checkLiked(song)) {
                    trackCount++;
                }
            }
        }
        for (Album album : this.albums) {
            for (Song song : album.getSongs()) {
                if (!this.likedSongs.checkLiked(song)) {
                    trackCount++;
                }
            }
        }
        trackCount += this.likedSongs.size();
        int i = 1;
        int y = 1;
        switch (recursion) {
            case 1:
                System.out.println("Total playlists: " + this.playlists.size());
                System.out.println("Total albums: " + this.albums.size());
                System.out.println("Total liked songs: " + this.likedSongs.size());
                System.out.println("With a total of " + trackCount + " songs");
                break;
            case 2:
                System.out.println("Liked Songs (" + this.likedSongs.size() + ")");
                System.out.println("Playlists (" + this.playlists.size() + "): ");
                i = 1;
                for (Playlist playlist : this.playlists) {
                    System.out
                            .println(
                                    i + "/" + this.playlists.size() + " - " + playlist.getName() + " | "
                                            + playlist.size()
                                            + " - " + MusicTime.musicTime(totalDuration(playlist.getSongs())));
                    i++;
                }
                i = 1;
                System.out.println("Albums (" + this.albums.size() + "): ");
                for (Album album : this.albums) {
                    System.out
                            .println(i + "/" + this.albums.size() + " - " + album.getName() + " | " + album.size()
                                    + " - " + MusicTime.musicTime(totalDuration(album.getSongs())));
                    System.out.println("    - Artists: " + album.getArtists().toString());
                    i++;
                }
                break;
            case 3:
                System.out.println("Liked Songs (" + this.likedSongs.size() + "): ");
                i = 1;
                for (Song likedSong : this.likedSongs.getSongs()) {
                    System.out.println("    " + i + "/" + this.likedSongs.size() + " = " + likedSong.getName() + " | "
                            + MusicTime.musicTime(likedSong.getDuration()));
                    System.out.println("        - Artists: " + likedSong.getArtists().toString());
                    i++;
                }
                System.out.println("Playlists (" + this.playlists.size() + "): ");
                i = 1;
                for (Playlist playlist : this.playlists) {
                    y = 1;
                    System.out
                            .println(
                                    i + "/" + this.playlists.size() + " - " + playlist.getName() + " | "
                                            + playlist.size()
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
                System.out.println("Albums (" + this.albums.size() + "): ");
                for (Album album : this.albums) {
                    y = 1;
                    System.out
                            .println(i + "/" + this.albums.size() + " - " + album.getName() + " | " + album.size()
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
