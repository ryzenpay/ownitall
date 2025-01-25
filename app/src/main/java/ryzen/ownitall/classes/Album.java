package ryzen.ownitall.classes;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.util.Levenshtein;

public class Album extends Playlist {
    private String id;
    LinkedHashSet<Artist> artists;

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
     */
    @JsonCreator
    public Album(@JsonProperty("name") String name, @JsonProperty("id") String id,
            @JsonProperty("songs") LinkedHashSet<Song> songs,
            @JsonProperty("links") LinkedHashMap<String, String> links,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverImage") String coverImage,
            @JsonProperty("artists") LinkedHashSet<Artist> artists) {
        super(name, songs, links, youtubePageToken, spotifyPageOffset, coverImage);
        if (id != null) {
            this.id = id;
        }
        if (artists != null && !artists.isEmpty()) {
            this.artists = new LinkedHashSet<>(artists);
        } else {
            this.artists = new LinkedHashSet<>();
        }
    }

    public void merge(Album album) {
        if (album == null || album.isEmpty()) {
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
        if (this.artists == null && album.getArtists() != null) {
            this.addArtists(album.getArtists());
        }
        this.addLinks(album.getLinks());
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void addSong(Song song) {
        super.addSong(song);
        if (!this.artists.contains(song.getArtist())) {
            this.artists.add(song.getArtist());
        }
    }

    public void addArtists(LinkedHashSet<Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            return;
        }
        this.artists.addAll(artists);
    }

    public void addArtist(Artist artist) {
        if (artist == null || artist.isEmpty()) {
            return;
        }
        this.artists.add(artist);
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
    public String getM3U() {
        // m3u header
        StringBuilder output = new StringBuilder();
        output.append("#EXTM3U").append("\n");
        // m3u album information
        output.append("#EXTALB:").append(this.toString()).append("\n");
        output.append("#EXTART:").append(this.getMainArtist()).append("\n");
        // m3u album contents
        for (Song song : this.getSongs()) {
            File file = new File(song.getFileName() + "." + downloadFormat);
            output.append("#EXTINF:").append(String.valueOf(song.getDuration().toSeconds())).append(",")
                    .append(song.toString()).append("\n");
            output.append(file.getPath()).append("\n");
        }
        output.append("#EXTIMG:").append("cover.jpg");
        return output.toString();
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
