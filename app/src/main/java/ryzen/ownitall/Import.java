package ryzen.ownitall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class Import {
    private boolean status;
    private String dataFolder;

    private LinkedHashMap<Album, ArrayList<Song>> albums;
    private LinkedHashMap<Playlist, ArrayList<Song>> playlists;
    private LikedSongs likedSongs;

    public Import(String dataFolder) {
        this.dataFolder = dataFolder;
        this.status = false;
        this.albums = new LinkedHashMap<>();
        this.playlists = new LinkedHashMap<>();
        this.likedSongs = new LikedSongs();
        int choice = promptImport();
        while (!status) {
            switch (choice) {
                case 1:
                    this.importYoutube();
                    break;
                case 2:
                    this.importSpotify();
                    break;
                case 3:
                    this.importLocal();
                    break;
                case 0:
                    System.out.println("Exiting import.");
                    break;
                default:
                    System.out.println("Invalid option. Please enter a number between 0-3.");
                    break;
            }
        }
    }

    private int promptImport() {
        System.out.println("Choose an import option:");
        System.out.println("[1] YouTube");
        System.out.println("[2] Spotify");
        System.out.println("[3] Local");
        System.out.println("[0] Exit");
        System.out.print("Enter your choice: ");
        return Input.getInstance().getInt();
    }

    private void importYoutube() {
        Sync sync = new Sync(this.dataFolder);
        YoutubeCredentials youtubeCredentials = sync.importYoutubeCredentials();
        Youtube youtube;
        if (youtubeCredentials == null) {
            System.out.println("No saved youtube credential records");
            youtube = new Youtube();
        } else if (youtubeCredentials.isNull()) {
            System.out.println("Incorrect youtube credentials found");
            youtube = new Youtube();
        } else {
            youtube = new Youtube(youtubeCredentials);
        }
        sync.exportYoutubeCredentials(youtube.getYoutubeCredentials());
        sync.close();
        System.out.println(
                "Getting all Youtube liked songs");
        LikedSongs youtubeLikedSongs = youtube.getLikedSongs();
        likedSongs.addSongs(youtubeLikedSongs.getSongs());
        likedSongs.setYoutubePageToken(youtubeLikedSongs.getYoutubePageToken());
        albums.putAll(youtube.getAlbums());
        System.out.println("Getting youtube music playlists");
        playlists.putAll(youtube.getPlaylists());
        this.status = true;
    }

    private void importSpotify() {
        Sync sync = new Sync(this.dataFolder);
        SpotifyCredentials spotifyCredentials = sync.importSpotifyCredentials();
        Spotify spotify;
        if (spotifyCredentials == null) {
            System.out.println("No saved spotify credential records");
            spotify = new Spotify();
        } else if (spotifyCredentials.isNull()) {
            System.out.println("Incorrect spotify credentials found");
            spotify = new Spotify();
        } else {
            spotify = new Spotify(spotifyCredentials);
        }
        sync.exportSpotifyCredentials(spotify.getSpotifyCredentials());
        sync.close();
        System.out.println(
                "Getting all spotify Playlists, Albums and liked songs: (This might take a minute)");
        LikedSongs spotifyLikedSongs = spotify.getLikedSongs();
        likedSongs.addSongs(spotifyLikedSongs.getSongs());
        likedSongs.setSpotifyPageOffset(spotifyLikedSongs.getSpotifyPageOffset());
        playlists.putAll(spotify.getPlaylists());
        albums.putAll(spotify.getAlbums());
        this.status = true;
    }

    private void importLocal() {
        Local local = new Local();
        System.out.println("Getting all music from your local library");
        likedSongs.addSongs(local.getLikedSongs());
        playlists.putAll(local.getPlaylists());
        albums.putAll(local.getAlbums());
        this.status = true;
    }

    public LinkedHashMap<Album, ArrayList<Song>> getAlbums() {
        return this.albums;
    }

    public LinkedHashMap<Playlist, ArrayList<Song>> getPlaylists() {
        return this.playlists;
    }

    public LinkedHashSet<Song> getLikedSongs() {
        return this.likedSongs.getSongs();
    }

    public void printOverview() {
        int trackCount = 0;
        for (ArrayList<Song> songs : playlists.values()) {
            trackCount += songs.size();
        }
        for (ArrayList<Song> songs : albums.values()) {
            trackCount += songs.size();
        }
        System.out.println("Imported " + this.albums.size() + " albums");
        System.out.println("Imported " + this.playlists.size() + " playlists");
        System.out.println("Imported " + this.likedSongs.getSize() + " liked songs");
        System.out.println("With a total of: " + trackCount + " songs");
    }
}
