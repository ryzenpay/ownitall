package ryzen.ownitall;

import java.util.LinkedHashSet;

import ryzen.ownitall.tools.MusicTime;

public class Collection {
    private LikedSongs likedSongs;
    private PlaylistSet playlists;
    private AlbumSet albums;

    public Collection() {
        this.likedSongs = new LikedSongs();
        this.playlists = new PlaylistSet();
        this.albums = new AlbumSet();
    }

    public void mergeAlbums(LinkedHashSet<Album> mergeAlbums) {
        for (Album album : mergeAlbums) {
            if (this.albums.contains(album)) {
                this.albums.get(album).merge(album);
            } else {
                this.albums.add(album);
            }
        }
    }

    public void mergePlaylists(LinkedHashSet<Playlist> mergePlaylists) {
        for (Playlist playlist : playlists) {
            if (this.playlists.contains(playlist)) {
                this.playlists.get(playlist).merge(playlist);
            } else {
                this.playlists.add(playlist);
            }
        }
    }

    public void mergeLikedSongs(LikedSongs mergeLikedSongs) {
        this.likedSongs.addSongs(mergeLikedSongs.getSongs()); // handled by playlist addSongs
    }

    public void mergeCollection(Collection collection) {
        this.mergeAlbums(collection.getAlbums());
        this.mergePlaylists(collection.getPlaylists());
        this.mergeLikedSongs(collection.getLikedSongs());
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
        int playlistTrackCount = 0;
        int albumTrackCount = 0;
        for (Playlist playlist : this.playlists) {
            playlistTrackCount += playlist.size();
        }
        for (Album album : this.albums) {
            albumTrackCount += album.size();
        }
        int trackCount = this.getTrackCount();
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

    public int getTrackCount() {
        int trackCount = 0;
        for (Playlist playlist : this.playlists) {
            trackCount += playlist.size();
        }
        for (Album album : this.albums) {
            trackCount += album.size();
        }
        return this.getStandaloneLikedSongs().size() + trackCount;
    }
}
