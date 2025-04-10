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
    private Download download;
    private Upload upload;
    private File localLibrary;

    public Local() throws InterruptedException {
        super();
        if ((Settings.uploadFolder == null || !Settings.uploadFolder.exists()) || this.localLibrary == null) {
            this.setLocalLibrary();
        }
    }

    private void setLocalLibrary() throws InterruptedException {
        while (this.localLibrary == null || !this.localLibrary.exists()) {
            System.out.print("Provide absolute path to local music library (folder): ");
            this.localLibrary = Input.request().getFile(true);

        }
    }

    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        if (upload == null) {
            upload = new Upload(localLibrary);
        }
        return upload.getLikedSongs();
    }

    @Override
    public void syncLikedSongs() throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.syncLikedSongs();
    }

    @Override
    public void uploadLikedSongs() throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.downloadLikedSongs();
    }

    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        if (upload == null) {
            upload = new Upload(localLibrary);
        }
        return upload.getPlaylists();
    }

    @Override
    public void syncPlaylists() throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.syncPlaylists();
    }

    @Override
    public void uploadPlaylists() throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.downloadPlaylists();
    }

    @Override
    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        File playlistFolder = new File(playlistId);
        return Upload.getPlaylist(playlistFolder);
    }

    @Override
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.syncPlaylist(playlist);
    }

    @Override
    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.downloadPlaylist(playlist);
    }

    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        if (upload == null) {
            upload = new Upload(localLibrary);
        }
        return upload.getAlbums();
    }

    @Override
    public void syncAlbums() throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.syncAlbums();
    }

    @Override
    public void uploadAlbums() throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.downloadAlbums();
    }

    @Override
    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        File albumFolder = new File(albumId);
        return Upload.getAlbum(albumFolder);
    }

    @Override
    public void syncAlbum(Album album) throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.syncAlbum(album);
    }

    @Override
    public void uploadAlbum(Album album) throws InterruptedException {
        if (download == null) {
            download = new Download(localLibrary);
        }
        download.downloadAlbum(album);
    }
}
