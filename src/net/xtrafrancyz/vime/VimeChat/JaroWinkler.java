/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xtrafrancyz.vime.VimeChat;

import java.util.Arrays;

public class JaroWinkler {
    private JaroWinkler() {}
    
    /**
     * Compute Jaro-Winkler similarity.
     *
     * @param s1 The first string to compare.
     * @param s2 The second string to compare.
     * @return The Jaro-Winkler similarity in the range [0, 1]
     * @throws NullPointerException if s1 or s2 is null.
     */
    public static double similarity(String s1, String s2) {
        if (s1 == null)
            throw new NullPointerException("s1 must not be null");
        
        if (s2 == null)
            throw new NullPointerException("s2 must not be null");
        
        if (s1.equals(s2))
            return 1;
        
        int[] mtp = matches(s1, s2);
        float m = mtp[0];
        if (m == 0)
            return 0f;
        
        double j = ((m / s1.length() + m / s2.length() + (m - mtp[1]) / m)) / 3;
        return j < 0.7d ? j : j + Math.min(0.1d, 1.0d / mtp[3]) * mtp[2] * (1 - j);
    }
    
    /**
     * Return 1 - similarity.
     *
     * @param s1 The first string to compare.
     * @param s2 The second string to compare.
     * @return 1 - similarity.
     * @throws NullPointerException if s1 or s2 is null.
     */
    public static double distance(String s1, String s2) {
        return 1.0 - similarity(s1, s2);
    }
    
    private static int[] matches(String s1, String s2) {
        String max, min;
        if (s1.length() > s2.length()) {
            max = s1;
            min = s2;
        } else {
            max = s2;
            min = s1;
        }
        int range = Math.max(max.length() / 2 - 1, 0);
        int[] matchIndexes = new int[min.length()];
        Arrays.fill(matchIndexes, -1);
        boolean[] matchFlags = new boolean[max.length()];
        int matches = 0;
        for (int mi = 0; mi < min.length(); mi++) {
            char c1 = min.charAt(mi);
            for (int xi = Math.max(mi - range, 0),
                 xn = Math.min(mi + range + 1, max.length()); xi < xn; xi++) {
                if (!matchFlags[xi] && c1 == max.charAt(xi)) {
                    matchIndexes[mi] = xi;
                    matchFlags[xi] = true;
                    matches++;
                    break;
                }
            }
        }
        char[] ms1 = new char[matches];
        char[] ms2 = new char[matches];
        for (int i = 0, si = 0; i < min.length(); i++) {
            if (matchIndexes[i] != -1) {
                ms1[si] = min.charAt(i);
                si++;
            }
        }
        for (int i = 0, si = 0; i < max.length(); i++) {
            if (matchFlags[i]) {
                ms2[si] = max.charAt(i);
                si++;
            }
        }
        int transpositions = 0;
        for (int mi = 0; mi < ms1.length; mi++) {
            if (ms1[mi] != ms2[mi]) {
                transpositions++;
            }
        }
        int prefix = 0;
        for (int mi = 0; mi < min.length(); mi++) {
            if (s1.charAt(mi) == s2.charAt(mi)) {
                prefix++;
            } else {
                break;
            }
        }
        return new int[]{matches, transpositions / 2, prefix, max.length()};
    }
}
