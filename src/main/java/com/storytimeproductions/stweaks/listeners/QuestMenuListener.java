package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.models.Quest;
import com.storytimeproductions.stweaks.commands.QuestMenuCommand;
import com.storytimeproductions.stweaks.util.QuestsManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * QuestMenuListener handles player interactions within custom quest-related GUIs, including the
 * Quest Menu (listing quests with pagination) and the Quest View (detailed view of a selected
 * quest). It interprets and reacts to custom data tags attached to GUI items:
 *
 * <ul>
 *   <li>Triggers pagination by executing /quests [page]
 *   <li>Opens a detailed quest view by quest ID
 *   <li>Returns to the Quest Menu from the quest view
 *   <li>Blocks default inventory interactions within quest GUIs
 * </ul>
 */
public class QuestMenuListener implements Listener {

  private final Plugin plugin;
  private final QuestsManager questsManager;
  private final QuestMenuCommand questMenuCommand;

  /**
   * Constructs a new QuestMenuListener.
   *
   * @param plugin the plugin instance used for namespaced keys
   * @param questsManager the central quest registry
   * @param questMenuCommand the command handler containing the GUI rendering logic
   */
  public QuestMenuListener(
      Plugin plugin, QuestsManager questsManager, QuestMenuCommand questMenuCommand) {
    this.plugin = plugin;
    this.questsManager = questsManager;
    this.questMenuCommand = questMenuCommand;
  }

  /**
   * Handles click events inside custom quest-related inventories. - If a page navigation item is
   * clicked (has a "page" tag), triggers the appropriate command. - If a quest item is clicked (has
   * a "questId" tag), opens the quest view menu. - If the "exit" tag is present, returns to the
   * main quest menu. - All interactions inside these GUIs are cancelled to prevent normal item
   * movement.
   *
   * @param event the InventoryClickEvent triggered when a player clicks an item
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

    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem == null || !clickedItem.hasItemMeta()) {
      return;
    }

    InventoryView view = player.getOpenInventory();
    String title = view.title().toString();

    // Determine if the player is in a relevant custom GUI
    boolean inQuestMenu = title.contains("Quest Menu");
    boolean inQuestView = title.contains("Quest Details");

    if (!inQuestMenu && !inQuestView) {
      return;
    }

    ItemMeta meta = clickedItem.getItemMeta();
    PersistentDataContainer data = meta.getPersistentDataContainer();

    NamespacedKey pageKey = new NamespacedKey(plugin, "page");
    NamespacedKey questIdKey = new NamespacedKey(plugin, "questId");
    NamespacedKey exitKey = new NamespacedKey(plugin, "exit");

    // Handle page navigation
    if (data.has(pageKey, PersistentDataType.INTEGER)) {
      int clickedPage = data.get(pageKey, PersistentDataType.INTEGER);
      if (clickedPage > 0) {
        player.performCommand("quests " + clickedPage);
      }
    }

    // Handle quest selection
    if (data.has(questIdKey, PersistentDataType.STRING)) {
      String questId = data.get(questIdKey, PersistentDataType.STRING);
      Quest quest = questsManager.getQuestById(questId);
      if (quest != null) {
        questMenuCommand.openQuestViewMenu(player, quest);
      }
    }

    // Handle "exit to quest menu"
    if (data.has(exitKey, PersistentDataType.INTEGER)) {
      player.performCommand("quests");
    }

    // Prevent any default item interaction in quest GUIs
    event.setCancelled(true);
  }
}
