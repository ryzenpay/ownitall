package ryzen.ownitall.tools;

//https://www.geeksforgeeks.org/java-program-to-implement-levenshtein-distance-computing-algorithm/

import java.util.Arrays;

public class Levenshtein {

    // Computes the Levenshtein distance between two strings
    public static int computeDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = minmEdits(dp[i - 1][j - 1] + numOfReplacement(str1.charAt(i - 1), str2.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[str1.length()][str2.length()];
    }

    // Calculates the similarity percentage based on Levenshtein distance
    public static double computeSimilarity(String str1, String str2) {
        int distance = computeDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0)
            return 100.0; // Both strings are empty
        return (1.0 - (double) distance / maxLength) * 100;
    }

    public static boolean computeSimilarityCheck(String str1, String str2, double wantedSimularity) {
        double simularity = computeSimilarity(str1, str2);
        if (simularity >= wantedSimularity) {
            return true;
        }
        return false;
    }

    // Determines if replacement is needed
    private static int numOfReplacement(char c1, char c2) {
        return c1 == c2 ? 0 : 1;
    }

    // Returns the minimum value among given numbers
    private static int minmEdits(int... nums) {
        return Arrays.stream(nums).min().orElse(Integer.MAX_VALUE);
    }
}
