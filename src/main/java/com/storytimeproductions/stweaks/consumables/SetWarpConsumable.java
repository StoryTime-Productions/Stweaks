package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/** Consumable item that allows a player to use /setwarp with a warp name. */
public class SetWarpConsumable implements ItemConsumable {

  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  @Override
  public String getItemModel() {
    return "storytime:setwarp_token";
  }

  @Override
  public Component getName() {
    return Component.text("Set Warp Token");
  }

  @Override
  public List<Component> getLore() {
    return List.of(
        Component.text("Consume to set a warp at your location."),
        Component.text("You will be prompted for a warp name."));
  }

  @Override
  public String getCommand() {
    return "setwarp";
  }

  @Override
  public List<String> getParameterNames() {
    return List.of("warpName");
  }

  @Override
  public String getPermissionNode() {
    return "essentials.setwarp";
  }
}
