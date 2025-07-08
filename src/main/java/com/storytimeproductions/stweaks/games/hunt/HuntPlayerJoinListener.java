package com.storytimeproductions.stweaks.games.hunt;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Listener that handles player join events specifically for Hunt game functionality. Ensures that
 * players have their entity size reset when they join the server.
 */
public class HuntPlayerJoinListener implements Listener {

  private final HuntDisguiseManager disguiseManager;
  private final JavaPlugin plugin;

  /**
   * Constructs a new HuntPlayerJoinListener.
   *
   * @param disguiseManager The disguise manager to reset player sizes
   * @param plugin The JavaPlugin instance
   */
  public HuntPlayerJoinListener(HuntDisguiseManager disguiseManager, JavaPlugin plugin) {
    this.disguiseManager = disguiseManager;
    this.plugin = plugin;
  }

  /**
   * Handles player join events to reset entity size.
   *
   * @param event The PlayerJoinEvent
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    // Schedule the size reset with a small delay to ensure the player is fully
    // loaded
    plugin
        .getServer()
        .getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              disguiseManager.resetPlayerSize(player);
              plugin.getLogger().info("Reset entity size for player: " + player.getName());
            },
            20L); // 1 second delay
  }
}
