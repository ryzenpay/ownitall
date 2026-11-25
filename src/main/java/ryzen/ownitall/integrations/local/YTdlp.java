package ryzen.ownitall.integrations.local;

import java.io.File;
import java.util.ArrayList;

import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.integrations.Integration.Export;
import ryzen.ownitall.integrations.Integration.Sync;
import ryzen.ownitall.integrations.Youtube;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools.AuthenticationException;

/**
 * <p>
 * YTdlp class.
 * </p>
 *
 * @author ryzen
 */
public class YTdlp extends Local implements Sync, Export {
    private static final Logger logger = new Logger(YTdlp.class);
    // log parsing markers
    private static final String idMarker = "ownitall-match:";
    private static final String fileMarker = "ownitall-file:";

    private static final int candidates = 6;
    private static final int durationAttempts = 2;
    private static final long durationMargin = 15;
    private static final long minDuration = 45;
    private static final long maxDuration = 600;
    private static final long minViewCount = 100000;
    private static final String junkTitles = "(?i)\\b(live|cover|remix|lyrics?|slowed|sped|karaoke|instrumental|reaction|8d|mix)\\b";
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
        if (!Settings.ffmpegFile.exists()) {
            throw new AuthenticationException("YTdlp missing ffmpeg");
        }
        if (Settings.ytdlpUseYoutube) {
            this.youtube = new Youtube();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * every attempt loosens the search, so a wrong first page still ends in a
     * download instead of
     * repeating the same command three times
     * </p>
     */
    public ArrayList<String> createCommand(Song song, File downloadFile, int attempt)
            throws InterruptedException {
        if (song == null || downloadFile == null) {
            logger.debug("null song or downloadFile provided in downloadSong");
            return null;
        }
        ArrayList<String> command = new ArrayList<>();
        // executables
        command.add(Settings.ytdlpFile.getAbsolutePath());
        command.add("--ffmpeg-location");
        command.add(Settings.ffmpegFile.getAbsolutePath());
        // logging, --newline keeps progress to one line per update (Local reads the
        // stream by
        // line) and the --print lines are parsed back out after the download, warnings
        // are kept
        // because yt-dlp shouting about its own version being old is the usual cause of
        // a miss
        command.add("--newline");
        command.add("--no-progress");
        command.add("--print");
        command.add("before_dl:" + idMarker + "%(id)s|%(title)s");
        command.add("--print");
        command.add("after_move:" + fileMarker + "%(filepath)s");
        // network
        command.add("--socket-timeout");
        command.add("30");
        command.add("--retries");
        command.add("3");
        command.add("--fragment-retries");
        command.add("3");
        command.add("-N");
        command.add("4");
        // keeps youtube from rate limiting / bot checking a whole playlist of songs
        command.add("--sleep-requests");
        command.add("0.5");
        // cookies, optional, either an exported file or read straight from a browser
        if (Settings.ytdlpCookieFile != null && Settings.ytdlpCookieFile.exists()) {
            command.add("--cookies");
            command.add(Settings.ytdlpCookieFile.getAbsolutePath());
        } else if (Settings.ytdlpCookieBrowser != null && !Settings.ytdlpCookieBrowser.isEmpty()) {
            command.add("--cookies-from-browser");
            command.add(Settings.ytdlpCookieBrowser);
        }
        // searching, yt-dlp walks the results in order and keeps the first one passing
        // the filter
        command.add("--no-playlist"); // Prevent downloading playlists
        command.add("--default-search");
        command.add("ytsearch" + candidates);
        command.add("--max-downloads");
        command.add("1");
        command.add("--match-filters");
        command.add(this.createFilter(song, attempt));
        // metadata and formatting
        command.add("--extract-audio");
        // command.add("--embed-thumbnail"); // metadata we have overwrites this
        command.add("--format");
        command.add("bestaudio[ext=m4a]/bestaudio/best"); // m4a keeps the cover image intact
        command.add("--audio-format");
        command.add(Settings.downloadFormat);
        command.add("--audio-quality");
        command.add(Integer.toString(Settings.ytdlpQuality));
        // download location, --output is passed raw on purpose: ProcessBuilder runs
        // yt-dlp
        // without a shell so quoting it here would put the quotes in the filename
        command.add("--paths");
        command.add(downloadFile.getParent());
        command.add("--force-overwrites"); // a half written attempt must not count as done
        command.add("--output");
        // yt-dlp names the raw download after its own container and --audio-format then
        // converts
        // it into the extension downloadFile already has, naming the file with that
        // extension up
        // front would make yt-dlp skip the conversion and leave a m4a/webm file called
        // like a
        // <format> file
        command.add(this.getBaseName(downloadFile) + ".%(ext)s");
        /**
         * search for video using the query / use url
         * ^ keep this at the end, incase of fucked up syntax making the other flags
         * drop
         */
        String searchQuery = null;
        if (song.getId("youtube") != null) {
            searchQuery = "https://youtube.com/watch?v=" + song.getId("youtube");
        } else if (this.youtube != null) {
            // searches youtube and caches the matched id on the song
            String id = this.youtube.getSongId(song);
            if (id != null) {
                searchQuery = "https://youtube.com/watch?v=" + id;
                song.addId("youtube", id);
            }
        }
        if (searchQuery == null) {
            searchQuery = this.createQuery(song, attempt);
        }
        command.add(searchQuery);
        return command;
    }

    /**
     * criteria a youtube upload has to pass to be used for this song, yt-dlp
     * rejects the results
     * failing it and moves on to the next one
     * <p>
     * the language is stricter than it looks: no function calls (no abs()), no
     * 'filter:' prefix
     * and multiple conditions joined with '&amp;' inside a single expression
     * </p>
     *
     * @param song    - song being downloaded
     * @param attempt - download attempt, higher attempts accept more
     * @return - yt-dlp match filter expression
     */
    private String createFilter(Song song, int attempt) {
        long duration = song.getDuration().toSeconds();
        long minimum = minDuration;
        long maximum = maxDuration;
        if (attempt < durationAttempts && duration > 0) {
            long margin = durationMargin * (attempt + 1);
            minimum = Math.max(1, duration - margin);
            maximum = duration + margin;
        }
        String filter = "duration>=" + minimum + "&duration<=" + maximum + "&!is_live";
        if (attempt == 0) {
            // '?' means 'also pass when youtube did not give us the field'
            filter += "&view_count>?" + minViewCount + "&title!~='" + junkTitles + "'";
        }
        return filter;
    }

    /**
     * youtube search criteria, the engine weights the front of the query so the
     * artist goes first
     * and the brackets come out: "Can't Help Falling In Love - Elvis Presley
     * (official audio)"
     * returns karaoke uploads while "Elvis Presley - Can't Help Falling In Love
     * official audio"
     * returns the label upload
     *
     * @param song    - song being downloaded
     * @param attempt - download attempt, higher attempts use a plainer query
     * @return - search query (or the url when yt-dlp does not need to search)
     */
    private String createQuery(Song song, int attempt) {
        // library titles carry '(feat. ...)' / '(Remastered 2011)' suffixes, youtube
        // matches them
        // as extra search terms and they only push the real upload down the results
        String title = this.stripBrackets(song.getName());
        if (title.isEmpty()) {
            title = song.getName().trim();
        }
        StringBuilder query = new StringBuilder();
        if (song.getMainArtist() != null) {
            query.append(song.getMainArtist().getName().trim()).append(" - ");
        }
        query.append(title);
        if (attempt == 0) {
            query.append(" official audio");
        } else if (attempt >= durationAttempts && song.getAlbumName() != null) {
            // last try, the title clearly did not match anything, the album can pin down
            // deep cuts
            query.append(" ").append(this.stripBrackets(song.getAlbumName()));
        }
        // no shell is involved but youtube itself gives "" a meaning
        return query.toString().replaceAll("[\\\\/<>|:\"]", "").trim();
    }

    /**
     * removes every '(something)' group from a name
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    private String stripBrackets(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("\\s*\\([^)]*\\)", "").trim();
    }

    /**
     * name of a file without its extension
     *
     * @param file a {@link java.io.File} object
     * @return a {@link java.lang.String} object
     */
    private String getBaseName(File file) {
        String name = file.getName();
        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex == -1) {
            return name;
        }
        return name.substring(0, extensionIndex);
    }

    /**
     * last line yt-dlp printed about the upload it picked, yt-dlp only prints this
     * for results
     * passing the filter, so it is the upload that was (or nearly was) used
     *
     * {@inheritDoc}
     */
    protected String getMatchDetails(String log) {
        String matched = null;
        for (String line : log.split("\n")) {
            if (line.startsWith(idMarker)) {
                matched = line.substring(idMarker.length());
            }
        }
        return matched;
    }

    /**
     * {@inheritDoc}
     * <p>
     * yt-dlp picks the extension itself after converting, so the path it prints is
     * preferred over
     * the name it was told to write
     * </p>
     */
    protected File getDownloadedFile(File expected, String log) {
        File downloadedFile = null;
        for (String line : log.split("\n")) {
            if (line.startsWith(fileMarker)) {
                downloadedFile = new File(line.substring(fileMarker.length()).trim());
            }
        }
        if (downloadedFile != null && downloadedFile.exists()) {
            return downloadedFile;
        }
        return expected;
    }

    /**
     * {@inheritDoc}
     * <p>
     * caches the video id that was actually used so the next run downloads that
     * exact upload
     * instead of trusting the search again
     * </p>
     */
    protected void onDownloaded(Song song, String log) {
        String matched = this.getMatchDetails(log);
        if (matched == null) {
            return;
        }
        String id = matched.split("\\|")[0];
        if (!id.isEmpty() && !id.equals(song.getId("youtube"))) {
            logger.info("Matched youtube upload '" + matched + "' for '" + song.toString() + "'");
            song.addId("youtube", id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void handleError(int exitCode, String log) throws DownloadException {
        String matched = this.getMatchDetails(log);
        if (exitCode == 2) {
            throw new DownloadException("Error with yt_dlp user provided options (2)");
        } else if (exitCode == 100) {
            throw new DownloadException("Your yt-dlp needs to update (100), 'yt-dlp --update-to stable'");
        } else if (exitCode == 101) {
            // --max-downloads makes yt-dlp exit 101 even after a successful download, so
            // this is
            // only reached when no file was written: the search page had nothing passing
            // createFilter or yt-dlp's youtube extractor broke, either way the next attempt
            // loosens the search instead of giving up
            logger.warn("yt-dlp left without a file (101)"
                    + (matched == null ? ", no search result passed the criteria"
                            : ", last match '" + matched + "'"));
            return;
        }
        throw new DownloadException(log);
    }
}
