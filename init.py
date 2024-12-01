import spotipy
from spotipy.oauth2 import SpotifyOAuth
import os
from dotenv import load_dotenv

import util

def get_all_liked_songs(sp):
    liked_songs = []
    offset = 0
    limit = 50
    while True:
        results = sp.current_user_saved_tracks(limit=limit, offset=offset)
        for item in results['items']:
            track = item['track']
            liked_songs.append(f"{track['name']} - {track['artists'][0]['name']}")
        if len(results['items']) < limit:
            break
        offset += limit
        print(f"    Fetched {offset} liked songs so far...")
    return liked_songs

def get_all_saved_albums(sp):
    albums_dict = {}
    offset = 0
    limit = 50
    while True:
        results = sp.current_user_saved_albums(limit=limit, offset=offset)
        for item in results['items']:
            album = item['album']
            album_name = album['name'].replace(' ', '_')
            albums_dict[album_name] = [f"{track['name']} - {album['artists'][0]['name']}" for track in album['tracks']['items']]
        if len(results['items']) < limit:
            break
        offset += limit
        print(f"    Fetched {offset} albums so far...")
    return albums_dict

def save_to_file(directory, filename, songs):
    os.makedirs(directory, exist_ok=True)
    with open(os.path.join(directory, filename), "w", encoding="utf-8") as f:
        for song in songs:
            f.write(f"{song}\n")

def main():
    load_dotenv()

    scope = "user-library-read playlist-read-private"
    spotify_client = os.getenv("SPOTIFY_CLIENT")
    spotify_token = os.getenv("SPOTIFY_TOKEN")
    spotify_redirect = os.getenv("SPOTIFY_REDIRECT")

    sp = spotipy.Spotify(auth_manager=SpotifyOAuth(client_id=spotify_client, client_secret=spotify_token, redirect_uri=spotify_redirect, scope=scope))

    playlists_dict = {}
    
    print("Fetching all usermade playlists")
    offset = 0
    limit = 50
    while True:
        playlists = sp.current_user_playlists(limit=limit, offset=offset)
        for playlist in playlists['items']:
            if playlist is not None:
                playlist_name = playlist['name'].replace(' ', '_')
                playlists_dict[playlist_name] = util.get_discover_weekly_tracks(sp, playlist['id'])
                print(f"    Fetched {len(playlists_dict[playlist_name])} tracks from playlist: {playlist_name}")
        if len(playlists['items']) < limit:
            break
        offset += limit

    print("Fetching all liked songs")
    playlists_dict["Liked_Songs"] = get_all_liked_songs(sp)
    print(f"Total liked songs fetched: {len(playlists_dict['Liked_Songs'])}")

    print("Fetching all liked albums")
    albums_dict = get_all_saved_albums(sp)
    print(f"Total albums fetched: {len(albums_dict)}")

    print("Saving index of all playlists")
    for playlist_name, songs in playlists_dict.items():
        save_to_file("./playlists", f"{playlist_name}/index.txt", songs)

    print("Saving index of all albums")
    for album_name, songs in albums_dict.items():
        save_to_file("./albums", f"{album_name}/index.txt", songs)

if __name__ == "__main__":
    main()
