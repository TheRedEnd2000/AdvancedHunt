package de.theredend2000.advancedegghunt.util;

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

        // Compare with the first file
        return newFilename.compareToIgnoreCase(files[0].getName()) < 0;
    }

    /**
     * Checks if filename1 comes before filename2 alphabetically.
     *
     * @param filename1 The first filename
     * @param filename2 The second filename
     * @return true if filename1 comes before filename2 alphabetically, false otherwise
     */
    public static boolean isFileNameFirst(String filename1, String filename2) {
        return filename1.compareToIgnoreCase(filename2) < 0;
    }
}