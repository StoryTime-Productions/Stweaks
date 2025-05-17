package com.storytimeproductions.stweaks.util;

import java.util.List;
import org.bukkit.Material;

/** Represents a cosmetic item with an ID, item model, display name, lore, and crafting recipe. */
public class Cosmetic {
  private final String id;
  private final String itemModel;
  private final String displayName;
  private final List<String> lore;
  private final Material[][] recipe;

  /**
   * Constructs a new Cosmetic.
   *
   * @param id the unique identifier for the cosmetic
   * @param itemModel the item model identifier
   * @param displayName the display name of the cosmetic
   * @param lore the lore (description) of the cosmetic
   * @param recipe the crafting recipe for the cosmetic
   */
  public Cosmetic(
      String id, String itemModel, String displayName, List<String> lore, Material[][] recipe) {
    this.id = id;
    this.itemModel = itemModel;
    this.displayName = displayName;
    this.lore = lore;
    this.recipe = recipe;
  }

  /**
   * Gets the unique identifier for this cosmetic.
   *
   * @return the cosmetic ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the item model identifier.
   *
   * @return the item model
   */
  public String getItemModel() {
    return itemModel;
  }

  /**
   * Gets the display name of the cosmetic.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the lore (description) of the cosmetic.
   *
   * @return the lore as a list of strings
   */
  public List<String> getLore() {
    return lore;
  }

  /**
   * Gets the crafting recipe for the cosmetic.
   *
   * @return the recipe as a 2D array of Material
   */
  public Material[][] getRecipe() {
    return recipe;
  }
}
