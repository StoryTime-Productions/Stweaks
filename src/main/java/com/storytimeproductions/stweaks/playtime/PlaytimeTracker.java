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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // Use America/New_York for Eastern Time
        ZoneId easternZone = ZoneId.of("America/New_York");
        ZonedDateTime nowEastern = ZonedDateTime.now(easternZone);
        LocalDate today = nowEastern.toLocalDate();
        int currentHour = nowEastern.getHour();

        for (UUID uuid : playtimeMap.keySet()) {
          PlaytimeData data = playtimeMap.get(uuid);

          // Grant 1 hour at 1 AM Eastern if not already granted today
          if (currentHour >= 1) {
            LocalDate lastGrant = data.getLastHourGrantDate();
            if (lastGrant == null || !lastGrant.isEqual(today)) {
              data.addAvailableSeconds(3600, false);
              data.setLastHourGrantDate(today);
            }
          }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
          PlaytimeData playerData =
              playtimeMap.computeIfAbsent(player.getUniqueId(), k -> new PlaytimeData());
          if (!player.getWorld().getName().startsWith("world")) {
            continue;
          }
          if (!playerData.isAfk()) {
            playerData.addAvailableSeconds(-1, false);
          } else {
            Long afkStart = playerData.getAfkSince();
            if (afkStart != null && System.currentTimeMillis() - afkStart <= 3 * 60 * 1000) {
              playerData.addAvailableSeconds(-1, false);
            }
          }
        }
      }
    }.runTaskTimer(plugin, 0L, 20L * (long) getTotalMultiplier());
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
   * Computes the social multiplier using dynamic "party" regions. Each party is a group of players
   * where every member is within the social distance of at least one other member. For each party,
   * the multiplier is raised to the power of (partySize * (partySize - 1) / 2). The final global
   * multiplier is the base raised to the sum of all party exponents.
   */
  public static double computeGlobalSocialMultiplier() {
    double distance = getSocialDistance();
    // Only include players in the main worlds
    List<Player> players = new ArrayList<>();
    for (Player p : Bukkit.getOnlinePlayers()) {
      String worldName = p.getWorld().getName();
      if (worldName.startsWith("world")) {
        players.add(p);
      }
    }

    // Find parties using union-find (disjoint set)
    int n = players.size();
    int[] parent = new int[n];
    for (int i = 0; i < n; i++) {
      parent[i] = i;
    }

    // Union players that are within distance
    for (int i = 0; i < n; i++) {
      Player p1 = players.get(i);
      for (int j = i + 1; j < n; j++) {
        Player p2 = players.get(j);
        if (p1.getWorld().equals(p2.getWorld())
            && p1.getLocation().distance(p2.getLocation()) <= distance) {
          // Union i and j
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

    // Calculate the total exponent sum
    int totalExponent = 0;
    for (int size : partySizes.values()) {
      if (size > 1) {
        totalExponent += (size * (size - 1)) / 2;
      }
    }

    // Compute the final multiplier
    double socialMultiplier = getSocialMultiplier();
    double result = Math.pow(socialMultiplier, totalExponent);
    result = Math.floor(result * 100) / 100.0;

    return result == 1 ? 0 : result;
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
      total += getWeekendMultiplier();
    }
    total += computeGlobalSocialMultiplier();
    return Math.floor(total * 100) / 100.0;
  }

  /** Checks if it is currently the weekend (Saturday or Sunday) in Eastern Time. */
  public static boolean isWeekend() {
    ZoneId easternZone = ZoneId.of("America/New_York");
    ZonedDateTime nowEastern = ZonedDateTime.now(easternZone);
    int day = nowEastern.getDayOfWeek().getValue();
    return day == 6 || day == 7; // Saturday or Sunday
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
   * @return The playtime data for the player, or null if no data exists.
   */
  public static PlaytimeData getData(UUID uuid) {
    return playtimeMap.get(uuid);
  }

  /**
   * Retrieves the remaining time in minutes for a player.
   *
   * @param uuid The player's unique identifier.
   * @return The remaining minutes, or 60 if the player has no tracked data.
   */
  public static long getMinutes(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    if (data == null) {
      return 60;
    }
    long totalMinutes = data.getAvailableSeconds() / 60;
    return Math.max(totalMinutes, 0);
  }

  /**
   * Retrieves the remaining time in seconds for a player.
   *
   * @param uuid The player's unique identifier.
   * @return The remaining seconds, or 0 if no time is required.
   */
  public static long getSeconds(UUID uuid) {
    PlaytimeData data = playtimeMap.get(uuid);
    if (data == null) {
      return 3600;
    }
    long totalSeconds = data.getAvailableSeconds();
    return Math.max(totalSeconds, 0);
  }

  /**
   * Gets the total remaining time for a player as a {@link Duration}.
   *
   * @param uuid The player's unique identifier.
   * @return A Duration representing how much time is left.
   */
  public static Duration getTimeLeft(UUID uuid) {
    long secondsLeft = getSeconds(uuid);
    return Duration.ofSeconds(secondsLeft);
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
  public static long getPlaytime(UUID uuid) {
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
        LocalDate lastHourGrant = null;
        Date grantDateSql = rs.getDate("last_hour_grant");
        if (grantDateSql != null) {
          lastHourGrant = grantDateSql.toLocalDate();
        }
        PlaytimeData data = new PlaytimeData(availableSeconds);
        data.setLastHourGrantDate(lastHourGrant);
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
        INSERT INTO playtime (uuid, available_seconds, last_hour_grant, banked_tickets)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(uuid) DO UPDATE SET
          available_seconds = excluded.available_seconds,
          last_hour_grant = excluded.last_hour_grant,
          banked_tickets = excluded.banked_tickets
        """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      for (var entry : playtimeMap.entrySet()) {
        ps.setString(1, entry.getKey().toString());
        ps.setLong(2, entry.getValue().getAvailableSeconds());
        LocalDate grantDate = entry.getValue().getLastHourGrantDate();
        if (grantDate != null) {
          ps.setDate(3, Date.valueOf(grantDate));
        } else {
          ps.setNull(3, java.sql.Types.DATE);
        }
        ps.setInt(4, entry.getValue().getBankedTickets());
        ps.addBatch();
      }
      ps.executeBatch();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
