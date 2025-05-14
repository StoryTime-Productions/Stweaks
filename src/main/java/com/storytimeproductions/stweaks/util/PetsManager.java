package com.storytimeproductions.stweaks.util;

import com.storytimeproductions.models.Pet;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

/**
 * Manages the loading and retrieval of pets from the pets.yml configuration file. This class
 * handles parsing the configuration, validating pet data, and providing access to the loaded pets.
 */
public class PetsManager {
  private final List<Pet> allPets = new ArrayList<>();
  private final JavaPlugin plugin;
  private FileConfiguration petsConfig;

  /**
   * Constructs a PetsManager instance.
   *
   * @param plugin The main plugin instance.
   */
  public PetsManager(JavaPlugin plugin) {
    this.plugin = plugin;
    loadPetsConfig();
    loadPets();
  }

  /** Loads the pets.yml configuration file. */
  private void loadPetsConfig() {
    File petsFile = new File(plugin.getDataFolder(), "pets.yml");
    if (!petsFile.exists()) {
      plugin.saveResource("pets.yml", false);
    }
    petsConfig = YamlConfiguration.loadConfiguration(petsFile);
  }

  /** Loads pets from the pets.yml configuration file. */
  public void loadPets() {
    loadPetsConfig();

    allPets.clear();

    ConfigurationSection section = petsConfig.getConfigurationSection("pets");
    if (section == null) {
      return;
    }

    List<List<Material>> recipeMatrix = null;

    for (String id : section.getKeys(false)) {
      ConfigurationSection petSec = section.getConfigurationSection(id);
      if (petSec == null) {
        Bukkit.getLogger()
            .warning("Skipping pet '" + id + "' due to missing configuration section.");
        continue;
      }

      String subtitle = petSec.getString("subtitle");
      String perk = petSec.getString("perk");
      List<String> burps = petSec.getStringList("burp_sounds");
      List<String> quotes = petSec.getStringList("quotes");
      String foodStr = petSec.getString("food");

      if (subtitle == null
          || perk == null
          || burps.isEmpty()
          || quotes.isEmpty()
          || foodStr == null) {
        Bukkit.getLogger().warning("Skipping pet '" + id + "' due to missing fields.");
        continue;
      }

      if (!isValidPerk(perk)) {
        Bukkit.getLogger().warning("Invalid perk for pet '" + id + "': " + perk);
        continue;
      }

      Material foodMat = Material.matchMaterial(foodStr);
      if (foodMat == null) {
        Bukkit.getLogger().warning("Invalid food material for pet '" + id + "': " + foodStr);
        continue;
      }

      if (petSec.isList("recipe")) {
        List<?> recipeList = petSec.getList("recipe");

        if (recipeList == null || recipeList.size() != 3) {
          Bukkit.getLogger()
              .warning("Invalid recipe for pet '" + id + "': Must have exactly 3 rows.");
          continue;
        }

        recipeMatrix = new ArrayList<>();
        boolean validRecipe = true;

        for (Object rowObj : recipeList) {
          List<Material> rowMaterials = new ArrayList<>();

          if (rowObj instanceof List<?>) {
            List<?> row = (List<?>) rowObj;

            if (row.size() != 3) {
              Bukkit.getLogger()
                  .warning("Invalid recipe row for pet '" + id + "': Must have exactly 3 columns.");
              validRecipe = false;
              break;
            }

            for (Object itemObj : row) {
              if (itemObj instanceof String) {
                String itemStr = ((String) itemObj).trim();
                // Remove "minecraft:" prefix if present
                if (itemStr.startsWith("minecraft:")) {
                  itemStr = itemStr.substring(10);
                }

                if (itemStr.equals("air") || itemStr.isEmpty()) {
                  rowMaterials.add(Material.AIR);
                } else {
                  Material mat = Material.matchMaterial(itemStr);
                  if (mat == null) {
                    Bukkit.getLogger()
                        .warning("Unknown material in recipe for pet '" + id + "': " + itemStr);
                    validRecipe = false;
                    break;
                  }
                  rowMaterials.add(mat);
                }
              } else {
                Bukkit.getLogger()
                    .warning("Invalid item type in recipe for pet '" + id + "': " + itemObj);
                validRecipe = false;
                break;
              }
            }
          } else {
            Bukkit.getLogger().warning("Invalid row format in recipe for pet '" + id + "'");
            validRecipe = false;
            break;
          }

          if (!validRecipe) {
            break;
          }
          recipeMatrix.add(rowMaterials);
        }

        if (!validRecipe) {
          recipeMatrix = null;
          // Not continuing because we still want to add the pet even if the recipe is
          // invalid
        }
      }

      Pet pet = new Pet(id, subtitle, perk, burps, quotes, foodMat, recipeMatrix);
      allPets.add(pet);
    }

    Bukkit.getLogger().info("Loaded " + allPets.size() + " valid pets.");
  }

  private boolean isValidPerk(String perk) {
    if (perk.startsWith("item:")) {
      String[] parts = perk.split(":");
      if (parts.length != 2) {
        return false;
      }
      Material item = Material.matchMaterial(parts[1]);
      return item != null;
    } else if (perk.startsWith("effect:")) {
      String[] parts = perk.split(":");
      if (parts.length != 2) {
        return false;
      }
      try {
        NamespacedKey key = NamespacedKey.minecraft(parts[1].toLowerCase());
        PotionEffectType effect = Registry.POTION_EFFECT_TYPE.get(key);
        return effect != null;
      } catch (Exception e) {
        return false;
      }
    }
    return false;
  }

  /**
   * Retrieves a Pet by its ID.
   *
   * @param petId The ID of the pet to retrieve.
   * @return The Pet object if found, otherwise null.
   */
  public Pet getPetById(String petId) {
    for (Pet pet : allPets) {
      if (pet.getId().equals(petId)) {
        return pet;
      }
    }
    return null;
  }

  public List<Pet> getAllPets() {
    return allPets;
  }
}
