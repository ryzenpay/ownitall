package ryzen.ownitall;

import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.tools.Levenshtein;

public class Album { // extend from playlists?
    private String name;
    SongSet songs;
    Artist artist;
    private double simularityPercentage;

    /**
     * Default constructor of album without album cover
     * 
     * @param name - album name
     */
    public Album(String name) {
        this.name = name;
        this.songs = new SongSet();
        this.artist = null;
        this.simularityPercentage = Settings.load().getSimilarityPercentage();
    }

    /**
     * full album constructor
     * 
     * @param name    - album name
     * @param songs   - SongSet of songs
     * @param artists - constructed Artist
     */
    @JsonCreator
    public Album(@JsonProperty("name") String name,
            @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("artist") Artist artist) {
        this.name = name;
        if (songs != null && !songs.isEmpty()) {
            this.songs = new SongSet(songs);
        } else {
            this.songs = new SongSet();
        }
        if (artist != null && !artist.isEmpty()) {
            this.artist = artist;
        } else {
            artist = null;
        }
        this.simularityPercentage = Settings.load().getSimilarityPercentage();
    }

    /**
     * merge two albums together
     * 
     * @param album - constructed Album to merge
     */
    public void mergeAlbum(Album album) {
        if (album == null) {
            return;
        }
        this.addSongs(album.getSongs());
        if (this.artist == null && album.getArtist() != null) {
            this.setArtist(album.getArtist());
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
     * add songs to album
     * 
     * @param songs - arraylist of constructed Song
     */
    public void addSongs(LinkedHashSet<Song> songs) {
        for (Song song : songs) {
            this.addSong(song);
        }
    }

    /**
     * add song to album with checking for merging
     * 
     * @param song - constructed Song
     */
    public void addSong(Song song) {
        if (this.songs.contains(song)) {
            this.songs.get(song).mergeSong(song);
        } else {
            this.songs.add(song);
        }
    }

    public void setArtist(Artist artist) {
        if (artist == null || artist.isEmpty()) {
            return;
        }
        this.artist = artist;
    }

    public Artist getArtist() {
        return this.artist;
    }

    /**
     * get album songs
     * 
     * @return - arraylist of constructed Song
     */
    public LinkedHashSet<Song> getSongs() {
        return new SongSet(this.songs);
    }

    /**
     * get amount of songs in album
     * 
     * @return - int of album size
     */
    @JsonIgnore
    public int size() {
        return this.songs.size();
    }

    @Override
    @JsonIgnore
    public String toString() {
        String output = this.name;
        if (this.artist != null && !this.artist.isEmpty()) {
            output += " (" + this.artist.getName() + ")";
        }
        return output;
    }

    @Override
    @JsonIgnore
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Album album = (Album) object;
        if (this.hashCode() == album.hashCode()) {
            return true;
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
        int hashCode = this.name.toLowerCase().hashCode();
        if (this.artist != null && !this.artist.isEmpty()) {
            hashCode = this.artist.hashCode();
        }
        return hashCode;
    }
}
