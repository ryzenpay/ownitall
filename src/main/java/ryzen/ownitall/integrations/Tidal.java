package ryzen.ownitall.integrations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.WebTarget;

import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.integrations.interfaces.Import;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.WebTools.AuthenticationException;
import ryzen.ownitall.util.WebTools.QueryException;
import ryzen.ownitall.util.OAuth;
import ryzen.superbeans.tidal.Albums;
import ryzen.superbeans.tidal.Artists;
import ryzen.superbeans.tidal.Commons;
import ryzen.superbeans.tidal.Playlists;
import ryzen.superbeans.tidal.Tracks;
import ryzen.superbeans.tidal.UserCollections;
import ryzen.superbeans.tidal.Users;

// https://developer.tidal.com/documentation
// https://developer.tidal.com/apiref
// doesnt support export as it depends on ids
public class Tidal implements Import {
    private static final Logger logger = new Logger(Tidal.class);
    private String token;
    private String userID;
    private Map<String, String> headers;
    private static final String baseUrl = "https://openapi.tidal.com/v2";
    private static final ArrayList<String> scope = new ArrayList<>(
            Arrays.asList("collection.read", "collection.write", "playlists.read", "playlists.write"));

    public Tidal() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Tidal.class)) {
            logger.debug("Empty tidal credentials");
            throw new MissingSettingException(Tidal.class);
        }
        this.token = OAuth.getOauthToken("https://auth.tidal.com/v1/oauth2/token",
                "https://login.tidal.com/authorize?response_type=code",
                Settings.tidalClientID, null, scope);
        this.headers = Map.of("Accept", "application/vnd.api+json", "Authorization", "Bearer " + this.token);
        this.userID = this.getUserID();
        logger.debug("Successfully authenticated into tidal as " + this.userID);
    }

    private String getUserID() throws AuthenticationException {
        Users.Users_Single_Resource_Data_Document response = this.query("/users/me", null, null,
                Users.Users_Single_Resource_Data_Document.class);
        if (response != null && response.data != null && response.data.id != null) {
            return response.data.id;
        }
        throw new AuthenticationException("no user id in users/me response");
    }

    private <T> T query(String path, String pageCursor, ArrayList<String> include, Class<T> responseType) {
        try {
            WebTarget target = WebTools.target(baseUrl + path)
                    .queryParam("countryCode", "US");
            if (pageCursor != null) {
                target = target.queryParam("page[cursor]", pageCursor);
            }
            if (include != null) {
                for (String includeEntry : include) {
                    target = target.queryParam("include", includeEntry);
                }
            }
            return WebTools.query(target, "get", this.headers, null, responseType);
        } catch (QueryException e) {
            logger.warn("Exception while querying tidal: " + e);
            return null;
        }
    }

    private String nextCursor(Commons.Links links) {
        if (links == null || links.next == null || links.meta == null) {
            return null;
        }
        return links.meta.nextCursor;
    }

    private ArrayList<Artist> getArtists(List<Artists.Artists_Resource_Object> artistNodes) {
        ArrayList<Artist> artists = new ArrayList<>();
        if (artistNodes == null) {
            return artists;
        }
        for (Artists.Artists_Resource_Object artistNode : artistNodes) {
            if (artistNode == null || artistNode.attributes == null) {
                continue;
            }
            Artist artist = new Artist(artistNode.attributes.name);
            artist.addId("tidal", artistNode.id);
            artists.add(artist);
        }
        return artists;
    }

    private Song getSong(Tracks.Tracks_Resource_Object songItem) {
        if (songItem == null || songItem.attributes == null) {
            logger.debug("null songItem provided in getSong");
            return null;
        }
        String id = songItem.id;
        String title = songItem.attributes.title;
        Song song = new Song(title);
        song.addId("tidal", id);
        if (songItem.attributes.duration != null) {
            song.setDuration(Duration.parse(songItem.attributes.duration));
        }
        Tracks.Tracks_Artists_Multi_Relationship_Data_Document artistsResponse = this.query(
                "/tracks/" + id + "/relationships/artists", null,
                new ArrayList<>(Arrays.asList("artists")), Tracks.Tracks_Artists_Multi_Relationship_Data_Document.class);
        if (artistsResponse != null) {
            song.addArtists(this.getArtists(artistsResponse.included));
        }
        return song;
    }

    /**
     * songs out of an items sideload: the union members arrive as maps and are
     * rebound per member, skipping the videos this importer does not model
     */
    private ArrayList<Song> songsFromItems(List<Object> included) {
        ArrayList<Song> songs = new ArrayList<>();
        if (included == null) {
            return songs;
        }
        for (Object member : included) {
            Tracks.Tracks_Resource_Object item = WebTools.convert(member, Tracks.Tracks_Resource_Object.class);
            if (item == null || item.attributes == null || item.attributes.title == null) {
                continue;
            }
            Song song = this.getSong(item);
            if (song != null) {
                songs.add(song);
            }
        }
        return songs;
    }

    private ArrayList<Song> getAlbumItems(String path, int amount) {
        ArrayList<Song> songs = new ArrayList<>();
        String pageCursor = null;
        try (IPIterator<?> pb = IPIterator.manual(path, -1)) {
            while (true) {
                Albums.Albums_Items_Multi_Relationship_Data_Document response = this.query(path, pageCursor,
                        new ArrayList<>(Arrays.asList("items")),
                        Albums.Albums_Items_Multi_Relationship_Data_Document.class);
                if (response == null) {
                    logger.debug("null response received in getAlbumItems");
                    break;
                }
                for (Song song : this.songsFromItems(response.included)) {
                    pb.step(song.getName());
                    songs.add(song);
                }
                pageCursor = this.nextCursor(response.links);
                if (pageCursor == null) {
                    break;
                }
                // this is needed because tidal likes to loop playlists, YAY
                if (amount != -1 && songs.size() >= amount) {
                    logger.debug("songs capita reached");
                    break;
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting songs from " + path);
            return null;
        }
        return songs;
    }

    private ArrayList<Song> getPlaylistItems(String path, int amount) {
        ArrayList<Song> songs = new ArrayList<>();
        String pageCursor = null;
        try (IPIterator<?> pb = IPIterator.manual(path, -1)) {
            while (true) {
                Playlists.Playlists_Items_Multi_Relationship_Data_Document response = this.query(path, pageCursor,
                        new ArrayList<>(Arrays.asList("items")),
                        Playlists.Playlists_Items_Multi_Relationship_Data_Document.class);
                if (response == null) {
                    logger.debug("null response received in getPlaylistItems");
                    break;
                }
                for (Song song : this.songsFromItems(response.included)) {
                    pb.step(song.getName());
                    songs.add(song);
                }
                pageCursor = this.nextCursor(response.links);
                if (pageCursor == null) {
                    break;
                }
                // this is needed because tidal likes to loop playlists, YAY
                if (amount != -1 && songs.size() >= amount) {
                    logger.debug("songs capita reached");
                    break;
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting songs from " + path);
            return null;
        }
        return songs;
    }

    public LikedSongs getLikedSongs() {
        LikedSongs likedSongs = new LikedSongs();
        String pageCursor = null;
        try (IPIterator<?> pb = IPIterator.manual("Liked Songs", -1)) {
            while (true) {
                UserCollections.UserCollectionTracks_Items_Multi_Relationship_Data_Document response = this.query(
                        "/userCollections/" + this.userID + "/relationships/tracks", pageCursor,
                        new ArrayList<>(Arrays.asList("tracks")),
                        UserCollections.UserCollectionTracks_Items_Multi_Relationship_Data_Document.class);
                if (response == null) {
                    logger.warn("null response received in tidal likedsongs query");
                    break;
                }
                if (response.included != null) {
                    for (Tracks.Tracks_Resource_Object songItem : response.included) {
                        Song song = this.getSong(songItem);
                        if (song != null) {
                            pb.step(song.toString());
                            likedSongs.addSong(song);
                        }
                    }
                } else {
                    logger.debug("No songs in tidal likedsongs query");
                }
                pageCursor = this.nextCursor(response.links);
                if (pageCursor == null) {
                    break;
                }
            }
            return likedSongs;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing liked songs");
            return null;
        }
    }

    public Album getAlbum(String albumId, String albumName, String albumArtistName) {
        if (albumId == null) {
            logger.debug("null albumID provided in getAlbum");
            return null;
        }
        Albums.Albums_Single_Resource_Data_Document response = this.query("/albums/" + albumId, null, null,
                Albums.Albums_Single_Resource_Data_Document.class);
        if (response == null || response.data == null || response.data.attributes == null) {
            logger.debug("null response received in getAlbum");
            return null;
        }
        String id = response.data.id;
        Album album = new Album(response.data.attributes.title);
        album.addId("tidal", id);

        Albums.Albums_Artists_Multi_Relationship_Data_Document artistsResponse = this.query(
                "/albums/" + id + "/relationships/artists", null, new ArrayList<>(Arrays.asList("artists")),
                Albums.Albums_Artists_Multi_Relationship_Data_Document.class);
        if (artistsResponse != null) {
            album.addArtists(this.getArtists(artistsResponse.included));
        }
        Albums.Albums_CoverArt_Multi_Relationship_Data_Document coverResponse = this.query(
                "/albums/" + id + "/relationships/coverArt", null, new ArrayList<>(Arrays.asList("coverArt")),
                Albums.Albums_CoverArt_Multi_Relationship_Data_Document.class);
        if (coverResponse != null && coverResponse.included != null && !coverResponse.included.isEmpty()) {
            album.setCoverImage(this.artworkHref(coverResponse.included.get(0)));
        }
        ArrayList<Song> songs = this.getAlbumItems("/albums/" + id + "/relationships/items", -1);
        if (songs != null) {
            album.addSongs(songs);
        }
        return album;
    }

    public ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<>();
        String pageCursor = null;
        try (IPIterator<?> pb = IPIterator.manual("Albums", -1)) {
            while (true) {
                UserCollections.UserCollectionAlbums_Items_Multi_Relationship_Data_Document response = this.query(
                        "/userCollections/" + this.userID + "/relationships/albums", pageCursor,
                        new ArrayList<>(Arrays.asList("albums")),
                        UserCollections.UserCollectionAlbums_Items_Multi_Relationship_Data_Document.class);
                if (response == null) {
                    logger.debug("null response received in getAlbums");
                    break;
                }
                if (response.included != null) {
                    for (Albums.Albums_Resource_Object albumEntry : response.included) {
                        Album album = this.getAlbum(albumEntry.id, albumEntry.attributes.title, null);
                        if (album != null) {
                            pb.step(album.toString());
                            albums.add(album);
                        }
                    }
                } else {
                    logger.debug("No albums in tidal albums query");
                }
                pageCursor = this.nextCursor(response.links);
                if (pageCursor == null) {
                    break;
                }
            }
            return albums;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing albums");
            return null;
        }
    }

    private String artworkHref(ryzen.superbeans.tidal.Catalog.Artworks_Resource_Object artwork) {
        if (artwork == null || artwork.attributes == null || artwork.attributes.files == null
                || artwork.attributes.files.isEmpty() || artwork.attributes.files.get(0).href == null) {
            return null;
        }
        return artwork.attributes.files.get(0).href;
    }

    public Playlist getPlaylist(String playlistId, String playlistName) {
        if (playlistId == null) {
            logger.debug("null playlistId provided in getPlaylist");
            return null;
        }
        Playlists.Playlists_Single_Resource_Data_Document response = this.query("/playlists/" + playlistId, null, null,
                Playlists.Playlists_Single_Resource_Data_Document.class);
        if (response == null || response.data == null || response.data.attributes == null) {
            logger.debug("null response received in getPlaylist");
            return null;
        }
        String id = response.data.id;
        Playlist playlist = new Playlist(response.data.attributes.name);
        playlist.addId("tidal", id);

        Playlists.Playlists_CoverArt_Multi_Relationship_Data_Document coverResponse = this.query(
                "/playlists/" + id + "/relationships/coverArt", null, new ArrayList<>(Arrays.asList("coverArt")),
                Playlists.Playlists_CoverArt_Multi_Relationship_Data_Document.class);
        if (coverResponse != null && coverResponse.included != null && !coverResponse.included.isEmpty()) {
            playlist.setCoverImage(this.artworkHref(coverResponse.included.get(0)));
        }
        int size = response.data.attributes.numberOfItems != null ? response.data.attributes.numberOfItems : -1;
        ArrayList<Song> songs = this.getPlaylistItems("/playlists/" + id + "/relationships/items", size);
        if (songs != null) {
            playlist.addSongs(songs);
        }
        if (playlist.isEmpty()) {
            logger.debug("getPlaylist " + playlistId + " playlist is empty");
            return null;
        }
        return playlist;
    }

    public ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<>();
        String pageCursor = null;
        try (IPIterator<?> pb = IPIterator.manual("Playlists", -1)) {
            while (true) {
                UserCollections.UserCollectionPlaylists_Items_Multi_Relationship_Data_Document response = this.query(
                        "/userCollections/" + this.userID + "/relationships/playlists", pageCursor,
                        new ArrayList<>(Arrays.asList("playlists")),
                        UserCollections.UserCollectionPlaylists_Items_Multi_Relationship_Data_Document.class);
                if (response == null) {
                    logger.debug("null response received in getPlaylists");
                    break;
                }
                if (response.included != null) {
                    for (Object member : response.included) {
                        Playlists.Playlists_Resource_Object playlistEntry = WebTools.convert(member,
                                Playlists.Playlists_Resource_Object.class);
                        if (playlistEntry == null || playlistEntry.attributes == null) {
                            continue;
                        }
                        Playlist playlist = this.getPlaylist(playlistEntry.id, playlistEntry.attributes.name);
                        if (playlist != null) {
                            pb.step(playlist.toString());
                            playlists.add(playlist);
                        }
                    }
                } else {
                    logger.debug("No playlists in tidal playlists query");
                }
                pageCursor = this.nextCursor(response.links);
                if (pageCursor == null) {
                    break;
                }
            }
            return playlists;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing playlists");
            return null;
        }
    }
}
