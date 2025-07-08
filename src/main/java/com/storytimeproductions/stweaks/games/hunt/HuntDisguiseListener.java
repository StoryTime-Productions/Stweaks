package com.storytimeproductions.stweaks.games.hunt;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Listener for handling disguise-related events in the Hunt game. */
public class HuntDisguiseListener implements Listener {

  private final HuntDisguiseManager disguiseManager;
  private final HuntHologramManager hologramManager;

  /**
   * Constructs a new HuntDisguiseListener.
   *
   * @param disguiseManager The disguise manager instance
   * @param hologramManager The hologram manager instance
   */
  public HuntDisguiseListener(
      HuntDisguiseManager disguiseManager, HuntHologramManager hologramManager) {
    this.disguiseManager = disguiseManager;
    this.hologramManager = hologramManager;
  }

  /**
   * Handles player interactions with entities, specifically left-clicks on disguise armor stands.
   *
   * @param event The interaction event
   */
  @EventHandler
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    // Check if the clicked entity is an armor stand
    if (!(event.getRightClicked() instanceof ArmorStand armorStand)) {
      return;
    }

    // Check if it's a disguise stand
    if (!disguiseManager.isDisguiseStand(armorStand)) {
      return;
    }

    // Cancel the event to prevent any default behavior
    event.setCancelled(true);

    // Handle the disguise interaction
    final Player player = event.getPlayer();
    disguiseManager.handleDisguiseInteraction(player, armorStand, hologramManager);
  }

  /**
   * Handles player right-click interactions for screech sounds when disguised.
   *
   * @param event The interaction event
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    // Check if player is shift-right-clicking
    if (!player.isSneaking()
        || event.getAction() != Action.RIGHT_CLICK_AIR
            && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    // Check if player is disguised
    if (!disguiseManager.isPlayerDisguised(player.getUniqueId())) {
      return;
    }

    // Play character-specific screech sound
    disguiseManager.playCharacterScreech(player);
  }

  /**
   * Removes disguises when a player quits the server.
   *
   * @param event The player quit event
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    disguiseManager.removeDisguise(player);
  }
}
