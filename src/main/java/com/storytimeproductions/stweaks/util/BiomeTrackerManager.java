package com.storytimeproductions.stweaks.util;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages tracking of biomes discovered by players. The class interacts with the database to
 * persist and retrieve biome discovery data.
 */
public class BiomeTrackerManager {

  private final DbManager dbManager;
  private final JavaPlugin plugin;
  private final Map<String, Material> biomeItemMap = new HashMap<>();

  /**
   * Constructs a new BiomeTrackerManager instance.
   *
   * @param dbManager The DbManager instance used to interact with the SQLite database.
   */
  public BiomeTrackerManager(DbManager dbManager, JavaPlugin plugin) {
    this.dbManager = dbManager;
    this.plugin = plugin;
  }

  /**
   * Retrieves a set of biomes that the player with the given UUID has discovered.
   *
   * @param uuid The UUID of the player.
   * @return A set of biome keys that the player has discovered.
   */
  public Set<String> getDiscoveredBiomes(UUID uuid) {
    Set<String> biomes = new HashSet<>();
    String query = "SELECT biome_key FROM discovered_biomes WHERE uuid = ?";
    try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
      stmt.setString(1, uuid.toString());
      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        biomes.add(rs.getString("biome_key"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return biomes;
  }

  /**
   * Marks a biome as discovered for the player with the given UUID. If the biome is not already
   * marked as discovered, it will be inserted into the database.
   *
   * @param uuid The UUID of the player.
   * @param biomeKey The biome key to mark as discovered.
   * @return true if this is the first time the player has discovered this biome, false otherwise.
   */
  public boolean markBiomeDiscovered(UUID uuid, String biomeKey) {
    boolean isFirstDiscovery = false;
    String checkQuery = "SELECT 1 FROM discovered_biomes WHERE uuid = ? AND biome_key = ?";
    try (PreparedStatement checkStmt = dbManager.getConnection().prepareStatement(checkQuery)) {
      checkStmt.setString(1, uuid.toString());
      checkStmt.setString(2, biomeKey);
      ResultSet rs = checkStmt.executeQuery();
      if (!rs.next()) {
        isFirstDiscovery = true;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    String query = "INSERT OR IGNORE INTO discovered_biomes(uuid, biome_key) VALUES (?, ?)";
    try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {
      stmt.setString(1, uuid.toString());
      stmt.setString(2, biomeKey);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return isFirstDiscovery;
  }

  /**
   * Synchronizes and loads the biome-to-item mapping from the biome_item.yml configuration file. It
   * ensures that any biomes that are not already mapped in the file are added, and any biomes that
   * are no longer relevant are removed from the in-memory map.
   *
   * <p>The method reads the configuration file to populate the {@link #biomeItemMap} and ensures
   * that the biome items are correctly associated with their respective biomes.
   *
   * @param allBiomes A set of all available biomes in the game. This is used to ensure that the
   *     configuration file is in sync with the current set of biomes.
   */
  public void syncAndLoadBiomeItems(Set<Biome> allBiomes) {
    File file = new File(plugin.getDataFolder(), "biome_item.yml");
    FileConfiguration config = YamlConfiguration.loadConfiguration(file);

    // Step 1: Format current biome names
    Set<String> currentBiomeNames = new HashSet<>();
    for (Biome biome : allBiomes) {
      String formatted = formatBiomeName(biome.getKey().toString());
      currentBiomeNames.add(formatted);

      // Add missing entries
      if (!config.contains(formatted)) {
        config.set(formatted, Material.PAPER.toString());
      }
    }

    // Step 2: Remove outdated entries
    Set<String> keysToRemove = new HashSet<>();
    for (String key : config.getKeys(false)) {
      if (!currentBiomeNames.contains(key)) {
        keysToRemove.add(key);
      }
    }
    for (String key : keysToRemove) {
      config.set(key, null);
    }

    // Step 3: Save updated config
    try {
      config.save(file);
    } catch (IOException e) {
      plugin.getLogger().severe("Could not save biome_item.yml: " + e.getMessage());
    }

    // Step 4: Load into map
    biomeItemMap.clear();
    for (String biomeName : config.getKeys(false)) {
      try {
        Material mat = Material.valueOf(config.getString(biomeName));
        biomeItemMap.put(biomeName, mat);
      } catch (IllegalArgumentException e) {
        plugin.getLogger().warning("Invalid material for biome '" + biomeName + "'");
      }
    }
  }

  /**
   * Formats the biome key into a human-readable name by capitalizing the first letter of each word
   * and replacing underscores with spaces.
   *
   * @param key The biome key (e.g., "minecraft/desert").
   * @return The formatted biome name (e.g., "Desert").
   */
  public static String formatBiomeName(String key) {
    String rawName = key.toLowerCase();

    // Use the part after '/' if present, otherwise after ':'
    if (rawName.contains("/")) {
      rawName = rawName.substring(rawName.indexOf("/") + 1);
    } else if (rawName.contains(":")) {
      rawName = rawName.substring(rawName.indexOf(":") + 1);
    }

    // Replace underscores with spaces
    rawName = rawName.replace('_', ' ');

    // Capitalize each word
    String[] words = rawName.split(" ");
    StringBuilder formatted = new StringBuilder();
    for (String word : words) {
      if (!word.isEmpty()) {
        formatted
            .append(Character.toUpperCase(word.charAt(0)))
            .append(word.substring(1))
            .append(" ");
      }
    }

    return formatted.toString().trim();
  }
}
