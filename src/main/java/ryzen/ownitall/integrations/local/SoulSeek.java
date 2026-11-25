package ryzen.ownitall.integrations.local;

import java.io.File;
import java.util.ArrayList;

import org.apache.logging.log4j.Level;

import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.integrations.Integration.Export;
import ryzen.ownitall.integrations.Integration.Sync;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools.AuthenticationException;

//https://github.com/fiso64/slsk-batchdl
/**
 * <p>
 * SoulSeek class.
 * </p>
 *
 * @author ryzen
 */
public class SoulSeek extends Local implements Sync, Export {
    private static final Logger logger = new Logger(SoulSeek.class);

    /**
     * <p>
     * Constructor for SoulSeek.
     * </p>
     *
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public SoulSeek() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(SoulSeek.class)) {
            logger.debug("Empty SoulSeek credentials found");
            throw new MissingSettingException(SoulSeek.class);
        }
        if (!Settings.soulSeekFile.exists()) {
            throw new AuthenticationException("SoulSeek missing soulseek binary");
        }
        // unable to thread due to ports
        // using multiple ports also doesnt work because soulseek doesnt allow multiple
        // usersessions with same account
        Local.downloadThreads = 1;
    }

    /**
     * {@inheritDoc}
     * <p>
     * slsk-batchdl picks the closest match itself, so the attempt is ignored
     * </p>
     */
    public ArrayList<String> createCommand(Song song, File downloadFile, int attempt)
            throws InterruptedException {
        if (song == null || downloadFile == null) {
            logger.debug("null song or downloadFile provided in downloadSong");
            return null;
        }
        ArrayList<String> command = new ArrayList<>();
        command.add(Settings.soulSeekFile.getAbsolutePath());
        command.add("--user");
        command.add(Settings.soulSeekUsername);
        command.add("--pass");
        command.add(Settings.soulSeekPassword);
        command.add("--path");
        command.add(downloadFile.getParent());
        command.add("--input-type");
        command.add("string");
        command.add("--number");
        command.add("1");
        command.add("--no-write-index");
        command.add("--min-bitrate");
        command.add(String.valueOf(Settings.soulSeekBitRate));
        command.add("--name-format");
        command.add(downloadFile.getName());
        command.add("--fast-search");
        command.add(String.valueOf(Settings.downloadThreads));
        if (Logger.is(Level.DEBUG)) {
            command.add("-v");
        }
        /**
         * search for video using the query / use url
         * ^ keep this at the end, incase of fucked up syntax making the other flags
         * drop
         */
        String searchQuery = "title=" + song.getName();
        if (song.getMainArtist() != null) {
            searchQuery += ", artist=" + song.getMainArtist().getName();
        }
        if (song.getAlbumName() != null) {
            searchQuery += ", album=" + song.getAlbumName();
        }
        if (!song.getDuration().isZero()) {
            searchQuery += ", length=" + song.getDuration().toSeconds();
        }
        searchQuery = searchQuery.replaceAll("[\\\\/<>|:]", "");
        command.add(searchQuery);
        return command;
    }

    /**
     * {@inheritDoc}
     * <p>
     * slsk-batchdl only exits non zero on connection loss, which is always worth another attempt
     * </p>
     */
    public void handleError(int exitCode, String log) {
        logger.warn("Unknown exit code while downloading SoulSeek song (" + exitCode + ")");
    }
}
