package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/** Represents a consumable item that allows players to open an anvil anywhere. */
public class AnvilConsumable implements ItemConsumable {
  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  @Override
  public String getItemModel() {
    return "storytime:anvil_token";
  }

  @Override
  public Component getName() {
    return Component.text("Anvil Token");
  }

  @Override
  public List<Component> getLore() {
    return List.of(Component.text("Consume to open an anvil anywhere!"));
  }

  @Override
  public String getCommand() {
    return "anvil";
  }

  @Override
  public List<String> getParameterNames() {
    return List.of();
  }

  @Override
  public String getPermissionNode() {
    return "essentials.anvil";
  }
}
