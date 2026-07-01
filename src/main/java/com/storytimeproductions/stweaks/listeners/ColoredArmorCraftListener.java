package com.storytimeproductions.stweaks.listeners;

import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Prevents already-colored armor pieces from being used as ingredients in the colored armor
 * crafting recipes, which would otherwise allow infinite re-dyeing.
 */
public class ColoredArmorCraftListener implements Listener {

  private static final Set<String> COLORED_ARMOR_MODELS =
      Set.of(
          "storytime:blue_helmet", "storytime:blue_chestplate",
          "storytime:blue_leggings", "storytime:blue_boots",
          "storytime:red_helmet", "storytime:red_chestplate",
          "storytime:red_leggings", "storytime:red_boots",
          "storytime:yellow_helmet", "storytime:yellow_chestplate",
          "storytime:yellow_leggings", "storytime:yellow_boots",
          "storytime:green_helmet", "storytime:green_chestplate",
          "storytime:green_leggings", "storytime:green_boots",
          "storytime:pink_helmet", "storytime:pink_chestplate",
          "storytime:pink_leggings", "storytime:pink_boots");

  /**
   * Cancels the craft if the result is a colored armor piece but an ingredient already carries a
   * custom item model, meaning it is itself a colored piece being recycled.
   *
   * @param event the craft event
   */
  @EventHandler
  public void onCraft(CraftItemEvent event) {
    ItemStack result = event.getRecipe().getResult();
    if (!result.hasItemMeta()) {
      return;
    }

    NamespacedKey resultModel = result.getItemMeta().getItemModel();
    if (resultModel == null || !COLORED_ARMOR_MODELS.contains(resultModel.toString())) {
      return;
    }

    for (ItemStack ingredient : event.getInventory().getMatrix()) {
      if (ingredient == null || !ingredient.hasItemMeta()) {
        continue;
      }
      ItemMeta meta = ingredient.getItemMeta();
      if (meta.getItemModel() != null) {
        event.setCancelled(true);
        return;
      }
    }
  }
}
