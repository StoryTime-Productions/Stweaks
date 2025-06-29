package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/**
 * Represents a consumable item that allows players to change their own size using /entitysize
 * 'size'.
 */
public class PlayerSizeConsumable implements ItemConsumable {
  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  @Override
  public String getItemModel() {
    return "storytime:playersize_token";
  }

  @Override
  public Component getName() {
    return Component.text("Player Size Token");
  }

  @Override
  public List<Component> getLore() {
    return List.of(
        Component.text("Consume to change your size!"),
        Component.text("You will be prompted for a size value."));
  }

  @Override
  public String getCommand() {
    return "entitysize";
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
    return "entitysize.self";
  }
}
