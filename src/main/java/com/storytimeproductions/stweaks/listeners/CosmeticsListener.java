package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.stweaks.commands.CosmeticsMenuCommand;
import com.storytimeproductions.stweaks.util.Cosmetic;
import com.storytimeproductions.stweaks.util.CosmeticsManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener for handling clicks in the Cosmetics Menu GUI. Handles page navigation and prevents item
 * movement within the menu.
 */
public class CosmeticsListener implements Listener {

  private static final String GUI_TITLE = "Cosmetic";

  /**
   * Handles inventory click events for the Cosmetics Menu. Cancels all clicks and processes
   * navigation arrows for page changes.
   *
   * @param e the InventoryClickEvent triggered by the player
   */
  @EventHandler
  public void onInventoryClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player player)) {
      return;
    }

    Inventory clickedInventory = e.getClickedInventory();
    if (clickedInventory == null) {
      return;
    }

    String title = player.getOpenInventory().title().toString();
    if (!title.contains(GUI_TITLE)) {
      return;
    }

    // Cancel default interaction behavior
    e.setCancelled(true);

    ItemStack clickedItem = e.getCurrentItem();
    if (clickedItem == null || !clickedItem.hasItemMeta()) {
      return;
    }

    // Prepare keys
    NamespacedKey prevPageKey = new NamespacedKey("stweaks", "prev_page");
    NamespacedKey nextPageKey = new NamespacedKey("stweaks", "targeted_page_number");

    // Get persistent data container
    PersistentDataContainer data = clickedItem.getItemMeta().getPersistentDataContainer();

    // Handle previous page
    if (data.has(prevPageKey, PersistentDataType.INTEGER)) {
      int prevPage = data.get(prevPageKey, PersistentDataType.INTEGER);
      player.performCommand("cosmetics " + (prevPage + 1));
      return;
    }

    // Handle next page
    if (data.has(nextPageKey, PersistentDataType.INTEGER)) {
      int nextPage = data.get(nextPageKey, PersistentDataType.INTEGER);
      player.performCommand("cosmetics " + (nextPage + 1));
      return;
    }

    // Handle cosmetic view
    NamespacedKey cosmeticIdKey = new NamespacedKey("stweaks", "cosmetic_id");
    if (data.has(cosmeticIdKey, PersistentDataType.STRING)) {
      String cosmeticId = data.get(cosmeticIdKey, PersistentDataType.STRING);
      Cosmetic cosmetic = CosmeticsManager.getCosmeticById(cosmeticId);
      if (cosmetic != null) {
        CosmeticsMenuCommand.openCosmeticView(player, cosmetic, 0);
      }
      return;
    }

    NamespacedKey returnPageKey = new NamespacedKey("stweaks", "return_page");
    if (data.has(returnPageKey, PersistentDataType.INTEGER)) {
      int returnPage = data.get(returnPageKey, PersistentDataType.INTEGER);
      player.performCommand("cosmetics " + returnPage);
      return;
    }
  }
}
