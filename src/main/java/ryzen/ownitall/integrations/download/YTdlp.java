package ryzen.ownitall.integrations.download;

import java.io.File;
import java.util.ArrayList;

import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.integrations.Youtube;
import ryzen.ownitall.integrations.interfaces.Export;
import ryzen.ownitall.integrations.interfaces.Sync;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools.AuthenticationException;

/**
 * <p>
 * YTdlp class.
 * </p>
 *
 * @author ryzen
 */
public class YTdlp extends Download implements Sync, Export {
    private static final Logger logger = new Logger(YTdlp.class);
    private Youtube youtube;

    /**
     * default download constructor
     * setting all settings / credentials
     *
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public YTdlp() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(YTdlp.class)) {
            logger.debug("Empty YTdlp credentials found");
            throw new MissingSettingException(YTdlp.class);
        }
        if (!Settings.ytdlpFile.exists()) {
            throw new AuthenticationException("YTdlp missing yt_dlp binary");
        }
        if (Settings.ytdlpUseYoutube) {
            this.youtube = new Youtube();
        }
    }

    public ArrayList<String> createCommand(Song song, File downloadFile) throws InterruptedException {
        if (song == null || downloadFile == null) {
            logger.debug("null song or downloadFile provided in downloadSong");
            return null;
        }
        ArrayList<String> command = new ArrayList<>();
        // executables
        command.add(Settings.ytdlpFile.getAbsolutePath());
        command.add("--ffmpeg-location");
        command.add(Settings.ffmpegFile.getAbsolutePath());
        // command.add("--concurrent-fragments");
        // command.add(String.valueOf(settings.getDownloadThreads()));
        // set up youtube searching and only 1 result
        command.add("--default-search");
        command.add("ytsearch1");
        // exclude any found playlists or shorts
        command.add("--no-playlist"); // Prevent downloading playlists
        // anything here will break if it is false
        command.add("--break-match-filter");
        command.add("duration>45&duration<600"); // exclude shorts and videos +30 min
        // metadata and formatting
        command.add("--extract-audio");
        // command.add("--embed-thumbnail");
        command.add("--format");
        command.add("bestaudio/best");
        command.add("--audio-format");
        command.add(Settings.downloadFormat);
        command.add("--audio-quality");
        command.add(Integer.toString(Settings.ytdlpQuality));
        // command.add("--embed-metadata"); // metadata we have overwrites this
        // command.add("--no-write-comments");
        // download location
        command.add("--paths");
        command.add(downloadFile.getParent());
        if (Settings.ytdlpCookieFile != null && Settings.ytdlpCookieFile.exists()) {
            command.add(1, "--cookies");
            command.add(2, Settings.ytdlpCookieFile.getAbsolutePath());
        } else if (!Settings.ytdlpCookieBrowser.isEmpty()) {
            command.add(1, "--cookies-from-browser");
            command.add(2, Settings.ytdlpCookieBrowser);
        }
        command.add("--output");
        command.add("\"" + downloadFile.getName() + "\""); // https://github.com/ytdl-org/youtube-dl/issues/23115
        /**
         * search for video using the query / use url
         * ^ keep this at the end, incase of fucked up syntax making the other flags
         * drop
         */
        String searchQuery = null;
        if (song.getId("youtube") != null) {
            searchQuery = "https://youtube.com/watch?v=" + song.getId("youtube");
        } else if (this.youtube != null) {
            String id = this.youtube.getSongId(song);
            if (id != null) {
                searchQuery = "https://youtube.com/watch?v=" + id;
                song.addId("youtube", id);
            }
        }
        if (searchQuery == null) {
            // search query filters
            searchQuery = song.toString() + " (official audio)"; // youtube search criteria
            if (song.getAlbumName() != null) {
                searchQuery += "(" + song.getAlbumName() + ")";
            }
            // prevent any search impacting triggers + pipeline starters
            searchQuery = searchQuery.replaceAll("[\\\\/<>|:]", "");
        }
        command.add(searchQuery);
        return command;
    }

    public void handleError(int exitCode) throws DownloadException {
        if (exitCode == 2) {
            throw new DownloadException("Error with yt_dlp user provided options (2)");
        } else if (exitCode == 100) {
            throw new DownloadException("Your yt-dlp needs to update (100)");
        } else if (exitCode == 101) {
            throw new DownloadException("Download cancelled due to boundary criteria (101)");
        } else {
            logger.warn("Unknown error while downloading yt_dlp song (" + exitCode + ")");
        }
    }
}
