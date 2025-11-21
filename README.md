# Disclaimer:  
This project is in the works, some corners are unfinished.  
Usage of this program is at your own risk and can be against the TOS of certain services, use at your own caution.


# ownitall  
Own your music no matter the platform (whichever is supported)  
OwnItAll hopes to ease the movement of your music library between playforms, including local

# Current goals  
-[x] non-interactive mode (for automation)  
-[x] syncronization between libraries (put them all into ownitall and then have the same library elsewhere)  
-[ ]  Web GUI  

# documentation  
all there currently is, the automated javadoc documentation: https://ryzenpay.github.io/ownitall/  

## Flags
- -h : help command, and will display all current flags (this list might be out of date)  
- -i : non interactive mode which requires a string parameter in the format of ex: `1;1;4;1;0;3;2;C:\\Users\\ryzen\\Music;0;0`  
        - agreements are automatically accepted  
        - enter is automatically passed  
        - if windows filenames need to have `\\`, just `/` for linux
- -l : log level, supports: debug, info, off  
- -w : web gui (rather than default cli gui)  

## Library  
A library can be used to get the best metadata and coverart for your tracks, there are several options
### MusicBrainz
MusicBrainz is the most popular database for music tracks, but from personal experience has a lot of drawbacks
- low api limits (can take up to 4s per song)
- lot of "garbage" data which can really mess up ur library
    - example of this: songs have many different versions (instrumental, remix,..) which can be difficult to differentiate
### LastFM
- Great with popular songs, easy and precise searches
- doesnt have all data
will need:
    - api key (https://www.last.fm/api/authentication)


## YoutubeDL  
will need:
    - binary (https://github.com/yt-dlp/yt-dlp)
    - ffmpeg (https://www.ffmpeg.org/  )

## Spotify
will need (read below):  
    - client id
    - client secret
to get the client id and secret:  
    1. log in to spotify develper portal: https://developer.spotify.com/dashboard  
    2. create an app (with redirect url http://localhost:8081/oauth)
    3. go to dashboard -> click on your app  
    4. settings (top right)  
    5. save the client id and client secret  
  
When doing this, read every TOS as you are using them at the risk of your own spotify account  

## Youtube
will need (read below):  
    - application name  
    - API key  
    - oauth client id  
    - oauth client secret  
Youtube will provide you with all by doing the following:  (can also be done following step 1: https://developers.google.com/youtube/v3/quickstart/java)  
    1. log in and enable API services (if not already done)  
        - can be found on the front page: https://console.cloud.google.com/ -> API and services -> "Enable APIS and Services"  
    2. in the "library" tab after clicking on API, search for "YouTube Data API v3"  
    3. Click on it and click "enable"  
    4. click "oauth consent screen" (left side)  
    5. create new app with the following scopes enabled:  
        - https://www.googleapis.com/auth/youtube.readonly  
        - https://www.googleapis.com/auth/youtube.force-ssl  
    6. click "credentials" (left side)  
    7. click "create credentials" -> "APi key" and save this key  
    8. click "create credentials" -> "oauth client id"  
        - application type: desktop app  
    9. save the client id and secret  

When doing this, read every TOS as you are using them at the risk of your own youtube account  

## Local
the current format to importing your local music library is currently strict  
    - if you have a "liked songs" folder, update the liked songs folder name in settings to correspond 
    - if all songs in folder have the same "album" metadata, that folder becomes an album  
        - else its a playlist  
    - cover image of a playlist and an album need to be a file called "`<playlist/album name>`.<extension>" in the folder  

## Jellyfin
will need:  
    - jellyfin url  
    - jellyfin username  
    - jellyfin password  
To support multiple artists, set a custom delimiter:  
    - `dashboard -> libraries -> <select library> -> scroll to bottom -> custom delimiter`  
    - set it to `;`  

## SoulSeek
will need: 
    - binary (https://github.com/fiso64/slsk-batchdl)
    - soulseek username  
    - soulseek password  
Due to the soulseek binary and soulseek limits, it is hard encoded to 1 thread and can therefore take a long time to download an entire library  

## Tidal
will need (read below):
    - client id 
    - client secret
Tidal will provide you with the client id and secret, to get the client id and secret:  
    1. log in to tidal develper portal: https://developer.tidal.com/dashboard
    2. create an app
    3. go to dashboard -> click on your app  
    4. settings (top right)  
    5. add a redirect url to http://localhost:8081/oauth
        - add the scopes: collection.read%20collection.write%20playlists.read%20playlists.read
    6. save the client id and client secret  

## Apple Music
currently not supported as its paid (didnt know till halfway done)
will need:
    - client id
    - client secret

# Maven and compiling
## generate javadoc documentation
to update the javadoc documentation, run the command `mvn doc` in the root of the repository  
please fix any of the errors it throws as they show incomplete documentation  

## Compile new jar file
run the command `mvn clean compile assembly:single`  
look for the compiled `.jar` file in `target` directory  
run the compiled with `java -jar target/ownitall-<VERSION>-jar-with-dependencies.jar`

## JRE Requirements
current java version is 17, this can be installed from the official website: https://www.java.com/download/ie_manual.jsp  