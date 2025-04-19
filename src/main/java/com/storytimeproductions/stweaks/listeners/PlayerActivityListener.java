package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import com.storytimeproductions.stweaks.util.BossBarManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player movement and connection events to track activity and manage UI elements.
 *
 * <p>This listener updates the player's AFK status when they move and manages the display of
 * BossBars when players join or leave the server.
 */
public class PlayerActivityListener implements Listener {

  /**
   * Handles the player movement event.
   *
   * <p>This method is triggered whenever a player moves in the game. It marks the player as active
   * by updating their AFK status to {@code false} in the playtime tracker.
   *
   * @param event The {@link PlayerMoveEvent} containing movement information.
   */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    PlaytimeTracker.setAfk(event.getPlayer().getUniqueId(), false);
  }

  /**
   * Handles the player join event.
   *
   * <p>This method is triggered when a player joins the server. It updates and displays the
   * player's BossBar to show their remaining required playtime for the day.
   *
   * @param event The {@link PlayerJoinEvent} containing the joining player's information.
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    BossBarManager.updateBossBar(event.getPlayer());
  }

  /**
   * Handles the player quit event.
   *
   * <p>This method is triggered when a player leaves the server. It removes the player's BossBar to
   * clean up resources and avoid memory leaks.
   *
   * @param event The {@link PlayerQuitEvent} containing the quitting player's information.
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    BossBarManager.removeBossBar(event.getPlayer());
  }
}
