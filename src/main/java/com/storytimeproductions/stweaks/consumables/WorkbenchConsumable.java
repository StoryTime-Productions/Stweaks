package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/** Represents a consumable item that allows players to open a workbench anywhere. */
public class WorkbenchConsumable implements ItemConsumable {
  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  @Override
  public String getItemModel() {
    return "storytime:workbench_token";
  }

  @Override
  public Component getName() {
    return Component.text("Workbench Token");
  }

  @Override
  public List<Component> getLore() {
    return List.of(Component.text("Consume to open a workbench anywhere!"));
  }

  @Override
  public String getCommand() {
    return "workbench";
  }

  @Override
  public List<String> getParameterNames() {
    return List.of();
  }

  @Override
  public String getPermissionNode() {
    return "essentials.workbench";
  }
}
