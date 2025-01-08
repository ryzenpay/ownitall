# Disclaimer:
big disclaimer, it is still in works and no where near close to finished
another note i want to add before i forget: use of this program is at your own risk and can be agains the TOS of certain platforms, use at your own caution

# ownitall 
have full control over your music no matter the playform (whichever is currently supported),  
OwnItAll hopes to ease the movement of your music library between playforms including local in supported formats


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

When doing this, read every TOS as you are using them at the risk of your own spotify account  
## Local
the current format to importing your local music library is currently strict  
    - if you have a "liked songs" folder, name it "liked songs"  
    - if all songs in folder have the same "album" metadata, that folder becomes an album  
        - default is playlist
    - cover image of an album needs to be a file called "cover.jpg" in the folder  
    - "loose" songs in the music library folder (root of folder) are added to "liked songs"


# Gradle and compiling  
## generate javadoc documentation  
to update the javadoc documentation, run the command `./gradlew javadoc` in the root of the repository  
please fix any of the errors it throws as they show incomplete documentation  

## Compile new jar file  
to generate a new compiled jar file from source, run the command `./gradlew compile` in the root of the repository  
a jar file named `ownitall.jar` will be made and can be ran with the following:  `java -jar .\ownitall.jar`  