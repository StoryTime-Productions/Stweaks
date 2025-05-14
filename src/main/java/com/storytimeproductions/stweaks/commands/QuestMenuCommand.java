package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.models.Quest;
import com.storytimeproductions.stweaks.util.QuestsManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
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
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

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
      if (!sender.isOp()) {
        sender.sendMessage("You do not have permission to run this command.");
        return true;
      }

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
      if (!sender.isOp()) {
        sender.sendMessage("You do not have permission to run this command.");
        return true;
      }
      questsManager.reloadQuests();
      sender.sendMessage("Quests reloaded successfully.");
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
    List<String> waitingOnOtherPlayers = new ArrayList<>();
    List<String> requiredPlayerQuests = new ArrayList<>();
    List<String> defaultQuests = new ArrayList<>();
    List<String> completedQuests = new ArrayList<>();
    List<String> deadlinePassedQuests = new ArrayList<>();

    for (String questId : allQuestIdsRaw) {
      Quest quest = questsManager.getQuestById(questId);
      if (quest == null) {
        continue;
      }

      boolean isCompletedByPlayer = questsManager.isQuestCompleted(uuid, questId);
      List<UUID> requiredPlayers = quest.getRequiredPlayers();

      boolean hasDeadlinePassed = false;
      if (quest.getDeadline() != null) {
        try {
          LocalDateTime deadline = LocalDateTime.parse(quest.getDeadline(), formatter);
          hasDeadlinePassed = deadline.isBefore(LocalDateTime.now());
        } catch (DateTimeParseException e) {
          Bukkit.getLogger().warning("Failed to parse deadline for quest: " + quest.getName());
        }
      }

      if (isCompletedByPlayer) {
        if (requiredPlayers != null && !requiredPlayers.isEmpty()) {
          // Check if all required players have completed the quest
          boolean allPlayersCompleted =
              requiredPlayers.stream()
                  .allMatch(playerUuid -> questsManager.isQuestCompleted(playerUuid, questId));
          if (!allPlayersCompleted) {
            waitingOnOtherPlayers.add(questId);
          } else {
            completedQuests.add(questId);
          }
        } else {
          completedQuests.add(questId);
        }
      } else if (hasDeadlinePassed) {
        deadlinePassedQuests.add(questId);
      } else {
        if (requiredPlayers != null && requiredPlayers.contains(uuid)) {
          requiredPlayerQuests.add(questId);
        } else if (requiredPlayers == null || requiredPlayers.isEmpty()) {
          defaultQuests.add(questId);
        }
      }
    }

    // Concatenate them in the desired order
    List<String> allQuestIds = new ArrayList<>();
    allQuestIds.addAll(
        waitingOnOtherPlayers); // Quests completed by the player but waiting on others
    allQuestIds.addAll(requiredPlayerQuests); // Quests the player has not yet completed
    allQuestIds.addAll(defaultQuests); // Default quests
    allQuestIds.addAll(completedQuests); // Fully completed quests
    allQuestIds.addAll(deadlinePassedQuests); // Deadline-passed quests

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

          if (!quest.getRequiredPlayers().isEmpty()) {
            boolean allPlayersCompleted =
                quest.getRequiredPlayers().stream()
                    .allMatch(playerUuid -> questsManager.isQuestCompleted(playerUuid, questId));
            isCompleted = isCompleted && allPlayersCompleted;
          }

          ItemStack paper = createQuestPaper(quest, isCompleted, page);
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
    } else {
      // No previous page, set to gray stained glass pane
      gui.setItem(46, createPane(Material.GRAY_STAINED_GLASS_PANE, " "));
    }

    if (page < totalPages) {
      ItemStack next = createPane(Material.ARROW, "Next Page");
      ItemMeta nextMeta = next.getItemMeta();
      nextMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page + 1);
      next.setItemMeta(nextMeta);
      gui.setItem(52, next);
    } else {
      // No previous page, set to gray stained glass pane
      gui.setItem(52, createPane(Material.GRAY_STAINED_GLASS_PANE, " "));
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
  private ItemStack createQuestPaper(Quest quest, boolean isCompleted, int page) {
    // Check if the deadline has passed

    // Check if the deadline has passed
    boolean hasDeadlinePassed = false;
    if (quest.getDeadline() != null) {
      try {
        LocalDateTime deadline = LocalDateTime.parse(quest.getDeadline(), formatter);
        hasDeadlinePassed = deadline.isBefore(LocalDateTime.now());
      } catch (DateTimeParseException e) {
        Bukkit.getLogger().warning("Failed to parse deadline for quest: " + quest.getName());
      }
    }

    // Use red wool if the deadline has passed, otherwise use the quest's icon
    ItemStack item =
        new ItemStack(
            hasDeadlinePassed
                ? Material.RED_WOOL
                : (isCompleted ? Material.GREEN_WOOL : quest.getIcon()));
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

    meta.addItemFlags(
        ItemFlag.HIDE_ATTRIBUTES,
        ItemFlag.HIDE_UNBREAKABLE,
        ItemFlag.HIDE_ENCHANTS,
        ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

    Plugin plugin = Bukkit.getPluginManager().getPlugin("stweaks");
    NamespacedKey questIdKey =
        new NamespacedKey(Bukkit.getPluginManager().getPlugin("stweaks"), "questId");
    NamespacedKey pageKey = new NamespacedKey(plugin, "returnPage");
    meta.getPersistentDataContainer().set(questIdKey, PersistentDataType.STRING, quest.getId());
    meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);

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
  public void openQuestViewMenu(Player player, Quest quest, int page) {

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
    NamespacedKey pageKey = new NamespacedKey(plugin, "returnPage");
    exitMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
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

    boolean hasDeadlinePassed = false;
    if (quest.getDeadline() != null) {
      try {
        LocalDateTime deadline = LocalDateTime.parse(quest.getDeadline(), formatter);
        hasDeadlinePassed = deadline.isBefore(LocalDateTime.now());
      } catch (DateTimeParseException e) {
        Bukkit.getLogger().warning("Failed to parse deadline for quest: " + quest.getName());
      }
    }

    if (questsManager.isQuestCompleted(player.getUniqueId(), quest.getId())) {
      if (!quest.getRequiredPlayers().isEmpty()) {
        // Check if there is at least one other player who has not completed the quest
        boolean allPlayersCompleted =
            quest.getRequiredPlayers().stream()
                .allMatch(uuid -> questsManager.isQuestCompleted(uuid, quest.getId()));

        if (!allPlayersCompleted) {
          // Add a gold block named "Partially Complete" for quests with required players
          ItemStack partiallyComplete = new ItemStack(Material.GOLD_BLOCK);
          ItemMeta partiallyCompleteMeta = partiallyComplete.getItemMeta();
          partiallyCompleteMeta.displayName(
              Component.text("Partially Complete", NamedTextColor.GOLD)
                  .decoration(TextDecoration.ITALIC, false));
          partiallyComplete.setItemMeta(partiallyCompleteMeta);

          gui.setItem(46, exit); // Exit button on the left
          gui.setItem(52, partiallyComplete); // Place in the center slot
        } else {
          gui.setItem(49, exit); // Default exit button for fully completed quests
        }
      } else {
        gui.setItem(
            49, exit); // Default exit button for fully completed quests without required players
      }
    } else if (!hasDeadlinePassed) {
      gui.setItem(46, exit); // Exit button on the left
      gui.setItem(52, attempt); // Attempt button on the right
    } else {
      gui.setItem(49, exit);
    }

    // Quest name
    gui.setItem(20, createInfoItem(quest.getIcon(), "Quest Name", quest.getName()));

    // Lore
    gui.setItem(22, createInfoItem(Material.WRITABLE_BOOK, "Description", quest.getLore()));

    // Players & Deadline
    StringBuilder extra = new StringBuilder();

    if (!quest.getRequiredPlayers().isEmpty()) {
      extra.append("Players:\n");
      quest
          .getRequiredPlayers()
          .forEach(
              uuid -> {
                OfflinePlayer currentPlayer = Bukkit.getOfflinePlayer(uuid);
                if (currentPlayer != null) {
                  extra.append("- ").append(currentPlayer.getName()).append("\n");
                }
              });
    }

    if (extra.length() > 0) {
      extra.append("\n");
    }

    if (quest.getDeadline() != null) {
      extra.append("Deadline: \n").append(quest.getDeadline());
    }

    if (extra.isEmpty()) {
      extra.append("None");
    }

    gui.setItem(24, createInfoItem(Material.CLOCK, "Optional Requirements", extra.toString()));

    boolean isCompleted =
        hasDeadlinePassed || questsManager.isQuestCompleted(player.getUniqueId(), quest.getId());

    // Combined Required Items, Stat Requirements, and Rewards in slot 30
    Component combinedComponent =
        Component.text("Required Items:", NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false) // Ensure title is non-italic
            .append(formatItemList(player, quest.getItemRequirements(), !isCompleted))
            .append(Component.newline())
            .append(Component.newline())
            .append(
                Component.text("Required Stats:", NamedTextColor.BLUE)
                    .decoration(TextDecoration.ITALIC, false)) // Ensure title is non-italic
            .append(formatStatList(player, quest.getStatRequirements(), !isCompleted))
            .append(Component.newline())
            .append(Component.newline())
            .append(
                Component.text("Rewards:", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)) // Ensure title is non-italic
            .append(formatItemList(player, quest.getRewards(), false));

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

  private Component formatStatList(Player player, List<String> stats, boolean showFraction) {
    if (stats == null || stats.isEmpty()) {
      return Component.text("\nNone", NamedTextColor.GRAY);
    }

    return stats.stream()
        .map(
            raw -> {
              String[] parts = raw.split(":");
              if (parts.length != 3) {
                return Component.text("Invalid Stat Format", NamedTextColor.RED);
              }

              String statName = parts[0];
              String key = parts[1];

              Statistic statistic;
              try {
                statistic = Statistic.valueOf(statName.toUpperCase());
              } catch (IllegalArgumentException e) {
                return Component.text("Invalid Statistic", NamedTextColor.RED);
              }

              int currentAmount = 0;
              String additionalInfo = "";

              if (statistic.getType() == Statistic.Type.UNTYPED) {
                currentAmount = player.getStatistic(statistic);
              } else if (statistic.getType() == Statistic.Type.BLOCK
                  || statistic.getType() == Statistic.Type.ITEM) {
                Material material = Material.matchMaterial(key);
                if (material != null) {
                  currentAmount = player.getStatistic(statistic, material);
                }
              } else if (statistic.getType() == Statistic.Type.ENTITY) {
                EntityType entityType;
                try {
                  entityType = EntityType.valueOf(key.toUpperCase());
                  currentAmount = player.getStatistic(statistic, entityType);
                  additionalInfo =
                      " ("
                          + Arrays.stream(entityType.name().split("_"))
                              .map(
                                  word ->
                                      word.substring(0, 1).toUpperCase()
                                          + word.substring(1).toLowerCase())
                              .collect(Collectors.joining(" "))
                          + ")";
                } catch (IllegalArgumentException ignored) {
                  return Component.text("Invalid Entity Type", NamedTextColor.RED);
                }
              }

              int requiredAmount = Integer.parseInt(parts[2]);

              NamedTextColor color =
                  currentAmount >= requiredAmount ? NamedTextColor.GREEN : NamedTextColor.RED;

              if (!showFraction) {
                color = NamedTextColor.WHITE;
              }

              String display =
                  showFraction ? currentAmount + "/" + requiredAmount : "x" + requiredAmount;

              // Convert statistic name to title case
              String titleCaseName =
                  Arrays.stream(statName.split("_"))
                      .map(
                          word ->
                              word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                      .collect(Collectors.joining(" "));

              return Component.text(display + " " + titleCaseName + additionalInfo, color)
                  .decoration(TextDecoration.ITALIC, true);
            })
        .reduce(Component.empty(), (a, b) -> a.append(Component.newline()).append(b));
  }

  private Component formatItemList(Player player, List<String> items, boolean showFraction) {
    if (items == null || items.isEmpty()) {
      return Component.text("\nNone", NamedTextColor.GRAY);
    }

    return items.stream()
        .map(
            raw -> {
              int lastColon = raw.lastIndexOf(':');
              if (lastColon == -1 || lastColon == raw.length() - 1) {
                return Component.text("Invalid Item Format", NamedTextColor.RED);
              }

              String idAndNbt = raw.substring(0, lastColon);

              // Split material and NBT
              String materialKey;
              String nbtData = null;
              if (idAndNbt.contains("[")) {
                int nbtStart = idAndNbt.indexOf("[");
                materialKey = idAndNbt.substring(0, nbtStart);
                nbtData = idAndNbt.substring(nbtStart); // Extract NBT data
              } else {
                materialKey = idAndNbt;
              }

              Material material = Material.matchMaterial(materialKey);
              if (material == null) {
                return Component.text("Invalid Material", NamedTextColor.RED);
              }

              // Extract custom_name from NBT data
              String itemName = null;
              if (nbtData != null) {
                try {
                  int itemNameStart = nbtData.indexOf("custom_name='") + 11;
                  int jsonStart = nbtData.indexOf("{", itemNameStart);
                  int jsonEnd = nbtData.indexOf("}", jsonStart) + 1;
                  if (jsonStart > -1 && jsonEnd > jsonStart) {
                    String itemNameJson = nbtData.substring(jsonStart, jsonEnd);
                    // Parse JSON to extract the "text" field
                    itemName = extractTextFromJson(itemNameJson);
                  }
                } catch (Exception e) {
                  Bukkit.getLogger()
                      .warning("Failed to parse custom_name from NBT data: " + nbtData);
                }
              }

              // Count matching items in the player's inventory
              int currentAmount =
                  Arrays.stream(player.getInventory().getContents())
                      .filter(item -> item != null && item.getType() == material)
                      .mapToInt(ItemStack::getAmount)
                      .sum();

              int requiredAmount = Integer.parseInt(raw.substring(lastColon + 1));

              NamedTextColor color =
                  (currentAmount >= requiredAmount) ? NamedTextColor.GREEN : NamedTextColor.RED;

              if (!showFraction) {
                color = NamedTextColor.WHITE;
              }

              String display =
                  showFraction ? currentAmount + "/" + requiredAmount : "x" + requiredAmount;

              // Use custom_name if available, otherwise use the material name
              String displayName =
                  itemName != null
                      ? itemName
                      : Arrays.stream(material.name().split("_"))
                          .map(
                              word ->
                                  word.substring(0, 1).toUpperCase()
                                      + word.substring(1).toLowerCase())
                          .collect(Collectors.joining(" "));

              return Component.text(display + " " + displayName, color)
                  .decoration(TextDecoration.ITALIC, true);
            })
        .reduce(Component.empty(), (a, b) -> a.append(Component.newline()).append(b));
  }

  /**
   * Extracts the "text" field from a JSON string.
   *
   * @param json the JSON string
   * @return the value of the "text" field, or null if not found
   */
  private String extractTextFromJson(String json) {
    try {
      // Replace single quotes with double quotes for valid JSON parsing
      String validJson = json.replace("'", "\"");

      // Parse the JSON string
      com.google.gson.JsonObject jsonObject =
          com.google.gson.JsonParser.parseString(validJson).getAsJsonObject();

      // Extract the "text" field
      if (jsonObject.has("text")) {
        return jsonObject.get("text").getAsString();
      }
    } catch (Exception e) {
      Bukkit.getLogger().warning("Failed to parse JSON: " + json);
    }
    return null;
  }
}
