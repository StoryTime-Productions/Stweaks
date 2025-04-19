package com.storytimeproductions.stweaks.playtime;

import com.storytimeproductions.stweaks.config.SettingsManager;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages and tracks the playtime of players on the server.
 *
 * <p>This class stores playtime data for each player, handles AFK status, and provides utility
 * methods to query remaining required playtime. It also supports persistence via SQLite, allowing
 * playtime data to survive server restarts and reset automatically if a new day begins.
 */
public class PlaytimeTracker {
  public static final HashMap<UUID, PlaytimeData> playtimeMap = new HashMap<>();
  private static JavaPlugin plugin;

  /**
   * Initializes the PlaytimeTracker and starts periodic updates for all online players.
   *
   * @param pl The plugin instance.
   */
  public static void init(JavaPlugin pl) {
    plugin = pl;

    new BukkitRunnable() {
      @Override
      public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
          PlaytimeData data =
              PlaytimeTracker.playtimeMap.computeIfAbsent(
                  player.getUniqueId(), k -> new PlaytimeData());
          if (!data.isAfk()) {
            data.addSeconds(1);
          }
        }
      }
    }.runTaskTimer(plugin, 0L, 20L * (long) SettingsManager.getWeekendMultiplier());
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
   * @param uuid The unique identifier of the player.
   * @return The playtime data for the player, or null if no data exists.
   */
  public static PlaytimeData getData(UUID uuid) {
    return playtimeMap.get(uuid);
  }

  /**
   * Retrieves the remaining time in minutes for a player to complete their daily required playtime.
   *
   * @param uuid The player's unique identifier.
   * @return The remaining minutes, or 60 if the player has no tracked data.
   */
  public static long getMinutes(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    if (data == null) {
      return 60;
    }
    long totalMinutes = 60 - data.getMinutesPlayed();
    return Math.max(totalMinutes, 0);
  }

  /**
   * Retrieves the remaining time in seconds for a player to complete their daily required playtime.
   *
   * @param uuid The player's unique identifier.
   * @return The remaining seconds, or 0 if no time is required.
   */
  public static long getSeconds(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    if (data == null) {
      return 0;
    }
    long totalSeconds = 3600 - data.getTotalSecondsPlayed();
    return Math.max(totalSeconds, 0);
  }

  /**
   * Gets the total remaining time for a player as a {@link Duration}.
   *
   * @param uuid The player's unique identifier.
   * @return A Duration representing how much time is left to fulfill daily playtime.
   */
  public static Duration getTimeLeft(UUID uuid) {
    long secondsLeft = getSeconds(uuid);
    return Duration.ofSeconds(secondsLeft);
  }

  /**
   * Sets or replaces the current playtime data for a player.
   *
   * @param uuid The player's unique identifier.
   * @param seconds The playtime data to associate with the player.
   */
  public static void setPlaytime(UUID uuid, PlaytimeData seconds) {
    playtimeMap.put(uuid, seconds);
  }

  /**
   * Gets the total playtime in seconds for a player.
   *
   * @param uuid The player's unique identifier.
   * @return Total playtime in seconds, or 0 if not found.
   */
  public static long getPlaytime(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    return data != null ? data.getTotalSecondsPlayed() : 0L;
  }

  /**
   * Loads player playtime data from the SQLite database into memory.
   *
   * <p>If the stored `updated_last` date is not equal to the current date, the player's playtime is
   * reset to 0.
   *
   * @param conn A valid database connection.
   */
  public static void loadFromDatabase(Connection conn) {
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM playtime")) {

      LocalDate today = LocalDate.now();

      while (rs.next()) {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        double seconds = rs.getDouble("seconds_played");
        LocalDate lastUpdated = rs.getDate("updated_last").toLocalDate();

        if (!lastUpdated.isEqual(today)) {
          playtimeMap.put(uuid, new PlaytimeData(0));
        } else {
          playtimeMap.put(uuid, new PlaytimeData(seconds));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Saves the in-memory playtime data to the SQLite database.
   *
   * <p>Uses upsert logic: if the player's UUID already exists, the record is updated; otherwise, a
   * new row is inserted. The update includes the current date as `updated_last`.
   *
   * @param conn A valid database connection.
   */
  public static void saveToDatabase(Connection conn) {
    String sql =
        """
        INSERT INTO playtime (uuid, seconds_played, updated_last)
        VALUES (?, ?, ?)
        ON CONFLICT(uuid) DO UPDATE SET
          seconds_played = excluded.seconds_played,
          updated_last = excluded.updated_last
        """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      for (var entry : playtimeMap.entrySet()) {
        ps.setString(1, entry.getKey().toString());
        ps.setLong(2, entry.getValue().getTotalSecondsPlayed());
        ps.setDate(3, Date.valueOf(LocalDate.now()));
        ps.addBatch();
      }
      ps.executeBatch();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
