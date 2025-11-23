package ryzen.ownitall.util;

import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
                WebTools.downloadImage(coverImage, tempFile);
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

    public static String removeBrackets(String string) {
        if (string == null) {
            logger.debug("null string provided in removeBrackets");
            return null;
        }
        if (!string.contains("(")) {
            return string;
        }
        // ()
        string = string.replaceAll("\\(.*?\\)", "");
        // []
        string = string.replaceAll("\\[.*?\\]", "");
        // remove spaces in middle of string
        return string.replaceAll("  ", " ").trim();
    }

    public static String getAlbumNFO(String albumName, ArrayList<String> artistNames, ArrayList<String> songNames,
            String coverImage) {
        if (albumName == null) {
            logger.debug("null albumName provided in getAlbumNFO");
            return null;
        }
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("album");
            doc.appendChild(rootElement);

            // Title
            Element title = doc.createElement("title");
            title.appendChild(doc.createTextNode(albumName));
            rootElement.appendChild(title);

            // Artists
            Element artistsElement = doc.createElement("artists");
            rootElement.appendChild(artistsElement);
            for (String artistName : artistNames) {
                Element artistElement = doc.createElement("artist");
                artistElement.appendChild(doc.createTextNode(artistName));
                artistsElement.appendChild(artistElement);
            }

            // Songs
            Element tracksElement = doc.createElement("tracks");
            rootElement.appendChild(tracksElement);
            for (String songName : songNames) {
                Element trackElement = doc.createElement("track");

                Element trackTitle = doc.createElement("title");
                trackTitle.appendChild(doc.createTextNode(songName));
                trackElement.appendChild(trackTitle);

                tracksElement.appendChild(trackElement);
            }

            // Cover image
            if (coverImage != null) {
                Element thumb = doc.createElement("thumb");
                thumb.appendChild(doc.createTextNode(coverImage));
                rootElement.appendChild(thumb);
            }

            // Transform the DOM to XML string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);

            return writer.toString();

        } catch (Exception e) {
            logger.error("exception generating NFO content", e);
            return null;
        }
    }
}
