package ryzen.ownitall.util;

public class Levenshtein {
    private static final int MAX_PREALLOCATED_LENGTH = 200;
    private static final int[][] dp = new int[MAX_PREALLOCATED_LENGTH][MAX_PREALLOCATED_LENGTH];

    public static int computeDistance(String str1, String str2) {
        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();

        int m = str1.length();
        int n = str2.length();

        if (m > MAX_PREALLOCATED_LENGTH || n > MAX_PREALLOCATED_LENGTH) {
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

    public static boolean computeSimilarityCheck(String str1, String str2, double wantedSimilarity) {
        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();

        int maxLength = Math.max(str1.length(), str2.length());
        int maxAllowedDistance = (int) Math.ceil(maxLength * (1 - wantedSimilarity / 100));

        // Quick check for exact match
        if (str1.equals(str2))
            return true;

        // Quick length check
        if (Math.abs(str1.length() - str2.length()) > maxAllowedDistance)
            return false;

        // Compute actual distance only if necessary
        int distance = computeDistance(str1, str2);
        return distance <= maxAllowedDistance;
    }
}
