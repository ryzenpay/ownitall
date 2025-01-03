package ryzen.ownitall;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class Local {
    private File localLibrary;
    private LinkedHashSet<String> extensions = new LinkedHashSet<>(Arrays.asList("mp3", ".flac")); // TODO: support more
                                                                                                   // formats

    public Local() {
        System.out.println("Provide absolute path to local music library (folder): ");
        this.localLibrary = Input.getInstance().getFile();
    }

    public Local(String localFolderPath) {
        this.localLibrary = new File(localFolderPath);
    }

    public LinkedHashSet<Song> getLikedSongs() {
        LinkedHashSet<Song> likedSongs = new LinkedHashSet<>();
        for (File file : localLibrary.listFiles()) {
            if (file.isFile() && extensions.contains(getExtension(file))) {
                // TODO: get metadata from file:
                // https://products.groupdocs.com/metadata/java/extract/mp3/
            }
        }
        return likedSongs;
    }

    public String getExtension(File file) {
        String fileName = file.toString();
        int extensionIndex = fileName.lastIndexOf('.');
        return fileName.substring(extensionIndex + 1);
    }

}
