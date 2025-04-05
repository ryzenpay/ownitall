package ryzen.ownitall.methods;

import java.io.File;
import java.util.ArrayList;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.methods.local.Download;
import ryzen.ownitall.methods.local.Upload;
import ryzen.ownitall.util.Input;

public class Local extends Method {
    private static final Settings settings = Settings.load();
    private Download download;
    private Upload upload;
    private File localLibrary;

    public Local() throws InterruptedException {
        super();
        if (settings.isEmpty("uploadfolder") || this.localLibrary == null) {
            this.setLocalLibrary();
        }
        upload = new Upload(localLibrary);
        download = new Download(localLibrary);
    }

    private void setLocalLibrary() throws InterruptedException {
        while (this.localLibrary == null || !this.localLibrary.exists()) {
            System.out.print("Provide absolute path to local music library (folder): ");
            this.localLibrary = Input.request().getFile(true);

        }
    }

    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        return upload.getLikedSongs();
    }

    @Override
    public void syncLikedSongs() throws InterruptedException {
        download.syncLikedSongs();
    }

    @Override
    public void uploadLikedSongs() throws InterruptedException {
        download.downloadLikedSongs();
    }

    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        return upload.getPlaylists();
    }

    @Override
    public void syncPlaylists() throws InterruptedException {
        download.syncPlaylists();
    }

    @Override
    public void uploadPlaylists() throws InterruptedException {
        download.downloadPlaylists();
    }

    @Override
    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        File playlistFolder = new File(playlistId);
        return Upload.getPlaylist(playlistFolder);
    }

    @Override
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        download.syncPlaylist(playlist);
    }

    @Override
    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        download.downloadPlaylist(playlist);
    }

    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        return upload.getAlbums();
    }

    @Override
    public void syncAlbums() throws InterruptedException {
        download.syncAlbums();
    }

    @Override
    public void uploadAlbums() throws InterruptedException {
        download.downloadAlbums();
    }

    @Override
    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        File albumFolder = new File(albumId);
        return Upload.getAlbum(albumFolder);
    }

    @Override
    public void syncAlbum(Album album) throws InterruptedException {
        download.syncAlbum(album);
    }

    @Override
    public void uploadAlbum(Album album) throws InterruptedException {
        download.downloadAlbum(album);
    }
}
