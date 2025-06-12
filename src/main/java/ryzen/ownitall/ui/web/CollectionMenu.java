package ryzen.ownitall.ui.web;

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
        options.put("Import", "/method/import");
        options.put("Export", "/method/export");
        options.put("Sync", "/method/sync");
        options.put("Browse & Modify", "/collection/browse");
        return Templates.menu(model, "Collection Menu", options, "/collection/return");
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

    /**
     * <p>
     * addLikedSongForm.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/likedsongs/song")
    public String addLikedSongForm(Model model,
            @RequestParam(value = "callback", required = false) String callback) {
        if (callback == null) {
            callback = "/collection/browse";
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable name = new FormVariable("songName");
        name.setName("Song Name");
        name.setRequired(true);
        fields.add(name);
        FormVariable mainArtist = new FormVariable("artistName");
        mainArtist.setName("Main Artist");
        fields.add(mainArtist);
        return Templates.form(model, "Add Liked Song", fields, "/collection/likedsongs/song",
                callback);
    }

    /**
     * <p>
     * addLikedSong.
     * </p>
     *
     * @param model     a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/collection/likedsongs/song")
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

    @GetMapping("/collection/likedsongs/song/{song}")
    public String editLikedSongForm(Model model,
            @RequestParam(value = "callback", required = false) String callback,
            @PathVariable(value = "song") String songName) {
        if (callback == null) {
            callback = "/collection/browse";
        }
        Song song = Collection.getLikedSong(songName);
        if (song == null) {
            logger.warn(model, "Unable to find liked song '" + songName + "' in collection");
            return "redirect:" + callback;
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable name = new FormVariable("songName");
        name.setName("Song Name");
        name.setRequired(true);
        name.setValue(song.getName());
        fields.add(name);
        FormVariable mainArtist = new FormVariable("artistName");
        mainArtist.setName("Main Artist");
        mainArtist.setValue(song.getMainArtist().getName());
        fields.add(mainArtist);
        return Templates.form(model, "Edit Liked Song", fields, "/collection/likedsongs/song/" + song.getName(),
                callback);
    }

    @PostMapping("/collection/likedsongs/song/{song}")
    @ResponseBody
    public ResponseEntity<String> editLikedSong(Model model,
            @PathVariable(value = "song") String songName,
            @RequestBody LinkedHashMap<String, String> variables) {
        Song song = Collection.getLikedSong(songName);
        if (song == null) {
            logger.warn(model, "Unable to find song '" + songName + "' in collection");
            return ResponseEntity.badRequest().body("Unable to find song '" + songName + "' in collection");
        }
        String name = variables.get("songName");
        if (name == null) {
            logger.debug(model, "Missing songName");
            return ResponseEntity.badRequest().body("missing songName");
        }
        song.setName(name);
        String artistName = variables.get("artistName");
        if (artistName != null) {
            song.addArtist(new Artist(artistName));
        }
        return ResponseEntity.ok("Successfully modified liked song '" + song.getName() + "'");
    }

    /**
     * <p>
     * deleteLikedSong.
     * </p>
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
     * <p>
     * addPlaylistForm.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/playlist")
    public String addPlaylistForm(Model model,
            @RequestParam(value = "callback", required = false) String callback) {
        if (callback == null) {
            callback = "/collection/browse";
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable playlistName = new FormVariable("playistName");
        playlistName.setRequired(true);
        FormVariable playlistCoverImage = new FormVariable("coverImage");
        fields.add(playlistName);
        fields.add(playlistCoverImage);
        return Templates.form(model, "Add Playlist", fields, "/collection/playlist", callback);
    }

    /**
     * <p>
     * addPlaylist.
     * </p>
     *
     * @param model     a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/collection/playlist")
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

    @GetMapping("/collection/playlist/{playlist}")
    public String editPlaylistForm(Model model,
            @PathVariable(value = "playlist") String playlistName,
            @RequestParam(value = "callback", required = false) String callback) {
        if (callback == null) {
            callback = "/collection/browse";
        }
        Playlist playlist = Collection.getPlaylist(playlistName);
        if (playlist == null) {
            logger.info(model, "Unable to find playlist '" + playlistName + "' in collection");
            return "redirect:" + callback;
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable name = new FormVariable("playistName");
        name.setRequired(true);
        name.setValue(playlist.getName());
        fields.add(name);
        FormVariable coverImage = new FormVariable("coverImage");
        coverImage.setValue(playlist.getCoverImage().toString());
        fields.add(coverImage);
        return Templates.form(model, "Edit Playlist", fields, "/collection/playlist/" + playlist.getName(), callback);
    }

    @PostMapping("/collection/playlist/{playlist}")
    @ResponseBody
    public ResponseEntity<String> editPlaylist(Model model,
            @PathVariable(value = "playlist") String playlistName,
            @RequestBody LinkedHashMap<String, String> variables) {
        Playlist playlist = Collection.getPlaylist(playlistName);
        if (playlist == null) {
            return ResponseEntity.badRequest().body("Unable to find playlist in collection");
        }
        String name = variables.get("playlistName");
        if (name != null) {
            playlist.setName(name);
        }
        String coverImage = variables.get("coverImage");
        if (coverImage != null) {
            playlist.setCoverImage(coverImage);
        }
        // TODO: multiple choice with songs
        // needs form to be updated
        logger.info(model, "Successfully modified playlist '" + playlist.getName() + "'");
        return ResponseEntity.ok("Successfully modified playlist '" + playlist.getName() + "'");
    }

    /**
     * <p>
     * addPlaylistSongForm.
     * </p>
     *
     * @param model        a {@link org.springframework.ui.Model} object
     * @param playlistName a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/playlist/{playlist}/song")
    public String addPlaylistSongForm(Model model,
            @PathVariable(value = "playlist") String playlistName,
            @RequestParam(value = "callback", required = false) String callback) {
        if (callback == null) {
            callback = "/collection/browse";
        }
        Playlist playlist = Collection.getPlaylist(playlistName);
        if (playlist == null) {
            logger.warn(model, "Unable to find playlist '" + playlistName + "' in collection");
            return "redirect:" + callback;
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable songName = new FormVariable("songName");
        songName.setName("Song Name");
        songName.setRequired(true);
        fields.add(songName);
        FormVariable mainArtist = new FormVariable("artistName");
        mainArtist.setName("Main Artist");
        fields.add(mainArtist);
        return Templates.form(model, "Add Playlist Song", fields,
                "/collection/playlist/" + playlist.getName() + "/song",
                callback);
    }

    /**
     * <p>
     * addPlaylistSong.
     * </p>
     *
     * @param model     a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/collection/playlist/{playlist}/song")
    @ResponseBody
    public ResponseEntity<String> addPlaylistSong(Model model,
            @PathVariable(value = "playlist") String playlistName,
            @RequestBody LinkedHashMap<String, String> variables) {
        Playlist playlist = Collection.getPlaylist(playlistName);
        if (playlist == null) {
            logger.warn(model, "Unable to find playlist '" + playlistName + "' in collection");
            return ResponseEntity.badRequest().body("Unable to find playlist '" + playlistName + "' in collection");
        }
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

    @GetMapping("/collection/playlist/{playlist}/{song}")
    public String editPlaylistSongForm(Model model,
            @PathVariable(value = "playlist") String playlistName,
            @PathVariable(value = "song") String songName,
            @RequestParam(value = "callback", required = false) String callback) {
        if (callback == null) {
            callback = "/collection/browse";
        }
        Playlist playlist = Collection.getPlaylist(playlistName);
        if (playlist == null) {
            logger.warn(model, "Unable to find playlist '" + playlistName + "' in collection");
            return "redirect:" + callback;
        }
        Song song = playlist.getSong(songName);
        if (song == null) {
            logger.warn(model, "Unable to find song '" + songName + "' in playlist '" + playlist.getName() + "'");
            return "redirect:" + callback;
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable name = new FormVariable("songName");
        name.setName("Song Name");
        name.setRequired(true);
        name.setValue(song.getName());
        fields.add(name);
        FormVariable mainArtist = new FormVariable("artistName");
        mainArtist.setName("Main Artist");
        mainArtist.setValue(song.getMainArtist().toString());
        fields.add(mainArtist);
        return Templates.form(model, "Edit Playlist Song", fields,
                "/collection/playlist/" + playlist.getName() + "/" + song.getName(),
                callback);
    }

    @PostMapping("/collection/playlist/{playlist}/{song}")
    @ResponseBody
    public ResponseEntity<String> editPlaylistSong(Model model,
            @PathVariable(value = "playlist") String playlistName,
            @PathVariable(value = "song") String songName,
            @RequestBody LinkedHashMap<String, String> variables) {
        Playlist playlist = Collection.getPlaylist(songName);
        if (playlist == null) {
            logger.warn(model, "Unable to find playlist '" + playlistName + "' in collection");
            return ResponseEntity.badRequest().body("Unable to find playlist '" + playlistName + "' in collection");
        }
        Song song = playlist.getSong(songName);
        if (song == null) {
            logger.warn(model, "Unable to find song '" + songName + "' in playlist '" + playlist.getName() + "'");
            return ResponseEntity.badRequest().body(
                    "Unable to find song '" + songName + "' in playlist '" + playlist.getName() + "'");
        }
        String name = variables.get("songName");
        if (name == null) {
            logger.debug(model, "Missing songName");
            return ResponseEntity.badRequest().body("missing songName");
        }
        song.setName(name);
        String artistName = variables.get("artistName");
        if (artistName != null) {
            song.addArtist(new Artist(artistName));
        }
        return ResponseEntity.ok("Successfully modified song'" + song.getName() + "' in '" + playlist.getName() + "'");
    }

    /**
     * <p>
     * deletePlaylist.
     * </p>
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
     * <p>
     * deletePlaylistSong.
     * </p>
     *
     * @param playlistName a {@link java.lang.String} object
     * @param songName     a {@link java.lang.String} object
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
        Song song = playlist.getSong(songName);
        if (song == null) {
            logger.warn("Unable to find song '" + songName + "' in playlist '" + playlist.getName() + "'");
            return ResponseEntity.badRequest()
                    .body("Unable to find song '" + songName + "' in  playlist '" + playlist.getName() + "'");
        }
        playlist.removeSong(song);
        logger.info("Successfully deleted song '" + song.getName() + "' from playlist '" + playlist.getName()
                + "'");
        return ResponseEntity.ok("Successfully deleted song '" + song.getName() + "' from playlist '"
                + playlist.getName() + "'");
    }

    /**
     * <p>
     * addAlbumForm.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/album")
    public String addAlbumForm(Model model,
            @RequestParam(value = "callback", required = false) String callback) {
        if (callback == null) {
            callback = "/collection/browse";
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable albumName = new FormVariable("albumName");
        albumName.setName("Album Name");
        albumName.setRequired(true);
        fields.add(albumName);
        FormVariable mainArtist = new FormVariable("artistName");
        mainArtist.setName("Main Artist");
        fields.add(mainArtist);
        return Templates.form(model, "Get Album", fields, "/collection/album", callback);
    }

    /**
     * <p>
     * addAlbum.
     * </p>
     *
     * @param model     a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/collection/album")
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
     * <p>
     * deleteAlbum.
     * </p>
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
