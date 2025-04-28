package ryzen.ownitall.method.download;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.method.Method;

public class YT_dl extends Download {
    private static final Logger logger = LogManager.getLogger();

    /**
     * default download constructor
     * setting all settings / credentials
     * 
     * @throws InterruptedException - when user interrupts
     */
    public YT_dl() throws InterruptedException {
        if (Method.isCredentialsEmpty(YT_dl.class)) {
            throw new InterruptedException("empty YT_dl credentials");
        }
    }

    /**
     * download a specified song
     * 
     * @param song - constructed song
     * @param path - folder of where to place
     */
    @Override
    public void downloadSong(Song song, File path) {
        if (song == null || path == null) {
            logger.debug("null song or Path provided in downloadSong");
            return;
        }
        ArrayList<String> command = new ArrayList<>();
        // executables
        command.add(Settings.yt_dlFile.getAbsolutePath());
        command.add("--ffmpeg-location");
        command.add(Settings.ffmpegFile.getAbsolutePath());
        // command.add("--concurrent-fragments");
        // command.add(String.valueOf(settings.getDownloadThreads()));
        // set up youtube searching and only 1 result
        command.add("--default-search");
        command.add("ytsearch1");
        // exclude any found playlists or shorts
        command.add("--no-playlist"); // Prevent downloading playlists
        command.add("--break-match-filter");
        command.add("duration>=45"); // exclude shorts
        // metadata and formatting
        command.add("--extract-audio");
        // command.add("--embed-thumbnail");
        command.add("--format");
        command.add("bestaudio/best");
        command.add("--audio-format");
        command.add(Settings.downloadFormat);
        command.add("--audio-quality");
        command.add(Integer.toString(Settings.yt_dlQuality));
        // command.add("--embed-metadata"); // metadata we have overwrites this
        // command.add("--no-write-comments");
        // download location
        command.add("--paths");
        command.add(path.getAbsolutePath());
        command.add("--output");
        command.add(song.getFileName());
        /**
         * search for video using the query / use url
         * ^ keep this at the end, incase of fucked up syntax making the other flags
         * drop
         */
        String searchQuery;
        if (song.getId("youtube") != null) {
            searchQuery = "https://youtube.com/watch?v=" + song.getId("youtube");
        } else {
            // search query filters
            searchQuery = song.toString() + " (official audio)"; // youtube search criteria
            // prevent any search impacting triggers + pipeline starters
            searchQuery = searchQuery.replaceAll("[\\\\/<>|:]", "");
        }
        command.add(searchQuery);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            int retries = 0;
            File songFile = new File(path, song.getFileName());
            StringBuilder completeLog = new StringBuilder();
            while (!songFile.exists() && retries < 3) {
                if (retries == 1) {
                    // cookies for age restriction (do not default to them)
                    if (Settings.yt_dlCookieFile != null && Settings.yt_dlCookieFile.exists()) {
                        command.add(1, "--cookies");
                        command.add(2, Settings.yt_dlCookieFile.getAbsolutePath());
                    } else if (!Settings.yt_dlCookieBrowser.isEmpty()) {
                        command.add(1, "--cookies-from-browser");
                        command.add(2, Settings.yt_dlCookieBrowser);
                    }
                }
                Process process = processBuilder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    // Capture output for logging
                    while ((line = reader.readLine()) != null) {
                        completeLog.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    if (exitCode == 2) {
                        logger.debug("Error with user provided options: " + command.toString());
                        break;
                    } else if (exitCode == 100) {
                        logger.error("Your yt-dlp needs to update");
                        break;
                    } else if (exitCode == 101) {
                        logger.debug("Download cancelled due to boundary criteria: '" + searchQuery + "'");
                        break;
                    } else {
                        logger.error("Unkown error while downloading song: '" + song + "' with code: " + exitCode);
                        logger.error(command.toString());
                        logger.error(completeLog.toString());
                    }
                    logger.error("Attempt: " + retries);
                }
                retries++;
            }
            if (songFile.exists()) {
                writeMetaData(song, songFile);
            } else {
                logger.warn("song '" + song.toString() + "' failed to download, check logs");
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Exception preparing yt-dlp: ", e);
        }
    }
}
