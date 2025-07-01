package ryzen.ownitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashMap;
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

/**
 * <p>
 * MusicTools class.
 * </p>
 *
 * @author ryzen
 */
public class MusicTools {
    private static final Logger logger = new Logger(MusicTools.class);

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

    // /**
    // * <p>
    // * getExtension.
    // * </p>
    // *
    // * @param uri a {@link java.net.URI} object
    // * @return a {@link java.lang.String} object
    // */
    // public static String getExtension(URI uri) {
    // if (uri == null) {
    // logger.debug("null uri provided in getExtension");
    // return null;
    // }
    // String path = uri.getPath();
    // if (path == null || path.isEmpty()) {
    // logger.debug("empty path provided for url: '" + uri + "'");
    // return null;
    // }
    // int lastSlashIndex = path.lastIndexOf('/');
    // String lastSegment = path.substring(lastSlashIndex + 1);
    // int extensionIndex = lastSegment.lastIndexOf('.');
    // if (extensionIndex == -1 || extensionIndex == lastSegment.length() - 1) {
    // // logger.debug("url has no extension: '" + uri + "'");
    // return null;
    // }
    // return lastSegment.substring(extensionIndex + 1).toLowerCase();
    // }

    /**
     * write "text" data to a file
     *
     * @param file - file to write data to
     * @param data - data to write to file
     * @throws java.io.IOException - when exception in file writing
     */
    public static void writeData(File file, String data) throws IOException {
        if (file == null) {
            logger.debug("null file provided in writeData");
            return;
        }
        if (!file.getParentFile().exists()) {
            logger.debug("file's parent folder does not exist provided in writeData");
            return;
        }
        if (data == null || data.isEmpty()) {
            logger.debug("null or empty data provided in writeData");
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(data);
        }
    }

    /**
     * write song metadata
     *
     * @param id3Data    - linkedhashmap with FieldKey:String mapping
     * @param liked      - if song is liked
     * @param coverImage - uri of song coverimage
     * @param songFile   - file to write metadata to
     * @throws java.lang.Exception - exception when writing metadata
     */
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
                File tempFile = File.createTempFile(String.valueOf(songFile.getAbsolutePath().hashCode()),
                        ".png");
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
                                + songFile.getAbsolutePath() + "'",
                        e);
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

    /**
     * read metadata from song file
     *
     * @param songFile - song file to read metadata from
     * @return - linkedhashmap of FieldKey:String with file metadata
     * @throws java.lang.Exception - exception reading metadata
     */
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
            if (!tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID).isEmpty()) {
                data.put(FieldKey.MUSICBRAINZ_RELEASEID, tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID));
            }
            if (!tag.getFirst(FieldKey.ALBUM).isEmpty()) {
                data.put(FieldKey.ALBUM, tag.getFirst(FieldKey.ALBUM));
            }
        }
        return data;
    }

    /**
     * check if local song is liked using metadata
     * - requires 5 stars
     *
     * @param songFile - file to read metadata from
     * @return - true if liked, false if not
     * @throws java.lang.Exception - exception when reading metadata
     */
    public static boolean isSongLiked(File songFile) throws Exception {
        if (songFile == null || !songFile.isFile()) {
            logger.debug("null or non file provided in isSongLiked");
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

    /**
     * get duration of local song file
     *
     * @param songFile - songfile to check
     * @return - constructed Duration of songs duration
     * @throws java.lang.Exception - exception reading song metadata
     */
    public static Duration getSongDuration(File songFile) throws Exception {
        if (songFile == null || !songFile.isFile()) {
            logger.debug("null or non file provided in isSongLiked");
            return Duration.ZERO;
        }
        AudioFile audioFile = AudioFileIO.read(songFile);
        AudioHeader audioHeader = audioFile.getAudioHeader();
        return Duration.ofSeconds(audioHeader.getTrackLength());
    }

    /**
     * download an image from the web
     *
     * @param url  - URI to fetch image from
     * @param file - file to download the image to (will make a new one)
     * @throws java.io.IOException - java.io.IOException while downloading
     */
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
        } catch (FileNotFoundException e) {
            logger.debug("Image at url '" + url + "' not found");
        }
    }

    /**
     * sanitize a fileName to be universally acceptable
     *
     * @param fileName - String filename to sanitize
     * @return - sanitized String
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            logger.debug("null filename passed in SanitizeFileName");
            return null;
        }

        // Sanitize the name by replacing invalid characters with '#'
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9\\._]+", "");
        // Remove any trailing spaces
        sanitized = sanitized.trim();
        // Limit length to 255 characters
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        // Check if the sanitized name contains at least one alphabet character or
        // number
        if (sanitized.isEmpty()) {
            return String.valueOf(fileName.hashCode());
        }
        return sanitized;
    }

    /**
     * <p>
     * deleteFolder.
     * </p>
     *
     * @param folder a {@link java.io.File} object
     * @return a boolean
     */
    public static boolean deleteFolder(File folder) {
        if (folder == null) {
            logger.debug("null folder provided in deleteFolder");
            return false;
        }
        if (!folder.exists()) {
            return true;
        }
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                if (deleteFolder(file)) {
                    logger.info("Successfully deleted folder: " + file.getAbsolutePath());
                } else {
                    logger.warn("Failed to delete folder: " + file.getAbsolutePath());
                }
            } else {
                if (file.delete()) {
                    logger.info("Successfully deleted file: " + file.getAbsolutePath());
                } else {
                    logger.warn("Failed to delete file: " + file.getAbsolutePath());
                }
            }
        }
        return folder.delete();
    }
}
