package ryzen.ownitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.ID3v23Frame;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.jaudiotagger.tag.reference.ID3V2Version;

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

    public static void writeM3U(String fileName, String M3UData, File folder) throws Exception {
        if (!folder.exists()) {
            return;
        }
        File M3UFile = new File(folder, fileName + ".m3u");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(M3UFile))) {
            writer.write(M3UData);
        }
    }

    public static void writeMetaData(String songName, String artistName, URI coverImage, boolean liked,
            File songFile) throws Exception {
        if (!songFile.exists()) {
            return;
        }

        // Set ID3v2.3 as default
        TagOptionSingleton.getInstance().setID3V2Version(ID3V2Version.ID3_V23);

        AudioFile audioFile = AudioFileIO.read(songFile);
        ID3v23Tag tag = (ID3v23Tag) audioFile.getTagAndConvertOrCreateAndSetDefault();

        tag.setField(FieldKey.TITLE, songName);
        if (artistName != null) {
            tag.setField(FieldKey.ARTIST, artistName);
        }
        if (coverImage != null) {
            Artwork artwork = ArtworkFactory.createLinkedArtworkFromURL(coverImage.toString());
            tag.setField(artwork);
        }

        if (liked) {
            // Set rating using POPM frame
            ID3v23Frame popmFrame = new ID3v23Frame("POPM");
            FrameBodyPOPM popmBody = new FrameBodyPOPM();
            popmBody.setRating(255);
            popmFrame.setBody(popmBody);
            tag.setFrame(popmFrame);

            // Set custom "Love Rating" for MusicBee
            ID3v23Frame txxxFrame = new ID3v23Frame("TXXX");
            FrameBodyTXXX txxxBody = new FrameBodyTXXX();
            txxxBody.setDescription("Love Rating");
            txxxBody.setText("L");
            txxxFrame.setBody(txxxBody);
            tag.setFrame(txxxFrame);
        } else {
            // Remove rating if not liked
            tag.removeFrame("POPM");
            tag.removeFrame("TXXX");
        }
        audioFile.setTag(tag);
        AudioFileIO.write(audioFile);
    }

    public static void downloadImage(URI url, File folder) throws Exception {
        if (url == null) {
            return;
        }
        if (!folder.exists()) {
            return;
        }
        File imageFile = new File(folder, "cover.png");
        if (imageFile.exists()) {
            return;
        }
        try (InputStream in = url.toURL().openStream()) {
            Files.copy(in, imageFile.toPath());
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
        sanitized = sanitized.trim(); // remove any trailing spaces
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
