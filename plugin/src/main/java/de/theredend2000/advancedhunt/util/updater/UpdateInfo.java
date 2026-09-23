package de.theredend2000.advancedhunt.util.updater;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a remote update candidate.
 *
 * @param version       A cleaned version string used for comparison and file naming.
 * @param sourceVersion The source-specific version identifier used for downloads.
 * @param publishedAt   The release publish time.
 */
public final class UpdateInfo {
	private final String version;
	private final String sourceVersion;
	private final Instant publishedAt;
	private final UpdateChannel channel;

	public UpdateInfo(String version, String sourceVersion, Instant publishedAt) {
		this(version, sourceVersion, publishedAt, UpdateChannel.RELEASE);
	}

	public UpdateInfo(String version, String sourceVersion, Instant publishedAt, UpdateChannel channel) {
		this.version = version;
		this.sourceVersion = sourceVersion;
		this.publishedAt = publishedAt;
		this.channel = channel != null ? channel : UpdateChannel.RELEASE;
	}

	public String version() {
		return version;
	}

	public String sourceVersion() {
		return sourceVersion;
	}

	public Instant publishedAt() {
		return publishedAt;
	}

	public UpdateChannel channel() {
		return channel;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof UpdateInfo)) return false;
		UpdateInfo that = (UpdateInfo) o;
		return Objects.equals(version, that.version)
				&& Objects.equals(sourceVersion, that.sourceVersion)
				&& Objects.equals(publishedAt, that.publishedAt)
				&& channel == that.channel;
	}

	@Override
	public int hashCode() {
		return Objects.hash(version, sourceVersion, publishedAt, channel);
	}

	@Override
	public String toString() {
		return "UpdateInfo{" +
				"version='" + version + '\'' +
				", sourceVersion='" + sourceVersion + '\'' +
				", publishedAt=" + publishedAt +
				", channel=" + channel +
				'}';
	}
}
