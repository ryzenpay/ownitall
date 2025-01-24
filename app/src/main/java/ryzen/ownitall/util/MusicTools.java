package ryzen.ownitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
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

    public static void writeM3U(String title, String M3UData, String folderPath) throws Exception {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            return;
        }
        File M3UFile = new File(folder, title + ".m3u");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(M3UFile))) {
            writer.write(M3UData);
        }
    }

    public static void downloadImage(URL url, File folder) throws Exception {
        if (!folder.exists()) {
            return;
        }
        File imageFile = new File(folder, "cover.png");
        try (InputStream in = url.openStream()) {
            Files.copy(in, imageFile.toPath());
        }
    }
}
