import os

def save_to_file(directory, filename, songs):
    os.makedirs(directory, exist_ok=True)
    with open(os.path.join(directory, filename), "w", encoding="utf-8") as f:
        for song in songs:
            f.write(f"{song}\n")


def get_all_playlist_tracks(sp, playlist_id):
    tracks = []
    offset = 0
    while True:
        results = sp.playlist_tracks(playlist_id, offset=offset, limit=50)
        for item in results['items']:
            if item['track'] is not None:
                track = item['track']
                tracks.append(f"{track['name']} - {track['artists'][0]['name']}")
        if len(results['items']) < 50:
            break
        offset += 50
    return tracks