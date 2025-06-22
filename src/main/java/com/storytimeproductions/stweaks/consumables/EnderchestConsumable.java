package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/** Represents a consumable item that allows players to open their ender chest anywhere. */
public class EnderchestConsumable implements ItemConsumable {
  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  @Override
  public String getItemModel() {
    return "storytime:enderchest_token";
  }

  @Override
  public Component getName() {
    return Component.text("Ender Chest Token");
  }

  @Override
  public List<Component> getLore() {
    return List.of(Component.text("Consume to open your ender chest anywhere!"));
  }

  @Override
  public String getCommand() {
    return "enderchest";
  }

  @Override
  public List<String> getParameterNames() {
    return List.of();
  }

  @Override
  public String getPermissionNode() {
    return "essentials.enderchest";
  }
}
