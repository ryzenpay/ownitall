package ryzen.ownitall;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.MusicTime;

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

    public void mergeAlbums(LinkedHashSet<Album> mergeAlbums) {
        if (mergeAlbums == null || mergeAlbums.isEmpty()) {
            return;
        }
        for (Album album : mergeAlbums) {
            if (this.albums.contains(album)) {
                this.getAlbum(album).merge(album);
            } else {
                this.albums.add(album);
            }
        }
    }

    public void mergePlaylists(LinkedHashSet<Playlist> mergePlaylists) {
        if (mergePlaylists == null || mergePlaylists.isEmpty()) {
            return;
        }
        for (Playlist playlist : mergePlaylists) {
            if (this.playlists.contains(playlist)) {
                this.getPlaylist(playlist).merge(playlist);
            } else {
                this.playlists.add(playlist);
            }
        }
    }

    public void mergeLikedSongs(LikedSongs mergeLikedSongs) {
        if (mergeLikedSongs == null || mergeLikedSongs.isEmpty()) {
            return;
        }
        this.likedSongs.addSongs(mergeLikedSongs.getSongs()); // handled by playlist addSongs
    }

    public void mergeCollection(Collection collection) {
        logger.info("Updating Music Collection");
        ProgressBar pb = Main.progressBar("Update Collection", 3);
        pb.setExtraMessage("Albums");
        this.mergeAlbums(collection.getAlbums());
        pb.setExtraMessage("Playlists").step();
        this.mergePlaylists(collection.getPlaylists());
        pb.setExtraMessage("Liked Songs").step();
        this.mergeLikedSongs(collection.getLikedSongs());
        pb.setExtraMessage("Done").step();
        pb.close();
    }

    public LikedSongs getLikedSongs() {
        return this.likedSongs;
    }

    /**
     * function to get standalone liked songs (not in any albums or playlists)
     * 
     * @return - linkedhashset of standalone liked songs
     */
    public LinkedHashSet<Song> getStandaloneLikedSongs() {
        LinkedHashSet<Song> likedSongs = new LinkedHashSet<>();
        for (Playlist playlist : this.playlists) {
            for (Song song : playlist.getSongs()) {
                if (!this.likedSongs.contains(song)) {
                    likedSongs.add(song);
                }
            }
        }
        for (Album album : this.albums) {
            for (Song song : album.getSongs()) {
                if (!this.likedSongs.contains(song)) {
                    likedSongs.add(song);
                }
            }
        }
        return likedSongs;
    }

    public LinkedHashSet<Album> getAlbums() {
        return new LinkedHashSet<>(this.albums);
    }

    public Album getAlbum(Album album) {
        for (Album thisAlbum : this.albums) {
            if (thisAlbum.equals(album)) {
                return thisAlbum;
            }
        }
        return null;
    }

    public LinkedHashSet<Playlist> getPlaylists() {
        return new LinkedHashSet<>(this.playlists);
    }

    public Playlist getPlaylist(Playlist playlist) {
        for (Playlist thisPlaylist : this.playlists) {
            if (thisPlaylist.equals(playlist)) {
                return thisPlaylist;
            }
        }
        return null;
    }

    /**
     * print the inventory depending on its "depth"
     * 
     * @param recursion - 1 = number count, 2 = album and playlist names, 3 =
     *                  albums, playlist and song names
     */
    public void printInventory(int recursion) {
        int playlistTrackCount = 0;
        int albumTrackCount = 0;
        for (Playlist playlist : this.playlists) {
            playlistTrackCount += playlist.size();
        }
        for (Album album : this.albums) {
            albumTrackCount += album.size();
        }
        int trackCount = this.getStandaloneLikedSongs().size() + playlistTrackCount + albumTrackCount;
        int i = 1;
        int y = 1;
        switch (recursion) {
            case 1:
                System.out
                        .println("Total playlists: " + this.playlists.size() + "  (" + playlistTrackCount + " songs)");
                System.out.println("Total albums: " + this.albums.size() + "  (" + albumTrackCount + " songs)");
                System.out.println("Total liked songs: " + this.likedSongs.size());
                System.out.println("With a total of " + trackCount + " songs");
                break;
            case 2:
                System.out.println("Liked Songs (" + this.likedSongs.size() + ")");
                System.out.println("Playlists (" + this.playlists.size() + "): (" + playlistTrackCount + " songs)");
                i = 1;
                for (Playlist playlist : this.playlists) {
                    System.out
                            .println(
                                    i + "/" + this.playlists.size() + " - " + playlist.getName() + " | "
                                            + playlist.size()
                                            + " - "
                                            + MusicTime.musicTime(MusicTime.totalDuration(playlist.getSongs())));
                    i++;
                }
                i = 1;
                System.out.println("Albums (" + this.albums.size() + "): (" + albumTrackCount + " songs)");
                for (Album album : this.albums) {
                    System.out
                            .println(i + "/" + this.albums.size() + " - " + album.getName() + " | " + album.size()
                                    + " - " + MusicTime.musicTime(MusicTime.totalDuration(album.getSongs())));
                    if (album.getArtists() != null) {
                        System.out.println("    - Artist: " + album.getArtists().toString());
                    }
                    i++;
                }
                break;
            case 3:
                System.out.println("Liked Songs (" + this.likedSongs.size() + "): ");
                i = 1;
                for (Song likedSong : this.likedSongs.getSongs()) {
                    System.out.println("    " + i + "/" + this.likedSongs.size() + " = " + likedSong.getName() + " | "
                            + MusicTime.musicTime(likedSong.getDuration()));
                    if (likedSong.getArtist() != null) {
                        System.out.println("        - Artist: " + likedSong.getArtist().toString());
                    }
                    i++;
                }
                System.out.println("Playlists (" + this.playlists.size() + "): (" + playlistTrackCount + " songs)");
                i = 1;
                for (Playlist playlist : this.playlists) {
                    y = 1;
                    System.out
                            .println(
                                    i + "/" + this.playlists.size() + " - " + playlist.getName() + " | "
                                            + playlist.size()
                                            + " - " + MusicTime.musicTime(
                                                    MusicTime.totalDuration(playlist.getSongs())));
                    i++;
                    for (Song song : playlist.getSongs()) {
                        if (likedSongs.contains(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + playlist.size() + " = " + song.getName() + " | "
                                + MusicTime.musicTime(song.getDuration()));
                        if (song.getArtist() != null) {
                            System.out.println("        - Artist: " + song.getArtist().toString());
                        }
                        y++;
                    }
                }
                i = 1;
                System.out.println("Albums (" + this.albums.size() + "): (" + albumTrackCount + " songs)");
                for (Album album : this.albums) {
                    y = 1;
                    System.out
                            .println(i + "/" + this.albums.size() + " - " + album.getName() + " | " + album.size()
                                    + " - " + MusicTime.musicTime(MusicTime.totalDuration(album.getSongs())));
                    i++;
                    for (Song song : album.getSongs()) {
                        if (likedSongs.contains(song)) {
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                        }
                        System.out.println("   " + y + "/" + album.size() + " = " + song.getName() + " | "
                                + MusicTime.musicTime(song.getDuration()));
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
    }

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

    private void optionDeletePlaylist() {
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        for (Playlist playlist : this.playlists) {
            options.put(playlist.toString(), playlist);
        }
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "PLAYLIST DELETION MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    return;
                } else {
                    this.playlists.remove(options.get(choice));
                    logger.info("Successfully removed playlist: " + choice);
                    break;
                }
            }
        }
    }

    private void optionMergePlaylist() {
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        for (Playlist playlist : this.playlists) {
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
                            this.playlists.remove(options.get(choice2));
                            logger.info("Successfully merged playlist: " + choice2 + " into: " + choice);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void optionDeleteAlbum() {
        LinkedHashMap<String, Album> options = new LinkedHashMap<>();
        for (Album album : this.albums) {
            options.put(album.toString(), album);
        }
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "ALBUM DELETION MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    return;
                } else {
                    this.albums.remove(options.get(choice));
                    logger.info("Successfully removed album: " + choice);
                    break;
                }
            }
        }
    }

    private void optionDeleteLikedSong() {
        LinkedHashMap<String, Song> options = new LinkedHashMap<>();
        for (Song song : this.likedSongs.getSongs()) {
            options.put(song.toString(), song);
        }
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "SONG DELETION MENU");
            if (choice != null) {
                if (choice.equals("Exit")) {
                    return;
                } else {
                    this.likedSongs.removeSong(options.get(choice));
                    logger.info("Successfully removed liked song: " + choice);
                    break;
                }
            }
        }
    }
}
