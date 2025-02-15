package ryzen.ownitall.classes;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.util.Levenshtein;

public class Album extends Playlist {
    private static final Logger logger = LogManager.getLogger(Album.class);
    private LinkedHashSet<Artist> artists;

    /**
     * Default constructor of album without album cover
     * 
     * @param name - album name
     */
    public Album(String name) {
        super(name);
        this.artists = new LinkedHashSet<>();
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
     * @param links             - linkedhasmap of links
     */
    @JsonCreator
    public Album(@JsonProperty("name") String name,
            @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("links") LinkedHashMap<String, String> links,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverImage") String coverImage,
            @JsonProperty("artists") LinkedHashSet<Artist> artists) {
        super(name, songs, links, youtubePageToken, spotifyPageOffset, coverImage);
        this.artists = new LinkedHashSet<>();
        if (artists != null && !artists.isEmpty()) {
            this.addArtists(artists);
        }
    }

    /**
     * merge two albums together
     * used when adding to linkedhashset and one already exists
     * 
     * @param album - album to merge into current
     */
    public void merge(Album album) {
        if (album == null) {
            logger.debug("null album passed in merge");
            return;
        }
        this.addSongs(album.getSongs());
        if (this.getCoverImage() == null && album.getCoverImage() != null) {
            this.setCoverImage(album.getCoverImage());
        }
        if (album.getYoutubePageToken() != null) {
            this.setYoutubePageToken(album.getYoutubePageToken());
        }
        if (album.getSpotifyPageOffset() > this.getSpotifyPageOffset()) {
            this.setSpotifyPageOffset(album.getSpotifyPageOffset());
        }
        this.addArtists(album.getArtists());
    }

    /**
     * add song to album
     * also adds artists from song into current album artists
     * 
     * @param song - song to add
     */
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

    /**
     * add artists to album artists
     * 
     * @param artists - linkedhashset of artist to add
     */
    public void addArtists(LinkedHashSet<Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            logger.debug(this.toString() + ": empty artists array provided in addArtists");
            return;
        }
        this.artists.addAll(artists);
    }

    /**
     * add artist to album
     * 
     * @param artist - constructed artist to add
     */
    public void addArtist(Artist artist) {
        if (artist == null || artist.isEmpty()) {
            logger.debug(this.toString() + ": empty artist provided in addArtist");
            return;
        }
        // when artist becomes more complex, this will need a merge function
        this.artists.add(artist);
    }

    /**
     * get all album artists
     * 
     * @return - linkedhashset of artist
     */
    public LinkedHashSet<Artist> getArtists() {
        return this.artists;
    }

    /**
     * get album main artist
     * 
     * @return - first artist in album
     */
    @JsonIgnore
    public Artist getMainArtist() {
        Iterator<Artist> iterator = this.artists.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
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

    /**
     * get .nfo data for album
     */
    // TODO: gotta do .nfo for jellyfin
    @JsonIgnore
    public String getNFO() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("album");
            doc.appendChild(rootElement);

            // Title
            Element title = doc.createElement("title");
            title.appendChild(doc.createTextNode(this.getName()));
            rootElement.appendChild(title);

            // Artists
            Element artistsElement = doc.createElement("artists");
            rootElement.appendChild(artistsElement);
            for (Artist artist : this.artists) {
                Element artistElement = doc.createElement("artist");
                artistElement.appendChild(doc.createTextNode(artist.getName()));
                artistsElement.appendChild(artistElement);
            }

            // Songs
            Element tracksElement = doc.createElement("tracks");
            rootElement.appendChild(tracksElement);
            for (Song song : this.getSongs()) {
                Element trackElement = doc.createElement("track");

                Element trackTitle = doc.createElement("title");
                trackTitle.appendChild(doc.createTextNode(song.getName()));
                trackElement.appendChild(trackTitle);

                Element trackDuration = doc.createElement("duration");
                trackDuration.appendChild(doc.createTextNode(String.valueOf(song.getDuration())));
                trackElement.appendChild(trackDuration);

                tracksElement.appendChild(trackElement);
            }

            // Cover image
            if (this.getCoverImage() != null) {
                Element thumb = doc.createElement("thumb");
                thumb.appendChild(doc.createTextNode("cover.png"));
                rootElement.appendChild(thumb);
            }

            // Transform the DOM to XML string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);

            return writer.toString();

        } catch (Exception e) {
            logger.error("Error generating NFO content for " + this.toString() + ": " + e);
            return null;
        }
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
        if (this.getId("lastfm") != null && album.getId("lastfm") != null) {
            if (this.getId("lastfm").equals(album.getId("lastfm"))) {
                return true;
            }
        }
        if (Levenshtein.computeSimilarityCheck(this.toString(), album.toString(),
                simularityPercentage)) {
            return true;
        }
        return false;
    }

    @JsonIgnore
    public boolean contains(Song song) {
        if (song == null) {
            logger.debug("null song provided in contains");
            return false;
        }
        return super.contains(song);
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return Objects.hash(super.hashCode(), artists);
    }
}
