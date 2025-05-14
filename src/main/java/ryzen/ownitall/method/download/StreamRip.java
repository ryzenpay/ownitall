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

//https://github.com/nathom/streamrip

// maybe remove yt_dl and just use that?
// retries tries other sources
//TODO: implement streamrip
//requirements: python pip
public class StreamRip extends Download {
    private static final Logger logger = new Logger(StreamRip.class);

    public StreamRip() throws InterruptedException {
        if (Method.isCredentialsEmpty(StreamRip.class)) {
            logger.debug("Empty SoulSeek credentials found");
            throw new InterruptedException("empty SoulSeek credentials");
        }
        try {
            this.installStreamRip();
        } catch (IOException e) {
            logger.error("Exception installing streamrip", e);
        }
    }

    private void installStreamRip() throws IOException, InterruptedException {
        ArrayList<String> command = new ArrayList<>();
        command.add("pip3");
        command.add("install");
        command.add("streamrip");
        command.add("--upgrade");
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Error during StreamRip Installation: " + output.toString());
        }
    }

    @Override
    public void downloadSong(Song song, File path) {
        if (song == null || path == null) {
            logger.debug("null song or Path provided in downloadSong");
            return;
        }
        ArrayList<String> command = new ArrayList<>();
        // executables
        command.add("rip");
        command.add("--folder");
        command.add(path.getAbsolutePath());
        command.add("--quality");
        command.add("4");// 0=low, 4=high
        command.add("--no-db");
        command.add("--codec");
        command.add("mp3");
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
        }
    }
}
