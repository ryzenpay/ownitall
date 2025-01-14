package ryzen.ownitall.tools;

import java.time.Duration;
import java.util.ArrayList;

import ryzen.ownitall.Song;

public class MusicTime {
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
     * get the total duration of an arraylist of songs
     * 
     * @param songs - arraylist of constructed Song
     * @return - constructed Duration representing total duration of arraylist of
     *         songs
     */
    public static Duration totalDuration(ArrayList<Song> songs) {
        Duration totalDuration = Duration.ZERO;
        for (Song song : songs) {
            totalDuration = totalDuration.plus(song.getDuration());
        }
        return totalDuration;
    }
}
