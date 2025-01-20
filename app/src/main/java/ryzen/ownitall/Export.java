package ryzen.ownitall;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.tools.Menu;

public class Export {
    private LinkedHashMap<String, Runnable> options;
    private Collection collection;

    public Export(Collection collection) {
        this.collection = collection;
        this.options = new LinkedHashMap<>();
        options.put("Download (YoutubeDL)", this::optionDownload);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "EXPORT");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    private void optionDownload() {
        Youtubedl youtubedl = new Youtubedl();
        ProgressBar pb = Main.progressBar("YoutubeDL Export", 3);
        pb.setExtraMessage("Liked songs");
        youtubedl.downloadLikedSongs(this.collection.getLikedSongs()); // TODO: if song is liked + playlist/album it
                                                                       // will
        // download twice
        pb.setExtraMessage("Playlists").step();
        LinkedHashSet<Playlist> playlists = this.collection.getPlaylists();
        ProgressBar pbPlaylist = Main.progressBar("Playlist Downloads", playlists.size());
        for (Playlist playlist : playlists) {
            pbPlaylist.setExtraMessage(playlist.getName());
            youtubedl.downloadPlaylist(playlist);
            pbPlaylist.step();
        }
        pbPlaylist.setExtraMessage("Done").step();
        pbPlaylist.close();
        pb.setExtraMessage("Albums").step();
        LinkedHashSet<Album> albums = this.collection.getAlbums();
        ProgressBar pbAlbum = Main.progressBar("Album Downloads", albums.size());
        for (Album album : albums) {
            pbAlbum.setExtraMessage(album.getName());
            youtubedl.downloadAlbum(album);
            pbAlbum.step();
        }
        pbAlbum.setExtraMessage("Done").step();
        pbAlbum.close();
        pb.setExtraMessage("Done").step();
        pb.close();
    }
}
