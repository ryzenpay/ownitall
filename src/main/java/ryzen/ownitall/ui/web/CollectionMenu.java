package ryzen.ownitall.ui.web;

import java.util.LinkedHashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import ryzen.ownitall.Collection;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;

/**
 * <p>
 * CollectionMenu class.
 * </p>
 *
 * @author ryzen
 */
@Controller
public class CollectionMenu {

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

    @DeleteMapping("/collection/likedsongs/{song}")
    @ResponseBody
    public ResponseEntity<String> deleteLikedSong(@PathVariable(value = "song") String songName) {
        Song song = Collection.getLikedSong(songName);
        if (song == null) {
            return ResponseEntity.badRequest().body("Unable to find song '" + songName + "' in collection likedsongs");
        }
        Collection.removeLikedSong(song);
        return ResponseEntity.ok("Successfully removed likedsong '" + song.getName() + "'");
    }

    @DeleteMapping("/collection/playlist/{playlist}")
    @ResponseBody
    public ResponseEntity<String> deletePlaylist(@PathVariable(value = "playlist") String playlistName) {
        Playlist playlist = Collection.getPlaylist(playlistName);
        if (playlist == null) {
            return ResponseEntity.badRequest().body("Unable to find playlist '" + playlistName + "' in collection");
        }
        Collection.removePlaylist(playlist);
        return ResponseEntity.ok("Successfully deleted playlist '" + playlist.getName() + "'");
    }

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
                return ResponseEntity.ok("Successfully deleted song '" + song.getName() + "' from playlist '"
                        + playlist.getName() + "'");
            }
        }
        return ResponseEntity.badRequest()
                .body("Unable to find song '" + songName + "' in  playlist '" + playlist.getName() + "'");
    }

    @DeleteMapping("/collection/album/{album}")
    @ResponseBody
    public ResponseEntity<String> deleteAlbum(@PathVariable(value = "album") String albumName) {
        Album album = Collection.getAlbum(albumName);
        if (album == null) {
            return ResponseEntity.badRequest().body("Unable to find album '" + albumName + "' in collection");
        }
        Collection.removeAlbum(album);
        return ResponseEntity.ok("Successfully deleted album '" + album.getName() + "'");
    }

    @DeleteMapping("/collection/album/{album}/{song}")
    @ResponseBody
    public ResponseEntity<String> deleteAlbumSong(@PathVariable(value = "album") String albumName,
            @PathVariable(value = "song") String songName) {
        Album album = Collection.getAlbum(albumName);
        if (album == null) {
            return ResponseEntity.badRequest().body("Unable to find album '" + albumName + "' in collection");
        }
        for (Song song : album.getSongs()) {
            if (song.getName().equals(songName)) {
                album.removeSong(song);
                return ResponseEntity
                        .ok("Successfully deleted song '" + song.getName() + "' from album '" + album.getName() + "'");
            }
        }
        return ResponseEntity.badRequest()
                .body("Unable to find song '" + songName + "' in  album '" + album.getName() + "'");
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
