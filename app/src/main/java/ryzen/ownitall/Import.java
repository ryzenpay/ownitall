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

    /**
     * constructor for Import which also prompts user for import options
     * 
     * @param dataFolder - datafolder of where to get presaved credentials
     */
    public Import(String dataFolder) {
        this.dataFolder = dataFolder;
        this.status = false;
        this.albums = new LinkedHashSet<>();
        this.playlists = new LinkedHashSet<>();
        this.likedSongs = new LikedSongs();
        this.supported = new LinkedHashMap<>();
        supported.put("Exit", this::exit); // keep this one first
        supported.put("Youtube", this::importYoutube);
        supported.put("Spotify", this::importSpotify);
        supported.put("Local", this::importLocal);
        while (!this.status) {
            String choice = promptImport();
            if (choice != null) {
                supported.get(choice).run();
            }
        }
    }

    /**
     * prompt user for import options from the supported array
     * 
     * @return - String key value of the options hashmap
     */
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
        return options.get(choice);
    }

    /**
     * import music from youtube, getting or setting credentials as needed
     */
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
                "Getting all Youtube liked songs, albums and playlists: ");
        LikedSongs youtubeLikedSongs = youtube.getLikedSongs();
        System.out.println("    Processed " + youtubeLikedSongs.size() + " liked songs");
        likedSongs.addSongs(youtubeLikedSongs.getSongs());
        likedSongs.setYoutubePageToken(youtubeLikedSongs.getYoutubePageToken());
        LinkedHashSet<Album> youtubeAlbums = youtube.getAlbums();
        albums.addAll(youtubeAlbums);
        System.out.println("    Processed " + youtubeAlbums.size() + " albums");
        LinkedHashSet<Playlist> youtubePlaylists = youtube.getPlaylists();
        System.out.println("    Processed " + youtubePlaylists.size() + " playlists");
        playlists.addAll(youtubePlaylists);
        this.status = true;
    }

    /**
     * import music from spotify, getting or setting credentials as needed
     */
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
        System.out.println("    Processed " + spotifyLikedSongs.size() + " liked songs");
        likedSongs.addSongs(spotifyLikedSongs.getSongs());
        likedSongs.setSpotifyPageOffset(spotifyLikedSongs.getSpotifyPageOffset());
        LinkedHashSet<Album> spotifyAlbums = spotify.getAlbums();
        System.out.println("    Processed " + spotifyAlbums.size() + " albums");
        albums.addAll(spotifyAlbums);
        LinkedHashSet<Playlist> spotifyPlaylists = spotify.getPlaylists();
        System.out.println("    Processed " + spotifyPlaylists.size() + " playlists");
        playlists.addAll(spotifyPlaylists);
        this.status = true;
    }

    /**
     * import music from a local music library, prompting for location
     */
    private void importLocal() {
        Local local = new Local();
        System.out.println("Getting all music from your local library");
        LikedSongs localLikedSongs = local.getLikedSongs();
        System.out.println("Processed " + localLikedSongs.size() + " liked songs");
        likedSongs.addSongs(localLikedSongs.getSongs());
        LinkedHashSet<Album> localAlbums = local.getAlbums();
        System.out.println("Processed " + localAlbums.size() + " albums");
        albums.addAll(localAlbums);
        LinkedHashSet<Playlist> localPlaylists = local.getPlaylists();
        System.out.println("Processed " + localPlaylists.size() + " playlists");
        playlists.addAll(playlists);
        this.status = true;
    }

    private void exit() {
        this.status = true;
    }

    /**
     * get imported albums
     * 
     * @return - linkedhashset of constructed Album
     */
    public LinkedHashSet<Album> getAlbums() {
        return this.albums;
    }

    /**
     * get imported playlists
     * 
     * @return - linkedhashset of constructed Playlist
     */
    public LinkedHashSet<Playlist> getPlaylists() {
        return this.playlists;
    }

    /**
     * get liked songs
     * 
     * @return - linkedhashset of constructed Song
     */
    public LinkedHashSet<Song> getLikedSongs() {
        return new LinkedHashSet<>(this.likedSongs.getSongs());
    }

    /**
     * print overview of imported music
     * similar to Main inventory print with recursion 1
     */
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
