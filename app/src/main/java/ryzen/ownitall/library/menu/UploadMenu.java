package ryzen.ownitall.library.menu;

import java.io.File;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.library.Upload;
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
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "UPLOAD");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    private void optionUploadCollection() {
        Upload upload = new Upload();
        logger.info("Uploading local music...");
        ProgressBar pb = Progressbar.progressBar("Upload", 2);
        pb.setExtraMessage("Liked Songs");
        upload.getLikedSongs();
        pb.setExtraMessage("Saved Albums +/ Playlists").step();
        upload.processFolders();
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("done uploading local music");
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
        if (MusicTools.getExtension(folder).equalsIgnoreCase("m3u")) {
            this.collection.addPlaylist(Upload.processM3U(folder));
        } else {
            this.collection.addPlaylist(Upload.getPlaylist(folder));
        }
        logger.info("done uploading playlist");
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
        this.collection.addAlbum(Upload.getAlbum(folder));
        logger.info("Done uploading Album");

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
        this.collection.addLikedSongs(Upload.getSongs(folder));
        logger.info("Done Uploading Liked Songs");
    }
}
