package ryzen.ownitall.classes;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.util.Levenshtein;

public class Album extends Playlist {
    private static final Logger logger = LogManager.getLogger(Album.class);
    private LinkedHashSet<Artist> artists;
    private LinkedHashMap<String, String> links;

    /**
     * Default constructor of album without album cover
     * 
     * @param name - album name
     */
    public Album(String name) {
        super(name);
        this.artists = new LinkedHashSet<>();
        this.links = new LinkedHashMap<>();
    }

    /**
     * full album constructor
     * 
     * @param name              - album name
     * @param songs             - linkedhashset of songs
     * @param youtubePageToken  - youtube page token
     * @param spotifyPageOffset - spotify page token
     * @param coverImage        - cover art
     * @param artists           - linkedhashset of artists
     */
    @JsonCreator
    public Album(@JsonProperty("name") String name,
            @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverImage") String coverImage,
            @JsonProperty("artists") LinkedHashSet<Artist> artists,
            @JsonProperty("links") LinkedHashMap<String, String> links) {
        super(name, songs, youtubePageToken, spotifyPageOffset, coverImage);
        this.artists = new LinkedHashSet<>();
        this.links = new LinkedHashMap<>();
        this.addArtists(artists);
        this.addLinks(links);
    }

    public void merge(Album album) {
        if (album == null) {
            logger.debug("null album passed in merge");
            return;
        }
        this.addSongs(album.getSongs());
        if (this.getCoverImage() == null) {
            this.setCoverImage(album.getCoverImage());
        }
        this.setYoutubePageToken(album.getYoutubePageToken());
        if (album.getSpotifyPageOffset() > this.getSpotifyPageOffset()) {
            this.setSpotifyPageOffset(album.getSpotifyPageOffset());
        }
        this.addArtists(album.getArtists());
    }

    @Override
    public void addSong(Song song) {
        if (song == null) {
            logger.debug(this.toString() + ": null song provided in addSong");
            return;
        }
        super.addSong(song);

        // this is here because the super in the json constructor calls on addsongs
        // before artists is initialized (its ugly, i know)
        if (this.artists == null) {
            this.artists = new LinkedHashSet<>();
        }
        this.addArtist(song.getArtist());
    }

    public void addArtists(LinkedHashSet<Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            logger.debug(this.toString() + ": empty artists array provided in addArtists");
            return;
        }
        this.artists.addAll(artists);
    }

    public void addArtist(Artist artist) {
        if (artist == null || artist.isEmpty()) {
            logger.debug(this.toString() + ": empty artist provided in addArtist");
            return;
        }
        // when artist becomes more complex, this will need a merge function
        this.artists.add(artist);
    }

    public void addArtist(String artistName) {
        if (artistName == null || artistName.isEmpty()) {
            logger.debug(this.toString() + ": empty artistName provided in addArtist");
            return;
        }
        this.artists.add(new Artist(artistName));
    }

    public LinkedHashSet<Artist> getArtists() {
        return this.artists;
    }

    @JsonIgnore
    public Artist getMainArtist() {
        Iterator<Artist> iterator = this.artists.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    public void addLink(String key, String url) {
        if (key == null || url == null || key.isEmpty() || url.isEmpty()) {
            logger.debug(this.toString() + ": empty key or url in addLink");
            return;
        }
        this.links.put(key, url);
    }

    public void addLinks(LinkedHashMap<String, String> links) {
        if (links == null || links.isEmpty()) {
            logger.debug(this.toString() + ": empty links array provided in addLinks");
            return;
        }
        this.links.putAll(links);
    }

    @JsonIgnore
    public String getLink(String key) {
        if (key == null || key.isEmpty()) {
            logger.debug(this.toString() + ": empty key provided in getLink");
            return null;
        }
        return this.links.get(key);
    }

    public LinkedHashMap<String, String> getLinks() {
        return this.links;
    }

    @Override
    @JsonIgnore
    public String toString() {
        String output = super.toString();
        if (this.getMainArtist() != null) {
            output += " (" + this.getMainArtist().toString().trim() + ")";
        }
        return output;
    }

    @Override
    @JsonIgnore
    public String getM3U() {
        // m3u header
        StringBuilder output = new StringBuilder();
        output.append("#EXTM3U").append("\n");
        // m3u album information
        output.append("#EXTALB:").append(this.toString()).append("\n");
        output.append("#EXTART:").append(this.getMainArtist().toString().trim()).append("\n");
        // m3u album cover
        output.append("#EXTIMG:").append("cover.png").append("\n");
        // m3u album contents
        for (Song song : this.getSongs()) {
            File file = new File(song.getFileName() + "." + downloadFormat);
            output.append("#EXTINF:").append(String.valueOf(song.getDuration().toSeconds())).append(",")
                    .append(song.toString()).append("\n");
            output.append(file.getPath()).append("\n");
        }
        return output.toString();
    }

    @Override
    @JsonIgnore
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Album)) {
            return false;
        }
        Album album = (Album) object;
        // only valid if library used
        if (this.getLink("lastfm") != null) {
            if (this.getLink("lastfm").equals(album.getLink("lastfm"))) {
                return true;
            }
        }
        if (Levenshtein.computeSimilarityCheck(this.toString(), album.toString(),
                simularityPercentage)) {
            return true;
        }
        return false;
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return Objects.hash(super.hashCode(), artists);
    }
}
