package com.storytimeproductions.models;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/** Represents a consumable item in the game that can be used by players. */
public interface ItemConsumable {
  /**
   * Returns the base material for this consumable item.
   *
   * @return the base material, e.g., Material.PAPER for a paper item.
   */
  Material getBaseMaterial();

  /**
   * Returns the unique item model identifier for this consumable.
   *
   * @return a string representing the item model, e.g., "storytime:back_token".
   */
  String getItemModel(); // e.g. "storytime:back_token"

  /**
   * Returns the display name for this consumable item.
   *
   * @return a Component representing the name of the item, e.g., "Back Token".
   */
  Component getName();

  /**
   * Returns the lore for this consumable item.
   *
   * @return a List of Components representing the lore of the item,
   */
  List<Component> getLore();

  /**
   * Returns the command associated with this consumable item.
   *
   * @return a string representing the command to execute when this item is used, e.g., "back".
   */
  String getCommand(); // e.g. "back", "setwarp"

  /**
   * Returns a list of parameter names required for the command associated with this consumable.
   *
   * @return a List of strings representing the parameter names, e.g., ["warpName"].
   */
  List<String> getParameterNames(); // e.g. ["warpName"]

  /**
   * Returns the permission node required to use this consumable item.
   *
   * @return a string representing the permission node, or null if no
   */
  String getPermissionNode(); // nullable

  /**
   * Returns a list of parameter types for each parameter name. Each value should be one of:
   * "numeric", "alpha", or "alphanumeric" (default if not specified). The list must be the same
   * length as getParameterNames().
   *
   * @return a List of strings representing the type for each parameter.
   */
  default List<String> getParameterTypes() {
    return getParameterNames().stream().map(p -> "alphanumeric").toList();
  }
}
