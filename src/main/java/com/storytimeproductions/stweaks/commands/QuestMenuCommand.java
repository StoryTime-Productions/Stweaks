package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.stweaks.util.QuestsManager;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles the "/questmenu" command to open a graphical quest menu interface for players. This menu
 * displays available quests, quest completion statistics, and access to a QuestBook.
 */
public class QuestMenuCommand implements CommandExecutor {

  private final QuestsManager questsManager;

  /**
   * Constructs the command executor with a reference to the QuestsManager.
   *
   * @param questsManager the quests manager used to retrieve quest data
   */
  public QuestMenuCommand(QuestsManager questsManager) {
    this.questsManager = questsManager;
  }

  /**
   * Executes the /questmenu command. Only players can use this command.
   *
   * @param sender the source of the command
   * @param command the command that was executed
   * @param label the alias of the command
   * @param args the arguments passed with the command
   * @return true if the command was processed successfully, false otherwise
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can open the quest menu.");
      return true;
    }

    openQuestMenu(player);
    return true;
  }

  /**
   * Opens a custom GUI menu for the specified player, showing active and completed quests, along
   * with statistics and navigation options.
   *
   * @param player the player for whom the menu is opened
   */
  private void openQuestMenu(Player player) {
    UUID uuid = player.getUniqueId();
    Inventory menu =
        Bukkit.createInventory(null, 6 * 9, Component.text("Quest Menu", NamedTextColor.GOLD));

    // Add border panes
    for (int i = 0; i < 54; i++) {
      if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
        menu.setItem(i, createPane(Material.BLACK_STAINED_GLASS_PANE, " "));
      }
    }

    // Add quest papers
    // List<String> displayableQuestIds =
    // questsManager.getDisplayableQuestIdsFor(player.getUniqueId()).stream()
    // .toList();

    int[] slots = {
      2 + 2 * 9, 3 + 2 * 9, 4 + 2 * 9, 5 + 2 * 9, 6 + 2 * 9, // row 3
      2 + 3 * 9, 3 + 3 * 9, 4 + 3 * 9, 5 + 3 * 9, 6 + 3 * 9 // row 4
    };

    int textureNumber = 1;

    for (int i = 0; i < slots.length; i++) {
      menu.setItem(slots[i], createQuestPaper(textureNumber++));
    }

    // Add quest completion stats
    int completed = questsManager.getCompletedQuestCount(uuid);
    int open = questsManager.getOpenQuestCount(uuid);
    double percent = open + completed == 0 ? 0.0 : ((double) completed / (open + completed)) * 100;

    ItemStack statsPaper = new ItemStack(Material.PAPER);
    ItemMeta statsMeta = statsPaper.getItemMeta();
    statsMeta.displayName(Component.text("Quest Stats", NamedTextColor.GOLD));
    statsMeta.lore(
        List.of(
            Component.text("Open: ", NamedTextColor.YELLOW)
                .append(Component.text(open, NamedTextColor.GREEN)),
            Component.text("Completed: ", NamedTextColor.YELLOW)
                .append(Component.text(completed, NamedTextColor.RED)),
            Component.text("Completion: ", NamedTextColor.YELLOW)
                .append(Component.text(percent + "%", NamedTextColor.AQUA))));
    statsPaper.setItemMeta(statsMeta);
    menu.setItem(3 + 5 * 9, statsPaper);

    // Add QuestBook button
    ItemStack openBook = createPane(Material.LIME_STAINED_GLASS_PANE, "Open Questbook");
    menu.setItem(5 + 5 * 9, openBook);

    player.openInventory(menu);
  }

  /**
   * Creates a decorative glass pane item with a given name.
   *
   * @param type the material of the pane
   * @param name the display name of the item
   * @return the customized ItemStack
   */
  private ItemStack createPane(Material type, String name) {
    ItemStack item = new ItemStack(type);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text(name));
    item.setItemMeta(meta);
    return item;
  }

  /**
   * Creates a quest paper item representing a specific quest.
   *
   * @param questId the quest identifier
   * @param modelNumber the model number used for custom model data
   * @return the customized quest paper item
   */
  private ItemStack createQuestPaper(int modelNumber) {
    ItemStack item = new ItemStack(Material.PAPER);
    ItemMeta meta = item.getItemMeta();

    NamespacedKey key = new NamespacedKey("storytime", "quest_menu_" + modelNumber);

    meta.displayName(Component.text(key.toString(), NamedTextColor.GOLD));
    meta.setItemModel(key);
    item.setItemMeta(meta);
    return item;
  }
}
