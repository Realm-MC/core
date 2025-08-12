package br.com.realmmc.core.utils;

import org.apache.commons.text.similarity.LevenshteinDistance;

public final class SimilarityChecker {

    private static final double SIMILARITY_THRESHOLD = 0.80;

    private SimilarityChecker() {}

    public static boolean isSimilar(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return false;
        }
        double similarity = calculateSimilarity(str1, str2);
        return similarity >= SIMILARITY_THRESHOLD;
    }

    private static double calculateSimilarity(String str1, String str2) {
        String longer = str1.length() > str2.length() ? str1 : str2;
        String shorter = str1.length() > str2.length() ? str2 : str1;

        if (longer.isEmpty()) {
            return 1.0;
        }

        int distance = LevenshteinDistance.getDefaultInstance().apply(longer, shorter);
        return (longer.length() - distance) / (double) longer.length();
    }
}