package ryzen.ownitall.tools;

public class Levenshtein {
    private static final int[][] dp = new int[1000][1000]; // Preallocate matrix

    public static int computeDistance(String str1, String str2) {
        int m = str1.length();
        int n = str2.length();

        if (m > dp.length || n > dp[0].length) {
            return computeDistanceLarge(str1, str2);
        }

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 1; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1));
            }
        }

        return dp[m][n];
    }

    private static int computeDistanceLarge(String str1, String str2) {
        int[] prev = new int[str2.length() + 1];
        int[] curr = new int[str2.length() + 1];

        for (int j = 0; j <= str2.length(); j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= str2.length(); j++) {
                curr[j] = Math.min(Math.min(prev[j] + 1, curr[j - 1] + 1),
                        prev[j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1));
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[str2.length()];
    }

    public static double computeSimilarity(String str1, String str2) {
        int distance = computeDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        return maxLength == 0 ? 100.0 : (1.0 - (double) distance / maxLength) * 100;
    }

    public static boolean computeSimilarityCheck(String str1, String str2, double wantedSimilarity) {
        return computeSimilarity(str1, str2) >= wantedSimilarity;
    }
}
