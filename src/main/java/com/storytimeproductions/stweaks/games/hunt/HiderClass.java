package com.storytimeproductions.stweaks.games.hunt;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Represents the different classes available for hiders. */
public enum HiderClass {
  TRICKSTER(
      "Trickster",
      "Sets counter-traps against hunters",
      new ItemStack(Material.TRIPWIRE_HOOK),
      new ItemStack(Material.LEATHER_CHESTPLATE)),

  PHASER(
      "Phaser",
      "Can phase through walls temporarily",
      new ItemStack(Material.ENDER_PEARL),
      new ItemStack(Material.CHAINMAIL_CHESTPLATE)),

  CLOAKER(
      "Cloaker",
      "Can become invisible for limited time",
      new ItemStack(Material.POTION),
      new ItemStack(Material.IRON_CHESTPLATE));

  private final String displayName;
  private final String description;
  private final ItemStack tool;
  private final ItemStack armor;

  HiderClass(String displayName, String description, ItemStack tool, ItemStack armor) {
    this.displayName = displayName;
    this.description = description;
    this.tool = tool;
    this.armor = armor;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public ItemStack getTool() {
    return tool.clone();
  }

  public ItemStack getArmor() {
    return armor.clone();
  }
}
