package ryzen.ownitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
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

    public static void writeData(String fileName, String extension, String data, File folder) throws IOException {
        if (folder == null || fileName == null) {
            logger.debug("null folder or filename provided in writeData");
            return;
        }
        if (data == null || data.isEmpty()) {
            logger.debug("null or empty data provided in writeData");
            return;
        }
        if (!folder.exists()) {
            logger.debug("folder '" + folder.getAbsolutePath() + "' does not exist in writeData");
            return;
        }
        File dataFile = new File(folder, fileName + "." + extension);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile))) {
            writer.write(data);
        }
    }

    public static void writeMetaData(LinkedHashMap<FieldKey, String> id3Data, boolean liked, URI coverImage,
            File songFile) throws Exception {
        if (songFile == null) {
            logger.debug("null songFIle provided in writeMetaData");
            return;
        }
        if (!songFile.exists()) {
            logger.debug("Song File '" + songFile.getAbsolutePath() + "' does not exist in writeMetaData");
            return;
        }
        if (id3Data == null) {
            logger.debug("no id3data provided in writeMetaData");
            return;
        }
        // Set ID3v2.3 as default
        TagOptionSingleton.getInstance().setID3V2Version(ID3V2Version.ID3_V24);
        AudioFile audioFile = AudioFileIO.read(songFile);
        ID3v24Tag tag = (ID3v24Tag) audioFile.getTagAndConvertOrCreateAndSetDefault();
        for (FieldKey key : id3Data.keySet()) {
            tag.setField(key, id3Data.get(key));
        }
        if (coverImage != null) {
            try {
                // Download the image to a temporary file
                File tempFile = File.createTempFile(String.valueOf(songFile.getAbsolutePath().hashCode()), ".png");
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
                        "Exception writing coverImage '" + coverImage.toString() + "' for '"
                                + songFile.getAbsolutePath() + "': " + e);
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
        // save changes
        audioFile.commit();
        AudioFileIO.write(audioFile);
    }

    public static LinkedHashMap<FieldKey, String> readMetaData(File songFile) throws Exception {
        if (songFile == null || !songFile.exists()) {
            logger.debug("Invalid or non existant file provided in readMetaData");
            return null;
        }
        LinkedHashMap<FieldKey, String> data = new LinkedHashMap<>();
        AudioFile audioFile = AudioFileIO.read(songFile);
        Tag tag = audioFile.getTag();
        if (tag != null) {
            if (!tag.getFirst(FieldKey.TITLE).isEmpty()) {
                data.put(FieldKey.TITLE, tag.getFirst(FieldKey.TITLE));
            }
            if (!tag.getFirst(FieldKey.ARTIST).isEmpty()) {
                data.put(FieldKey.ARTIST, tag.getFirst(FieldKey.ARTIST));
            }
            if (!tag.getFirst(FieldKey.COVER_ART).isEmpty()) {
                data.put(FieldKey.COVER_ART, tag.getFirst(FieldKey.COVER_ART));
            }
            if (!tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID).isEmpty()) {
                data.put(FieldKey.MUSICBRAINZ_RELEASEID, tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID));
            }
            if (!tag.getFirst(FieldKey.ALBUM).isEmpty()) {
                data.put(FieldKey.ALBUM, tag.getFirst(FieldKey.ALBUM));
            }
        }
        return data;
    }

    public static boolean isSongLiked(File songFile) throws Exception {
        if (songFile == null || !songFile.isFile()) {
            logger.debug("Empty file or non file provided in isSongLiked");
            return false;
        }
        AudioFile audioFile = AudioFileIO.read(songFile);
        Tag tag = audioFile.getTag();
        if (tag != null) {
            String rating = tag.getFirst(FieldKey.RATING);
            if (rating.equals("255")) {
                return true;
            }
        }
        return false;
    }

    public static Duration getSongDuration(File songFile) throws Exception {
        if (songFile == null || !songFile.isFile()) {
            logger.debug("Empty file or non file provided in isSongLiked");
            return Duration.ZERO;
        }
        AudioFile audioFile = AudioFileIO.read(songFile);
        AudioHeader audioHeader = audioFile.getAudioHeader();
        return Duration.ofSeconds(audioHeader.getTrackLength());
    }

    public static void downloadImage(URI url, File file) throws IOException {
        if (url == null || file == null) {
            logger.debug("null url or file passed in downloadImage");
            return;
        }
        if (file.exists()) {
            logger.debug("coverimage already found: '" + file.getAbsolutePath() + "'");
            return;
        }
        try (InputStream in = url.toURL().openStream()) {
            Files.copy(in, file.toPath());
        }
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            logger.debug("null filename passed in SanitizeFileName");
            return null;
        }

        // Sanitize the name by replacing invalid characters with '#'
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9\\s\\(\\)\\$\\-]", "#");
        // Remove any trailing spaces
        sanitized = sanitized.trim();
        // Limit length to 255 characters
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        // Check if the sanitized name contains at least one alphabet character or
        // number
        if (!sanitized.matches(".*[a-zA-Z0-9].*")) {
            return String.valueOf(fileName.hashCode());
        }
        return sanitized;
    }

}