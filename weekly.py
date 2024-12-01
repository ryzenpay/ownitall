import spotipy
from spotipy.oauth2 import SpotifyOAuth
import os
from dotenv import load_dotenv

import util

def get_discover_weekly_tracks(sp):
    # Search for "Discover Weekly" playlist
    results = sp.search(q="Discover Weekly", type="playlist", limit=50)
    
    discover_weekly_id = None
    for playlist in results['playlists']['items']:
        if playlist['name'] == 'Discover Weekly' and playlist['owner']['id'] == 'spotify':
            discover_weekly_id = playlist['id']
            break
    
    if not discover_weekly_id:
        print("Discover Weekly playlist not found.")
        return []
    
    return util.get_all_playlist_tracks(sp, discover_weekly_id)

def main():
    load_dotenv()

    scope = "user-library-read playlist-read-private user-read-private"
    spotify_client = os.getenv("SPOTIFY_CLIENT")
    spotify_token = os.getenv("SPOTIFY_TOKEN")
    spotify_redirect = os.getenv("SPOTIFY_REDIRECT")

    sp = spotipy.Spotify(auth_manager=SpotifyOAuth(client_id=spotify_client, client_secret=spotify_token, redirect_uri=spotify_redirect, scope=scope))

    print("Fetching Discover Weekly playlist")
    discover_weekly_tracks = get_discover_weekly_tracks(sp)
    
    if discover_weekly_tracks:
        print(f"Found {len(discover_weekly_tracks)} tracks in Discover Weekly")
        util.save_to_file("./playlists", "Discover_Weekly/index.txt", discover_weekly_tracks)
        print("Discover Weekly playlist has been saved.")
    else:
        print("No tracks found in Discover Weekly playlist.")

if __name__ == "__main__":
    main()
