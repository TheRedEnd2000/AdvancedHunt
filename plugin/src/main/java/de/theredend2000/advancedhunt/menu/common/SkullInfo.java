package de.theredend2000.advancedhunt.menu.common;

/**
 * Simple immutable container for skull rendering data.
 * Exactly one of {@code texture} or {@code ownerName} is expected to be non-null.
 */
public final class SkullInfo {

	private final String texture;
	private final String ownerName;

	public SkullInfo(String texture, String ownerName) {
		this.texture = texture;
		this.ownerName = ownerName;
	}

	public String texture() {
		return texture;
	}

	public String ownerName() {
		return ownerName;
	}
}
