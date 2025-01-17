package ryzen.ownitall;

import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.tools.Levenshtein;

public class Album extends Playlist {
    ArtistSet artists;

    /**
     * Default constructor of album without album cover
     * 
     * @param name - album name
     */
    public Album(String name) {
        super(name);
        this.artists = new ArtistSet();
    }

    /**
     * full album constructor
     * 
     * @param name    - album name
     * @param songs   - SongSet of songs
     * @param artists - constructed Artist
     */
    @JsonCreator
    public Album(@JsonProperty("name") String name, @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverArt") String coverArt,
            @JsonProperty("artists") LinkedHashSet<Artist> artists) {
        super(name, songs, youtubePageToken, spotifyPageOffset, coverArt);
        if (this.artists != null && !artists.isEmpty()) {
            this.artists = new ArtistSet(artists);
        } else {
            this.artists = new ArtistSet();
        }
    }

    public void merge(Album album) {
        if (album == null) {
            return;
        }
        this.addSongs(album.getSongs());
        if (this.getCoverArt() == null && album.getCoverArt() != null) {
            this.setCoverArt(album.getCoverArt());
        }
        if (album.getYoutubePageToken() != null) {
            this.setYoutubePageToken(album.getYoutubePageToken());
        }
        if (album.getSpotifyPageOffset() > this.getSpotifyPageOffset()) {
            this.setSpotifyPageOffset(album.getSpotifyPageOffset());
        }
        if (this.artists == null && album.getArtists() != null) {
            this.addArtists(album.getArtists());
        }
    }

    public void addArtists(LinkedHashSet<Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            return;
        }
        this.artists.addAll(artists);
    }

    public void addArtist(Artist artist) {
        if (artist == null || artists.isEmpty()) {
            return;
        }
        this.artists.add(artist);
    }

    public LinkedHashSet<Artist> getArtists() {
        return new ArtistSet(this.artists);
    }

    @JsonIgnore
    public Artist getMainArtist() {
        return this.artists.get(0);
    }

    @Override
    @JsonIgnore
    public String toString() {
        String output = super.toString();
        if (this.getMainArtist() != null) {
            output += " (" + this.getMainArtist() + ")";
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
        int hashCode = super.hashCode();
        if (this.artists != null && !this.artists.isEmpty()) {
            hashCode += this.artists.hashCode();
        }
        return hashCode;
    }
}
