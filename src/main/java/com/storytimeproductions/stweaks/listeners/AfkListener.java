package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.stweaks.config.SettingsManager;
import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener class for handling player AFK (Away From Keyboard) events.
 *
 * <p>This class listens for player activity, including player movements and logins/logouts, to
 * determine if a player is considered AFK. It helps in tracking and enforcing the minimum playtime
 * by detecting inactivity.
 *
 * <p>It listens to the following events:
 *
 * <ul>
 *   <li>PlayerJoinEvent - When a player joins the game.
 *   <li>PlayerQuitEvent - When a player leaves the game.
 *   <li>PlayerMoveEvent - When a player moves in the game world.
 * </ul>
 *
 * <p>The class works in conjunction with the playtime tracking system and may reset or update a
 * player's AFK status based on movement or login activity.
 */
public class AfkListener implements Listener {
  private final HashMap<UUID, Long> lastActivity = new HashMap<>();

  /**
   * Handles when a player joins the server.
   *
   * <p>This method records the player's last activity time and starts a periodic task to check if
   * the player has been AFK for longer than their specified AFK threshold.
   *
   * @param event The PlayerJoinEvent triggered when a player joins the server.
   */
  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    lastActivity.put(player.getUniqueId(), System.currentTimeMillis());

    new BukkitRunnable() {
      @Override
      public void run() {
        if (!player.isOnline()) {
          cancel(); // Cancel task if player is offline
        }

        long now = System.currentTimeMillis();
        long last = lastActivity.getOrDefault(player.getUniqueId(), now);

        // Get the AFK threshold for this specific player
        int afkThresholdSeconds = SettingsManager.getAfkThresholdSeconds(player.getUniqueId());

        // Check if the player has been inactive for longer than the AFK threshold
        if ((now - last) > afkThresholdSeconds * 1000L) {
          PlaytimeTracker.setAfk(player.getUniqueId(), true); // Mark player as AFK
        }
      }
    }.runTaskTimer(Bukkit.getPluginManager().getPlugin("Stweaks"), 0L, 100L);
  }

  /**
   * Updates the last activity time for the player.
   *
   * @param player The player whose last activity time is being updated.
   */
  public void updateLastActivity(Player player) {
    lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
  }
}
