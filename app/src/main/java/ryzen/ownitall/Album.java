package ryzen.ownitall;

import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.tools.Levenshtein;

import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Album {
    private String name;
    LinkedHashSet<Song> songs;
    private LinkedHashSet<Artist> artists; // the first being the main, Set because no duplicates
    private URI coverImage;

    /**
     * Default constructor of album without album cover
     * 
     * @param name - album name
     */
    public Album(String name) {
        this.name = name;
        this.songs = new LinkedHashSet<>();
        this.artists = new LinkedHashSet<>();
        this.coverImage = null;
    }

    /**
     * merge two albums together
     * 
     * @param album - constructed Album to merge
     */
    public void mergeAlbum(Album album) {
        this.addSongs(album.getSongs());
        this.addArtists(album.getArtists());
        if (this.coverImage == null) {
            this.coverImage = album.getCoverImage();
        }
    }

    /**
     * get the name of the current album class
     * 
     * @return - album name
     */
    public String getName() {
        return this.name;
    }

    /**
     * set album name
     * 
     * @param name - string album name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * set album cover image
     * 
     * @param coverImage - string url of album cover
     */
    public void setCoverImage(String coverImage) {
        try {
            this.coverImage = new URI(coverImage);
        } catch (URISyntaxException e) {
            System.err.println("Error parsing cover image: " + coverImage);
        }
    }

    /**
     * get the album cover image
     * 
     * @return - URI cover image
     */
    public URI getCoverImage() {
        if (this.coverImage == null) {
            return null;
        }
        return this.coverImage;
    }

    /**
     * add array of songs to album's songs
     * 
     * @param songs - arraylist of constructed Song
     */
    public void addSongs(ArrayList<Song> songs) {
        this.songs.addAll(new LinkedHashSet<Song>(songs));
    }

    /**
     * set album class artists
     * 
     * @param artists - LinkedHashSet of artists
     */
    public void addArtists(ArrayList<Artist> artists) {
        this.artists.addAll(new LinkedHashSet<Artist>(artists));
    }

    /**
     * add artist to albums artists
     * 
     * @param artist - artist object/string ;)
     */
    public void addArtist(Artist artist) {
        this.artists.add(artist);
    }

    /**
     * get all artists on the album
     * 
     * @return - arraylist of artists
     */
    public ArrayList<Artist> getArtists() {
        return new ArrayList<Artist>(this.artists);
    }

    /**
     * get album songs
     * 
     * @return - arraylist of constructed Song
     */
    public ArrayList<Song> getSongs() {
        return new ArrayList<>(this.songs);
    }

    /**
     * gets albums main artist (first in array = first added)
     * 
     * @return - constructucted Artist
     */
    @JsonIgnore
    public Artist getMainArtist() {
        if (this.artists.isEmpty()) {
            return null;
        }
        return artists.iterator().next();
    }

    /**
     * get amount of songs in album
     * 
     * @return - int of album size
     */
    public int size() {
        return this.songs.size();
    }

    @Override
    public String toString() {
        String output = this.name;
        if (!this.artists.isEmpty()) {
            output += " | " + this.getMainArtist();
        }
        return output;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Album album = (Album) object;
        if (this.hashCode() == album.hashCode()) {
            return true;
        }
        if (Levenshtein.computeSimilarity(this.toString(), album.toString()) > 90) { // TODO: handle support if no
                                                                                     // artist
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = this.name.hashCode();
        if (!this.artists.isEmpty()) {
            hashCode = this.getMainArtist().hashCode();
        }
        return hashCode;
    }

    @JsonCreator
    public Album(@JsonProperty("name") String name,
            @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("artists") LinkedHashSet<Artist> artists,
            @JsonProperty("coverImage") String coverImage) {
        this.name = name;
        if (songs != null && !songs.isEmpty()) {
            this.songs = songs;
        } else {
            this.songs = new LinkedHashSet<>();
        }
        if (artists != null && !artists.isEmpty()) {
            this.artists = artists;
        } else {
            this.artists = new LinkedHashSet<>();
        }
        if (coverImage != null) {
            this.setCoverImage(coverImage);
        }
    }
}
