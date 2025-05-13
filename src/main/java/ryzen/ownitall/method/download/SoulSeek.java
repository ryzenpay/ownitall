package ryzen.ownitall.method.download;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.util.LogConfig;
import ryzen.ownitall.util.Logger;

//https://github.com/fiso64/slsk-batchdl
/**
 * <p>
 * SoulSeek class.
 * </p>
 *
 * @author ryzen
 */
@Method.Export
public class SoulSeek extends Download {
    private static final Logger logger = new Logger(SoulSeek.class);
    private static final int basePort = 49998;
    // to keep track of all unused / available ports to prevent threading conflict
    private static ArrayList<Integer> ports;

    /**
     * <p>
     * Constructor for SoulSeek.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public SoulSeek() throws InterruptedException {
        if (Method.isCredentialsEmpty(SoulSeek.class)) {
            logger.debug("Empty SoulSeek credentials found");
            throw new InterruptedException("empty SoulSeek credentials");
        }
        ports = new ArrayList<>();
        for (int i = 0; i < downloadThreads; i++) {
            ports.add(basePort + i);
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
        int currPort = ports.get(0);
        ports.remove(0);
        ArrayList<String> command = new ArrayList<>();
        // executables
        command.add(Credentials.soulSeekFile.getAbsolutePath());
        command.add("--user");
        command.add(Credentials.soulSeekUsername);
        command.add("--pass");
        command.add(Credentials.soulSeekPassword);
        command.add("--path");
        command.add(path.getAbsolutePath());
        command.add("--input-type");
        command.add("string");
        command.add("--number");
        command.add("1");
        command.add("--no-write-index");
        command.add("--min-bitrate");
        command.add(String.valueOf(Settings.soulSeekBitRate));
        command.add("--name-format");
        command.add(song.getFileName());
        command.add("--fast-search");
        command.add("--concurrent-downloads");
        command.add(String.valueOf(Settings.downloadThreads));
        command.add("--listen-port");
        command.add(String.valueOf(currPort));
        command.add("--searches-per-time");
        command.add("100");
        if (LogConfig.isDebug()) {
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
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            int retries = 0;
            File songFile = new File(path, song.getFileName());
            StringBuilder completeLog = new StringBuilder();
            while (!songFile.exists() && retries < 3) {
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
                    } else {
                        logger.error("Unkown error while downloading song: '" + song + "' with code: " + exitCode
                                + "\n Command: " + command.toString() + "\n Complete log: \n" + completeLog.toString(),
                                new Exception());
                    }
                    logger.warn("Attempt: " + retries);
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
        } finally {
            ports.add(currPort);
        }
    }
}
