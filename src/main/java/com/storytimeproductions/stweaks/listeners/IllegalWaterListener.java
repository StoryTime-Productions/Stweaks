package com.storytimeproductions.stweaks.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles the placement and interaction with the custom Illegal Water item.
 *
 * <p>This listener responds to player interactions with a custom item tagged as {@code
 * storytime:illegal_water}, which causes a non-spreading flowing water block to be placed above the
 * clicked block. It also ensures that the block stays as flowing water, plays a splash sound when
 * placed, and consumes the item when used.
 */
public class IllegalWaterListener implements Listener {

  private final JavaPlugin plugin;

  /**
   * Constructor for the IllegalWaterListener class.
   *
   * @param plugin the plugin instance used for scheduling tasks
   */
  public IllegalWaterListener(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Called when a player interacts with a block.
   *
   * <p>If the player is holding the Illegal Water item and right-clicks a block, a flowing water
   * block (level=1) is placed above the clicked block, and a splash sound is played. The block is
   * refreshed every 2 seconds to prevent it from decaying.
   *
   * <p>The item is consumed if the player is not in creative mode.
   *
   * @param event the player interaction event
   */
  @EventHandler
  public void onPlaceIllegalWater(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Player player = event.getPlayer();
    ItemStack item = player.getInventory().getItemInMainHand();

    if (!isIllegalWater(item)) {
      return;
    }

    Block targetBlock = event.getClickedBlock().getRelative(event.getBlockFace());

    if (!targetBlock.getType().isAir()) {
      return;
    }

    // Set the block to a flowing water block with level 1
    BlockData data = Bukkit.createBlockData("minecraft:water[level=0]");
    targetBlock.setBlockData(data, false);

    // Refresh the water block every 2 seconds to prevent it from decaying
    new BukkitRunnable() {
      @Override
      public void run() {
        if (targetBlock.getType() == Material.WATER) {
          BlockData current = targetBlock.getBlockData();
          if (current instanceof Levelled && ((Levelled) current).getLevel() != 1) {
            targetBlock.setBlockData(Bukkit.createBlockData("minecraft:water[level=0]"), false);
          }
        } else {
          cancel(); // Stop if block is no longer water
        }
      }
    }.runTaskTimer(plugin, 40L, 40L); // Delay 2 seconds, repeat every 2 seconds

    // Play a splash sound at the location where the water is placed
    player.playSound(targetBlock.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.0f);

    // Consume one item unless in creative mode
    if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
      item.setAmount(item.getAmount() - 1);
    }

    event.setCancelled(true); // Prevent the default item placement behavior
  }

  /**
   * Checks whether the given item is the custom Illegal Water item.
   *
   * @param item the item to check
   * @return true if the item has the custom model tag {@code storytime:illegal_water}, false
   *     otherwise
   */
  private boolean isIllegalWater(ItemStack item) {
    if (item == null || item.getType() != Material.BLUE_WOOL) {
      return false;
    }

    if (!item.hasItemMeta()) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();
    NamespacedKey key = meta.getItemModel();

    return key != null && key.equals(new NamespacedKey("storytime", "illegal_water"));
  }
}
