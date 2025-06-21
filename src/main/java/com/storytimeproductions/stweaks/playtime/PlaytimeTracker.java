package com.storytimeproductions.stweaks.playtime;

import com.storytimeproductions.stweaks.config.SettingsManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        ZoneId easternZone = ZoneId.of("America/New_York");
        ZonedDateTime nowEastern = ZonedDateTime.now(easternZone);
        int currentHour = nowEastern.getHour();

        for (UUID uuid : playtimeMap.keySet()) {
          PlaytimeData data = playtimeMap.get(uuid);

          Integer lastHourChecked = data.getLastHourChecked();
          if (lastHourChecked == null) {
            data.setLastHourChecked(currentHour);
            continue;
          }

          // If we have crossed from before 3 AM to 4 AM or later
          if (lastHourChecked < 3 && currentHour >= 3) {
            if (data.getAvailableSeconds() <= 3600) {
              data.setAvailableSeconds(3600);
            }
          }

          data.setLastHourChecked(currentHour);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
          PlaytimeData playerData =
              playtimeMap.computeIfAbsent(player.getUniqueId(), k -> new PlaytimeData());
          if (!player.getWorld().getName().startsWith("world")) {
            continue;
          }
          double secondsToRemove = 1.0 / Math.max(1.0, getTotalMultiplier());
          if (!playerData.isAfk()) {
            playerData.addAvailableSeconds(-secondsToRemove);
          } else {
            Long afkStart = playerData.getAfkSince();
            if (afkStart != null && System.currentTimeMillis() - afkStart <= 3 * 60 * 1000) {
              playerData.addAvailableSeconds(-secondsToRemove);
            } else {
              if (playerData.isKickOnAfkTimeout()) {
                player.kick(
                    Component.text(
                        "You have been kicked for being AFK too long.", NamedTextColor.RED));
              } else {
                playerData.addAvailableSeconds(-secondsToRemove);
              }
            }
          }
        }
      }
    }.runTaskTimer(plugin, 0L, 20L);
  }

  // --- Multiplier Getters using SettingsManager ---
  public static double getBaseMultiplier() {
    return SettingsManager.getBaseMultiplier();
  }

  public static double getWeekendMultiplier() {
    return SettingsManager.getWeekendMultiplier();
  }

  public static double getSocialMultiplier() {
    return SettingsManager.getSocialMultiplier();
  }

  public static double getSocialDistance() {
    return SettingsManager.getSocialDistance();
  }

  /**
   * Computes the social multiplier as the socialMultiplier times the weighted average of all party
   * sizes, with more weight attributed to larger parties.
   */
  public static double computeGlobalSocialMultiplier() {
    List<Player> players = new ArrayList<>();
    for (Player p : Bukkit.getOnlinePlayers()) {
      String worldName = p.getWorld().getName();
      if (worldName.startsWith("world")) {
        players.add(p);
      }
    }

    // Union-find to find parties
    int n = players.size();
    if (n == 1) {
      return 0.0;
    }

    int[] parent = new int[n];
    for (int i = 0; i < n; i++) {
      parent[i] = i;
    }

    double distance = getSocialDistance();
    for (int i = 0; i < n; i++) {
      Player p1 = players.get(i);
      for (int j = i + 1; j < n; j++) {
        Player p2 = players.get(j);
        if (p1.getWorld().equals(p2.getWorld())
            && p1.getLocation().distance(p2.getLocation()) <= distance) {
          int rootI = find(parent, i);
          int rootJ = find(parent, j);
          if (rootI != rootJ) {
            parent[rootJ] = rootI;
          }
        }
      }
    }

    // Count party sizes
    Map<Integer, Integer> partySizes = new HashMap<>();
    for (int i = 0; i < n; i++) {
      int root = find(parent, i);
      partySizes.put(root, partySizes.getOrDefault(root, 0) + 1);
    }

    // Weighted average: weight = size^2 (larger parties have more influence)
    double weightedSum = 0.0;
    double totalWeight = 0.0;
    for (int size : partySizes.values()) {
      double weight = size * size;
      weightedSum += size * weight;
      totalWeight += weight;
    }

    double weightedAverage = totalWeight > 0 ? (weightedSum / totalWeight) : 0.0;
    double socialMultiplier = getSocialMultiplier();
    double result = socialMultiplier * weightedAverage;
    result = Math.floor(result * 100) / 100.0;

    return result;
  }

  // Helper for union-find
  private static int find(int[] parent, int i) {
    if (parent[i] != i) {
      parent[i] = find(parent, parent[i]);
    }
    return parent[i];
  }

  /** Computes the total multiplier for a player. Includes base, weekend, and social multipliers. */
  public static double getTotalMultiplier() {
    double total = getBaseMultiplier();
    if (isWeekend()) {
      total *= getWeekendMultiplier();
    }
    total += computeGlobalSocialMultiplier();
    return Math.floor(total * 100) / 100.0;
  }

  /** Checks if it is currently the weekend in Eastern Time. */
  public static boolean isWeekend() {
    ZoneId easternZone = ZoneId.of("America/New_York");
    ZonedDateTime nowEastern = ZonedDateTime.now(easternZone);
    int day = nowEastern.getDayOfWeek().getValue();
    return day == 5 || day == 6 || day == 7; // Friday or Saturday or Sunday
  }

  /**
   * Resets the recorded available seconds for a specific player.
   *
   * <p>This method retrieves the {@link PlaytimeData} associated with the given player's UUID and,
   * if available, resets their tracked available seconds back to zero. It has no effect if no
   * playtime data is currently stored for the player.
   *
   * @param playerUuid The UUID of the player whose available seconds should be reset.
   */
  public static void resetPlaytime(UUID playerUuid) {
    PlaytimeData data = getData(playerUuid);
    if (data != null) {
      data.reset();
    }
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
   * @return The playtime data for the player, or a new PlaytimeData if none exists.
   */
  public static PlaytimeData getData(UUID uuid) {
    return playtimeMap.computeIfAbsent(uuid, k -> new PlaytimeData());
  }

  /**
   * Retrieves the remaining time in minutes for a player.
   *
   * @param uuid The player's unique identifier.
   * @return The remaining minutes, or 60 if the player has no tracked data.
   */
  public static double getMinutes(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    if (data == null) {
      return 60;
    }
    double totalMinutes = data.getAvailableSeconds() / 60;
    return Math.max(totalMinutes, 0);
  }

  /**
   * Retrieves the remaining time in seconds for a player.
   *
   * @param uuid The player's unique identifier.
   * @return The remaining seconds, or 0 if no time is required.
   */
  public static double getSeconds(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    if (data == null) {
      return 3600;
    }
    double totalSeconds = data.getAvailableSeconds();
    return Math.max(totalSeconds, 0);
  }

  /**
   * Gets the total remaining time for a player as a {@link Duration}.
   *
   * @param uuid The player's unique identifier.
   * @return A Duration representing how much time is left.
   */
  public static Duration getTimeLeft(UUID uuid) {
    double secondsLeft = getSeconds(uuid);
    return Duration.ofSeconds((long) secondsLeft);
  }

  /**
   * Sets or replaces the current playtime data for a player.
   *
   * @param uuid The player's unique identifier.
   * @param data The playtime data to associate with the player.
   */
  public static void setPlaytime(UUID uuid, PlaytimeData data) {
    playtimeMap.put(uuid, data);
  }

  /**
   * Gets the total available time in seconds for a player.
   *
   * @param uuid The player's unique identifier.
   * @return Total available time in seconds, or 0 if not found.
   */
  public static double getPlaytime(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    return data != null ? data.getAvailableSeconds() : 0L;
  }

  /**
   * Loads player playtime data from the SQLite database into memory.
   *
   * @param conn A valid database connection.
   */
  public static void loadFromDatabase(Connection conn) {
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM playtime")) {
      while (rs.next()) {
        long availableSeconds = rs.getLong("available_seconds");
        int bankedTickets = 0;
        try {
          bankedTickets = rs.getInt("banked_tickets");
        } catch (SQLException ex) {
          ex.printStackTrace();
        }
        PlaytimeData data = new PlaytimeData(availableSeconds);
        data.setBankedTickets(bankedTickets);

        UUID uuid = UUID.fromString(rs.getString("uuid"));
        playtimeMap.put(uuid, data);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Saves the in-memory playtime data to the SQLite database.
   *
   * <p>Uses upsert logic: if the player's UUID already exists, the record is updated; otherwise, a
   * new row is inserted.
   *
   * @param conn A valid database connection.
   */
  public static void saveToDatabase(Connection conn) {
    String sql =
        """
        INSERT INTO playtime (uuid, available_seconds, banked_tickets)
        VALUES (?, ?, ?)
        ON CONFLICT(uuid) DO UPDATE SET
          available_seconds = excluded.available_seconds,
          banked_tickets = excluded.banked_tickets
        """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      for (var entry : playtimeMap.entrySet()) {
        ps.setString(1, entry.getKey().toString());
        ps.setDouble(2, entry.getValue().getAvailableSeconds());
        ps.setInt(3, entry.getValue().getBankedTickets()); // <-- fix index from 4 to 3
        ps.addBatch();
      }
      ps.executeBatch();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
