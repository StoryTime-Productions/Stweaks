package com.storytimeproductions.stweaks.playtime;

import com.storytimeproductions.stweaks.util.TimeUtils;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages and tracks the playtime of players on the server.
 *
 * <p>This class is responsible for storing the playtime data of each player, updating the playtime
 * periodically, and managing the player's AFK status. It provides methods to initialize the
 * playtime tracking, set a player's AFK status, retrieve playtime data, and shutdown the tracker.
 */
public class PlaytimeTracker {
  private static final HashMap<UUID, PlaytimeData> playtimeMap = new HashMap<>();
  private static JavaPlugin plugin;

  /**
   * Initializes the PlaytimeTracker and starts periodic playtime updates.
   *
   * <p>This method should be called when the plugin starts. It initializes the playtime map and
   * schedules a task to update the playtime for each online player every minute. If a player is not
   * marked as AFK, their playtime is incremented based on the current day's multiplier.
   *
   * @param pl The plugin instance that will be used to schedule the periodic task.
   */
  public static void init(JavaPlugin pl) {
    plugin = pl;

    new BukkitRunnable() {
      @Override
      public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
          PlaytimeData data =
              playtimeMap.computeIfAbsent(player.getUniqueId(), k -> new PlaytimeData());
          if (!data.isAfk()) {
            data.addMinute(TimeUtils.getTodayMultiplier());
          }
        }
      }
    }.runTaskTimer(plugin, 0L, 1200L); // 1 min
  }

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
}
