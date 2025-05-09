package com.storytimeproductions.stweaks.util;

import com.storytimeproductions.models.Quest;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
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

  /** Reloads all quests from both the YAML file and the database. */
  public void reloadQuests() {
    // Clear current quests
    allQuests.clear();

    // Reload from YAML
    loadQuestsFromYaml();

    // Reload from database
    loadCompletedQuestsFromDb();
  }

  /**
   * Returns the number of open quests available to the specified player.
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
   * Checks if a quest is completed for a specific player.
   *
   * @param playerId the UUID of the player
   * @param questId the ID of the quest
   * @return true if the quest is completed, false otherwise
   */
  public boolean isQuestCompleted(UUID playerId, String questId) {
    Set<String> completed = completedQuests.get(playerId);
    return completed != null && completed.contains(questId);
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

    int loadedCount = 0;
    for (String questId : config.getKeys(false)) {
      try {
        String name = config.getString(questId + ".name");
        String lore = config.getString(questId + ".lore");
        String iconName = config.getString(questId + ".icon", "PAPER"); // default to PAPER
        Material icon = Material.matchMaterial(iconName);
        if (icon == null) {
          plugin
              .getLogger()
              .warning(
                  "Invalid icon material in quest "
                      + questId
                      + ": "
                      + iconName
                      + ". Defaulting to PAPER.");
          icon = Material.PAPER;
        }
        List<String> requirements = config.getStringList(questId + ".itemRequirements");
        List<String> rewards = config.getStringList(questId + ".rewards");
        List<String> requiredPlayerStrings = config.getStringList(questId + ".requiredPlayers");
        LocalDateTime deadline = config.getObject(questId + ".deadline", LocalDateTime.class);

        if (name == null || lore == null) {
          throw new IllegalArgumentException("Missing name or lore.");
        }

        List<UUID> requiredPlayers = new ArrayList<>();
        for (String s : requiredPlayerStrings) {
          try {
            requiredPlayers.add(UUID.fromString(s));
          } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID in quest " + questId + ": " + s);
          }
        }

        Quest quest =
            new Quest(questId, name, lore, requirements, rewards, requiredPlayers, deadline, icon);
        allQuests.put(questId, quest);
        loadedCount++;

      } catch (Exception e) {
        plugin.getLogger().severe("Failed to load quest '" + questId + "': " + e.getMessage());
      }
    }

    plugin.getLogger().info("Loaded " + loadedCount + " quests from quests.yml.");
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

      plugin
          .getLogger()
          .info("Loaded completed quests for " + completedQuests.size() + " players.");
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
