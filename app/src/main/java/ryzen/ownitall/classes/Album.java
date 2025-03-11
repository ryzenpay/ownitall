package ryzen.ownitall.classes;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Album extends Playlist {
    private static final Logger logger = LogManager.getLogger(Album.class);
    private ArrayList<Artist> artists;

    /**
     * Default constructor of album without album cover
     * 
     * @param name - album name
     */
    public Album(String name) {
        super(name);
        this.artists = new ArrayList<>();
    }

    /**
     * full album constructor
     * 
     * @param name       - album name
     * @param songs      - linkedhashset of songs
     * @param coverImage - cover art
     * @param artists    - linkedhashset of artists
     * @param links      - linkedhasmap of links
     */
    @JsonCreator
    public Album(@JsonProperty("name") String name,
            @JsonProperty("songs") ArrayList<Song> songs,
            @JsonProperty("links") LinkedHashMap<String, String> links, @JsonProperty("coverImage") String coverImage,
            @JsonProperty("artists") ArrayList<Artist> artists) {
        super(name, songs, links, coverImage);
        this.artists = new ArrayList<>();
        if (artists != null) {
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
        super.merge(album);
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
        song.setAlbumName(this.getName());
        // ensure every song in album has (default) coverimage
        if (song.getCoverImage() == null && this.getCoverImage() != null) {
            song.setCoverImage(this.getCoverImage());
        }
        this.addArtist(song.getArtist());
    }

    /**
     * add artists to album artists
     * 
     * @param artists - linkedhashset of artist to add
     */
    public void addArtists(ArrayList<Artist> artists) {
        if (artists == null) {
            logger.debug(this.toString() + ": null artists array provided in addArtists");
            return;
        }
        for (Artist artist : artists) {
            this.addArtist(artist);
        }
    }

    /**
     * add artist to album
     * 
     * @param artist - constructed artist to add
     */
    public void addArtist(Artist artist) {
        if (artist == null) {
            logger.debug(this.toString() + ": empty artist provided in addArtist");
            return;
        }
        // because of the playlist overwrite
        if (this.artists == null) {
            this.artists = new ArrayList<>();
        }
        Artist foundArtist = this.getArtist(artist);
        if (foundArtist != null) {
            foundArtist.merge(artist);
        } else {
            this.artists.add(artist);
        }
    }

    /**
     * get all album artists
     * 
     * @return - linkedhashset of artist
     */
    public ArrayList<Artist> getArtists() {
        return this.artists;
    }

    public Artist getArtist(Artist artist) {
        if (artist == null) {
            logger.debug(this.toString() + ": null artist provided in getArtist");
            return null;
        }
        for (Artist thisArtist : this.getArtists()) {
            if (thisArtist.equals(artist)) {
                return thisArtist;
            }
        }
        return null;
    }

    /**
     * get album main artist
     * 
     * @return - first artist in album
     */
    @JsonIgnore
    public Artist getMainArtist() {
        if (this.artists.isEmpty()) {
            return null;
        }
        return this.artists.get(0);
    }

    @Override
    @JsonIgnore
    public String toString() {
        String output = super.toString();
        if (this.artists != null && !this.artists.isEmpty()) {
            output += " (" + this.getMainArtist().toString().trim() + ")";
        }
        return output;
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
        for (String id : this.getIds().keySet()) {
            if (this.getId(id).equals(album.getId(id))) {
                return true;
            }
        }
        if (this.toString().equalsIgnoreCase(album.toString())) {
            return true;
        }
        if (this.getName().equalsIgnoreCase(album.getName())) {
            // album with matching name and atleast one artist
            for (Artist artist : this.getArtists()) {
                if (artist.equals(album.getArtist(artist))) {
                    return true;
                }
            }
            if (this.size() == album.size()) {
                return true;
            }
        }
        return false;
    }
}
