package com.storytimeproductions.stweaks.util;

import com.storytimeproductions.models.Quest;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.Statistic.Type;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages the lifecycle of quests, including loading from YAML, tracking completed quests per
 * player, and querying quest availability.
 */
public class QuestsManager {
  private final Map<String, Quest> allQuests = new HashMap<>();
  private final Map<UUID, Set<String>> completedQuests = new HashMap<>();
  private final DbManager dbManager;
  private final JavaPlugin plugin;

  /**
   * Constructs a new QuestsManager with the given database manager and plugin instance.
   *
   * @param dbManager the database manager for loading/saving completed quests
   * @param plugin the plugin instance for logging and file access
   */
  public QuestsManager(DbManager dbManager, JavaPlugin plugin) {
    this.dbManager = dbManager;
    this.plugin = plugin;
    loadCompletedQuestsFromDb();
    loadQuestsFromYaml();
  }

  /**
   * Reloads all quests from the YAML file and refreshes the completed quests mapping from the
   * database.
   */
  public void reloadQuests() {
    // Clear current quests
    allQuests.clear();

    // Reload from YAML
    loadQuestsFromYaml();

    // Reload from database
    loadCompletedQuestsFromDb();
  }

  private double similarity(String s1, String s2) {
    int maxLen = Math.max(s1.length(), s2.length());
    if (maxLen == 0) {
      return 1.0;
    }
    return (maxLen - levenshteinDistance(s1.toLowerCase(), s2.toLowerCase())) / (double) maxLen;
  }

  private int levenshteinDistance(String s1, String s2) {
    int[] costs = new int[s2.length() + 1];
    for (int j = 0; j < costs.length; j++) {
      costs[j] = j;
    }
    for (int i = 1; i <= s1.length(); i++) {
      costs[0] = i;
      int nw = i - 1;
      for (int j = 1; j <= s2.length(); j++) {
        int cj =
            Math.min(
                1 + Math.min(costs[j], costs[j - 1]),
                s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
        nw = costs[j];
        costs[j] = cj;
      }
    }
    return costs[s2.length()];
  }

  /**
   * Retrieves a quest by its name.
   *
   * @param name the name of the quest
   * @return the Quest object corresponding to the given name, or null if not found
   */
  public String getQuestByName(String name) {
    double bestScore = 0.0;
    String bestMatchId = null;

    for (Quest quest : allQuests.values()) {
      double score = similarity(quest.getName(), name);
      if (score > bestScore && score >= 0.8) {
        bestScore = score;
        bestMatchId = quest.getId();
      }
    }

    return bestMatchId;
  }

  /**
   * Checks if the player has the required items for a specific quest.
   *
   * @param player the player to check
   * @param quest the quest to check against
   * @return true if the player has all required items, false otherwise
   */
  public boolean hasRequiredItems(Player player, Quest quest) {
    Map<Material, Integer> itemRequirements = new HashMap<>();

    for (String req : quest.getItemRequirements()) {
      String[] parts = req.split(":");
      if (parts.length < 3) {
        Bukkit.getLogger().warning("Invalid item requirement format: " + req);
        continue;
      }

      String namespacedKey = parts[0] + ":" + parts[1];
      Material material = Material.matchMaterial(namespacedKey);
      if (material == null) {
        Bukkit.getLogger().warning("Unknown material: " + namespacedKey);
        continue;
      }

      int amount;
      try {
        amount = Integer.parseInt(parts[2]);
      } catch (NumberFormatException e) {
        Bukkit.getLogger()
            .warning("Invalid quantity for material " + namespacedKey + ": " + parts[2]);
        continue;
      }

      itemRequirements.put(material, amount);
    }

    for (Map.Entry<Material, Integer> entry : itemRequirements.entrySet()) {
      Material material = entry.getKey();
      int requiredAmount = entry.getValue();

      int playerAmount = countItems(player, material);

      if (playerAmount < requiredAmount) {
        Bukkit.getLogger().info("Player does NOT have enough of " + material.name());
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the player has met all required statistics for a specific quest.
   *
   * @param player the player to check
   * @param quest the quest to check against
   * @return true if the player meets all stat requirements, false otherwise
   */
  public boolean hasRequiredStats(Player player, Quest quest) {
    if (quest.getStatRequirements() == null) {
      Bukkit.getLogger().info("No stat requirements for quest: " + quest.getName());
      return true;
    }

    for (String req : quest.getStatRequirements()) {
      Bukkit.getLogger().info("Checking stat requirement: " + req);

      String[] parts = req.split(":");
      if (parts.length != 3) {
        Bukkit.getLogger().warning("Invalid stat requirement format: " + req);
        continue;
      }

      String statName = parts[0];
      String key = parts[1]; // Might be empty
      int requiredAmount;

      try {
        requiredAmount = Integer.parseInt(parts[2]);
        Bukkit.getLogger().info("Parsed required amount: " + requiredAmount);
      } catch (NumberFormatException e) {
        Bukkit.getLogger().warning("Invalid stat quantity in: " + req);
        continue;
      }

      Statistic statistic;
      try {
        statistic = Statistic.valueOf(statName.toUpperCase());
        Bukkit.getLogger().info("Parsed statistic: " + statistic);
      } catch (IllegalArgumentException e) {
        Bukkit.getLogger().warning("Unknown statistic: " + statName);
        continue;
      }

      int currentStat;

      // Handle typed statistics
      if (statistic.getType() == Type.UNTYPED) {
        currentStat = player.getStatistic(statistic);
        Bukkit.getLogger().info("Current stat value (UNTYPED): " + currentStat);
      } else if (statistic.getType() == Type.BLOCK || statistic.getType() == Type.ITEM) {
        Material material = Material.matchMaterial(key);
        if (material == null) {
          Bukkit.getLogger().warning("Invalid material for stat " + statistic + ": " + key);
          continue;
        }
        currentStat = player.getStatistic(statistic, material);
        Bukkit.getLogger()
            .info("Current stat value (BLOCK/ITEM): " + currentStat + " for material: " + material);
      } else if (statistic.getType() == Type.ENTITY) {
        EntityType entityType;
        try {
          entityType = EntityType.valueOf(key.toUpperCase().replace("MINECRAFT:", ""));
          Bukkit.getLogger().info("Parsed entity type: " + entityType);
        } catch (IllegalArgumentException e) {
          Bukkit.getLogger().warning("Invalid entity type for stat " + statistic + ": " + key);
          continue;
        }
        currentStat = player.getStatistic(statistic, entityType);
        Bukkit.getLogger()
            .info("Current stat value (ENTITY): " + currentStat + " for entity: " + entityType);
      } else {
        Bukkit.getLogger().warning("Unsupported stat type: " + statistic);
        continue;
      }

      if (currentStat < requiredAmount) {
        Bukkit.getLogger()
            .info(
                "Player does NOT meet stat "
                    + statistic
                    + " (has "
                    + currentStat
                    + ", needs "
                    + requiredAmount
                    + ")");
        return false;
      } else {
        Bukkit.getLogger()
            .info(
                "Player meets stat "
                    + statistic
                    + " (has "
                    + currentStat
                    + ", needs "
                    + requiredAmount
                    + ")");
      }
    }

    Bukkit.getLogger().info("Player meets all stat requirements for quest: " + quest.getName());
    return true;
  }

  private int countItems(Player player, Material material) {
    return Arrays.stream(player.getInventory().getContents())
        .filter(item -> item != null && item.getType() == material)
        .mapToInt(ItemStack::getAmount)
        .sum();
  }

  /**
   * Consumes the required items from the player's inventory for a specific quest.
   *
   * @param player the player whose inventory to modify
   * @param quest the quest for which to consume items
   */
  public void consumeRequiredItems(Player player, Quest quest) {
    Map<Material, Integer> itemRequirements = new HashMap<>();

    for (String req : quest.getItemRequirements()) {
      String[] parts = req.split(":");
      if (parts.length < 3) {
        Bukkit.getLogger().warning("Invalid item requirement format: " + req);
        continue;
      }

      String namespacedKey = parts[0] + ":" + parts[1];
      Material material = Material.matchMaterial(namespacedKey);
      if (material == null) {
        Bukkit.getLogger().warning("Unknown material: " + namespacedKey);
        continue;
      }

      int amount;
      try {
        amount = Integer.parseInt(parts[2]);
      } catch (NumberFormatException e) {
        Bukkit.getLogger()
            .warning("Invalid quantity for material " + namespacedKey + ": " + parts[2]);
        continue;
      }

      itemRequirements.put(material, amount);
    }

    for (Map.Entry<Material, Integer> entry : itemRequirements.entrySet()) {
      Material material = entry.getKey();
      int remainingToRemove = entry.getValue();

      ItemStack[] contents = player.getInventory().getContents();
      for (int i = 0; i < contents.length; i++) {
        ItemStack item = contents[i];
        if (item != null && item.getType() == material) {
          int amount = item.getAmount();
          if (amount <= remainingToRemove) {
            remainingToRemove -= amount;
            player.getInventory().setItem(i, null);
          } else {
            item.setAmount(amount - remainingToRemove);
            remainingToRemove = 0;
            break;
          }
        }
      }

      if (remainingToRemove > 0) {
        Bukkit.getLogger()
            .warning(
                "Could not remove full amount of "
                    + material.name()
                    + ". Missing "
                    + remainingToRemove);
      }
    }

    player.updateInventory(); // Ensure client reflects changes immediately
  }

  /**
   * Gives the rewards to the player for completing a specific quest.
   *
   * @param player the player to whom to give rewards
   * @param quest the quest for which to give rewards
   */
  public void giveRewards(Player player, Quest quest) {
    for (String reward : quest.getRewards()) {
      String[] parts = reward.split(":");
      if (parts.length < 3) {
        Bukkit.getLogger().warning("Invalid reward format: " + reward);
        continue;
      }

      String namespacedKey = parts[0] + ":" + parts[1];
      int amount;
      try {
        amount = Integer.parseInt(parts[parts.length - 1]);
      } catch (NumberFormatException e) {
        Bukkit.getLogger()
            .warning("Invalid amount in reward: " + parts[2] + " for material " + namespacedKey);
        continue;
      }

      // Check for NBT data
      String nbtData = null;
      if (reward.contains("[")) {
        int nbtStart = reward.indexOf("[");
        int nbtEnd = reward.lastIndexOf("]");
        if (nbtEnd > nbtStart) {
          nbtData = reward.substring(nbtStart, nbtEnd + 1);
        } else {
          Bukkit.getLogger().warning("Invalid NBT data format in reward: " + reward);
        }
      }

      if (nbtData != null) {
        nbtData = preprocessNbtData(nbtData);

        // Manually run the give command with NBT data
        String giveCommand =
            String.format(
                "minecraft:give %s %s%s %d",
                player.getName(),
                namespacedKey.substring(0, namespacedKey.indexOf("[")),
                nbtData,
                amount);
        Bukkit.getLogger().info("Executing command: " + giveCommand);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCommand);
      } else {
        // Create and give the item normally
        Material material = Material.matchMaterial(namespacedKey);
        if (material == null) {
          Bukkit.getLogger().warning("Unknown material in reward: " + namespacedKey);
          continue;
        }

        ItemStack rewardStack = new ItemStack(material, amount);
        HashMap<Integer, ItemStack> notStored = player.getInventory().addItem(rewardStack);
        if (!notStored.isEmpty()) {
          // If inventory is full, drop the item at the player's location
          for (ItemStack leftover : notStored.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
          }
        }
      }
    }
    player.updateInventory(); // Sync inventory with client
  }

  /**
   * Preprocesses the NBT data to replace single quotes with double quotes for JSON-like parts.
   *
   * @param nbtData the raw NBT data string
   * @return the processed NBT data string
   */
  private String preprocessNbtData(String nbtData) {
    StringBuilder processed = new StringBuilder();
    boolean insideQuotes = false;

    for (int i = 0; i < nbtData.length(); i++) {
      char currentChar = nbtData.charAt(i);

      if (currentChar == '\'') {
        // Replace single quotes with double quotes only if inside JSON-like parts
        if (insideQuotes) {
          processed.append('"');
        } else {
          processed.append(currentChar);
        }
      } else {
        processed.append(currentChar);
      }

      // Toggle insideQuotes when encountering a single quote or double quote
      if (currentChar == '{' || currentChar == '[') {
        insideQuotes = true;
      } else if (currentChar == '}' || currentChar == ']') {
        insideQuotes = false;
      }
    }

    return processed.toString();
  }

  /**
   * Unsets the completion status of a quest for a specific player, both in memory and in the
   * database.
   *
   * @param playerUuid the UUID of the player
   * @param questId the ID of the quest to unset completion for
   */
  public void unsetQuestCompletion(UUID playerUuid, String questId) {
    // Remove the quest from the player's completed quest list
    Set<String> playerCompletedQuests = completedQuests.get(playerUuid);
    if (playerCompletedQuests != null) {
      playerCompletedQuests.remove(questId);
      // If the player has no more completed quests, remove their entry from the map
      if (playerCompletedQuests.isEmpty()) {
        completedQuests.remove(playerUuid);
      }
    }

    // Remove the completion from the database
    String sql = "DELETE FROM completed_quests WHERE uuid = ? AND quest_id = ?";
    try (Connection conn = dbManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, playerUuid.toString());
      stmt.setString(2, questId);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Retrieves a list of players who have completed a specific quest.
   *
   * @param quest the quest for which to find completed players
   * @return a list of UUIDs representing players who have completed the quest
   */
  public List<UUID> getCompletedPlayers(Quest quest) {
    return completedQuests.entrySet().stream()
        .filter(entry -> entry.getValue().contains(quest.getId()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * Returns the number of open quests available to the specified player by username.
   *
   * @param playerUuid the UUID of the player
   * @return the number of quests the player has not yet completed
   */
  public int getOpenQuestCount(UUID playerUuid) {
    List<String> displayable = getDisplayableQuestIdsFor(playerUuid);
    Set<String> completed = completedQuests.getOrDefault(playerUuid, new HashSet<>());
    return (int) displayable.stream().filter(id -> !completed.contains(id)).count();
  }

  /**
   * Returns the number of quests the player has completed.
   *
   * @param playerUuid the UUID of the player
   * @return the number of completed quests
   */
  public int getCompletedQuestCount(UUID playerUuid) {
    return completedQuests.getOrDefault(playerUuid, new HashSet<>()).size();
  }

  /**
   * Returns a list of quest IDs that are visible to the given player. Only quests that are either
   * open to all or explicitly include the player are returned.
   *
   * @param playerUuid the UUID of the player
   * @return a list of quest IDs available for display
   */
  public List<String> getDisplayableQuestIdsFor(UUID playerUuid) {
    List<String> displayable = new ArrayList<>();
    for (Map.Entry<String, Quest> entry : allQuests.entrySet()) {
      Quest quest = entry.getValue();
      List<UUID> requiredPlayers = quest.getRequiredPlayers();
      if (requiredPlayers == null
          || requiredPlayers.isEmpty()
          || requiredPlayers.contains(playerUuid)) {
        displayable.add(entry.getKey());
      }
    }
    return displayable;
  }

  /**
   * Retrieves a quest by its unique identifier.
   *
   * @param questId the unique identifier of the quest
   * @return the Quest object corresponding to the given ID, or null if not found
   */
  public Quest getQuestById(String questId) {
    return allQuests.get(questId); // Assuming questDatabase is a Map<String, Quest>
  }

  /**
   * Checks if a quest is completed for a specific player. If the quest has required players, it is
   * only considered completed when all required players have completed it.
   *
   * @param playerId the UUID of the player
   * @param questId the ID of the quest
   * @return true if the quest is completed (globally or individually), false otherwise
   */
  public boolean isQuestCompleted(UUID playerId, String questId) {
    Quest quest = allQuests.get(questId);
    if (quest == null) {
      return false;
    }

    Set<String> completed = completedQuests.get(playerId);
    if (completed == null || !completed.contains(questId)) {
      return false;
    }

    return true;
  }

  /**
   * Loads all quest definitions from the quests.yml configuration file. Existing quest definitions
   * are cleared before loading.
   */
  public void loadQuestsFromYaml() {
    allQuests.clear();
    File file = new File(plugin.getDataFolder(), "quests.yml");

    if (!file.exists()) {
      plugin.getLogger().warning("quests.yml not found. Creating default file...");

      try {
        if (!plugin.getDataFolder().exists()) {
          plugin.getDataFolder().mkdirs();
        }

        plugin.saveResource("quests.yml", false);
      } catch (IllegalArgumentException e) {
        plugin.getLogger().severe("Failed to create default quests.yml: " + e.getMessage());
        return;
      }
    }

    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

    for (String questId : config.getKeys(false)) {
      try {
        String iconName = config.getString(questId + ".icon", "PAPER"); // default to PAPER
        Material icon = Material.matchMaterial(iconName);
        if (icon == null) {
          icon = Material.PAPER;
        }

        String deadlineStr = config.getString(questId + ".deadline");
        LocalDateTime deadline = null;
        if (deadlineStr != null) {
          deadline = LocalDateTime.parse(deadlineStr);
        }

        String name = config.getString(questId + ".name");
        String lore = config.getString(questId + ".lore");
        if (name == null || lore == null) {
          throw new IllegalArgumentException("Missing name or lore.");
        }

        List<UUID> requiredPlayers = new ArrayList<>();
        List<String> requiredPlayerStrings = config.getStringList(questId + ".requiredPlayers");

        for (String username : requiredPlayerStrings) {
          OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
          if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            requiredPlayers.add(offlinePlayer.getUniqueId());
          } else {
            plugin
                .getLogger()
                .warning("Unknown or never-seen player in quest " + questId + ": " + username);
          }
        }

        // Validate item requirements
        List<String> itemRequirements = config.getStringList(questId + ".itemRequirements");
        List<String> validItemRequirements = new ArrayList<>();
        for (String req : itemRequirements) {
          String[] parts = req.split(":");
          if (parts.length < 3) {
            plugin
                .getLogger()
                .warning("Invalid item requirement format in quest " + questId + ": " + req);
            throw new IllegalArgumentException("Invalid item requirement format.");
          }

          String namespacedKey = parts[0] + ":" + parts[1];
          Material material = Material.matchMaterial(namespacedKey);
          if (material == null) {
            plugin
                .getLogger()
                .warning(
                    "Unknown material in item requirement for quest "
                        + questId
                        + ": "
                        + namespacedKey);
            throw new IllegalArgumentException("Unknown material in item requirement.");
          }

          try {
            Integer.parseInt(parts[2]); // Validate the amount
          } catch (NumberFormatException e) {
            plugin
                .getLogger()
                .warning("Invalid quantity in item requirement for quest " + questId + ": " + req);
            throw new IllegalArgumentException("Invalid quantity in item requirement.");
          }

          validItemRequirements.add(req);
        }

        // Validate stat requirements
        List<String> statRequirements = config.getStringList(questId + ".statRequirements");
        List<String> validStatRequirements = new ArrayList<>();
        for (String req : statRequirements) {
          String[] parts = req.split(":");
          if (parts.length != 3) {
            plugin
                .getLogger()
                .warning("Invalid stat requirement format in quest " + questId + ": " + req);
            throw new IllegalArgumentException("Invalid stat requirement format.");
          }

          String statName = parts[0];
          try {
            Statistic.valueOf(statName.toUpperCase()); // Validate the statistic
          } catch (IllegalArgumentException e) {
            plugin
                .getLogger()
                .warning(
                    "Unknown statistic in stat requirement for quest " + questId + ": " + statName);
            throw new IllegalArgumentException("Unknown statistic in stat requirement.");
          }

          try {
            Integer.parseInt(parts[2]); // Validate the required amount
          } catch (NumberFormatException e) {
            plugin
                .getLogger()
                .warning("Invalid quantity in stat requirement for quest " + questId + ": " + req);
            throw new IllegalArgumentException("Invalid quantity in stat requirement.");
          }

          validStatRequirements.add(req);
        }

        List<String> rewards = config.getStringList(questId + ".rewards");
        List<String> validRewards = new ArrayList<>();

        for (String reward : rewards) {
          String raw = reward.startsWith("item:") ? reward.substring(5) : reward;

          int amountIndex = raw.lastIndexOf(":");
          if (amountIndex == -1) {
            plugin.getLogger().warning("Invalid reward format in quest " + questId + ": " + reward);
            throw new IllegalArgumentException("Invalid reward format.");
          }

          String idAndNbt = raw.substring(0, amountIndex);
          String amountStr = raw.substring(amountIndex + 1);

          // Split material and NBT
          String materialKey;
          String nbtData = null;
          if (idAndNbt.contains("[")) {
            int nbtStart = idAndNbt.indexOf("[");
            materialKey = idAndNbt.substring(0, nbtStart);
            nbtData = idAndNbt.substring(nbtStart);
          } else {
            materialKey = idAndNbt;
          }

          Material material = Material.matchMaterial(materialKey);
          if (material == null) {
            plugin
                .getLogger()
                .warning("Unknown material in reward for quest " + questId + ": " + materialKey);
            throw new IllegalArgumentException("Unknown material in reward.");
          }

          try {
            Integer.parseInt(amountStr);
          } catch (NumberFormatException e) {
            plugin
                .getLogger()
                .warning("Invalid quantity in reward for quest " + questId + ": " + reward);
            throw new IllegalArgumentException("Invalid quantity in reward.");
          }

          if (nbtData != null) {
            if (!nbtData.contains("custom_name")) {
              plugin
                  .getLogger()
                  .warning(
                      "NBT data in reward for quest "
                          + questId
                          + " must contain 'custom_name': "
                          + nbtData);
              throw new IllegalArgumentException("NBT data must contain 'custom_name'.");
            }
          }
          validRewards.add(reward);
        }

        Quest quest =
            new Quest(
                questId,
                name,
                lore,
                validItemRequirements,
                validStatRequirements,
                validRewards,
                requiredPlayers,
                deadline,
                icon);
        allQuests.put(questId, quest);
      } catch (Exception e) {
        plugin.getLogger().severe("Failed to load quest '" + questId + "': " + e.getMessage());
        continue;
      }
    }
  }

  /**
   * Loads the mapping of players to completed quests from the database. This populates the {@code
   * completedQuests} map.
   */
  public void loadCompletedQuestsFromDb() {
    completedQuests.clear();
    String sql = "SELECT uuid, quest_id FROM completed_quests";

    try (Connection conn = dbManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String questId = rs.getString("quest_id");
        completedQuests.computeIfAbsent(uuid, k -> new HashSet<>()).add(questId);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns a list of quests available to a specific player. These are quests the player has not
   * yet completed and that are either open or explicitly assigned.
   *
   * @param playerUuid the UUID of the player
   * @return a list of quests available for the player to complete
   */
  public List<Quest> getAvailableQuestsFor(UUID playerUuid) {
    return allQuests.values().stream()
        .filter(
            q -> q.getRequiredPlayers().isEmpty() || q.getRequiredPlayers().contains(playerUuid))
        .filter(
            q ->
                !completedQuests
                    .getOrDefault(playerUuid, Collections.emptySet())
                    .contains(q.getId()))
        .collect(Collectors.toList());
  }

  /**
   * Marks a quest as completed for a given player and persists the change in the database.
   *
   * @param playerUuid the UUID of the player
   * @param questId the ID of the completed quest
   */
  public void markQuestCompleted(UUID playerUuid, String questId) {
    completedQuests.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(questId);

    try (PreparedStatement stmt =
        dbManager
            .getConnection()
            .prepareStatement(
                "INSERT OR IGNORE INTO completed_quests (uuid, quest_id) VALUES (?, ?)")) {
      stmt.setString(1, playerUuid.toString());
      stmt.setString(2, questId);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
