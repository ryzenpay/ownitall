package ryzen.ownitall.methods.menu;

import java.io.File;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.methods.Upload;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.Progressbar;

public class UploadMenu {
    private static final Logger logger = LogManager.getLogger(UploadMenu.class);
    private Collection collection = Collection.load();

    public UploadMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Upload Library", this::optionUploadCollection);
        options.put("Upload Playlist", this::optionUploadPlaylist);
        options.put("Upload Album", this::optionUploadAlbum);
        options.put("Upload Liked Songs", this::optionUploadLikedSongs);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "UPLOAD");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting upload menu choice");
        }
    }

    private void optionUploadCollection() {
        Upload upload;
        try {
            upload = new Upload();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting up upload");
            return;
        }
        logger.debug("Uploading local music...");
        try (ProgressBar pb = Progressbar.progressBar("Upload", 3)) {
            pb.setExtraMessage("Liked Songs");
            collection.addLikedSongs(upload.getLikedSongs());
            pb.setExtraMessage("Albums").step();
            collection.addAlbums(upload.getAlbums());
            pb.setExtraMessage("Playlists").step();
            collection.addPlaylists(upload.getPlaylists());
            pb.setExtraMessage("Done").step();
            logger.debug("done uploading local music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading collection");
        }
    }

    private void optionUploadPlaylist() {
        File folder = null;
        try {
            while (folder == null) {
                System.out.print("*Please provide playlist path / m3u file: ");
                folder = Input.request().getFile(true);
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted getting playlist path");
            return;
        }
        logger.info("Uploading local Playlist...");
        try {
            if (MusicTools.getExtension(folder).equalsIgnoreCase("m3u")) {
                collection.addPlaylist(Upload.getM3UPlaylist(folder));
            } else {
                collection.addPlaylist(Upload.getPlaylist(folder));
            }
            logger.info("done uploading playlist");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading playlist");
        }
    }

    private void optionUploadAlbum() {
        File folder = null;
        try {
            while (folder == null) {
                System.out.print("*Please provide album path: ");
                folder = Input.request().getFile(true);
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album path");
            return;
        }
        logger.info("Uploading local Album...");
        try {
            collection.addAlbum(Upload.getAlbum(folder));
            logger.info("Done uploading Album");
        } catch (InterruptedException e) {
            logger.debug("Interruption caught while uploading album");
        }

    }

    private void optionUploadLikedSongs() {
        File folder = null;
        try {
            while (folder == null) {
                System.out.print("*Please provide folder path: ");
                folder = Input.request().getFile(true);
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting liked songs path");
            return;
        }
        logger.info("Uploading Liked Songs...");
        try {
            collection.addLikedSongs(Upload.getLikedSongs(folder));
            logger.info("Done Uploading Liked Songs");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading liked songs");
        }
    }
}
