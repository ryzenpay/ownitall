package ryzen.ownitall.ui.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Library;

/**
 * <p>
 * CollectionMenu class.
 * </p>
 *
 * @author ryzen
 */
@Controller
public class CollectionMenu {
    private static final Logger logger = new Logger(CollectionMenu.class);

    /**
     * <p>
     * collectionMenu.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection")
    public String collectionMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import", "/collection/import");
        options.put("Export", "/collection/export");
        options.put("Sync", "/collection/sync");
        options.put("Browse & Modify", "/collection/browse");
        model.addAttribute("menuName", "Collection Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/collection/return");
        return "menu";
    }

    /**
     * <p>
     * optionImport.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/import")
    public String optionImport() {
        return "redirect:/method/import";
    }

    /**
     * <p>
     * optionExport.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/export")
    public String optionExport() {
        return "redirect:/method/export";
    }

    /**
     * <p>
     * optionSync.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/sync")
    public String optionSync() {
        return "redirect:/method/sync";
    }

    /**
     * <p>
     * optionBrowse.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/browse")
    public String optionBrowse(Model model) {
        model.addAttribute("playlists", Collection.getPlaylists());
        model.addAttribute("albums", Collection.getAlbums());
        model.addAttribute("likedsongs", Collection.getLikedSongs());
        model.addAttribute("callback", "/collection");
        return "browse";
    }

    // TODO: edit playlists / liked songs / playlist songs
    /**
     * <p>addLikedSongForm.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/create/likedsongs/song")
    public String addLikedSongForm(Model model) {
        if (Library.load() == null) {
            model.addAttribute("warn", "Library must be enabled for this feature");
            return "redirect:/collection/browse";
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable songName = new FormVariable("songName");
        songName.setName("Song Name");
        songName.setRequired(true);
        fields.add(songName);
        FormVariable mainArtist = new FormVariable("artistName");
        mainArtist.setName("Main Artist");
        fields.add(mainArtist);
        model.addAttribute("formName", "Get Liked Song");
        model.addAttribute("values", fields);
        model.addAttribute("postUrl", "/collection/create/likedsongs/song");
        model.addAttribute("callback", "/collection/browse");
        return "form";
    }

    /**
     * <p>addLikedSong.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/collection/create/likedsongs/song")
    @ResponseBody
    public ResponseEntity<String> addLikedSong(Model model, @RequestBody LinkedHashMap<String, String> variables) {
        String songName = variables.get("songName");
        if (songName == null) {
            logger.debug(model, "Missing songName");
            return ResponseEntity.badRequest().body("missing songName");
        }
        String artistName = variables.get("artistName");
        Song song = new Song(variables.get("songName"));
        if (artistName != null) {
            song.addArtist(new Artist(artistName));
        }
        Library library = Library.load();
        if (library != null) {
            try {
                Song foundSong = library.getSong(song);
                if (foundSong != null) {
                    song = foundSong;
                } else if (Settings.libraryVerified) {
                    logger.warn(model,
                            "Song was not found in library and `LibraryVerified` is set to true, not adding song");
                    return ResponseEntity.badRequest().body(
                            "Song was not found in library and `LibraryVerified` is set to true, not adding song");
                }
                Collection.addLikedSong(song);
                return ResponseEntity.ok("Successfully added liked song '" + song.getName() + "'");
            } catch (InterruptedException e) {
                logger.debug(model, "Interruption caugth while adding song");
                return ResponseEntity.badRequest().body(
                        "Interruption caugth while adding song");
            }
        } else {
            return ResponseEntity.badRequest().body("Library must be enabled for this feature or library not set up");
        }
    }

    /**
     * <p>deleteLikedSong.</p>
     *
     * @param songName a {@link java.lang.String} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @DeleteMapping("/collection/likedsongs/{song}")
    @ResponseBody
    public ResponseEntity<String> deleteLikedSong(@PathVariable(value = "song") String songName) {
        Song song = Collection.getLikedSong(songName);
        if (song == null) {
            return ResponseEntity.badRequest().body("Unable to find song '" + songName + "' in collection likedsongs");
        }
        Collection.removeLikedSong(song);
        logger.debug("Successfully removed likedsong '" + song.getName() + "'");
        return ResponseEntity.ok("Successfully removed likedsong '" + song.getName() + "'");
    }

    /**
     * <p>addPlaylistSongForm.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @param playlistName a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/create/playlist/song")
    public String addPlaylistSongForm(Model model,
            @RequestParam(value = "playlist", required = false) String playlistName) {
        if (Library.load() == null) {
            model.addAttribute("warn", "Library must be enabled for this feature");
            return "redirect:/collection/browse";
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable collections = new FormVariable("playlistName");
        collections.setName("Playlist");
        collections.setDescription("Choose playlist to add song to");
        ArrayList<String> collectionOptions = new ArrayList<>();
        for (Playlist playlist : Collection.getPlaylists()) {
            collectionOptions.add(playlist.getName());
        }
        collections.setOptions(collectionOptions.toArray(new String[0]));
        collections.setRequired(true);
        if (playlistName != null) {
            collections.setValue(playlistName);
        }
        fields.add(collections);
        FormVariable songName = new FormVariable("songName");
        songName.setName("Song Name");
        songName.setRequired(true);
        fields.add(songName);
        FormVariable mainArtist = new FormVariable("artistName");
        mainArtist.setName("Main Artist");
        fields.add(mainArtist);
        model.addAttribute("formName", "Get Playlist Song");
        model.addAttribute("values", fields);
        model.addAttribute("postUrl", "/collection/create/playlist/song");
        model.addAttribute("callback", "/collection/browse");
        return "form";
    }

    /**
     * <p>addPlaylistSong.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/collection/create/playlist/song")
    @ResponseBody
    public ResponseEntity<String> addPlaylistSong(Model model, @RequestBody LinkedHashMap<String, String> variables) {
        String playlistName = variables.get("playlistName");
        if (playlistName == null) {
            logger.debug(model, "Missing playlistName");
            return ResponseEntity.badRequest().body("missing playlistName");
        }
        String songName = variables.get("songName");
        if (songName == null) {
            logger.debug(model, "Missing songName");
            return ResponseEntity.badRequest().body("missing songName");
        }
        Playlist playlist = Collection.getPlaylist(playlistName);
        if (playlist == null) {
            logger.debug(model, "Unable to find playlist '" + playlistName + "' in collection");
            return ResponseEntity.badRequest().body("Unable to find playlist '" + playlistName + "' in collection");
        }
        String artistName = variables.get("artistName");
        Song song = new Song(variables.get("songName"));
        if (artistName != null) {
            song.addArtist(new Artist(artistName));
        }
        Library library = Library.load();
        if (library != null) {
            try {
                Song foundSong = library.getSong(song);
                if (foundSong != null) {
                    song = foundSong;
                } else if (Settings.libraryVerified) {
                    logger.warn(model,
                            "Song was not found in library and `LibraryVerified` is set to true, not adding song");
                    return ResponseEntity.badRequest().body(
                            "Song was not found in library and `LibraryVerified` is set to true, not adding song");
                }
                playlist.addSong(song);
                return ResponseEntity.ok(
                        "Successfully added song '" + song.getName() + "' to playlist '" + playlist.getName() + "'");
            } catch (InterruptedException e) {
                logger.debug(model, "Interruption caugth while adding song");
                return ResponseEntity.badRequest().body(
                        "Interruption caugth while adding song");
            }
        } else {
            return ResponseEntity.badRequest().body("Library must be enabled for this feature or library not set up");
        }
    }

    /**
     * <p>addPlaylistForm.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/create/playlist")
    public String addPlaylistForm(Model model) {
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable playlistName = new FormVariable("playistName");
        playlistName.setRequired(true);
        fields.add(playlistName);
        FormVariable playlistCoverImage = new FormVariable("coverImage");
        fields.add(playlistCoverImage);
        model.addAttribute("formName", "Get Playlist");
        model.addAttribute("values", fields);
        model.addAttribute("postUrl", "/collection/create/playlist");
        model.addAttribute("callback", "/collection/browse");
        return "form";
    }

    /**
     * <p>addPlaylist.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/collection/create/playlist")
    @ResponseBody
    public ResponseEntity<String> addPlaylist(Model model, @RequestBody LinkedHashMap<String, String> variables) {
        String playlistName = variables.get("playlistName");
        if (playlistName == null) {
            logger.debug(model, "Missing playlistName");
            return ResponseEntity.badRequest().body("missing playlistName");
        }
        String coverImage = variables.get("coverImage");
        Playlist playlist = new Playlist(playlistName);
        if (coverImage != null) {
            playlist.setCoverImage(coverImage);
        }
        Collection.addPlaylist(playlist);
        logger.debug(model, "Successfully added playlist '" + playlist.getName() + "'");
        return ResponseEntity.ok("Successfully added playlist '" + playlist.getName() + "'");
    }

    /**
     * <p>deletePlaylist.</p>
     *
     * @param playlistName a {@link java.lang.String} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @DeleteMapping("/collection/playlist/{playlist}")
    @ResponseBody
    public ResponseEntity<String> deletePlaylist(@PathVariable(value = "playlist") String playlistName) {
        Playlist playlist = Collection.getPlaylist(playlistName);
        if (playlist == null) {
            return ResponseEntity.badRequest().body("Unable to find playlist '" + playlistName + "' in collection");
        }
        Collection.removePlaylist(playlist);
        logger.debug("Successfully deleted playlist '" + playlist.getName() + "'");
        return ResponseEntity.ok("Successfully deleted playlist '" + playlist.getName() + "'");
    }

    /**
     * <p>deletePlaylistSong.</p>
     *
     * @param playlistName a {@link java.lang.String} object
     * @param songName a {@link java.lang.String} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @DeleteMapping("/collection/playlist/{playlist}/{song}")
    @ResponseBody
    public ResponseEntity<String> deletePlaylistSong(@PathVariable(value = "playlist") String playlistName,
            @PathVariable(value = "song") String songName) {
        Playlist playlist = Collection.getPlaylist(playlistName);
        if (playlist == null) {
            return ResponseEntity.badRequest().body("Unable to find playlist '" + playlistName + "' in collection");
        }
        for (Song song : playlist.getSongs()) {
            if (song.getName().equals(songName)) {
                playlist.removeSong(song);
                logger.debug("Successfully deleted song '" + song.getName() + "' from playlist '" + playlist.getName()
                        + "'");
                return ResponseEntity.ok("Successfully deleted song '" + song.getName() + "' from playlist '"
                        + playlist.getName() + "'");
            }
        }
        return ResponseEntity.badRequest()
                .body("Unable to find song '" + songName + "' in  playlist '" + playlist.getName() + "'");
    }

    /**
     * <p>addAlbumForm.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/create/album")
    public String addAlbumForm(Model model) {
        if (Library.load() == null) {
            model.addAttribute("warn", "Library must be enabled for this feature");
            return "redirect:/collection/browse";
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable albumName = new FormVariable("albumName");
        albumName.setName("Album Name");
        albumName.setRequired(true);
        fields.add(albumName);
        FormVariable mainArtist = new FormVariable("artistName");
        mainArtist.setName("Main Artist");
        fields.add(mainArtist);
        model.addAttribute("formName", "Get Album");
        model.addAttribute("values", fields);
        model.addAttribute("postUrl", "/collection/create/album");
        model.addAttribute("callback", "/collection/browse");
        return "form";
    }

    /**
     * <p>addAlbum.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/collection/create/album")
    @ResponseBody
    public ResponseEntity<String> addAlbum(Model model, @RequestBody LinkedHashMap<String, String> variables) {
        String albumName = variables.get("albumName");
        if (albumName == null) {
            logger.debug(model, "Missing albumName");
            return ResponseEntity.badRequest().body("missing albumName");
        }
        String artistName = variables.get("artistName");
        Album album = new Album(albumName);
        if (artistName != null) {
            album.addArtist(new Artist(artistName));
        }
        Library library = Library.load();
        if (library != null) {
            try {
                Album foundAlbum = library.getAlbum(album);
                if (foundAlbum != null) {
                    album = foundAlbum;
                } else if (Settings.libraryVerified) {
                    logger.warn(
                            "Album was not found in library and `LibraryVerified` is set to true, not adding Album");
                    return ResponseEntity.badRequest().body(
                            "Album was not found in library and `LibraryVerified` is set to true, not adding Album");
                }
                Collection.addAlbum(album);
                logger.info("Successfully added album '" + album.toString() + "' to collection");
                return ResponseEntity.ok("Successfully added album '" + album.toString() + "' to collection");
            } catch (InterruptedException e) {
                logger.debug("Interruption caught while getting album");
                return ResponseEntity.badRequest().body("Interruption caught while getting album");
            }
        } else {
            return ResponseEntity.badRequest().body("Library must be enabled for this feature or library not set up");
        }
    }

    /**
     * <p>deleteAlbum.</p>
     *
     * @param albumName a {@link java.lang.String} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @DeleteMapping("/collection/album/{album}")
    @ResponseBody
    public ResponseEntity<String> deleteAlbum(@PathVariable(value = "album") String albumName) {
        Album album = Collection.getAlbum(albumName);
        if (album == null) {
            return ResponseEntity.badRequest().body("Unable to find album '" + albumName + "' in collection");
        }
        Collection.removeAlbum(album);
        logger.debug("Successfully deleted album '" + album.getName() + "'");
        return ResponseEntity.ok("Successfully deleted album '" + album.getName() + "'");
    }

    /**
     * <p>
     * optionReturn.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/return")
    public String optionReturn(Model model) {
        return "redirect:/";
    }
}
