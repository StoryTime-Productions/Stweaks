package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/**
 * Represents a consumable item that allows players to change the size of the entity they are
 * looking at using /entitysize entity looking 'size'.
 */
public class EntitySizeConsumable implements ItemConsumable {
  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  @Override
  public String getItemModel() {
    return "storytime:entitysize_token";
  }

  @Override
  public Component getName() {
    return Component.text("Entity Size Token");
  }

  @Override
  public List<Component> getLore() {
    return List.of(
        Component.text("Consume to change the size of the entity you are looking at!"),
        Component.text("You will be prompted for a size value."));
  }

  @Override
  public String getCommand() {
    return "entitysize entity looking";
  }

  @Override
  public List<String> getParameterNames() {
    return List.of("size");
  }

  @Override
  public List<String> getParameterTypes() {
    return List.of("numeric");
  }

  @Override
  public String getPermissionNode() {
    return "entitysize.entity.looking";
  }
}
