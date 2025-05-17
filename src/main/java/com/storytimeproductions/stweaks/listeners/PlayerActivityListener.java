package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.stweaks.config.SettingsManager;
import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import com.storytimeproductions.stweaks.util.BossBarManager;
import com.storytimeproductions.stweaks.util.TablistManager;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listens for player movement and connection events to track activity and manage UI elements.
 *
 * <p>This listener also performs AFK detection based on player movement. Players who remain
 * inactive for a configured threshold (5 minutes) are marked as AFK in the {@link PlaytimeTracker}.
 */
public class PlayerActivityListener implements Listener {

  /** Stores the last recorded movement timestamp (in milliseconds) for each player. */
  private static final HashMap<UUID, Long> lastMovement = new HashMap<>();

  /** Threshold (in milliseconds) after which a player is considered AFK (default: 1 minutes). */
  private static final long AFK_THRESHOLD_MILLIS = 1 * 60 * 1000;

  /**
   * Constructs a new {@code PlayerActivityListener} and starts a repeating task to check AFK
   * statuses.
   *
   * @param plugin The main plugin instance used to schedule tasks.
   */
  public PlayerActivityListener(JavaPlugin plugin) {
    // Start the periodic AFK checker
    new BukkitRunnable() {
      @Override
      public void run() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
          TablistManager.updateTablist(player, SettingsManager.getWeekendMultiplier());
          UUID uuid = player.getUniqueId();
          long lastActive = lastMovement.getOrDefault(uuid, now);
          boolean afk = (now - lastActive) > AFK_THRESHOLD_MILLIS;
          PlaytimeTracker.setAfk(uuid, afk);
        }
      }
    }.runTaskTimer(plugin, 0L, 20); // Check every 30 seconds
  }

  /**
   * Handles the player movement event.
   *
   * <p>This method updates the last movement timestamp for the player and marks them as not AFK.
   *
   * @param event The {@link PlayerMoveEvent} containing movement information.
   */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    lastMovement.put(uuid, System.currentTimeMillis());
    PlaytimeTracker.setAfk(uuid, false); // Reset AFK on movement
  }

  /**
   * Handles the player join event.
   *
   * <p>Initializes the player's movement timestamp, updates the BossBar and tablist UI, and makes
   * the player execute the /lobby command upon joining the server.
   *
   * @param event The {@link PlayerJoinEvent} containing the joining player's information.
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    lastMovement.put(uuid, System.currentTimeMillis()); // Initialize last movement
    BossBarManager.updateBossBar(player);
    TablistManager.updateTablist(player, SettingsManager.getWeekendMultiplier());

    // Make the player execute /lobby on join
    Bukkit.getScheduler()
        .runTaskLater(
            Bukkit.getPluginManager().getPlugin("Stweaks"),
            () -> player.performCommand("lobby"),
            10L);
  }

  /**
   * Handles the player quit event.
   *
   * <p>Removes the player's last movement entry and cleans up their BossBar UI element.
   *
   * @param event The {@link PlayerQuitEvent} containing the quitting player's information.
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    lastMovement.remove(uuid); // Clean up
    BossBarManager.removeBossBar(event.getPlayer());
  }
}
