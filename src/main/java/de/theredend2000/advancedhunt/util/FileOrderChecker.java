package de.theredend2000.advancedhunt.util;

import java.io.File;

public class FileOrderChecker {

    /**
     * Checks if the given filename would be first alphabetically in the specified directory,
     * assuming File.listFiles() returns files in alphabetical order.
     *
     * @param directory The directory to check
     * @param newFilename The filename to check
     * @return true if the new filename would be first alphabetically, false otherwise
     */
    public static boolean isNewFileFirst(File directory, String newFilename) {
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return true; // If directory is empty, new file would be first
        }

        // Check if the file is not in the directory
        boolean fileNotInDirectory = true;
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(newFilename)) {
                fileNotInDirectory = false;
                break;
            }
        }

        // Compare with the first file or if the file is not in the directory
        return fileNotInDirectory || newFilename.compareToIgnoreCase(files[0].getName()) < 0;
    }

    /**
     * Checks if filename1 comes before filename2 alphabetically.
     *
     * @param filename1 The first filename
     * @param filename2 The second filename
     * @return true if filename1 comes before filename2 alphabetically, false otherwise
     */
    public static boolean isFileNameFirst(String filename1, String filename2) {
        String version1 = extractVersion(filename1);
        String version2 = extractVersion(filename2);

        return compareVersions(version1, version2) > 0;
    }

    private static String extractVersion(String filename) {
        int dashIndex = filename.lastIndexOf('-');
        int dotJarIndex = filename.lastIndexOf(".jar");

        if (dashIndex != -1 && dotJarIndex != -1 && dashIndex < dotJarIndex) {
            return filename.substring(dashIndex + 1, dotJarIndex);
        }

        return "";
    }

    private static int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }

        return 0;
    }

}