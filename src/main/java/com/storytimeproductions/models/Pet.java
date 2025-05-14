package com.storytimeproductions.models;

import java.util.List;
import org.bukkit.Material;

/**
 * Represents a custom pet in the plugin, which includes metadata such as a subtitle, special perk
 * behavior, sound effects, quotes, required food, and a crafting recipe.
 *
 * <p>Instances of this class are loaded and managed by the PetsManager.
 */
public class Pet {
  private final String id;
  private final String subtitle;
  private final String perk;
  private final List<String> burpSounds;
  private final List<String> quotes;
  private final Material food;
  private final List<List<Material>> recipe;

  /**
   * Constructs a new Pet object with the provided attributes.
   *
   * @param id Unique string identifier for the pet (used as internal key).
   * @param subtitle Short description or subtitle of the pet (used for UI).
   * @param perk String representing the effect or item bonus this pet provides. Format is either
   *     "effect:'effect_name':'duration'" or "item:'item_name':'amount'".
   * @param burpSounds List of custom sound names the pet may play randomly. Format should be like
   *     "storytime:'sound_id'".
   * @param quotes List of fun or thematic quotes that the pet may say at intervals.
   * @param food The material that must be present in a player's inventory for the perk to activate.
   * @param recipe 3x3 crafting matrix representing the materials used to craft this pet.
   */
  public Pet(
      String id,
      String subtitle,
      String perk,
      List<String> burpSounds,
      List<String> quotes,
      Material food,
      List<List<Material>> recipe) {
    this.id = id;
    this.subtitle = subtitle;
    this.perk = perk;
    this.burpSounds = burpSounds;
    this.quotes = quotes;
    this.food = food;
    this.recipe = recipe;
  }

  /**
   * Returns the unique identifier of the pet.
   *
   * @return pet ID
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the subtitle or description of the pet.
   *
   * @return pet subtitle
   */
  public String getSubtitle() {
    return subtitle;
  }

  /**
   * Returns the perk string that defines what the pet does. Example: "effect:speed:200" or
   * "item:gold_ingot:1"
   *
   * @return perk definition string
   */
  public String getPerk() {
    return perk;
  }

  /**
   * Returns the list of sound effect names this pet may randomly play.
   *
   * @return list of sound names
   */
  public List<String> getBurpSounds() {
    return burpSounds;
  }

  /**
   * Returns the list of quotes the pet may say at random intervals.
   *
   * @return list of pet quotes
   */
  public List<String> getQuotes() {
    return quotes;
  }

  /**
   * Returns the food item required for the pet's perk to activate.
   *
   * @return Material representing the required food
   */
  public Material getFood() {
    return food;
  }

  /**
   * Returns the crafting recipe as a 3x3 matrix of materials. Each inner list represents a row in
   * the crafting grid.
   *
   * @return crafting recipe matrix
   */
  public List<List<Material>> getRecipe() {
    return recipe;
  }
}
