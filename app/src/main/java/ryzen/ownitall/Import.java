package ryzen.ownitall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class Import {
    private boolean status;
    private String dataFolder;

    private LinkedHashSet<Album> albums;
    private LinkedHashSet<Playlist> playlists;
    private LikedSongs likedSongs;
    private LinkedHashMap<String, Runnable> supported;

    public Import(String dataFolder) {
        this.dataFolder = dataFolder;
        this.status = false;
        this.albums = new LinkedHashSet<>();
        this.playlists = new LinkedHashSet<>();
        this.likedSongs = new LikedSongs();
        this.supported = new LinkedHashMap<>();
        supported.put("Youtube", this::importYoutube); // keeps insertion order like this
        supported.put("Spotify", this::importSpotify);
        supported.put("Local", this::importLocal);
        while (!this.status) {
            String choice = promptImport();
            if (choice != null) {
                supported.get(choice).run();
            }
        }
    }

    private String promptImport() {
        System.out.println("Choose an import option from the following: ");
        int i = 1;
        for (String option : this.supported.keySet()) {
            System.out.println("[" + i + "] " + option);
            i++;
        }
        System.out.println("[0] Exit");
        System.out.print("Enter your choice: ");
        int choice = Input.getInstance().getInt();
        if (choice < 0 || choice > supported.size()) {
            System.out.println("Incorrect option, try again");
            return null;
        }
        ArrayList<String> options = new ArrayList<>(this.supported.keySet());
        return options.get(choice - 1);
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
        System.out.println(
                "Getting all Youtube liked songs");
        LikedSongs youtubeLikedSongs = youtube.getLikedSongs();
        likedSongs.addSongs(youtubeLikedSongs.getSongs());
        likedSongs.setYoutubePageToken(youtubeLikedSongs.getYoutubePageToken());
        albums.addAll(youtube.getAlbums());
        System.out.println("Getting youtube music playlists");
        playlists.addAll(youtube.getPlaylists());
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
        System.out.println(
                "Getting all spotify Playlists, Albums and liked songs: (This might take a minute)");
        LikedSongs spotifyLikedSongs = spotify.getLikedSongs();
        likedSongs.addSongs(spotifyLikedSongs.getSongs());
        likedSongs.setSpotifyPageOffset(spotifyLikedSongs.getSpotifyPageOffset());
        playlists.addAll(spotify.getPlaylists());
        albums.addAll(spotify.getAlbums());
        this.status = true;
    }

    private void importLocal() {
        Local local = new Local();
        System.out.println("Getting all music from your local library");
        likedSongs.addSongs(local.getLikedSongs());
        playlists.addAll(local.getPlaylists());
        albums.addAll(local.getAlbums());
        this.status = true;
    }

    public LinkedHashSet<Album> getAlbums() {
        return this.albums;
    }

    public LinkedHashSet<Playlist> getPlaylists() {
        return this.playlists;
    }

    public LinkedHashSet<Song> getLikedSongs() {
        return new LinkedHashSet<>(this.likedSongs.getSongs());
    }

    public void printOverview() {
        int trackCount = 0;
        for (Playlist playlist : this.playlists) {
            trackCount += playlist.size();
        }
        for (Album album : this.albums) {
            trackCount += album.size();
        }
        System.out.println("Imported " + this.albums.size() + " albums");
        System.out.println("Imported " + this.playlists.size() + " playlists");
        System.out.println("Imported " + this.likedSongs.size() + " liked songs");
        System.out.println("With a total of: " + trackCount + " songs");
    }
}
