package ryzen.ownitall.integrations;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.integrations.Integration.Export;
import ryzen.ownitall.integrations.Integration.Import;
import ryzen.ownitall.integrations.Integration.Sync;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.WebTools.AuthenticationException;
import ryzen.ownitall.util.WebTools.QueryException;
import ryzen.superbeans.lidarr.Albums.AddAlbumOptions;
import ryzen.superbeans.lidarr.Albums.AlbumResource;
import ryzen.superbeans.lidarr.Artists.AddArtistOptions;
import ryzen.superbeans.lidarr.Artists.ArtistResource;
import ryzen.superbeans.lidarr.Profiles.MetadataProfileResource;
import ryzen.superbeans.lidarr.Profiles.QualityProfileResource;
import ryzen.superbeans.lidarr.Profiles.RootFolderResource;
import ryzen.superbeans.lidarr.Server.SystemResource;
import ryzen.superbeans.lidarr.Tracks.TrackResource;

/**
 * <p>
 * Lidarr class: reads the artists, albums and tracks a lidarr server has into
 * the collection, and writes the collection back to it as download requests:
 * a posted, monitored album is exactly a lidarr grab request. Lidarr keeps no
 * liked songs or playlists, so those methods answer null or log and the
 * collection stays what lidarr models: albums on disk.
 * </p>
 *
 * @author ryzen
 */
public class Lidarr implements Import, Export, Sync {
    private static final Logger logger = new Logger(Lidarr.class);
    private static final Library library = Library.load();
    private String url;
    private Map<String, String> headers;
    private LinkedHashMap<Integer, ArtistResource> artistsById;
    private LinkedHashMap<String, AlbumResource> albumsByForeignId;
    // what a new artist needs to land somewhere: lidarr makes every one of
    // these mandatory, so the server's own defaults are read once and kept
    private Integer qualityProfileId;
    private Integer metadataProfileId;
    private String rootFolderPath;

    /**
     * <p>
     * Constructor for Lidarr.
     * </p>
     *
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public Lidarr() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Lidarr.class)) {
            logger.debug("Empty lidarr credentials");
            throw new MissingSettingException(Lidarr.class);
        }
        this.url = Settings.lidarrURL;
        while (this.url.endsWith("/")) {
            this.url = this.url.substring(0, this.url.length() - 1);
        }
        this.headers = Map.of("X-Api-Key", Settings.lidarrApiKey, "Accept", "application/json");
        this.authenticate();
        logger.debug("Successfully authenticated into lidarr");
    }

    // https://lidarr.audio/docs/api/#operation/system.status
    private void authenticate() throws AuthenticationException {
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("system").path("status");
        try {
            SystemResource status = WebTools.get(target, this.headers, SystemResource.class);
            if (status == null || status.appName == null) {
                throw new AuthenticationException("Failed to authenticate with lidarr");
            }
            logger.debug("Authenticated into lidarr version " + status.version);
        } catch (QueryException e) {
            logger.error("Exception while querying lidarr", e);
            throw new AuthenticationException("Failed to authenticate with lidarr", e);
        }
    }

    /**
     * the added artists in one pass, so an album's artist name and musicbrainz
     * id come from the map instead of one artist query per album
     *
     * @return a {@link java.util.LinkedHashMap} object
     * @throws java.lang.InterruptedException if any.
     */
    private LinkedHashMap<Integer, ArtistResource> artistsById() throws InterruptedException {
        if (this.artistsById != null) {
            return this.artistsById;
        }
        LinkedHashMap<Integer, ArtistResource> byId = new LinkedHashMap<>();
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("artist");
        try {
            // the list route answers a bare json array, so each member arrives
            // as a map and is rebound per member
            List<?> response = WebTools.get(target, this.headers, List.class);
            if (response != null) {
                for (Object member : response) {
                    ArtistResource artist = WebTools.convert(member, ArtistResource.class);
                    if (artist != null && artist.id != null) {
                        byId.put(artist.id, artist);
                    }
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while querying lidarr", e);
        }
        logger.debug("Indexed " + byId.size() + " lidarr artists");
        this.artistsById = byId;
        return byId;
    }

    /**
     * the added albums keyed by their lowercased musicbrainz id, so an upload
     * can tell "already requested" from "missing" without one lookup per album
     *
     * @return a {@link java.util.LinkedHashMap} object
     * @throws java.lang.InterruptedException if any.
     */
    private LinkedHashMap<String, AlbumResource> albumIndex() throws InterruptedException {
        if (this.albumsByForeignId != null) {
            return this.albumsByForeignId;
        }
        LinkedHashMap<String, AlbumResource> byForeignId = new LinkedHashMap<>();
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("album").path("all");
        try {
            List<?> response = WebTools.get(target, this.headers, List.class);
            if (response != null) {
                for (Object member : response) {
                    AlbumResource album = WebTools.convert(member, AlbumResource.class);
                    if (album != null && album.foreignAlbumId != null) {
                        byForeignId.put(album.foreignAlbumId.toLowerCase(), album);
                    }
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while querying lidarr", e);
        }
        logger.debug("Indexed " + byForeignId.size() + " lidarr albums");
        this.albumsByForeignId = byForeignId;
        return byForeignId;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /**
     * the quality profile, metadata profile and root folder a new artist needs:
     * what the settings state wins, anything left open is read off the server
     * once (first accessible root folder and its own defaults, then first
     * configured profile of each kind)
     */
    private void ensureDefaults() {
        if (this.rootFolderPath != null && this.qualityProfileId != null && this.metadataProfileId != null) {
            return;
        }
        if (Settings.lidarrRootFolderPath != null && !Settings.lidarrRootFolderPath.isEmpty()) {
            this.rootFolderPath = Settings.lidarrRootFolderPath;
        }
        if (Settings.lidarrQualityProfileId > 0) {
            this.qualityProfileId = Settings.lidarrQualityProfileId;
        }
        if (Settings.lidarrMetadataProfileId > 0) {
            this.metadataProfileId = Settings.lidarrMetadataProfileId;
        }
        if (this.rootFolderPath == null) {
            WebTarget target = WebTools.target(this.url).path("api").path("v1").path("rootfolder");
            try {
                List<?> response = WebTools.get(target, this.headers, List.class);
                if (response != null) {
                    for (Object member : response) {
                        RootFolderResource folder = WebTools.convert(member, RootFolderResource.class);
                        if (folder == null || !Boolean.TRUE.equals(folder.accessible) || folder.path == null) {
                            continue;
                        }
                        this.rootFolderPath = folder.path;
                        if (this.qualityProfileId == null) {
                            this.qualityProfileId = folder.defaultQualityProfileId;
                        }
                        if (this.metadataProfileId == null) {
                            this.metadataProfileId = folder.defaultMetadataProfileId;
                        }
                        break;
                    }
                }
            } catch (QueryException e) {
                logger.error("Exception while querying lidarr", e);
            }
        }
        if (this.qualityProfileId == null) {
            WebTarget target2 = WebTools.target(this.url).path("api").path("v1").path("qualityprofile");
            try {
                List<?> response = WebTools.get(target2, this.headers, List.class);
                if (response != null && !response.isEmpty()) {
                    QualityProfileResource profile = WebTools.convert(response.get(0),
                            QualityProfileResource.class);
                    if (profile != null && profile.id != null) {
                        this.qualityProfileId = profile.id;
                    }
                }
            } catch (QueryException e) {
                logger.error("Exception while querying lidarr", e);
            }
        }
        if (this.metadataProfileId == null) {
            WebTarget target3 = WebTools.target(this.url).path("api").path("v1").path("metadataprofile");
            try {
                List<?> response = WebTools.get(target3, this.headers, List.class);
                if (response != null && !response.isEmpty()) {
                    MetadataProfileResource profile = WebTools.convert(response.get(0),
                            MetadataProfileResource.class);
                    if (profile != null && profile.id != null) {
                        this.metadataProfileId = profile.id;
                    }
                }
            } catch (QueryException e) {
                logger.error("Exception while querying lidarr", e);
            }
        }
        if (this.rootFolderPath == null || this.qualityProfileId == null || this.metadataProfileId == null) {
            logger.warn("Lidarr has no usable root folder with quality and metadata profiles and the "
                    + "settings name none, new artists cannot be added from the collection");
        }
    }

    /**
     * the album as lidarr describes it, songs included, then handed to the
     * library for correction the way every other integration does
     *
     * @param item        a {@link ryzen.superbeans.lidarr.Albums.AlbumResource} object
     * @param artistNames a {@link java.util.LinkedHashMap} object
     * @return a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    private Album albumFrom(AlbumResource item, Map<Integer, ArtistResource> artistNames)
            throws InterruptedException {
        if (item.id == null) {
            return null;
        }
        Album album = new Album(item.title);
        album.addId("lidarr", String.valueOf(item.id));
        ArtistResource artist = artistNames.get(item.artistId);
        if (artist == null) {
            artist = item.artist;
        }
        String artistName = artist != null ? artist.artistName : null;
        if (artist != null && artistName != null) {
            Artist albumArtist = new Artist(artistName);
            if (artist.foreignArtistId != null) {
                albumArtist.addId("mbid", artist.foreignArtistId);
            }
            album.addArtist(albumArtist);
        }
        if (item.foreignAlbumId != null) {
            album.addId("mbid", item.foreignAlbumId);
        }
        ArrayList<Song> songs = this.getAlbumSongs(String.valueOf(item.id), artistName, item.title);
        if (songs != null) {
            album.addSongs(songs);
        }
        if (library != null) {
            Album foundAlbum = library.getAlbum(album);
            if (foundAlbum != null) {
                album = foundAlbum;
            } else if (Settings.libraryVerified) {
                album = null;
            }
        }
        return album;
    }

    /**
     * the song as the album's track list already describes it: artists and
     * album ride along from the album being paged, the length rides in
     * duration (milliseconds) and the musicbrainz id in foreignRecordingId
     *
     * @param track      a {@link ryzen.superbeans.lidarr.Tracks.TrackResource} object
     * @param artistName a {@link java.lang.String} object
     * @param albumName  a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    private Song songFrom(TrackResource track, String artistName, String albumName) throws InterruptedException {
        Song song = new Song(track.title);
        if (artistName != null) {
            song.addArtist(new Artist(artistName));
        }
        if (albumName != null) {
            song.setAlbumName(albumName);
        }
        if (track.duration != null && track.duration > 0) {
            song.setDuration(track.duration, ChronoUnit.MILLIS);
        }
        if (track.foreignRecordingId != null) {
            song.addId("mbid", track.foreignRecordingId);
        }
        return this.matchedSong(song);
    }

    /**
     * hand the song to the library for correction, the way getSong always has
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    private Song matchedSong(Song song) throws InterruptedException {
        if (library == null) {
            return song;
        }
        Song foundSong = library.getSong(song);
        if (foundSong != null) {
            return foundSong;
        } else if (Settings.libraryVerified) {
            return null;
        }
        return song;
    }

    /** {@inheritDoc} */
    @Override
    public LikedSongs getLikedSongs() {
        logger.debug("Unsupported method getLikedSongs called");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Playlist> getPlaylists() {
        logger.debug("Unsupported method getPlaylists called");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Playlist getPlaylist(String playlistId, String playlistName) {
        logger.debug("Unsupported method getPlaylist called");
        return null;
    }

    // https://lidarr.audio/docs/api/#operation/album.all
    /** {@inheritDoc} */
    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        ArrayList<AlbumResource> response = new ArrayList<>();
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("album").path("all");
        try {
            List<?> albums = WebTools.get(target, this.headers, List.class);
            if (albums != null) {
                for (Object member : albums) {
                    AlbumResource album = WebTools.convert(member, AlbumResource.class);
                    if (album != null) {
                        response.add(album);
                    }
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while querying lidarr", e);
        }
        if (response.isEmpty()) {
            logger.debug("Unable to get ids of albums");
            return null;
        }
        Map<Integer, ArtistResource> artistNames = this.artistsById();
        ArrayList<Album> albums = new ArrayList<>();
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (AlbumResource item : IPIterator.wrap(response, "Albums", response.size())) {
                interruptionHandler.checkInterruption();
                Album album = this.albumFrom(item, artistNames);
                if (album != null && !album.isEmpty()) {
                    albums.add(album);
                }
            }
        }
        if (!albums.isEmpty()) {
            return albums;
        }
        return null;
    }

    // https://lidarr.audio/docs/api/#operation/album.single
    /** {@inheritDoc} */
    @Override
    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        if (albumId == null) {
            logger.debug("null album id provided in getAlbum");
            return null;
        }
        Album album = new Album(albumName);
        if (albumArtistName != null) {
            album.addArtist(new Artist(albumArtistName));
        }
        album.addId("lidarr", albumId);
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("album").path(albumId);
        try {
            AlbumResource item = WebTools.get(target, this.headers, AlbumResource.class);
            if (item != null) {
                if (item.foreignAlbumId != null) {
                    album.addId("mbid", item.foreignAlbumId);
                }
                ArtistResource artist = item.artist != null ? item.artist
                        : this.artistsById().get(item.artistId);
                if (artist != null && artist.artistName != null) {
                    Artist albumArtist = new Artist(artist.artistName);
                    if (artist.foreignArtistId != null) {
                        albumArtist.addId("mbid", artist.foreignArtistId);
                    }
                    album.addArtist(albumArtist);
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while querying lidarr", e);
        }
        ArrayList<Song> songs = this.getAlbumSongs(albumId, albumArtistName, albumName);
        if (songs != null) {
            album.addSongs(songs);
        }
        if (library != null) {
            Album foundAlbum = library.getAlbum(album);
            if (foundAlbum != null) {
                album = foundAlbum;
            } else if (Settings.libraryVerified) {
                album = null;
            }
        }
        return album;
    }

    // https://lidarr.audio/docs/api/#operation/track.list
    /**
     * <p>
     * getAlbumSongs. the track query answers a whole album's tracks with their
     * musicbrainz ids and lengths already on them, so no song is refetched.
     * </p>
     *
     * @param albumId    a {@link java.lang.String} object
     * @param artistName a {@link java.lang.String} object
     * @param albumName  a {@link java.lang.String} object
     * @return a {@link java.util.ArrayList} object
     * @throws java.lang.InterruptedException if any.
     */
    public ArrayList<Song> getAlbumSongs(String albumId, String artistName, String albumName)
            throws InterruptedException {
        if (albumId == null) {
            logger.debug("empty or null albumId provided in getAlbumSongs");
            return null;
        }
        ArrayList<TrackResource> response = new ArrayList<>();
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("track")
                .queryParam("albumId", albumId);
        try {
            List<?> tracks = WebTools.get(target, this.headers, List.class);
            if (tracks != null) {
                for (Object member : tracks) {
                    TrackResource track = WebTools.convert(member, TrackResource.class);
                    if (track != null) {
                        response.add(track);
                    }
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while querying lidarr", e);
        }
        if (response.isEmpty()) {
            logger.debug("Unable to get ids of album songs: " + albumId);
            return null;
        }
        ArrayList<Song> songs = new ArrayList<>();
        try (IPIterator<?> pb = IPIterator.manual(albumId, response.size())) {
            for (TrackResource track : response) {
                if (track.title != null) {
                    Song song = this.songFrom(track, artistName, albumName);
                    if (song != null) {
                        songs.add(song);
                    }
                }
                pb.step(track.title != null ? track.title : String.valueOf(track.id));
            }
        }
        if (!songs.isEmpty()) {
            return songs;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void uploadLikedSongs() {
        logger.debug("Unsupported method uploadLikedSongs called");
    }

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylists() {
        logger.debug("Unsupported method uploadPlaylists called");
    }

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylist(Playlist playlist) {
        logger.debug("Unsupported method uploadPlaylist called");
    }

    // https://lidarr.audio/docs/api/#operation/album.create
    /** {@inheritDoc} */
    @Override
    public void uploadAlbums() throws InterruptedException {
        ArrayList<Album> albums = Collection.getAlbums();
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (Album album : IPIterator.wrap(albums, "Albums", albums.size())) {
                interruptionHandler.checkInterruption();
                this.uploadAlbum(album);
            }
        }
    }

    // https://lidarr.audio/docs/api/#operation/album.create
    /** {@inheritDoc} */
    @Override
    public void uploadAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided in uploadAlbum");
            return;
        }
        if (album.getMainArtist() == null) {
            logger.debug("Album '" + album.getName() + "' has no artist to hand to lidarr");
            return;
        }
        Integer artistId = this.getArtistId(album.getMainArtist());
        if (artistId == null) {
            return;
        }
        String mbid = album.getId("mbid");
        AlbumResource existing = mbid != null ? this.albumIndex().get(mbid.toLowerCase())
                : this.findAlbumByName(album, artistId);
        if (existing != null) {
            album.addId("lidarr", String.valueOf(existing.id));
            if (Boolean.TRUE.equals(existing.monitored)) {
                logger.debug("Album '" + album.getName() + "' is already monitored on lidarr");
                return;
            }
            if (this.setMonitored(existing, true)) {
                logger.debug("Monitored existing lidarr album '" + album.getName() + "'");
            }
            return;
        }
        String foreignAlbumId = mbid;
        if (foreignAlbumId == null) {
            AlbumResource lookup = this.lookupAlbum(album, artistId);
            if (lookup == null || lookup.foreignAlbumId == null) {
                logger.debug("Album '" + album.getName() + "' has no musicbrainz id and lidarr found no "
                        + "release to request it as");
                return;
            }
            foreignAlbumId = lookup.foreignAlbumId;
        }
        AlbumResource request = new AlbumResource();
        request.artistId = artistId;
        request.foreignAlbumId = foreignAlbumId;
        request.title = album.getName();
        request.monitored = true;
        request.anyReleaseOk = true;
        AddAlbumOptions options = new AddAlbumOptions();
        // manual so lidarr takes this album as asked, searching so the album
        // is a download request the moment it lands
        options.addType = "manual";
        options.searchForNewAlbum = true;
        request.addOptions = options;
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("album");
        try {
            AlbumResource created = WebTools.post(target, this.headers,
                    Entity.entity(WebTools.json(request), MediaType.APPLICATION_JSON), AlbumResource.class);
            if (created != null && created.id != null) {
                this.albumIndex().put(foreignAlbumId.toLowerCase(), created);
                album.addId("lidarr", String.valueOf(created.id));
                logger.debug("Requested album '" + album.getName() + "' from lidarr");
                return;
            }
        } catch (QueryException | JsonProcessingException e) {
            logger.error("Exception while sending to lidarr", e);
        }
        logger.debug("Failed to add album '" + album.getName() + "' to lidarr");
    }

    /** {@inheritDoc} */
    @Override
    public void syncLikedSongs() {
        logger.debug("Unsupported method syncLikedSongs called");
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylists() {
        logger.debug("Unsupported method syncPlaylists called");
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylist(Playlist playlist) {
        logger.debug("Unsupported method syncPlaylist called");
    }

    // https://lidarr.audio/docs/api/#operation/album.update
    /** {@inheritDoc} */
    @Override
    public void syncAlbums() throws InterruptedException {
        this.uploadAlbums();
        logger.debug("Getting lidarr albums to unmonitor mismatches");
        LinkedHashMap<String, AlbumResource> remote = this.albumIndex();
        ArrayList<Album> collection = Collection.getAlbums();
        LinkedHashSet<String> wantedIds = new LinkedHashSet<>();
        LinkedHashSet<String> wantedLidarrIds = new LinkedHashSet<>();
        for (Album album : collection) {
            String mbid = album.getId("mbid");
            if (mbid != null) {
                wantedIds.add(mbid.toLowerCase());
            }
            String lidarrId = album.getId("lidarr");
            if (lidarrId != null) {
                wantedLidarrIds.add(lidarrId);
            }
        }
        ArrayList<AlbumResource> stale = new ArrayList<>();
        for (AlbumResource item : remote.values()) {
            if (item.id == null || !Boolean.TRUE.equals(item.monitored)) {
                continue;
            }
            if (item.foreignAlbumId != null && wantedIds.contains(item.foreignAlbumId.toLowerCase())) {
                continue;
            }
            if (wantedLidarrIds.contains(String.valueOf(item.id))) {
                continue;
            }
            Album local = new Album(item.title);
            ArtistResource artist = item.artist != null ? item.artist : this.artistsById().get(item.artistId);
            if (artist != null && artist.artistName != null) {
                local.addArtist(new Artist(artist.artistName));
            }
            if (!collection.contains(local)) {
                stale.add(item);
            }
        }
        for (AlbumResource item : IPIterator.wrap(stale, "Unmonitoring", stale.size())) {
            if (this.setMonitored(item, false)) {
                logger.warn("Album '" + item.title
                        + "' was found on lidarr but not in collection, unmonitored it to stay up to date");
            }
        }
    }

    // https://lidarr.audio/docs/api/#operation/album.update
    /** {@inheritDoc} */
    @Override
    public void syncAlbum(Album album) throws InterruptedException {
        // a lidarr album is its whole tracklist on disk: syncing one means
        // making sure lidarr is asked for it, files are lidarr's to keep
        this.uploadAlbum(album);
    }

    // https://lidarr.audio/docs/api/#operation/artist.lookup
    /**
     * the artist as a lidarr artist id, adding the artist when the server does
     * not know it yet: lidarr only takes artists it can key by their
     * musicbrainz id, so that decides the lookup the way the name does not
     *
     * @param artist a {@link ryzen.ownitall.classes.Artist} object
     * @return a {@link java.lang.Integer} object
     * @throws java.lang.InterruptedException if any.
     */
    private Integer getArtistId(Artist artist) throws InterruptedException {
        String mbid = artist.getId("mbid");
        String name = this.normalize(artist.getName());
        for (Map.Entry<Integer, ArtistResource> entry : this.artistsById().entrySet()) {
            ArtistResource remote = entry.getValue();
            if (mbid != null && mbid.equals(remote.foreignArtistId)) {
                return entry.getKey();
            }
        }
        for (Map.Entry<Integer, ArtistResource> entry : this.artistsById().entrySet()) {
            if (name.equals(this.normalize(entry.getValue().artistName))) {
                return entry.getKey();
            }
        }
        return this.addArtist(artist, mbid, name);
    }

    private Integer addArtist(Artist artist, String mbid, String name) throws InterruptedException {
        ArrayList<ArtistResource> candidates = new ArrayList<>();
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("artist").path("lookup")
                .queryParam("term", artist.getName());
        try {
            List<?> response = WebTools.get(target, this.headers, List.class);
            if (response != null) {
                for (Object member : response) {
                    ArtistResource candidate = WebTools.convert(member, ArtistResource.class);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while querying lidarr", e);
        }
        ArtistResource match = this.findArtistMatch(candidates, mbid, name);
        if (match == null || match.foreignArtistId == null) {
            logger.debug("Artist '" + artist.getName() + "' has no musicbrainz id and lidarr found no "
                    + "artist to add it from");
            return null;
        }
        this.ensureDefaults();
        if (this.rootFolderPath == null || this.qualityProfileId == null || this.metadataProfileId == null) {
            return null;
        }
        ArtistResource request = new ArtistResource();
        request.artistName = match.artistName;
        request.foreignArtistId = match.foreignArtistId;
        request.qualityProfileId = this.qualityProfileId;
        request.metadataProfileId = this.metadataProfileId;
        request.rootFolderPath = this.rootFolderPath;
        request.monitored = true;
        AddArtistOptions options = new AddArtistOptions();
        // none so only the albums ownitall adds get grabbed, never the whole
        // discography of an artist the collection touched once
        options.monitor = "none";
        options.monitored = true;
        options.searchForMissingAlbums = false;
        request.addOptions = options;
        WebTarget target2 = WebTools.target(this.url).path("api").path("v1").path("artist");
        try {
            ArtistResource created = WebTools.post(target2, this.headers,
                    Entity.entity(WebTools.json(request), MediaType.APPLICATION_JSON), ArtistResource.class);
            if (created != null && created.id != null) {
                this.artistsById.put(created.id, created);
                logger.debug("Added artist '" + created.artistName + "' to lidarr");
                return created.id;
            }
        } catch (QueryException | JsonProcessingException e) {
            logger.error("Exception while sending to lidarr", e);
        }
        logger.debug("Failed to add artist '" + artist.getName() + "' to lidarr");
        return null;
    }

    private ArtistResource findArtistMatch(ArrayList<ArtistResource> candidates, String mbid, String name) {
        if (mbid != null) {
            for (ArtistResource candidate : candidates) {
                if (mbid.equals(candidate.foreignArtistId)) {
                    return candidate;
                }
            }
        }
        for (ArtistResource candidate : candidates) {
            if (name.equals(this.normalize(candidate.artistName)) && candidate.foreignArtistId != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * the album among the indexed ones when the collection's copy carries no
     * musicbrainz id to key on: same title, same artist, exactly once
     */
    private AlbumResource findAlbumByName(Album album, Integer artistId) throws InterruptedException {
        String title = this.normalize(album.getName());
        for (AlbumResource remote : this.albumIndex().values()) {
            if (title.equals(this.normalize(remote.title)) && artistId.equals(remote.artistId)) {
                return remote;
            }
        }
        return null;
    }

    // https://lidarr.audio/docs/api/#operation/album.lookup
    /**
     * a release group lidarr can take this album to: looked up by title, kept
     * when the artist and title both line up
     */
    private AlbumResource lookupAlbum(Album album, Integer artistId) throws InterruptedException {
        ArtistResource artist = this.artistsById.get(artistId);
        if (artist == null) {
            return null;
        }
        ArrayList<AlbumResource> candidates = new ArrayList<>();
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("album").path("lookup")
                .queryParam("term", album.getName());
        try {
            List<?> response = WebTools.get(target, this.headers, List.class);
            if (response != null) {
                for (Object member : response) {
                    AlbumResource candidate = WebTools.convert(member, AlbumResource.class);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while querying lidarr", e);
        }
        String title = this.normalize(album.getName());
        for (AlbumResource candidate : candidates) {
            if (candidate.foreignAlbumId == null || candidate.artist == null) {
                continue;
            }
            boolean sameArtist = artist.foreignArtistId != null
                    ? artist.foreignArtistId.equals(candidate.artist.foreignArtistId)
                    : this.normalize(artist.artistName).equals(this.normalize(candidate.artist.artistName));
            if (sameArtist && title.equals(this.normalize(candidate.title))) {
                return candidate;
            }
        }
        return null;
    }

    private AlbumResource getAlbumById(Object albumId) {
        WebTarget target = WebTools.target(this.url).path("api").path("v1").path("album")
                .path(String.valueOf(albumId));
        try {
            return WebTools.get(target, this.headers, AlbumResource.class);
        } catch (QueryException e) {
            logger.error("Exception while querying lidarr", e);
            return null;
        }
    }

    // https://lidarr.audio/docs/api/#operation/album.update
    /**
     * flip one indexed album's monitored flag: put back the resource as the
     * server gave it with only the flag changed, the way the lidarr ui does
     */
    private boolean setMonitored(AlbumResource album, boolean monitored) {
        AlbumResource fresh = this.getAlbumById(album.id);
        if (fresh == null) {
            logger.debug("Unable to refetch lidarr album '" + album.title + "' (" + album.id + ")");
            return false;
        }
        fresh.monitored = monitored;
        WebTarget target2 = WebTools.target(this.url).path("api").path("v1").path("album")
                .path(String.valueOf(album.id));
        try {
            AlbumResource updated = WebTools.put(target2, this.headers,
                    Entity.entity(WebTools.json(fresh), MediaType.APPLICATION_JSON), AlbumResource.class);
            if (updated == null) {
                logger.debug("Failed to " + (monitored ? "monitor" : "unmonitor") + " lidarr album '"
                        + album.title + "' (" + album.id + ")");
                return false;
            }
        } catch (QueryException | JsonProcessingException e) {
            logger.error("Exception while sending to lidarr", e);
            return false;
        }
        album.monitored = monitored;
        return true;
    }
}
