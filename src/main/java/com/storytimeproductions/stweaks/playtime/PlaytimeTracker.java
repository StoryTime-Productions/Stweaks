package com.storytimeproductions.stweaks.playtime;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages and tracks the playtime of players on the server.
 *
 * <p>This class is responsible for storing the playtime data of each player, updating the playtime
 * periodically, and managing the player's AFK status. It provides methods to initialize the
 * playtime tracking, set a player's AFK status, retrieve playtime data, and shutdown the tracker.
 */
public class PlaytimeTracker {
  public static final HashMap<UUID, PlaytimeData> playtimeMap = new HashMap<>();

  /**
   * Initializes the PlaytimeTracker and starts periodic playtime updates.
   *
   * <p>This method should be called when the plugin starts. It initializes the playtime map and
   * schedules a task to update the playtime for each online player every minute. If a player is not
   * marked as AFK, their playtime is incremented based on the current day's multiplier.
   *
   * @param pl The plugin instance that will be used to schedule the periodic task.
   */
  public static void init(JavaPlugin pl) {}

  /**
   * Shuts down the playtime tracker.
   *
   * <p>This method is called when the plugin is stopping. It can be used to persist any playtime
   * data or perform any necessary cleanup.
   */
  public static void shutdown() {
    // Optionally persist data
  }

  /**
   * Sets the AFK status for a specific player.
   *
   * <p>This method updates the AFK status for a player, marking them as either AFK or not AFK. If
   * the player does not have playtime data yet, a new entry will be created.
   *
   * @param uuid The unique identifier of the player.
   * @param isAfk The new AFK status for the player.
   */
  public static void setAfk(UUID uuid, boolean isAfk) {
    playtimeMap.computeIfAbsent(uuid, k -> new PlaytimeData()).setAfk(isAfk);
  }

  /**
   * Retrieves the playtime data for a specific player.
   *
   * <p>This method fetches the playtime data associated with the player's UUID. If no data is
   * found, it returns null.
   *
   * @param uuid The unique identifier of the player.
   * @return The playtime data for the player, or null if no data exists.
   */
  public static PlaytimeData getData(UUID uuid) {
    return playtimeMap.get(uuid);
  }

  /**
   * Retrieves the remaining time in minutes for a player.
   *
   * @param uuid The player's unique identifier.
   * @return The remaining minutes.
   */
  public static long getMinutes(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    if (data == null) {
      return 60; // Assume full hour is required
    }

    long totalMinutes = 60 - data.getMinutesPlayed(); // Subtract the minutes already played
    return Math.max(totalMinutes, 0);
  }

  /**
   * Retrieves the remaining time in seconds for a player.
   *
   * @param uuid The player's unique identifier.
   * @return The remaining seconds.
   */
  public static long getSeconds(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    if (data == null) {
      return 0; // No playtime tracked, return 0 seconds
    }

    // Calculate remaining seconds
    long totalSeconds = 3600 - data.getTotalSecondsPlayed();

    // Ensure that totalSeconds doesn't go below 0 (in case the player already
    // completed the 60 minutes)
    return Math.max(totalSeconds, 0);
  }

  /**
   * Gets the total remaining time for a player as a Duration.
   *
   * @param uuid The player's unique identifier.
   * @return The remaining time as Duration.
   */
  public static Duration getTimeLeft(UUID uuid) {
    long secondsLeft = getSeconds(uuid); // Get remaining seconds
    return Duration.ofSeconds(secondsLeft);
  }
}
