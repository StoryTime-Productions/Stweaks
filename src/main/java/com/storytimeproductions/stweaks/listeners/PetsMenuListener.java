package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.models.Pet;
import com.storytimeproductions.stweaks.commands.PetsMenuCommand;
import com.storytimeproductions.stweaks.util.PetsManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Handles interactions with the pet-related GUIs: - The main pets menu that lists all available
 * pets. - The individual pet view showing its crafting recipe.
 *
 * <p>This listener prevents default click behavior in these menus, and delegates actions such as
 * opening views or returning to the main menu.
 */
public class PetsMenuListener implements Listener {
  private final Plugin plugin;
  private final PetsManager petsManager;
  private final PetsMenuCommand petsMenuCommand;

  /**
   * Constructs the PetsMenuListener.
   *
   * @param plugin the main plugin instance
   * @param petsManager manager responsible for loading and accessing pet data
   * @param petsMenuCommand reference to the command handler used to open pet GUIs
   */
  public PetsMenuListener(Plugin plugin, PetsManager petsManager, PetsMenuCommand petsMenuCommand) {
    this.petsManager = petsManager;
    this.petsMenuCommand = petsMenuCommand;
    this.plugin = plugin;
  }

  /**
   * Event handler that listens for clicks in inventories. Intercepts clicks within the pet GUI
   * menus to: - Open an individual pet view when a pet is selected - Return to the main menu when
   * the back button is clicked
   *
   * @param event the InventoryClickEvent triggered by the player
   */
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    Inventory clickedInventory = event.getClickedInventory();
    if (clickedInventory == null) {
      return;
    }

    // Check if the inventory is one of the custom pet menus
    InventoryView view = player.getOpenInventory();
    String title = view.title().toString();
    if (!title.contains("Pet")) {
      return;
    }

    // Cancel default interaction behavior
    event.setCancelled(true);

    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem == null || !clickedItem.hasItemMeta()) {
      return;
    }

    PersistentDataContainer data = clickedItem.getItemMeta().getPersistentDataContainer();
    NamespacedKey petIdKey = new NamespacedKey(plugin, "petId");
    NamespacedKey backToMainMenuKey = new NamespacedKey(plugin, "returnPetsMenu");
    NamespacedKey returnPageKey = new NamespacedKey(plugin, "returnPage");
    NamespacedKey prevPageKey = new NamespacedKey(plugin, "prevPage");
    NamespacedKey nextPageKey = new NamespacedKey(plugin, "nextPage");

    // Handle item clicks in the main pets menu
    if (title.contains("Pet")) {
      if (data.has(petIdKey, PersistentDataType.STRING)) {
        String petId = data.get(petIdKey, PersistentDataType.STRING);
        Integer returnPage = data.get(returnPageKey, PersistentDataType.INTEGER);
        Pet pet = petsManager.getPetById(petId);
        if (pet != null) {
          petsMenuCommand.openPetView(player, pet, returnPage);
        }
      }

      // Handle "Previous Page" button
      if (data.has(prevPageKey, PersistentDataType.INTEGER)) {
        int prevPage = data.get(prevPageKey, PersistentDataType.INTEGER);
        petsMenuCommand.openMainPetsMenu(player, prevPage);
      }

      // Handle "Next Page" button
      if (data.has(nextPageKey, PersistentDataType.INTEGER)) {
        int nextPage = data.get(nextPageKey, PersistentDataType.INTEGER);
        petsMenuCommand.openMainPetsMenu(player, nextPage);
      }

      if (data.has(backToMainMenuKey, PersistentDataType.INTEGER)) {
        Integer returnPage = data.get(returnPageKey, PersistentDataType.INTEGER);
        petsMenuCommand.openMainPetsMenu(player, returnPage);
      }
    }
  }
}
