package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.models.Quest;
import com.storytimeproductions.stweaks.commands.QuestMenuCommand;
import com.storytimeproductions.stweaks.util.QuestsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
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

    if (event.getAction().toString().contains("PLACE")) {
      event.setCancelled(true);
      return;
    }

    // Handle page navigation
    ItemMeta meta = clickedItem.getItemMeta();
    PersistentDataContainer data = meta.getPersistentDataContainer();

    NamespacedKey pageKey = new NamespacedKey(plugin, "page");
    if (data.has(pageKey, PersistentDataType.INTEGER)) {
      int clickedPage = data.get(pageKey, PersistentDataType.INTEGER);
      if (clickedPage > 0) {
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);

        player.performCommand("quests " + clickedPage);
      }
    }

    // Handle quest selection
    NamespacedKey questIdKey = new NamespacedKey(plugin, "questId");
    if (data.has(questIdKey, PersistentDataType.STRING)) {
      String questId = data.get(questIdKey, PersistentDataType.STRING);
      Quest quest = questsManager.getQuestById(questId);
      if (quest != null) {
        questMenuCommand.openQuestViewMenu(player, quest);
      }
    }

    // Handle "exit to quest menu"
    NamespacedKey exitKey = new NamespacedKey(plugin, "exit");
    if (data.has(exitKey, PersistentDataType.INTEGER)) {
      player.performCommand("quests");
    }

    // Handle quest completion attempt
    NamespacedKey attemptKey = new NamespacedKey(plugin, "verify_completion");
    if (data.has(attemptKey, PersistentDataType.INTEGER)) {
      // Get the current quest being viewed
      if (data.has(questIdKey, PersistentDataType.STRING)) {
        String questId = data.get(questIdKey, PersistentDataType.STRING);
        Quest quest = questsManager.getQuestById(questId);

        if (quest != null && !questsManager.isQuestCompleted(player.getUniqueId(), questId)) {
          // Check if player has all required items
          if (questsManager.hasRequiredItems(player, quest)) {
            // Remove items and give rewards
            questsManager.consumeRequiredItems(player, quest);
            questsManager.giveRewards(player, quest);

            // Mark quest as completed
            questsManager.markQuestCompleted(player.getUniqueId(), questId);

            // Broadcast completion
            if (quest.getRequiredPlayers().isEmpty()) {
              // Broadcast to everyone
              Bukkit.getServer()
                  .broadcast(
                      Component.text()
                          .append(Component.text(player.getName(), NamedTextColor.GREEN))
                          .append(
                              Component.text(" has completed the quest: ", NamedTextColor.WHITE))
                          .append(Component.text(quest.getName(), NamedTextColor.GOLD))
                          .build());
            } else {
              // Broadcast only to required players
              quest
                  .getRequiredPlayers()
                  .forEach(
                      uuid -> {
                        Player requiredPlayer = Bukkit.getPlayer(uuid);
                        if (requiredPlayer != null) {
                          requiredPlayer.sendMessage(
                              Component.text()
                                  .append(Component.text(player.getName(), NamedTextColor.GREEN))
                                  .append(
                                      Component.text(
                                          " has completed the quest: ", NamedTextColor.WHITE))
                                  .append(Component.text(quest.getName(), NamedTextColor.GOLD))
                                  .build());
                        }
                      });
            }

            Location location = player.getLocation();
            Firework firework =
                (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
            FireworkMeta fireworkMeta = firework.getFireworkMeta();
            fireworkMeta.addEffect(
                FireworkEffect.builder()
                    .withColor(Color.RED, org.bukkit.Color.YELLOW)
                    .withFade(Color.ORANGE)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .trail(true)
                    .flicker(true)
                    .build());
            fireworkMeta.setPower(1);
            firework.setFireworkMeta(fireworkMeta);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);

            // Refresh GUI
            questMenuCommand.openQuestViewMenu(player, quest);
          } else {
            player.sendMessage(
                Component.text(
                    "You do not have the required items to complete this quest.",
                    NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
          }
        }
      }
    }

    // Prevent any default item interaction in quest GUIs
    event.setCancelled(true);
  }
}
