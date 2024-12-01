import spotipy
from spotipy.oauth2 import SpotifyOAuth
import os
from dotenv import load_dotenv

def get_discover_weekly_tracks(sp):
    discover_weekly_id = None
    playlists = sp.current_user_playlists()
    for playlist in playlists['items']:
        if playlist['name'] == 'Discover Weekly':
            discover_weekly_id = playlist['id']
            break
    
    if not discover_weekly_id:
        print("Discover Weekly playlist not found.")
        return []
    
    tracks = []
    results = sp.playlist_tracks(discover_weekly_id)
    for item in results['items']:
        if item['track'] is not None:
            track = item['track']
            tracks.append(f"{track['name']} - {track['artists'][0]['name']}")
    return tracks

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

    print("Fetching Discover Weekly playlist")
    discover_weekly_tracks = get_discover_weekly_tracks(sp)
    
    if discover_weekly_tracks:
        print(f"Found {len(discover_weekly_tracks)} tracks in Discover Weekly")
        save_to_file("./playlists", "Discover_Weekly/index.txt", discover_weekly_tracks)
        print("Discover Weekly playlist has been saved.")
    else:
        print("No tracks found in Discover Weekly playlist.")

if __name__ == "__main__":
    main()
