# Disclaimer:  
This is still in the works and no where near close to finished.  
Use of this program is at your own risk and can be against the TOS of certain platforms, use at your own caution.  

# ownitall  
own your music library no matter the platform (whichever is currently supported)  
OwnItAll hopes to ease the movement of your music library between playforms including local

# Current goals  
- non-interactive mode (for automation)  
- syncronization between libraries (put them all into ownitall and then have the same library everywhere)  
-  JFrame GUI  

# documentation  
all there currently is, is the automated javadoc documentation: https://ryzenpay.github.io/ownitall/  

## YoutubeDL  
download yt-dl.exe from the official repository: https://github.com/yt-dlp/yt-dlp 
save the absolute executable path and provide it when needed  
### requirements  
- ffmpeg: https://www.ffmpeg.org/   

## Library (LastFM)  
to get the best data possible, everything is checked with lastFM  
to get a last FM api key follow their documentation: https://www.last.fm/api/authentication  

## Spotify
when attempting to import or export to spotify, you will need to provide the 3 following things:  
    - client id  
    - client secret  
    - redirect url (for easier setup, use mine: https://ryzen.rip/ownitall)   
spotify will provide you with the client id and secret, and the redirect url will be your or the website you decide to use  
to get the clienit id and secret:  
    1. log in to spotify develper portal: https://developer.spotify.com/dashboard  
    2. create an app (setting the redirect url to your desired)  
    3. go to dashboard -> click on your app  
    4. settings (top right)  
    5. save the client id and client secret  
  
When doing this, read every TOS as you are using them at the risk of your own spotify account  

## Youtube
when attempting to import or export to youtube, you will need the following things:  
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
    - if you have a "liked songs" folder, name it "liked songs"  
    - if all songs in folder have the same "album" metadata, that folder becomes an album  
        - default is playlist  
    - cover image of an album needs to be a file called "<ALBUM NAME>.png" in the folder  
    - "loose" songs in the music library folder (root of folder) are added to "liked songs"  


# Gradle and compiling
## generate javadoc documentation
to update the javadoc documentation, run the command `./gradlew javadoc` in the root of the repository  
please fix any of the errors it throws as they show incomplete documentation  

## Compile new jar file
to generate a new compiled jar file from source, run the command `./gradlew compile` in the root of the repository  
a jar file named `ownitall.jar` will be made and can be ran with the following:  `java -jar .\ownitall.jar`  

## JRE Requirements
current java version is 9, this can be installed from the official website: https://www.java.com/download/ie_manual.jsp  

## Useful commands for debugging  
start in info mode: `java -jar .\ownitall.jar -Dlog4j.configurationFile=app\src\main\resources\log_info.xml`  
start in debug mode: `java -jar .\ownitall.jar -Dlog4j.configurationFile=app\src\main\resources\debug_info.xml`