package ryzen.ownitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.jaudiotagger.tag.reference.ID3V2Version;
import org.jaudiotagger.tag.reference.PictureTypes;

public class MusicTools {
    private static final Logger logger = LogManager.getLogger(MusicTools.class);
    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
    }

    /**
     * convert duration into music time (mm:ss)
     * 
     * @param duration - constructed Duration
     * @return - string in format ((hh:)mm:ss)
     */
    public static String musicTime(Duration duration) {
        if (duration == null) {
            logger.debug("null duration provided in musicTime");
            return null;
        }
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
        if (file == null) {
            logger.debug("null file provided in getExtension");
            return null;
        }
        String fileName = file.getName();
        int extensionIndex = fileName.lastIndexOf('.');
        return fileName.substring(extensionIndex + 1).toLowerCase();
    }

    public static void writeData(String fileName, String extension, String data, File folder) throws Exception {
        if (folder == null || fileName == null) {
            logger.debug("null folder or filename provided in writem3u");
            return;
        }
        if (data == null || data.isEmpty()) {
            logger.debug("null or empty m3u data provided in writem3u");
            return;
        }
        if (!folder.exists()) {
            logger.debug("folder " + folder.getAbsolutePath() + " does not exist in writeM3U");
            return;
        }
        File dataFile = new File(folder, fileName + "." + extension);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile))) {
            writer.write(data);
        }
    }

    public static void writeMetaData(String songName, String artistName, URI coverImage, boolean liked,
            String albumName,
            File songFile) throws Exception {
        // TODO: write musicbrainz id
        // and then also fetch it in upload
        if (songFile == null) {
            logger.debug("null songFile provided in writeMetaData");
            return;
        }
        if (!songFile.exists()) {
            logger.debug("Song File " + songFile.getAbsolutePath() + " does not exist in writeMetaData");
            return;
        }
        // Set ID3v2.3 as default
        TagOptionSingleton.getInstance().setID3V2Version(ID3V2Version.ID3_V24);

        AudioFile audioFile = AudioFileIO.read(songFile);
        ID3v24Tag tag = (ID3v24Tag) audioFile.getTagAndConvertOrCreateAndSetDefault();

        tag.setField(FieldKey.TITLE, songName);
        if (artistName != null) {
            tag.setField(FieldKey.ARTIST, artistName);
        }
        if (coverImage != null) {
            try {
                // Download the image to a temporary file
                File tempFile = File.createTempFile(String.valueOf(songName.hashCode()), ".png");
                tempFile.delete(); // to prevent throwing off the downloadimage function
                downloadImage(coverImage, tempFile);
                if (tempFile.exists()) {
                    byte[] imageData = Files.readAllBytes(tempFile.toPath());
                    // Create artwork from the downloaded file
                    Artwork artwork = ArtworkFactory.createArtworkFromFile(tempFile);
                    artwork.setBinaryData(imageData);
                    artwork.setMimeType("image/png");
                    artwork.setPictureType(PictureTypes.DEFAULT_ID);
                    tag.deleteArtworkField();
                    tag.setField(artwork);
                    tempFile.delete();
                }
            } catch (Exception e) {
                logger.error(
                        "Exception writing coverImage " + coverImage.toString() + " for " + songFile.getAbsolutePath());
            }
        }
        if (liked) {
            // Set rating using POPM frame
            tag.setField(FieldKey.RATING, "255");
            // ID3v24Frame popmFrame = new ID3v24Frame("POPM");
            // FrameBodyPOPM popmBody = new FrameBodyPOPM();
            // popmBody.setRating(255);
            // popmFrame.setBody(popmBody);
            // tag.setFrame(popmFrame);

            // Set custom "Love Rating" for MusicBee
            ID3v24Frame txxxFrame = new ID3v24Frame("TXXX");
            FrameBodyTXXX txxxBody = new FrameBodyTXXX();
            txxxBody.setDescription("Love");
            txxxBody.setText("L");
            txxxFrame.setBody(txxxBody);
            tag.setFrame(txxxFrame);
        } else {
            // Remove rating if not liked
            // tag.removeFrame("POPM");
            tag.deleteField(FieldKey.RATING);
            tag.removeFrame("TXXX");
        }
        if (albumName != null) {
            tag.setField(FieldKey.ALBUM, albumName);
        }
        audioFile.commit();
        AudioFileIO.write(audioFile);
    }

    public static void downloadImage(URI url, File file) throws Exception {
        if (url == null || file == null) {
            logger.debug("null url or file passed in downloadImage");
            return;
        }
        if (file.exists()) {
            logger.debug("coverimage already found: " + file.getAbsolutePath());
            return;
        }
        try (InputStream in = url.toURL().openStream()) {
            Files.copy(in, file.toPath());
        }
    }

    public static String sanitizeFileName(String fileName) {
        // TODO: revise sanitizeFileName to not use urlencode
        if (fileName == null) {
            logger.debug("null filename passed in SanitizeFileName");
            return null;
        }
        String sanitized = null;
        try {
            sanitized = URLEncoder.encode(fileName, "UTF-8");
            // Limit length to 255 characters
            if (sanitized.length() > 255) {
                sanitized = sanitized.substring(0, 255);
            }
            // Remove starting and trailing '.' characters
            sanitized = sanitized.replaceAll("^\\.*|\\.*$", "");
            // Check if the sanitized name contains at least one alphabet character +/number
            if (!sanitized.matches(".*[a-zA-Z0-9].*")) {
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception encoding filename: " + e);
        }
        return sanitized;
    }
}
