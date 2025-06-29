package com.storytimeproductions.stweaks.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Manages the loading and retrieval of cosmetic items from configuration. */
public class CosmeticsManager {

  private static final Map<String, Cosmetic> cosmetics = new HashMap<>();
  private FileConfiguration config;
  private final JavaPlugin plugin;

  /**
   * Constructs a new CosmeticsManager and loads cosmetics from configuration.
   *
   * @param plugin the JavaPlugin instance
   */
  public CosmeticsManager(JavaPlugin plugin) {
    this.plugin = plugin;
    loadCosmeticsConfig();
    this.loadCosmetics();
  }

  /** Loads the cosmetics.yml configuration file. */
  private void loadCosmeticsConfig() {
    File petsFile = new File(plugin.getDataFolder(), "cosmetics.yml");
    if (!petsFile.exists()) {
      plugin.saveResource("cosmetics.yml", false);
    }
    config = YamlConfiguration.loadConfiguration(petsFile);
  }

  /**
   * Loads all cosmetics from the configuration file into memory. Invalid or incomplete cosmetics
   * are skipped.
   */
  public void loadCosmetics() {
    cosmetics.clear();

    loadCosmeticsConfig();

    for (String id : config.getConfigurationSection("cosmetics").getKeys(false)) {
      String path = "cosmetics." + id;

      String itemModel = config.getString(path + ".item_model");
      String name = config.getString(path + ".name");
      List<String> recipeStrings = config.getStringList(path + ".recipe");

      if (itemModel == null || name == null || recipeStrings.size() != 3) {
        Bukkit.getLogger()
            .warning("Skipping cosmetic '" + id + "' due to missing or invalid fields.");
        continue;
      }

      Material[][] recipe = new Material[3][3];
      boolean valid = true;

      for (int row = 0; row < 3; row++) {
        String[] split = recipeStrings.get(row).split(" ");
        if (split.length != 3) {
          valid = false;
          break;
        }

        for (int col = 0; col < 3; col++) {
          String itemName = split[col];
          if (itemName.equalsIgnoreCase("AIR")) {
            recipe[row][col] = Material.AIR;
          } else {
            Material mat = Material.matchMaterial(itemName);
            if (mat == null) {
              Bukkit.getLogger()
                  .log(
                      Level.WARNING,
                      "Invalid material '{0}' in recipe for {1}",
                      new Object[] {itemName, id});
              valid = false;
              break;
            }
            recipe[row][col] = mat;
          }
        }
      }

      if (!valid) {
        Bukkit.getLogger().warning("Skipping cosmetic '" + id + "' due to invalid recipe.");
        continue;
      }

      List<String> lore = config.getStringList(path + ".lore");
      Cosmetic cosmetic = new Cosmetic(id, itemModel, name, lore, recipe);
      cosmetics.put(id, cosmetic);
    }
  }

  /**
   * Gets a list of all loaded cosmetics.
   *
   * @return a list of all Cosmetic objects
   */
  public static List<Cosmetic> getAllCosmetics() {
    return new ArrayList<>(cosmetics.values());
  }

  /**
   * Gets a cosmetic by its unique identifier.
   *
   * @param id the cosmetic ID
   * @return the Cosmetic object, or null if not found
   */
  public static Cosmetic getCosmetic(String id) {
    return cosmetics.get(id);
  }

  /**
   * Gets a cosmetic by its unique identifier.
   *
   * @param id the cosmetic ID
   * @return the Cosmetic object, or null if not found
   */
  public static Cosmetic getCosmeticById(String id) {
    return cosmetics.get(id);
  }
}
