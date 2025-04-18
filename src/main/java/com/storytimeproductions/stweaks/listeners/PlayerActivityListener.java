package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listens for player movement events and tracks player activity.
 *
 * <p>This listener handles the event when a player moves, marking them as active by updating the
 * AFK status in the playtime tracker.
 */
public class PlayerActivityListener implements Listener {

  /**
   * Handles the player move event.
   *
   * <p>This method is triggered whenever a player moves in the game. It updates the player's AFK
   * status, marking them as active by setting their AFK status to false in the playtime tracker.
   *
   * @param event The player move event that contains the player information.
   */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    PlaytimeTracker.setAfk(event.getPlayer().getUniqueId(), false);
  }
}
