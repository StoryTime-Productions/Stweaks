package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.models.Quest;
import com.storytimeproductions.stweaks.util.QuestsManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

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
   * Executes the /quests command. Only players can use this command.
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

    if (args.length == 3 && args[0].equalsIgnoreCase("unset")) {
      String playerName = args[1];
      String questName = args[2];

      OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
      if (!target.hasPlayedBefore() && !target.isOnline()) {
        sender.sendMessage("Player not found or hasn't played before.");
        return true;
      }

      String questId = questsManager.getQuestByName(questName);
      if (questId == null) {
        sender.sendMessage("Quest not found.");
        return true;
      }

      questsManager.unsetQuestCompletion(target.getUniqueId(), questId);
      sender.sendMessage("Quest '" + questName + "' marked as incomplete for " + playerName + ".");
      return true;
    }

    if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
      questsManager.reloadQuests();
      return true;
    }

    int page = 1;
    if (args.length >= 1) {
      try {
        page = Math.max(1, Integer.parseInt(args[0]));
      } catch (NumberFormatException e) {
        player.sendMessage("Invalid page number. Using page 1.");
      }
    }
    openQuestMenu(player, page);
    return true;
  }

  /**
   * Opens a custom GUI menu for the specified player, showing active and completed quests, along
   * with statistics and navigation options.
   *
   * @param player the player for whom the menu is opened
   */
  private void openQuestMenu(Player player, int page) {
    questsManager.reloadQuests();

    UUID uuid = player.getUniqueId();
    List<String> allQuestIdsRaw = questsManager.getDisplayableQuestIdsFor(uuid);
    List<String> requiredPlayerQuests = new ArrayList<>();
    List<String> defaultQuests = new ArrayList<>();
    List<String> completedQuests = new ArrayList<>();

    for (String questId : allQuestIdsRaw) {
      if (questsManager.isQuestCompleted(uuid, questId)) {
        completedQuests.add(questId);
      } else {
        Quest quest = questsManager.getQuestById(questId);
        List<UUID> requiredPlayers = quest.getRequiredPlayers();
        if (requiredPlayers != null && requiredPlayers.contains(uuid)) {
          requiredPlayerQuests.add(questId);
        } else if (requiredPlayers == null || requiredPlayers.isEmpty()) {
          defaultQuests.add(questId);
        }
      }
    }

    // Concatenate them in the desired order
    List<String> allQuestIds = new ArrayList<>();
    allQuestIds.addAll(requiredPlayerQuests);
    allQuestIds.addAll(defaultQuests);
    allQuestIds.addAll(completedQuests);

    int maxItems = 14;
    int totalPages = (int) Math.ceil(allQuestIds.size() / (double) maxItems);
    page = Math.max(1, Math.min(page, totalPages));

    Inventory gui =
        Bukkit.createInventory(
            null,
            6 * 9,
            Component.text("Quest Menu - Page " + page).decoration(TextDecoration.ITALIC, false));

    // Add border panes
    for (int i = 0; i < 54; i++) {
      if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
        gui.setItem(i, createPane(Material.BLACK_STAINED_GLASS_PANE, " "));
      }
    }

    // Checkerboard layout: y from 1–4, x from 1–7, if (x+y)%2==0
    int itemsPlaced = 0;
    int startIndex = (page - 1) * maxItems;
    int endIndex = Math.min(startIndex + maxItems, allQuestIds.size());

    for (int y = 1; y <= 4; y++) {
      for (int x = 1; x <= 7; x++) {
        if ((x + y) % 2 == 0 && startIndex + itemsPlaced < endIndex) {
          String questId = allQuestIds.get(startIndex + itemsPlaced);
          Quest quest = questsManager.getQuestById(questId);
          boolean isCompleted = questsManager.isQuestCompleted(player.getUniqueId(), questId);
          ItemStack paper = createQuestPaper(quest, isCompleted);
          int slot = y * 9 + x;
          gui.setItem(slot, paper);
          itemsPlaced++;
        }
      }
    }

    // Stats item in center bottom (slot 49)
    int completed = questsManager.getCompletedQuestCount(uuid);
    int open = questsManager.getOpenQuestCount(uuid);
    double percent = open + completed == 0 ? 0.0 : ((double) completed / (open + completed)) * 100;

    ItemStack stats = new ItemStack(Material.PAPER);
    ItemMeta statsMeta = stats.getItemMeta();
    statsMeta.displayName(Component.text("Quest Stats"));
    statsMeta.lore(
        List.of(
            Component.text("Open: ", NamedTextColor.YELLOW)
                .append(
                    Component.text(open, NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)),
            Component.text("Completed: ", NamedTextColor.YELLOW)
                .append(
                    Component.text(completed, NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)),
            Component.text("Completion: ", NamedTextColor.YELLOW)
                .append(
                    Component.text(String.format("%.2f%%", percent), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false))));
    stats.setItemMeta(statsMeta);
    gui.setItem(49, stats);

    // Prev and Next buttons
    NamespacedKey pageKey =
        new NamespacedKey(Bukkit.getPluginManager().getPlugin("stweaks"), "page");

    if (page > 1) {
      ItemStack previous = createPane(Material.ARROW, "Previous Page");
      ItemMeta previousMeta = previous.getItemMeta();
      previousMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page - 1);
      previous.setItemMeta(previousMeta);
      gui.setItem(46, previous);
    }

    if (page < totalPages) {
      ItemStack next = createPane(Material.ARROW, "Next Page");
      ItemMeta nextMeta = next.getItemMeta();
      nextMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page + 1);
      next.setItemMeta(nextMeta);
      gui.setItem(52, next);
    }

    player.openInventory(gui);
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
  private ItemStack createQuestPaper(Quest quest, boolean isCompleted) {
    ItemStack item = new ItemStack(isCompleted ? Material.GREEN_WOOL : quest.getIcon());
    ItemMeta meta = item.getItemMeta();

    // Display name
    meta.displayName(
        Component.text(quest.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));

    List<Component> lore = new ArrayList<>();

    // Lore (description)
    if (quest.getLore() != null && !quest.getLore().isBlank()) {
      for (String line : quest.getLore().split("\n")) {
        List<String> wrappedLines = wrapText(line.trim(), 50 - 2);
        for (int i = 0; i < wrappedLines.size(); i++) {
          // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
          String prefix = (i == 0) ? "\u2022 " : "  ";
          // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
          lore.add(Component.text(prefix + wrappedLines.get(i), NamedTextColor.WHITE));
        }
      }
      lore.add(Component.empty());
    }

    // Deadline
    if (quest.getDeadline() != null) {
      lore.add(
          Component.text("Deadline: ", NamedTextColor.RED)
              .append(Component.text(quest.getDeadline(), NamedTextColor.WHITE))
              .decoration(TextDecoration.ITALIC, false));
      lore.add(Component.empty());
    }

    // Completion Status
    lore.add(
        Component.text("Status: ", NamedTextColor.AQUA)
            .append(
                Component.text(
                        isCompleted ? "Completed" : "In Progress",
                        isCompleted ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)));

    NamespacedKey questIdKey =
        new NamespacedKey(Bukkit.getPluginManager().getPlugin("stweaks"), "questId");
    meta.getPersistentDataContainer().set(questIdKey, PersistentDataType.STRING, quest.getId());

    meta.lore(lore);
    item.setItemMeta(meta);
    return item;
  }

  /**
   * Opens a detailed view of a specific quest for the player.
   *
   * @param player the player who will see the quest details
   * @param quest the quest to be displayed
   */
  public void openQuestViewMenu(Player player, Quest quest) {

    Inventory gui =
        Bukkit.createInventory(
            null, 6 * 9, Component.text("Quest Details").decoration(TextDecoration.ITALIC, false));

    // Border
    for (int i = 0; i < 54; i++) {
      if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
        gui.setItem(i, createPane(Material.BLACK_STAINED_GLASS_PANE, " "));
      }
    }

    // Exit button
    ItemStack exit = new ItemStack(Material.BARRIER);
    ItemMeta exitMeta = exit.getItemMeta();
    exitMeta.displayName(Component.text("Exit to Quest Menu"));
    exit.setItemMeta(exitMeta);

    Plugin plugin = Bukkit.getPluginManager().getPlugin("stweaks");

    NamespacedKey exitKey = new NamespacedKey(plugin, "exit");
    exitMeta.getPersistentDataContainer().set(exitKey, PersistentDataType.INTEGER, 1);
    exit.setItemMeta(exitMeta);

    ItemStack attempt = new ItemStack(Material.EMERALD_BLOCK);
    ItemMeta attemptMeta = attempt.getItemMeta();
    attemptMeta.displayName(Component.text("Verify Completion"));
    NamespacedKey attemptKey = new NamespacedKey(plugin, "verify_completion");
    NamespacedKey questIdKey = new NamespacedKey(plugin, "questId");
    attemptMeta.getPersistentDataContainer().set(attemptKey, PersistentDataType.INTEGER, 1);
    attemptMeta
        .getPersistentDataContainer()
        .set(questIdKey, PersistentDataType.STRING, quest.getId());
    attempt.setItemMeta(attemptMeta);

    if (questsManager.isQuestCompleted(player.getUniqueId(), quest.getId())) {
      gui.setItem(49, exit);
    } else {
      gui.setItem(46, exit);
      gui.setItem(52, attempt);
    }

    // Quest name
    gui.setItem(20, createInfoItem(quest.getIcon(), "Quest Name", quest.getName()));

    // Lore
    gui.setItem(22, createInfoItem(Material.WRITABLE_BOOK, "Description", quest.getLore()));

    // Players & Deadline
    StringBuilder extra = new StringBuilder();

    if (!quest.getRequiredPlayers().isEmpty()) {
      extra.append("Players: ");
      quest
          .getRequiredPlayers()
          .forEach(
              uuid -> {
                OfflinePlayer currentPlayer = Bukkit.getOfflinePlayer(uuid);
                if (currentPlayer != null) {
                  extra.append(currentPlayer.getName()).append(", ");
                }
              });
      if (extra.length() > 8) {
        extra.setLength(extra.length() - 2);
      }
      extra.append("\n\n");
    }

    if (quest.getDeadline() != null) {
      extra.append("Deadline: ").append(quest.getDeadline());
    }

    if (extra.isEmpty()) {
      extra.append("None");
    }

    gui.setItem(24, createInfoItem(Material.CLOCK, "Optional Requirements", extra.toString()));

    // Combined Required Items and Rewards in slot 30 using Kyori components
    Component combinedComponent =
        Component.text("Required Items:\n", NamedTextColor.YELLOW)
            .append(
                Component.text(formatItemList(quest.getItemRequirements()), NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text("Rewards:\n", NamedTextColor.GREEN))
            .append(Component.text(formatItemList(quest.getRewards()), NamedTextColor.WHITE));

    gui.setItem(30, createInfoItem(Material.CHEST, "Items & Rewards", combinedComponent));

    // Completed Players in slot 32 using questsManager
    List<UUID> completedPlayers = questsManager.getCompletedPlayers(quest);
    String completedText =
        completedPlayers.isEmpty()
            ? "None yet"
            : completedPlayers.stream()
                .map(
                    uuid -> {
                      OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                      return p.getName() != null ? p.getName() : uuid.toString();
                    })
                .collect(Collectors.joining(",\n"));

    gui.setItem(32, createInfoItem(Material.PLAYER_HEAD, "Completed By", completedText));

    player.openInventory(gui);
  }

  private ItemStack createInfoItem(Material material, String title, String content) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        Component.text(title, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

    if (content != null && !content.isBlank()) {
      List<Component> lore = new ArrayList<>();
      for (String paragraph : content.split("\n")) {
        List<String> wrappedLines = wrapText(paragraph, 50);
        for (String line : wrappedLines) {
          if (line.startsWith("Deadline: ") || line.startsWith("Players: ")) {
            String prefix = line.startsWith("Deadline: ") ? "Deadline: " : "Players: ";
            String value = line.substring(prefix.length());

            lore.add(
                Component.text(prefix, NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(
                        Component.text(value, NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, true)));
          } else {
            lore.add(
                Component.text(line, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, true));
          }
        }
      }
      meta.lore(lore);
    }

    item.setItemMeta(meta);
    return item;
  }

  private ItemStack createInfoItem(Material material, String title, Component content) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();

    meta.displayName(
        Component.text(title, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

    if (content != null) {
      List<Component> lore = new ArrayList<>();
      // Split content into lines using newlines inside the Component
      String plainText = LegacyComponentSerializer.legacySection().serialize(content);
      for (String paragraph : plainText.split("\n")) {
        List<String> wrappedLines = wrapText(paragraph, 50);
        for (String line : wrappedLines) {
          lore.add(Component.text(line, NamedTextColor.WHITE));
        }
      }
      meta.lore(lore);
    }

    item.setItemMeta(meta);
    return item;
  }

  private List<String> wrapText(String text, int maxLineLength) {
    List<String> lines = new ArrayList<>();
    String[] words = text.split(" ");
    StringBuilder line = new StringBuilder();

    for (String word : words) {
      if (line.length() + word.length() + 1 > maxLineLength) {
        lines.add(line.toString().trim());
        line = new StringBuilder();
      }
      line.append(word).append(" ");
    }

    if (!line.isEmpty()) {
      lines.add(line.toString().trim());
    }

    return lines;
  }

  private String formatItemList(List<String> items) {
    if (items == null || items.isEmpty()) {
      return "None";
    }
    return items.stream()
        .map(
            raw -> {
              // Remove prefix and split
              String noPrefix = raw.replaceFirst("^minecraft:", "");
              String[] parts = noPrefix.split(":");

              // Get quantity and item name
              String count = parts[parts.length - 1];
              String itemNameRaw = String.join("_", Arrays.copyOf(parts, parts.length - 1));

              // Convert to Title Case
              String itemName =
                  Arrays.stream(itemNameRaw.split("_"))
                      .map(
                          word ->
                              word.isEmpty()
                                  ? word
                                  : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                      .collect(Collectors.joining(" "));

              return "x" + count + " " + itemName;
            })
        .collect(Collectors.joining("\n"));
  }
}
