package com.storytimeproductions.stweaks.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles behavior when the custom FBI Disc item (based on a cookie) is consumed.
 *
 * <p>When a player consumes the FBI Disc:
 *
 * <ul>
 *   <li>They receive SPEED, HASTE, and JUMP BOOST effects for 30 seconds each.
 *   <li>The item is identified using a custom model tag: {@code storytime:fbi_disc}.
 * </ul>
 */
public class FbiDiscListener implements Listener {

  /**
   * Called when a player consumes any item.
   *
   * <p>If the consumed item is identified as the FBI Disc, the player will receive a series of
   * sugar-rush-like potion effects for 30 seconds.
   *
   * @param event the item consumption event
   */
  @EventHandler
  public void onFbiDiscEaten(PlayerItemConsumeEvent event) {
    ItemStack item = event.getItem();

    if (!isFbiDisc(item)) {
      return;
    }

    Player player = event.getPlayer();

    player.addPotionEffect(
        new PotionEffect(PotionEffectType.SPEED, 30 * 20, 1)); // Speed II for 30s
    player.addPotionEffect(
        new PotionEffect(PotionEffectType.HASTE, 30 * 20, 1)); // Haste II for 30s
    player.addPotionEffect(
        new PotionEffect(PotionEffectType.JUMP_BOOST, 30 * 20, 1)); // Jump Boost II for 30s

    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
  }

  /**
   * Checks whether the given item is the custom FBI Disc item.
   *
   * <p>This checks that the item:
   *
   * <ul>
   *   <li>Is a COOKIE
   *   <li>Has metadata
   *   <li>Has a custom item model tag matching {@code storytime:fbi_disc}
   * </ul>
   *
   * @param item the item to check
   * @return true if the item is a valid FBI Disc, false otherwise
   */
  private boolean isFbiDisc(ItemStack item) {
    if (item == null || item.getType() != Material.COOKIE) {
      return false;
    }

    if (!item.hasItemMeta()) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();
    NamespacedKey key = meta.getItemModel();

    return key != null && key.equals(new NamespacedKey("storytime", "fbi_disc"));
  }
}
