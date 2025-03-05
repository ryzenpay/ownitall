package ryzen.ownitall.methods.menu;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Library;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;

public class ManualMenu {
    private static final Logger logger = LogManager.getLogger(ManualMenu.class);
    private static final Settings settings = Settings.load();
    private static Collection collection = Collection.load();
    private static Library library = Library.load();

    public ManualMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Add Album", this::optionAddAlbum);
        options.put("Add Playlist", this::optionAddPlaylist);
        options.put("Add Song", this::optionAddSong);
        options.put("Add Artist", this::optionAddArtist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "ADD INVENTORY MENU");
                if (choice != null) {
                    if (choice.equals("Exit")) {
                        break;
                    } else {
                        options.get(choice).run();
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting manual menu choice");
        }
    }

    private void optionAddAlbum() {
        String albumName = null;
        String artistName = null;
        try {
            while (albumName == null || albumName.isEmpty()) {
                System.out.print("*Enter Album Name: ");
                albumName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Album Name");
            return;
        }
        try {
            System.out.print("Enter Album Main Artist: ");
            artistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Album Artist Name");
        }
        Album album = new Album(albumName);
        if (artistName != null) {
            album.addArtist(new Artist(artistName));
        }
        if (library != null) {
            try {
                Album foundAlbum = library.getAlbum(album);
                if (foundAlbum != null) {
                    album = foundAlbum;
                } else if (settings.isLibraryVerified()) {
                    logger.warn(
                            "Album was not found in library and `LibraryVerified` is set to true, not adding Album");
                    return;
                }
            } catch (InterruptedException e) {
                logger.debug("Interruption caught while getting album");
                return;
            }
        }

        collection.addAlbum(album);
        logger.info("Successfully added album '" + album.toString() + "' to collection");
    }

    private void optionAddPlaylist() {
        String playlistName = null;
        try {
            while (playlistName == null || playlistName.isEmpty()) {
                System.out.print("*Enter Playlist Name: ");
                playlistName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Playlist Name");
            return;
        }
        Playlist playlist = new Playlist(playlistName);
        collection.addPlaylist(playlist);
        logger.info("Successfully added playlist '" + playlist.toString() + "' to collection");
    }

    private void optionAddSong() {
        while (true) {
            LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
            options.put("Liked Songs", collection.getLikedSongs());
            for (Playlist playlist : collection.getPlaylists()) {
                options.put(playlist.toString(), playlist);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "PLAYLIST SELECTION MENU");
                if (choice != null) {
                    if (choice.equals("Exit")) {
                        return;
                    } else {
                        Song song = interactiveCreateSong();
                        if (song != null) {
                            options.get(choice).addSong(song);
                            logger.info("Succesfully added '" + song.getName() + "' to: '" + choice + "'");
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting manual add song playlist option");
                return;
            }
        }
    }

    private Song interactiveCreateSong() {
        String songName = null;
        String artistName = null;

        try {
            while (songName == null || songName.isEmpty()) {
                System.out.print("*Enter Song Name (without artists): ");
                songName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting songname");
            return null;
        }
        try {
            System.out.print("Enter main artist name: ");
            artistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting song's artist");
        }

        Song song = new Song(songName);
        if (artistName != null) {
            song.setArtist(new Artist(artistName));
        }
        if (library != null) {
            try {
                Song foundSong = library.getSong(song);
                if (foundSong != null) {
                    song = foundSong;
                } else if (settings.isLibraryVerified()) {
                    logger.warn(
                            "Song was not found in library and `LibraryVerified` is set to true, not adding song");
                    return null;
                }
            } catch (InterruptedException e) {
                logger.debug("Interruption caugth while adding song");
                return null;
            }
        }
        return song;
    }

    private void optionAddArtist() {
        if (settings.getLibrayType() != 1) {
            logger.warn("LastFM library type is required for this");
            return;
        }
        String artistName = null;
        try {
            while (artistName == null || artistName.isEmpty()) {
                System.out.print("*Enter Artist Name: ");
                artistName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Artist Name");
            return;
        }
        try {
            Artist artist = library.getArtist(new Artist(artistName));
            if (artist == null) {
                return;
            }
            LinkedHashSet<Album> albums = library.getArtistAlbums(artist);
            if (albums != null) {
                collection.addAlbums(albums);
                logger.info("Successfully added " + albums.size() + " albums from '" + artist.toString()
                        + "' to collection");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while adding artist");
            return;
        }
    }
}
