package ryzen.ownitall.library;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import jakarta.ws.rs.client.WebTarget;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.WebTools;
import ryzen.superbeans.lastfm.Albums;
import ryzen.superbeans.lastfm.Artists;
import ryzen.superbeans.lastfm.Commons;
import ryzen.superbeans.lastfm.Search;
import ryzen.superbeans.lastfm.Tracks;

/**
 * <p>
 * LastFM class.
 * </p>
 *
 * @author ryzen
 */
public class LastFM extends Library {
    private static final Logger logger = new Logger(LastFM.class);
    private static final String baseUrl = "http://ws.audioscrobbler.com/2.0";
    private static final String defaultImg = "https://lastfm.freetls.fastly.net/i/u/300x300/2a96cbd8b46e442fc41c2b86b821562f.png";

    /**
     * default LastFM constructor
     * initializes all values and loads from cache
     *
     * @throws ryzen.ownitall.Settings.MissingSettingException if any.
     */
    public LastFM() throws MissingSettingException {
        super();
        if (Settings.load().isGroupEmpty(LastFM.class)) {
            logger.debug("Empty LastFM credentials");
            throw new MissingSettingException(LastFM.class);
        }
    }

    /**
     * {@inheritDoc}
     *
     * get album from lastfm
     * https://www.last.fm/api/show/album.getInfo
     */
    @Override
    public Album getAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album passed in getAlbum");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("album", album.getName());
        if (album.getMainArtist() == null) {
            logger.debug(album.toString() + ": Album requires atleast one artist");
            return null;
        }
        Artist albumArtist = this.getArtist(album.getMainArtist());
        if (albumArtist != null) {
            params.put("artist", albumArtist.getName());
        }
        params.put("autocorrect", "1");
        if (albums.containsKey(params.toString())) {
            return albums.get(params.toString());
        }
        Albums.AlbumGetinfo envelope = this.query("album.getInfo", params, Albums.AlbumGetinfo.class);
        Albums.Album foundAlbum = envelope == null ? null : envelope.album;
        if (foundAlbum == null) {
            foundAlbum = this.searchAlbum(params.get("album"), params.get("artist"));
        }
        if (foundAlbum != null) {
            // new album here so the songs are the correction version + complete
            album = new Album(foundAlbum.name);
            Artist artist = this.getArtist(new Artist(foundAlbum.artist));
            if (artist != null) {
                album.addArtist(artist);
            }
            if (foundAlbum.mbid != null && !foundAlbum.mbid.isEmpty()) {
                // only needed for albums
                album.addId("mbid", foundAlbum.mbid);
            }
            String coverImage = this.coverImage(foundAlbum.image);
            if (coverImage != null) {
                album.setCoverImage(coverImage);
            } else {
                logger.debug(album.toString() + ": album missing images");
            }
            if (foundAlbum.tracks != null && foundAlbum.tracks.track != null
                    && !foundAlbum.tracks.track.isEmpty()) {
                for (Tracks.Track trackNode : foundAlbum.tracks.track) {
                    Song song = new Song(trackNode.name);
                    if (trackNode.artist != null) {
                        Artist songArtist = this.getArtist(new Artist(trackNode.artist.name));
                        if (songArtist != null) {
                            song.addArtist(artist);
                        }
                    } else {
                        logger.debug(album.toString() + " track missing artist");
                    }
                    song = this.getSong(song);
                    if (song != null) {
                        album.addSong(song);
                    }
                }
            } else {
                logger.debug(album.toString() + ": album missing tracks");
            }
            if (!album.isEmpty()) {
                albums.put(params.toString(), album);
                return album;
            }
        } else {
            logger.debug(album.toString() + ": album.getInfo missing album");
        }
        if (!album.getName().equals(MusicTools.removeBrackets(album.getName()))) {
            logger.debug("Trying album '" + album.getName() + "' again with trimmed brackets");
            album.setName(MusicTools.removeBrackets(album.getName()));
            return getAlbum(album);
        }
        logger.info("Unable to find album '" + album.toString() + "' in library");
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * get song from lastfm
     */
    @Override
    public Song getSong(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("null song passed in getSong");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("track", song.getName());
        if (song.getMainArtist() != null) {
            Artist artist = this.getArtist(song.getMainArtist());
            if (artist != null) {
                params.put("artist", artist.getName());
            }
        }
        params.put("autocorrect", "1");
        if (songs.containsKey(params.toString())) {
            return songs.get(params.toString());
        }
        Tracks.TrackGetinfo envelope = this.query("track.getInfo", params, Tracks.TrackGetinfo.class);
        Tracks.Track foundTrack = envelope == null ? null : envelope.track;
        if (foundTrack == null) {
            foundTrack = this.searchTrack(params.get("track"), params.get("artist"));
        }
        if (foundTrack != null) {
            song.setName(foundTrack.name);
            if (foundTrack.mbid != null) {
                song.addId("mbid", foundTrack.mbid);
            }
            if (foundTrack.duration != null) {
                try {
                    song.setDuration(Long.parseLong(foundTrack.duration), ChronoUnit.MILLIS);
                } catch (NumberFormatException e) {
                    logger.debug(song.toString() + ": unparseable track duration '" + foundTrack.duration + "'");
                }
            }
            if (foundTrack.artist != null) {
                Artist artist = this.getArtist(new Artist(foundTrack.artist.name));
                if (artist != null) {
                    song.addArtist(artist);
                }
            } else {
                logger.debug(song.toString() + ": track missing artist");
            }
            if (foundTrack.album != null) {
                song.setAlbumName(foundTrack.album.title);
                String coverImage = this.coverImage(foundTrack.album.image);
                if (coverImage != null) {
                    song.setCoverImage(coverImage);
                } else {
                    logger.debug(song.toString() + ": song missing images");
                }
            } else {
                logger.debug(song.toString() + ": track missing album");
            }
            songs.put(params.toString(), song);
            return song;
        } else {
            logger.debug(song.toString() + ": track.getInfo missing track");
        }
        if (!song.getName().equals(MusicTools.removeBrackets(song.getName()))) {
            logger.debug("Trying song '" + song.getName() + "' again with trimmed brackets");
            song.setName(MusicTools.removeBrackets(song.getName()));
            return getSong(song);
        }
        logger.info("Unable to find song '" + song.toString() + "' in library");
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * get artist from LastFM
     */
    @Override
    public Artist getArtist(Artist artist) throws InterruptedException {
        if (artist == null) {
            logger.debug("null artist passed in getArtist");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("artist", artist.getName());
        if (artists.containsKey(params.toString())) {
            return artists.get(params.toString());
        }
        Artists.ArtistGetinfo envelope = this.query("artist.getInfo", params, Artists.ArtistGetinfo.class);
        Artists.Artist foundArtist = envelope == null ? null : envelope.artist;
        if (foundArtist == null) {
            foundArtist = this.searchArtist(params.get("artist"));
        }
        if (foundArtist != null) {
            artist.setName(foundArtist.name);
            if (foundArtist.mbid != null) {
                artist.addId("mbid", foundArtist.mbid);
            }
            String coverImage = this.coverImage(foundArtist.image);
            if (coverImage != null) {
                artist.setCoverImage(coverImage);
            } else {
                logger.debug(artist.toString() + ": artist missing images");
            }
            artists.put(params.toString(), artist);
            return artist;
        } else {
            logger.debug(artist.toString() + ": artist.getInfo missing artist");
        }
        logger.info("Unable to find artist " + artist.toString() + " in Library");
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * get all albums from artist
     */
    @Override
    public ArrayList<Album> getArtistAlbums(Artist artist) throws InterruptedException {
        if (artist == null) {
            logger.debug("null artist passed to getArtistAlbums");
            return null;
        }
        Artist foundArtist = this.getArtist(artist);
        if (foundArtist != null) {
            artist = foundArtist;
        }
        ArrayList<Album> albums = new ArrayList<>();
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("artist", artist.getName());
        Artists.ArtistGettopalbums envelope = this.query("artist.getTopAlbums", params,
                Artists.ArtistGettopalbums.class);
        Artists.Topalbums topalbums = envelope == null ? null : envelope.topalbums;
        if (topalbums != null) {
            if (topalbums.album != null && !topalbums.album.isEmpty()) {
                for (Artists.TopalbumsAlbum topAlbum : topalbums.album) {
                    Album album = new Album(topAlbum.name);
                    album.addArtist(artist);
                    album = this.getAlbum(album);
                    if (album != null) {
                        albums.add(album);
                    }
                }
            } else {
                logger.debug(artist.toString() + ": missing data in artist albums");
            }
        } else {
            logger.debug(artist.toString() + ": missing data getting top albums");
        }
        if (albums.isEmpty()) {
            logger.info("Unable to find albums for '" + artist.getName() + "' in Library");
            return null;
        }
        return albums;
    }

    /**
     * track.getInfo only finds the exact spelling it is given, track.search
     * finds a close one: the best match is fed back through getInfo, by mbid
     * when the search found one because the name it returns may still differ
     *
     * @param track  - the track name getInfo did not find
     * @param artist - the artist name the query used, may be null
     * @return - the bound track or null if the search found nothing either
     * @throws java.lang.InterruptedException if any.
     */
    private Tracks.Track searchTrack(String track, String artist) throws InterruptedException {
        LinkedHashMap<String, String> searchParams = new LinkedHashMap<>();
        searchParams.put("track", track);
        if (artist != null) {
            searchParams.put("artist", artist);
        }
        Search.TrackSearch envelope = this.query("track.search", searchParams, Search.TrackSearch.class);
        Search.Results results = envelope == null ? null : envelope.results;
        if (results == null || results.trackmatches == null || results.trackmatches.track == null
                || results.trackmatches.track.isEmpty()) {
            logger.debug("track.search found no match for '" + track + "'");
            return null;
        }
        Search.TrackmatchesTrack match = results.trackmatches.track.get(0);
        logger.debug("track.search matched '" + track + "' as '" + match.name + "' by " + match.artist);
        LinkedHashMap<String, String> infoParams = new LinkedHashMap<>();
        if (match.mbid != null && !match.mbid.isEmpty()) {
            infoParams.put("mbid", match.mbid);
        } else {
            infoParams.put("track", match.name);
            if (match.artist != null) {
                infoParams.put("artist", match.artist);
            }
        }
        infoParams.put("autocorrect", "1");
        Tracks.TrackGetinfo info = this.query("track.getInfo", infoParams, Tracks.TrackGetinfo.class);
        return info == null ? null : info.track;
    }

    /**
     * album.search rescue for an album getInfo could not find by spelling
     *
     * @param album  - the album name getInfo did not find
     * @param artist - the artist name the query used, may be null
     * @return - the bound album or null if the search found nothing either
     * @throws java.lang.InterruptedException if any.
     */
    private Albums.Album searchAlbum(String album, String artist) throws InterruptedException {
        LinkedHashMap<String, String> searchParams = new LinkedHashMap<>();
        searchParams.put("album", album);
        if (artist != null) {
            searchParams.put("artist", artist);
        }
        Search.AlbumSearch envelope = this.query("album.search", searchParams, Search.AlbumSearch.class);
        Search.Results results = envelope == null ? null : envelope.results;
        if (results == null || results.albummatches == null || results.albummatches.album == null
                || results.albummatches.album.isEmpty()) {
            logger.debug("album.search found no match for '" + album + "'");
            return null;
        }
        Albums.Album match = results.albummatches.album.get(0);
        logger.debug("album.search matched '" + album + "' as '" + match.name + "' by " + match.artist);
        LinkedHashMap<String, String> infoParams = new LinkedHashMap<>();
        if (match.mbid != null && !match.mbid.isEmpty()) {
            infoParams.put("mbid", match.mbid);
        } else {
            infoParams.put("album", match.name);
            if (match.artist != null) {
                infoParams.put("artist", match.artist);
            }
        }
        infoParams.put("autocorrect", "1");
        Albums.AlbumGetinfo info = this.query("album.getInfo", infoParams, Albums.AlbumGetinfo.class);
        return info == null ? null : info.album;
    }

    /**
     * artist.search rescue for an artist getInfo could not find by spelling
     *
     * @param artist - the artist name getInfo did not find
     * @return - the bound artist or null if the search found nothing either
     * @throws java.lang.InterruptedException if any.
     */
    private Artists.Artist searchArtist(String artist) throws InterruptedException {
        LinkedHashMap<String, String> searchParams = new LinkedHashMap<>();
        searchParams.put("artist", artist);
        Search.ArtistSearch envelope = this.query("artist.search", searchParams, Search.ArtistSearch.class);
        Search.Results results = envelope == null ? null : envelope.results;
        if (results == null || results.artistmatches == null || results.artistmatches.artist == null
                || results.artistmatches.artist.isEmpty()) {
            logger.debug("artist.search found no match for '" + artist + "'");
            return null;
        }
        Artists.Artist match = results.artistmatches.artist.get(0);
        logger.debug("artist.search matched '" + artist + "' as '" + match.name + "'");
        LinkedHashMap<String, String> infoParams = new LinkedHashMap<>();
        if (match.mbid != null && !match.mbid.isEmpty()) {
            infoParams.put("mbid", match.mbid);
        } else {
            infoParams.put("artist", match.name);
        }
        Artists.ArtistGetinfo info = this.query("artist.getInfo", infoParams, Artists.ArtistGetinfo.class);
        return info == null ? null : info.artist;
    }

    /**
     * lastfm orders an image list small to large, so the last entry is the biggest,
     * and the placeholder it hands back for an artist with no art is not a cover
     *
     * @param images - list of sized image urls
     * @return - the cover url or null if there is none
     */
    private String coverImage(List<Commons.Image> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        String coverImage = images.get(images.size() - 1)._text;
        if (coverImage == null || coverImage.isEmpty() || defaultImg.equals(coverImage)) {
            return null;
        }
        return coverImage;
    }

    /**
     * state a lastfm request: the method it asks for, the credentials every call
     * needs, and the params of the call itself, each as a parameter rather than
     * a joined query string
     *
     * @param <T>          the response bean type
     * @param type         - ex: track.getInfo
     * @param params       - the params of that method, may be empty
     * @param responseType - the superbeans bean the whole response binds to
     * @return - bound response or null if error
     * @throws InterruptedException - thrown when user interrupts
     */
    private <T> T query(String type, LinkedHashMap<String, String> params, Class<T> responseType)
            throws InterruptedException {
        if (type == null || type.isEmpty()) {
            logger.debug("null or empty type provided in query");
            return null;
        }
        if (params == null) {
            logger.debug("null params provided in query");
            return null;
        }
        WebTarget target = WebTools.target(baseUrl)
                .queryParam("method", type)
                .queryParam("api_key", Settings.lastFMApiKey)
                .queryParam("format", "json");
        for (String param : params.keySet()) {
            target = target.queryParam(param, params.get(param));
        }
        return this.query(target, responseType);
    }
}
