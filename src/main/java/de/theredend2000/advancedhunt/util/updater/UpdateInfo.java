package de.theredend2000.advancedhunt.util.updater;

import java.time.Instant;

/**
 * Represents a remote update candidate.
 *
 * @param version       A cleaned version string used for comparison and file naming.
 * @param sourceVersion The source-specific version identifier used for downloads.
 * @param publishedAt   The release publish time.
 */
public record UpdateInfo(String version, String sourceVersion, Instant publishedAt) {
}
