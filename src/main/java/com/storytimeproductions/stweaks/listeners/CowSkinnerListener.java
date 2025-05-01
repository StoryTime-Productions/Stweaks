package com.storytimeproductions.stweaks.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles custom crafting and interaction behavior for the Cow Skinner tool.
 *
 * <p>- Automatically tags crafted shears using a specific recipe with custom NBT data. - When used
 * on a cow, drops a random amount of leather (1-3). - Enforces a per-cow, 24-hour cooldown between
 * uses.
 */
public class CowSkinnerListener implements Listener {
  private final Map<String, Long> cowCooldowns = new HashMap<>(); // To track cooldowns per cow
  private static final long COOLDOWN_MILLIS = 24000 * 50;

  /**
   * Handles the event when a player right-clicks an entity.
   *
   * <p>If the entity is a Cow and the player is holding the Cow Skinner, the plugin will drop
   * leather and notify the player—unless the cooldown is still active for that cow.
   *
   * @param event the entity interaction event
   */
  @EventHandler
  public void onCowRightClick(PlayerInteractEntityEvent event) {
    Player player = event.getPlayer();
    if (!(event.getRightClicked() instanceof Cow)) {
      return;
    }

    Cow cow = (Cow) event.getRightClicked();
    String cowId = cow.getUniqueId().toString(); // Use the cow's UUID to track cooldown

    // Check if the player is holding the Cow Skinner
    ItemStack item = player.getInventory().getItemInMainHand();
    if (!isCowSkinner(item)) {
      return;
    }

    // Check if the cow has been skinned recently
    if (!canSkinCow(cowId)) {
      return;
    }

    // Drop leather
    Location loc = cow.getLocation();
    int amount = 1 + new Random().nextInt(3); // Random amount between 1 and 3
    cow.getWorld().dropItemNaturally(loc, new ItemStack(Material.LEATHER, amount));

    player.playSound(loc, Sound.ENTITY_SHEEP_SHEAR, 1.0f, 1.0f);

    // Set the cooldown for this cow
    setCowCooldown(cowId);
  }

  /**
   * Determines whether the provided item is the custom Cow Skinner.
   *
   * <p>This checks that the item is shears and has a custom NBT tag "storytime:type" with the value
   * "cow_skinner".
   *
   * @param item the item to check
   * @return true if the item is a Cow Skinner, false otherwise
   */
  private boolean isCowSkinner(ItemStack item) {
    if (item == null || item.getType() != Material.SHEARS) {
      return false;
    }

    if (!item.hasItemMeta()) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();
    NamespacedKey itemModel = meta.getItemModel();

    if (itemModel == null) {
      return false;
    }

    return new NamespacedKey("storytime", "cow_skinner").equals(itemModel);
  }

  /**
   * Checks if the cow can be skinned (if the cooldown period has passed).
   *
   * @param cowId the unique ID of the cow
   * @return true if the cow can be skinned, false if it's on cooldown
   */
  private boolean canSkinCow(String cowId) {
    long currentTime = System.currentTimeMillis();
    if (!cowCooldowns.containsKey(cowId)) {
      return true; // No cooldown, cow can be skinned
    }

    long lastUsed = cowCooldowns.get(cowId);
    return currentTime - lastUsed >= COOLDOWN_MILLIS; // Check if 24 hours have passed
  }

  /**
   * Sets the cooldown for a specific cow, marking the time it was last skinned.
   *
   * @param cowId the unique ID of the cow
   */
  private void setCowCooldown(String cowId) {
    cowCooldowns.put(cowId, System.currentTimeMillis());
  }
}
