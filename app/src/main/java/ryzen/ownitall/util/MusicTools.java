package ryzen.ownitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashSet;

import ryzen.ownitall.classes.Song;

public class MusicTools {
    /**
     * convert duration into music time (mm:ss)
     * 
     * @param duration - constructed Duration
     * @return - string in format ((hh:)mm:ss)
     */
    public static String musicTime(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public static Duration totalDuration(LinkedHashSet<Song> songs) {
        Duration totalDuration = Duration.ZERO;
        for (Song song : songs) {
            totalDuration = totalDuration.plus(song.getDuration());
        }
        return totalDuration;
    }

    /**
     * get file extension of a file
     * 
     * @param file - constructed File to get extension from
     * @return - String of file extension
     */
    public static String getExtension(File file) {
        String fileName = file.getName();
        int extensionIndex = fileName.lastIndexOf('.');
        return fileName.substring(extensionIndex + 1).toLowerCase();
    }

    public static void writeM3U(String title, String M3UData, File folder) throws Exception {
        if (!folder.exists()) {
            return;
        }
        File M3UFile = new File(folder, title + ".m3u");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(M3UFile))) {
            writer.write(M3UData);
        }
    }

    public static void downloadImage(URI url, File folder) throws Exception {
        if (!folder.exists()) {
            return;
        }
        File imageFile = new File(folder, "cover.png");
        try (InputStream in = url.toURL().openStream()) {
            Files.copy(in, imageFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        // Sanitize the name by removing invalid characters
        byte[] utf8Bytes = fileName.getBytes(StandardCharsets.UTF_8);
        String sanitized = new String(utf8Bytes, StandardCharsets.UTF_8);
        // Remove any invalid characters including pipe "|"
        sanitized = sanitized.replaceAll("[^\\u0000-\\u007F]", ""); // Remove non-ASCII characters
        sanitized = sanitized.replaceAll("[\\\\/<>|:]", ""); // Remove specific invalid characters
        // Limit length to 255 characters
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        // Validate path
        // try {
        // Paths.get(sanitized);
        // } catch (InvalidPathException | NullPointerException e) {
        // sanitized = "";
        // }
        // Fallback if the sanitized name is empty
        if (sanitized.isEmpty()) {
            sanitized = String.valueOf(fileName.hashCode());
        }
        return sanitized;
    }
}
