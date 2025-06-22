package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/**
 * Represents a consumable item that allows players to return to a previously teleported region.
 * This item is represented by a paper with a specific model and lore.
 */
public class BackConsumable implements ItemConsumable {
  /**
   * Returns the base material for this consumable item.
   *
   * @return the base material, which is PAPER for this consumable.
   */
  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  /**
   * Returns the unique item model identifier for this consumable.
   *
   * @return a string representing the item model, e.g., "storytime:back"
   */
  @Override
  public String getItemModel() {
    return "storytime:back_token";
  }

  /**
   * Returns the display name for this consumable item.
   *
   * @return a Component representing the name of the item, e.g., "Back Token".
   */
  @Override
  public Component getName() {
    return Component.text("Back Token");
  }

  /**
   * Returns the lore for this consumable item.
   *
   * <p>context or instructions for the player.
   */
  @Override
  public List<Component> getLore() {
    return List.of(Component.text("Use to return to a previously teleported region!"));
  }

  /**
   * Returns the command associated with this consumable item.
   *
   * @return a string representing the command to execute when this item is used, e.g., "back".
   */
  @Override
  public String getCommand() {
    return "back";
  }

  /**
   * Returns the names of parameters required by the command associated with this consumable.
   *
   * @return an empty list, as this consumable does not require any parameters.
   */
  @Override
  public List<String> getParameterNames() {
    return List.of();
  }

  /**
   * Returns the permission node required to use this consumable item.
   *
   * @return a string representing the permission node, or null if no permission
   */
  @Override
  public String getPermissionNode() {
    return "essentials.back";
  }
}
