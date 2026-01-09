package de.theredend2000.advancedhunt.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class PlayerSnapshot {
	private final Player player;
	private final Location location;

	public PlayerSnapshot(Player player, Location location) {
		this.player = player;
		this.location = location;
	}

	public Player player() {
		return player;
	}

	public Location location() {
		return location;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PlayerSnapshot)) return false;
		PlayerSnapshot that = (PlayerSnapshot) o;
		return Objects.equals(player, that.player) && Objects.equals(location, that.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(player, location);
	}

	@Override
	public String toString() {
		return "PlayerSnapshot{" +
				"player=" + player +
				", location=" + location +
				'}';
	}
}
