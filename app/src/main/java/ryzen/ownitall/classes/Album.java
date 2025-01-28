package ryzen.ownitall.classes;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.util.Levenshtein;
import ryzen.ownitall.util.MusicTools;

public class Album extends Playlist {
    LinkedHashSet<Artist> artists;
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
            @JsonProperty("links") LinkedHashMap<String, String> links,
            @JsonProperty("youtubePageToken") String youtubePageToken,
            @JsonProperty("spotifyPageOffset") int spotifyPageOffset, @JsonProperty("coverImage") String coverImage,
            @JsonProperty("artists") LinkedHashSet<Artist> artists) {
        super(name, songs, youtubePageToken, spotifyPageOffset, coverImage);
        if (artists != null && !artists.isEmpty()) {
            this.artists = new LinkedHashSet<>(artists);
        } else {
            this.artists = new LinkedHashSet<>();
        }
        if (links != null && !links.isEmpty()) {
            this.links = new LinkedHashMap<>(links);
        } else {
            this.links = new LinkedHashMap<>();
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

    public void addLink(String key, String url) {
        this.links.put(key, url);
    }

    public void addLinks(LinkedHashMap<String, String> links) {
        this.links.putAll(links);
    }

    @JsonIgnore
    public String getLink(String key) {
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
        // m3u album cover
        output.append("#EXTIMG:").append("cover.jpg").append("\n");
        // m3u album contents
        for (Song song : this.getSongs()) {
            File file = new File(MusicTools.sanitizeFileName(song.getName()) + "." + downloadFormat);
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
        if (this.hashCode() == album.hashCode()) {
            return true;
        }
        return Levenshtein.computeSimilarityCheck(this.toString(), album.toString(),
                simularityPercentage);
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return Objects.hash(super.hashCode(), artists);
    }
}
