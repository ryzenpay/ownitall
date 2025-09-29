package ryzen.ownitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileTools {
    private static final Logger logger = new Logger(FileTools.class);

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
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9\\-\\_ ]+", "");
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
