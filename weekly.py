import os
from dotenv import load_dotenv
import spotipy
from spotipy.oauth2 import SpotifyOAuth

import util

def main():
    
    load_dotenv()

    scope = "user-library-read playlist-read-private"
    spotify_client = os.getenv("SPOTIFY_CLIENT")
    spotify_token = os.getenv("SPOTIFY_TOKEN")
    spotify_redirect = os.getenv("SPOTIFY_REDIRECT")
    spotify_radar = os.getenv("SPOTIFY_RADAR")

    sp = spotipy.Spotify(auth_manager=SpotifyOAuth(client_id=spotify_client, client_secret=spotify_token, redirect_uri=spotify_redirect, scope=scope))

    playlist_urls = spotify_radar.split(',')

    for playlist_url in playlist_urls:
        playlist_url = playlist_url.strip()
        if playlist_url.startswith("https://open.spotify.com/playlist/"):
            print(f"Fetching tracks from playlist: {playlist_url}")
            playlist_id = playlist_url.split('/')[-1].split('?')[0]
            tracks = util.get_all_playlist_tracks(sp, playlist_id)
            playlist_id = playlist_url.split('/')[-1].split('?')[0]
            util.save_to_file("./radar", f"{playlist_id}/index.txt", tracks)
            print(f"Saved {len(tracks)} tracks from playlist {playlist_id}")

    print("All specified playlists have been processed and saved.")

if __name__ == "__main__":
    main()
