package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/** Represents a consumable item that allows players to open a smithing table anywhere. */
public class SmithingTableConsumable implements ItemConsumable {
  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  @Override
  public String getItemModel() {
    return "storytime:smithingtable_token";
  }

  @Override
  public Component getName() {
    return Component.text("Smithing Table Token");
  }

  @Override
  public List<Component> getLore() {
    return List.of(Component.text("Consume to open a smithing table anywhere!"));
  }

  @Override
  public String getCommand() {
    return "smithingtable";
  }

  @Override
  public List<String> getParameterNames() {
    return List.of();
  }

  @Override
  public String getPermissionNode() {
    return "essentials.smithingtable";
  }
}
