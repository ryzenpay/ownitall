package ryzen.ownitall.library;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.ws.rs.client.WebTarget;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.WebTools;
import ryzen.superbeans.musicbrainz.Artists;
import ryzen.superbeans.musicbrainz.Commons;
import ryzen.superbeans.musicbrainz.Recordings;
import ryzen.superbeans.musicbrainz.Releases;
import ryzen.superbeans.musicbrainz.Search;

/**
 * <p>
 * MusicBrainz class.
 * </p>
 *
 * <p>
 * MusicBrainz documents everything a distributor has ever shipped, so a search
 * for a title by a named artist answers with a hundred entities that all carry
 * the words. This takes the top one musicbrainz ranks, and asks nothing of it
 * beyond what cannot be argued about: that a pressing is the official one
 * rather
 * than a bootleg of it, that an artist is a person or a band rather than a
 * character page, that a recording is not the live take of a song whose name
 * carries no such note, and that a length on the candidate does not contradict
 * a
 * length measured off the file. Nothing here weighs or scores a candidate: an
 * answer either passes those and is taken in the order the index gave them, or
 * it
 * does not and the next one is looked at.
 * </p>
 *
 * <p>
 * The passes run narrow to wide, a filtered search before the search that gave
 * the filters up, so a wide pass is only reached when nothing survived the
 * narrow
 * one. An album is asked of two indexes, the release group first and then that
 * group's pressings, because an album is a work and an edition of it and a
 * title
 * match alone cannot tell them apart.
 * </p>
 *
 * @author ryzen
 */
public class MusicBrainz extends Library {
    private static final Logger logger = new Logger(MusicBrainz.class);
    private static final String baseUrl = "https://musicbrainz.org/ws/2";
    private static final String coverArtUrl = "https://coverartarchive.org";
    /**
     * candidates a search asks for, so that the top one is the top one that
     * passes rather than the top one handed back
     */
    private static final int searchLimit = 25;
    /**
     * the milliseconds a recording length may sit from a file's duration and
     * still be the same take: tags are written to the second and rips are cut to
     * the frame, so the window is generous and the same tolerance both filters
     * the narrow search and decides whether a length contradicts the file
     */
    private static final long durationToleranceMillis = 2000;
    /**
     * the words that mean an entity is a version of something rather than the
     * thing: musicbrainz writes them in a disambiguation for exactly that case,
     * and a tag carries them in brackets when the file is one too
     */
    private static final String[] versionHints = { "live", "cover", "demo", "remix", "remaster", "edit", "instrumental",
            "alt", "outtake", "mono", "stereo" };

    /**
     * default MusicBrainz constructor
     * initializes all values and loads from cache
     */
    public MusicBrainz() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public Album getAlbum(Album album) throws InterruptedException {
        String albumKey = "{release=" + (album == null ? null : album.getName()) + "}";
        String foundId = album == null ? null : ids.get(albumKey);
        if (foundId != null) {
            return this.getAlbum(foundId);
        }
        String id = this.searchAlbum(album);
        if (id == null) {
            return null;
        }
        Album found = this.getAlbum(id);
        if (found != null) {
            ids.put(albumKey, id);
        }
        return found;
    }

    /**
     * an album is a release group and an edition of it, so this asks the group
     * index which group the name belongs to and only then asks the release index
     * for that group's pressings: searching releases by title alone is how a
     * library entry ends up as the 2009 japanese paper sleeve, because it is as
     * good a title match as the record itself
     *
     * @param album - the album to find
     * @return - the release mbid of the best edition, or null
     * @throws java.lang.InterruptedException if any.
     */
    private String searchAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album passed in searchAlbum");
            return null;
        }
        if (album.getId("mbid") != null) {
            return album.getId("mbid");
        }
        String artistId = null;
        if (album.getMainArtist() != null) {
            artistId = this.searchArtist(album.getMainArtist());
        }
        String groupId = this.searchReleaseGroup(album, artistId);
        if (groupId != null) {
            String releaseId = this.searchReleases(album, artistId, groupId, true);
            if (releaseId != null) {
                return releaseId;
            }
            // a group whose pressings are all promos or bootlegs still has
            // pressings, and the wider pass names which of them is top
            releaseId = this.searchReleases(album, artistId, groupId, false);
            if (releaseId != null) {
                return releaseId;
            }
        }
        // nothing in that group passed, so whatever else carries the title beats
        // nothing at all
        String releaseId = this.searchReleases(album, artistId, null, false);
        if (releaseId != null) {
            return releaseId;
        }
        if (!album.getName().equals(MusicTools.removeBrackets(album.getName()))) {
            logger.debug("Trying album '" + album.getName() + "' again with trimmed brackets");
            album.setName(MusicTools.removeBrackets(album.getName()));
            return searchAlbum(album);
        }
        logger.info("Unable to find album '" + album.toString() + "' in library");
        return null;
    }

    /**
     * the release group an album name belongs to, the index that holds the album
     * type and the first release date: the top ranked group that is an album of
     * that name by that artist, with a pass without the type filter behind it for
     * the groups musicbrainz has never typed
     *
     * @param album    - the album to find
     * @param artistId - the main artist's mbid, may be null
     * @return - the release group mbid, or null if none passed
     * @throws java.lang.InterruptedException if any.
     */
    private String searchReleaseGroup(Album album, String artistId) throws InterruptedException {
        String artistName = album.getMainArtist() == null ? null : album.getMainArtist().getName();
        String groupId = this.searchReleaseGroups(album, artistId, artistName, true);
        if (groupId != null) {
            return groupId;
        }
        return this.searchReleaseGroups(album, artistId, artistName, false);
    }

    /**
     * one pass of the release group index
     *
     * @param album      - the album to find
     * @param artistId   - the main artist's mbid, may be null
     * @param artistName - the main artist's name, may be null
     * @param strict     - whether the group has to be typed an album
     * @return - the release group mbid, or null if none passed
     * @throws java.lang.InterruptedException if any.
     */
    private String searchReleaseGroups(Album album, String artistId, String artistName, boolean strict)
            throws InterruptedException {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("name", this.luceneValue(album.getName()));
        if (strict) {
            params.put("type", "\"Album\"");
        }
        if (artistId != null) {
            params.put("arid", this.luceneValue(artistId));
        } else if (artistName != null && !artistName.trim().isEmpty()) {
            params.put("artist", this.luceneValue(artistName));
        }
        Search.ReleaseGroupSearch response = this.query(this.searchTarget("release-group", params),
                Search.ReleaseGroupSearch.class);
        if (response == null || response.release_groups == null || response.release_groups.isEmpty()) {
            logger.debug("release-group search found nothing for album '" + album.getName() + "'");
            return null;
        }
        for (Search.ReleaseGroups group : response.release_groups) {
            if (group == null || group.id == null || group.id.trim().isEmpty()) {
                continue;
            }
            // a name asked of the group index is not answered by a group with
            // another name, whatever the query matched it on, and a recording
            // credit spelled differently from the tag is not this artist either
            if (!normalized(group.title).equals(normalized(album.getName()))) {
                continue;
            }
            if (artistId != null && !this.creditsHaveId(group.artist_credit, artistId)) {
                continue;
            }
            if (artistId == null && artistName != null && !artistName.trim().isEmpty()
                    && !this.creditsMatch(group.artist_credit, artistName)) {
                continue;
            }
            logger.debug("Album '" + album.getName() + "' is release group '" + group.id + "' ("
                    + group.primary_type + " " + group.first_release_date + ")");
            return group.id;
        }
        logger.debug("No release group passed for album '" + album.getName() + "'");
        return null;
    }

    /**
     * which pressing of a release group to hand back: the pressings of one group
     * are the same album, so the only pressing worth asking about is the one
     * musicbrainz ranks above the rest, and the only thing that rules a pressing
     * out is its status. strict asks for official, since a bootleg carries the
     * same title and the same songs and nothing else about it
     *
     * @param album    - the album being looked for
     * @param artistId - the main artist's mbid, may be null
     * @param groupId  - restrict to this release group's editions, null to search
     *                 the title instead
     * @param strict   - whether an official status is required
     * @return - the release mbid, or null if nothing passed
     * @throws java.lang.InterruptedException if any.
     */
    private String searchReleases(Album album, String artistId, String groupId, boolean strict)
            throws InterruptedException {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        if (groupId != null) {
            params.put("rgid", this.luceneValue(groupId));
        } else {
            params.put("release", this.luceneValue(album.getName()));
        }
        if (artistId != null) {
            params.put("arid", this.luceneValue(artistId));
        }
        String barcode = album.getId("barcode");
        if (barcode != null && !barcode.trim().isEmpty()) {
            params.put("barcode", this.luceneValue(barcode));
        }
        Search.ReleaseSearch response = this.query(this.searchTarget("release", params),
                Search.ReleaseSearch.class);
        if (response == null || response.releases == null || response.releases.isEmpty()) {
            logger.debug("release search found nothing for album '" + album.getName() + "'");
            return null;
        }
        for (Commons.LabelLookupReleases release : response.releases) {
            if (release == null || release.id == null || release.id.trim().isEmpty()) {
                continue;
            }
            // a bootleg of an album carries its title and its songs and nothing
            // else to tell it apart, and musicbrainz marks anything it cannot
            // prove official as one
            if (strict && !"Official".equals(release.status)) {
                continue;
            }
            // searching the title rather than the group says nothing about whose
            // album it is, so that is the one thing still worth asking
            if (groupId == null && artistId != null && !this.creditsHaveId(release.artist_credit, artistId)) {
                continue;
            }
            logger.debug("Album '" + album.getName() + "' matched release '" + release.id + "' (" + release.status
                    + " " + release.date + " " + release.country + " " + release.track_count + " tracks)");
            return release.id;
        }
        logger.debug("No release passed for album '" + album.getName() + "'");
        return null;
    }

    /**
     * <p>
     * getAlbum.
     * </p>
     *
     * @param id a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public Album getAlbum(String id) throws InterruptedException {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getAlbum");
            return null;
        }
        Album foundAlbum = albums.get(id);
        if (foundAlbum != null) {
            return foundAlbum;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("recordings");
        inclusions.add("artists");
        // check cache
        Releases.ReleaseLookup response = this.query(this.lookupTarget("release", id, inclusions),
                Releases.ReleaseLookup.class);
        if (response != null) {
            Album album = new Album(response.title);
            album.addId("mbid", response.id);
            if (response.cover_art_archive != null) {
                if (response.cover_art_archive.count != null && response.cover_art_archive.count > 1) {
                    URI albumCover = this.getCoverArt(response.id);
                    if (albumCover != null) {
                        album.setCoverImage(albumCover);
                    }
                }
            } else {
                logger.debug("Album '" + album.getName() + "' missing coverart");
            }
            if (response.artist_credit != null) {
                for (Commons.ArtistCredit credit : response.artist_credit) {
                    if (credit.artist == null) {
                        continue;
                    }
                    Artist artist = this.getArtist(credit.artist.id);
                    if (artist != null) {
                        album.addArtist(artist);
                    }
                }
            } else {
                logger.debug("album '" + album.getName() + "' missing artists");
            }
            if (response.media != null) {
                for (Releases.Media medium : response.media) {
                    if (medium.tracks == null) {
                        logger.debug("Album '" + album.getName() + "' disc missing songs");
                        continue;
                    }
                    for (Releases.Tracks track : medium.tracks) {
                        if (track.recording == null) {
                            continue;
                        }
                        Song song = this.getSong(track.recording.id);
                        if (song != null) {
                            album.addSong(song);
                        }
                    }
                }
            } else {
                logger.debug("Album '" + album.getName() + "' missing songs");
            }
            albums.put(id, album);
            return album;
        }
        logger.info("Unable to find Album: '" + id + "' in library");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Song getSong(Song song) throws InterruptedException {
        String songKey = "{recording=" + (song == null ? null : song.getName()) + "}";
        String foundId = song == null ? null : ids.get(songKey);
        if (foundId != null) {
            return this.getSong(foundId);
        }
        String id = this.searchSong(song);
        if (id == null) {
            return null;
        }
        Song found = this.getSong(id);
        if (found != null) {
            // cached only once the recording has been read, for the same reason
            // as the album above: a search that found an id the lookup could not
            // read, or read as the wrong take, used to be kept and handed to every
            // later copy of that song
            ids.put(songKey, id);
        }
        return found;
    }

    /**
     * the recording index and nothing else: a recording is a song and a release
     * is a pressing of one, so asking the recording index for the album title as
     * well or'd a second field into the query and every song on an album named
     * after the song matched it. what the file itself knows, its isrc and its
     * measured length, filters the narrow pass, since both are facts about this
     * recording rather than opinions about which one is best
     *
     * @param song - the song to find
     * @return - the recording mbid of the top ranked match, or null
     * @throws java.lang.InterruptedException if any.
     */
    private String searchSong(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("null song passed in searchSong");
            return null;
        }
        if (song.getId("mbid") != null) {
            return song.getId("mbid");
        }
        String artistId = null;
        if (song.getMainArtist() != null) {
            artistId = this.searchArtist(song.getMainArtist());
        }
        String isrc = song.getId("isrc");
        long duration = song.getDuration() == null ? -1 : song.getDuration().toMillis();
        String recordingId = this.searchRecordings(song, artistId, isrc, duration, true);
        if (recordingId != null) {
            return recordingId;
        }
        recordingId = this.searchRecordings(song, artistId, isrc, duration, false);
        if (recordingId != null) {
            return recordingId;
        }
        if (!song.getName().equals(MusicTools.removeBrackets(song.getName()))) {
            logger.debug("Trying song '" + song.getName() + "' again with trimmed brackets");
            song.setName(MusicTools.removeBrackets(song.getName()));
            return searchSong(song);
        }
        logger.info("Unable to find song '" + song.toString() + "' in library");
        return null;
    }

    /**
     * one pass of the recording index; strict keeps the isrc and the length
     * window, the two facts a file can hold about its own recording
     *
     * @param song     - the song being looked for
     * @param artistId - the main artist's mbid, may be null
     * @param isrc     - the file's isrc, may be null
     * @param duration - the file's length in milliseconds, negative when the tag
     *                 carries none
     * @param strict   - whether the isrc and length filters stay on
     * @return - the recording mbid, or null if nothing passed
     * @throws java.lang.InterruptedException if any.
     */
    private String searchRecordings(Song song, String artistId, String isrc, long duration, boolean strict)
            throws InterruptedException {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("recording", this.luceneValue(song.getName()));
        if (artistId != null) {
            params.put("arid", this.luceneValue(artistId));
        } else if (song.getMainArtist() != null && song.getMainArtist().getName() != null
                && !song.getMainArtist().getName().trim().isEmpty()) {
            params.put("artist", this.luceneValue(song.getMainArtist().getName()));
        }
        if (strict && isrc != null && !isrc.trim().isEmpty()) {
            params.put("isrc", this.luceneValue(isrc));
        }
        if (strict && duration >= 0) {
            params.put("dur", "[" + Math.max(0, duration - durationToleranceMillis) + " TO "
                    + (duration + durationToleranceMillis) + "]");
        }
        params.put("video", "\"false\"");
        Search.RecordingSearch response = this.query(this.searchTarget("recording", params),
                Search.RecordingSearch.class);
        if (response == null || response.recordings == null || response.recordings.isEmpty()) {
            logger.debug("recording search found nothing for song '" + song.getName() + "'");
            return null;
        }
        String artistName = song.getMainArtist() == null ? null : song.getMainArtist().getName();
        for (Search.RecordingSearchRecordings recording : response.recordings) {
            if (recording == null || recording.id == null || recording.id.trim().isEmpty()) {
                continue;
            }
            // a length the file rules out is two different recordings whatever
            // else they share; no length on either side is no contradiction, since
            // a great many recordings carry none and some tags carry no duration
            if (recording.length != null && duration >= 0
                    && Math.abs(recording.length - duration) > durationToleranceMillis) {
                continue;
            }
            if (conflictsOnVersion(recording.title, recording.disambiguation, song.getName())) {
                continue;
            }
            if (artistId != null && !this.creditsHaveId(recording.artist_credit, artistId)) {
                continue;
            }
            if (artistId == null && artistName != null && !artistName.trim().isEmpty()
                    && !this.creditsMatch(recording.artist_credit, artistName)) {
                continue;
            }
            logger.debug("Song '" + song.getName() + "' matched recording '" + recording.id + "' ("
                    + recording.title + " " + recording.length + "ms"
                    + (recording.disambiguation != null && !recording.disambiguation.trim().isEmpty()
                            ? " [" + recording.disambiguation + "]" : "") + ")");
            return recording.id;
        }
        logger.debug("No recording passed for song '" + song.getName() + "'");
        return null;
    }

    /**
     * <p>
     * getSong.
     * </p>
     *
     * @param id a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    public Song getSong(String id) throws InterruptedException {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getSong");
            return null;
        }
        Song foundSong = songs.get(id);
        if (foundSong != null) {
            return foundSong;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("artists");
        inclusions.add("url-rels");
        inclusions.add("releases");
        Recordings.RecordingLookup response = this.query(this.lookupTarget("recording", id, inclusions),
                Recordings.RecordingLookup.class);
        if (response != null) {
            Song song = new Song(response.title);
            if (response.length != null) {
                song.setDuration(response.length.longValue(), ChronoUnit.MILLIS);
            }
            song.addId("mbid", response.id);
            if (response.artist_credit != null && !response.artist_credit.isEmpty()
                    && response.artist_credit.get(0).artist != null) {
                Artist artist = this.getArtist(response.artist_credit.get(0).artist.id);
                if (artist != null) {
                    song.addArtist(artist);
                }
            } else {
                logger.debug("Song '" + song.getName() + "' missing artists");
            }
            if (response.releases != null && !response.releases.isEmpty()) {
                URI songCover = this.getCoverArt(response.releases.get(0).id);
                if (songCover != null) {
                    song.setCoverImage(songCover);
                }
            }
            this.addStreamingIds(song, response.relations);
            songs.put(id, song);
            return song;
        }
        logger.info("Could not find Song '" + id + "' in library");
        return null;
    }

    /**
     * pull the platform ids out of a recording's url relations: the spotify
     * track or youtube video the musicbrainz editors name for this exact
     * recording beats the top hit of any platform search, and the spotify and
     * youtube integrations read both ids straight off the song
     *
     * @param song      - the song to attach the found ids to
     * @param relations - the recording's url relations from the lookup
     */
    private void addStreamingIds(Song song, List<Recordings.Relations> relations) {
        if (relations == null) {
            return;
        }
        String youtube = null;
        for (Recordings.Relations relation : relations) {
            if (relation == null || relation.url == null || relation.url.resource == null) {
                continue;
            }
            String resource = relation.url.resource;
            if (song.getId("spotify") == null) {
                String spotifyId = this.matchingId(resource, "open.spotify.com", "/track/");
                if (spotifyId != null) {
                    song.addId("spotify", spotifyId);
                    logger.debug("MusicBrainz relations give spotify id '" + spotifyId + "' for '"
                            + song.getName() + "'");
                }
            }
            String videoId = this.youtubeVideoId(resource);
            if (videoId != null) {
                if (resource.contains("music.youtube.com")) {
                    youtube = videoId;
                } else if (youtube == null) {
                    youtube = videoId;
                }
            }
        }
        if (youtube != null) {
            song.addId("youtube", youtube);
            logger.debug("MusicBrainz relations give youtube id '" + youtube + "' for '" + song.getName() + "'");
        }
    }

    private String matchingId(String resource, String host, String marker) {
        if (!resource.contains(host) || !resource.contains(marker)) {
            return null;
        }
        String tail = resource.substring(resource.indexOf(marker) + marker.length());
        String id = tail.split("[?#]")[0];
        return id.isEmpty() ? null : id;
    }

    private String youtubeVideoId(String resource) {
        if (resource.contains("youtu.be/")) {
            String tail = resource.substring(resource.indexOf("youtu.be/") + "youtu.be/".length());
            String id = tail.split("[?#&]")[0];
            return id.isEmpty() ? null : id;
        }
        if (!resource.contains("youtube.com")) {
            return null;
        }
        String query = resource.contains("?") ? resource.substring(resource.indexOf('?') + 1) : "";
        for (String param : query.split("&")) {
            if (param.startsWith("v=")) {
                String id = param.substring(2).split("#")[0];
                return id.isEmpty() ? null : id;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Artist getArtist(Artist artist) throws InterruptedException {
        String artistKey = "{artist=" + (artist == null ? null : artist.getName()) + "}";
        String foundId = artist == null ? null : ids.get(artistKey);
        if (foundId != null) {
            return this.getArtist(foundId);
        }
        String id = this.searchArtist(artist);
        if (id == null) {
            return null;
        }
        Artist found = this.getArtist(id);
        if (found != null) {
            // keyed like the alias cache below, so a lookup of this artist seeds
            // it and every other spelling of the name finds it without searching
            ids.putIfAbsent(artistKey, id);
        }
        return found;
    }

    /**
     * <p>
     * searchArtist. the artist index answers a name with every entity carrying
     * it, so a name that is not perfectly curated picks up the character pages
     * and the entities never given a type: the narrow pass keeps the types that
     * release music, the wide one keeps whatever answered, and the top ranked
     * entity of either is taken as it comes
     * </p>
     *
     * @param artist a {@link ryzen.ownitall.classes.Artist} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String searchArtist(Artist artist) throws InterruptedException {
        if (artist == null) {
            logger.debug("null artist passed in searchArtist");
            return null;
        }
        if (artist.getId("mbid") != null) {
            return artist.getId("mbid");
        }
        String artistId = this.searchArtists(artist, true);
        if (artistId != null) {
            return artistId;
        }
        return this.searchArtists(artist, false);
    }

    /**
     * one pass of the artist index
     *
     * @param artist - the artist being looked for
     * @param strict - whether character and untyped entities are excluded
     * @return - the artist mbid, or null if nothing passed
     * @throws java.lang.InterruptedException if any.
     */
    private String searchArtists(Artist artist, boolean strict) throws InterruptedException {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("artist", this.luceneValue(artist.getName()));
        Search.ArtistSearch response = this.query(this.searchTarget("artist", params),
                Search.ArtistSearch.class);
        if (response == null || response.artists == null || response.artists.isEmpty()) {
            logger.debug("could not find artist '" + artist.getName() + "' in library");
            return null;
        }
        for (Search.ArtistSearchEndedArtists candidate : response.artists) {
            if (candidate == null || candidate.id == null || candidate.id.trim().isEmpty()) {
                continue;
            }
            // character pages and untyped entities carry artist names the same way
            // a person does and answer a search just as well
            if (strict && !("Person".equals(candidate.type) || "Group".equals(candidate.type))) {
                continue;
            }
            logger.debug("Artist '" + artist.getName() + "' matched '" + candidate.id + "' (" + candidate.name + " "
                    + candidate.type + ")");
            return candidate.id;
        }
        logger.debug("No artist passed for '" + artist.getName() + "'");
        return null;
    }

    /**
     * <p>
     * getArtist.
     * </p>
     *
     * @param id a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Artist} object
     * @throws java.lang.InterruptedException if any.
     */
    public Artist getArtist(String id) throws InterruptedException {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getArtist");
            return null;
        }
        Artist foundArtist = artists.get(id);
        if (foundArtist != null) {
            return foundArtist;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("aliases");
        Artists.ArtistLookup response = this.query(this.lookupTarget("artist", id, inclusions),
                Artists.ArtistLookup.class);
        if (response != null) {
            Artist artist = new Artist(response.name);
            artist.addId("mbid", response.id);
            this.cacheArtistNames(id, response.name, response.aliases);
            artists.put(id, artist);
            return artist;
        }
        logger.info("Could not find Artist '" + id + "' in library");
        return null;
    }

    /**
     * seed the search cache with the artist's own name and every alias the
     * lookup carries, keyed exactly like searchArtist keys its params
     *
     * @param id      - the artist mbid
     * @param name    - the artist's primary name
     * @param aliases - the other spellings the lookup found, may be null
     */
    private void cacheArtistNames(String id, String name, List<Commons.Aliases> aliases) {
        if (name != null && !name.isEmpty()) {
            ids.putIfAbsent("{artist=" + name + "}", id);
        }
        if (aliases != null) {
            for (Commons.Aliases alias : aliases) {
                if (alias.name != null && !alias.name.isEmpty()) {
                    ids.putIfAbsent("{artist=" + alias.name + "}", id);
                }
            }
        }
    }

    private URI getCoverArt(String id) {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getCoverArt");
            return null;
        }
        WebTarget target = WebTools.target(coverArtUrl).path("release").path(id).path("front");
        URI redirect = WebTools.redirect(target);
        if (redirect != null) {
            return redirect;
        }
        logger.debug("No coverart found for: '" + id + "'");
        return null;
    }

    /**
     * a search of one type: every param becomes a clause of one lucene filter,
     * in the order they are cached under. a value arrives already quoted, or as a
     * lucene construct of its own, so a title carrying a quote cannot rewrite the
     * query and a range cannot be quoted into a string
     *
     * @param type   - ex: recording
     * @param params - field names to filter on
     * @return a {@link jakarta.ws.rs.client.WebTarget} object
     */
    private WebTarget searchTarget(String type, LinkedHashMap<String, String> params) {
        if (type == null || type.isEmpty()) {
            logger.debug("null or empty type provided in searchTarget");
            return null;
        }
        if (params == null || params.isEmpty()) {
            logger.debug("no params provided in searchTarget");
            return null;
        }
        StringBuilder filter = new StringBuilder();
        for (String param : params.keySet()) {
            String value = params.get(param);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (filter.length() > 0) {
                filter.append(" AND ");
            }
            filter.append(param).append(':').append(value);
        }
        WebTarget target = WebTools.target(baseUrl).path(type)
                .queryParam("query", filter.toString())
                .queryParam("fmt", "json")
                .queryParam("limit", searchLimit);
        return target;
    }

    /**
     * a lookup of one id: the relations to ride along come as one '+' separated
     * list, which webtools encodes as %2B, the only form musicbrainz takes
     * (a comma is read as one long relation name and answered with a 400)
     *
     * @param type       - ex: release
     * @param id         - the musicbrainz id
     * @param inclusions - relations to ask for, may be empty
     * @return a {@link jakarta.ws.rs.client.WebTarget} object
     */
    private WebTarget lookupTarget(String type, String id, LinkedHashSet<String> inclusions) {
        if (type == null || type.isEmpty()) {
            logger.debug("null or empty type provided in lookupTarget");
            return null;
        }
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in lookupTarget");
            return null;
        }
        WebTarget target = WebTools.target(baseUrl).path(type).path(id);
        if (inclusions != null && !inclusions.isEmpty()) {
            target = target.queryParam("inc", String.join("+", inclusions));
        }
        return target.queryParam("fmt", "json");
    }

    /**
     * a value as lucene wants it: quoted, with the characters lucene reads as
     * syntax escaped, so a title carrying a quote or a bracket filters on the
     * title rather than on a query lucene cannot parse
     *
     * @param value - the value to filter on
     * @return the quoted value, or null when there was none
     */
    private String luceneValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * whether any credit of a candidate is the artist asked for, joinphrase and
     * credit spellings included since those are what a tag copies
     *
     * @param credits    - the candidate's artist credit, may be null
     * @param artistName - the artist name asked for, may be null
     * @return true if one of them is it
     */
    private boolean creditsMatch(List<Commons.ArtistCredit> credits, String artistName) {
        if (credits == null || artistName == null || artistName.trim().isEmpty()) {
            return false;
        }
        String wanted = normalized(artistName);
        for (Commons.ArtistCredit credit : credits) {
            if (credit == null) {
                continue;
            }
            if (wanted.equals(normalized(credit.name))
                    || (credit.artist != null && wanted.equals(normalized(credit.artist.name)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * whether any credit of a candidate is the artist the artist search found,
     * which is the only artist test that survives a name credited differently
     *
     * @param credits  - the candidate's artist credit, may be null
     * @param artistId - the artist's mbid
     * @return true if one of them is it
     */
    private boolean creditsHaveId(List<Commons.ArtistCredit> credits, String artistId) {
        if (credits == null || artistId == null || artistId.trim().isEmpty()) {
            return false;
        }
        for (Commons.ArtistCredit credit : credits) {
            if (credit != null && credit.artist != null && artistId.equals(credit.artist.id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * whether a candidate's version disagrees with the file's: 'Creep (Live)' and
     * 'live, 1992' are the same take, 'Creep (Live)' and 'karaoke version' are two
     * different things that both are not the studio one, and a bare title asked of
     * a candidate that names a version is asking for the studio take of something
     * that is not it. a comment naming no version we know is an editor's note
     * about the same recording, so it rules nothing out. the version travels in
     * the title as often as in the comment, and musicbrainz will happily rank
     * 'Everything in Its Right Place (The Vertigo remix)' above the song
     *
     * @param title          - the candidate's title
     * @param disambiguation - the candidate's disambiguation
     * @param name           - the song name asked for
     * @return true if the two name different versions
     */
    private static boolean conflictsOnVersion(String title, String disambiguation, String name) {
        String wanted = versionWords(name);
        String offered = versionWords(title + " " + disambiguation);
        if (wanted.isEmpty()) {
            return !offered.isEmpty();
        }
        return !offered.isEmpty() && !sharesWord(wanted, offered);
    }

    /**
     * the versions a piece of text names, in the words this class sorts on
     *
     * @param text - a song name or a disambiguation comment, may be null
     * @return the version words it holds, space separated, empty for none
     */
    private static String versionWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String lower = text.toLowerCase();
        StringBuilder words = new StringBuilder();
        for (String hint : versionHints) {
            if (lower.contains(hint)) {
                if (words.length() > 0) {
                    words.append(' ');
                }
                words.append(hint);
            }
        }
        return words.toString();
    }

    /**
     * whether two sets of version words name the same version
     *
     * @param wanted  - the versions the name asked for names
     * @param offered - the versions the candidate names
     * @return true if they have one in common
     */
    private static boolean sharesWord(String wanted, String offered) {
        for (String word : offered.split(" ")) {
            if ((" " + wanted + " ").contains(" " + word + " ")) {
                return true;
            }
        }
        return false;
    }

    /**
     * a name compared on letters and digits alone: musicbrainz writes the
     * apostrophe in an artist's name as a typographic quote, and a tag copies
     * whatever keyboard typed it
     *
     * @param name a {@link java.lang.String} object
     * @return the name to compare
     */
    private static String normalized(String name) {
        return name == null ? "" : name.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
