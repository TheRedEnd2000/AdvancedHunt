package de.theredend2000.advancedhunt.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipBackupUtil {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    
    /** Maximum file size allowed per file (100MB) to prevent OOM */
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    private ZipBackupUtil() {
    }

    public static File createZipBackup(File sourceDir, File backupsDir, String prefix) throws IOException {
        if (sourceDir == null) {
            throw new IllegalArgumentException("sourceDir is null");
        }
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IOException("Source directory does not exist or is not a directory: " + sourceDir);
        }
        if (backupsDir == null) {
            backupsDir = new File(sourceDir, "backups");
        }
        if (!backupsDir.exists() && !backupsDir.mkdirs()) {
            throw new IOException("Failed to create backups directory: " + backupsDir);
        }

        String ts = LocalDateTime.now().format(TS);
        String safePrefix = (prefix == null || prefix.trim().isEmpty()) ? "backup" : prefix.trim();
        File outFile = new File(backupsDir, safePrefix + "-" + ts + ".zip");

        Path base = sourceDir.toPath().toAbsolutePath().normalize();
        Path outPath = outFile.toPath().toAbsolutePath().normalize();
        Path backupsPath = backupsDir.toPath().toAbsolutePath().normalize();

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
             Stream<Path> walk = Files.walk(base)) {
            
            walk.filter(p -> !Files.isDirectory(p))
                .forEach(p -> {
                    Path absPath = p.toAbsolutePath().normalize();
                    
                    // Security: Verify path is still under base directory (symlink protection)
                    if (!absPath.startsWith(base)) {
                        return;
                    }
                    
                    // Avoid including the output zip itself.
                    if (absPath.equals(outPath)) {
                        return;
                    }
                    
                    // Avoid including any backups (prevents backup nesting).
                    if (absPath.startsWith(backupsPath)) {
                        return;
                    }

                    try {
                        // File size validation to prevent OOM
                        long fileSize = Files.size(absPath);
                        if (fileSize > MAX_FILE_SIZE) {
                            return; // Skip files larger than 100MB
                        }
                        
                        String rel = base.relativize(absPath).toString().replace('\\', '/');
                        zos.putNextEntry(new ZipEntry(rel));
                        try (InputStream in = Files.newInputStream(absPath)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                zos.write(buffer, 0, read);
                            }
                        }
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (UncheckedIOException wrapped) {
            throw wrapped.getCause();
        }

        return outFile;
    }
}
