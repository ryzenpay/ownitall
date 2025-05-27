package ryzen.ownitall.method.download;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.exceptions.MissingSettingException;
import ryzen.ownitall.util.exceptions.AuthenticationException;

/**
 * <p>
 * YT_dl class.
 * </p>
 *
 * @author ryzen
 */
@Method.Export
public class YT_dl extends Download {
    private static final Logger logger = new Logger(YT_dl.class);

    /**
     * default download constructor
     * setting all settings / credentials
     *
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     */
    public YT_dl() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(YT_dl.class)) {
            logger.debug("Empty YT_dl credentials found");
            throw new MissingSettingException(YT_dl.class);
        }
        if (!Settings.yt_dlFile.exists()) {
            throw new AuthenticationException("YT_DL missing yt_dl binary");
        }
    }

    /**
     * {@inheritDoc}
     *
     * download a specified song
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
        if (!song.getDuration().isZero()) {
            command.add("--match-filters");
            long upperBound = song.getDuration().getSeconds() + 10L;
            long lowerBound = song.getDuration().getSeconds() - 10L;
            command.add("duration<=" + upperBound + "&duration>=" + lowerBound);
        } else {
            command.add("--break-match-filter");
            command.add("duration>=45"); // exclude shorts
        }
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
        command.add(Collection.getSongFileName(song));
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
            if (song.getAlbumName() != null) {
                searchQuery += "(" + song.getAlbumName() + ")";
            }
            // prevent any search impacting triggers + pipeline starters
            searchQuery = searchQuery.replaceAll("[\\\\/<>|:]", "");
        }
        command.add(searchQuery);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            int retries = 0;
            File songFile = new File(path, Collection.getSongFileName(song));
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
                // https://github.com/yt-dlp/yt-dlp/issues/4262
                if (exitCode != 0) {
                    logger.warn("Attempt: " + retries);
                    if (exitCode == 2) {
                        logger.warn("Error with user provided options: " + command.toString());
                        break;
                    } else if (exitCode == 100) {
                        logger.warn("Your yt-dlp needs to update");
                        break;
                    } else if (exitCode == 101) {
                        logger.warn("Download cancelled due to boundary criteria: '" + searchQuery + "'");
                        break;
                    } else {
                        logger.warn("Unkown error while downloading song: '" + song + "' with code: " + exitCode
                                + "\n Command: " + command.toString() + "\n Complete log: \n" + completeLog.toString());
                    }
                }
                retries++;
            }
            if (songFile.exists()) {
                writeMetaData(song, songFile);
            } else {
                logger.warn("song '" + song.toString() + "' failed to download, check logs");
            }
        } catch (IOException e) {
            logger.error("Exception preparing yt-dlp: ", e);
        } catch (InterruptedException e) {
            logger.debug("Interruption caught while downloading yt-dlp song");
            InterruptionHandler.forceInterruption();
        }
    }
}
