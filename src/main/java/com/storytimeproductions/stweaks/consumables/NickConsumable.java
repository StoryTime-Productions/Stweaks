package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/** Represents a consumable item that allows players to change their nickname. */
public class NickConsumable implements ItemConsumable {
  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  @Override
  public String getItemModel() {
    return "storytime:nick_token";
  }

  @Override
  public Component getName() {
    return Component.text("Nick Token");
  }

  @Override
  public List<Component> getLore() {
    return List.of(
        Component.text("Consume to change your nickname!"),
        Component.text("You will be prompted for a nickname."));
  }

  @Override
  public String getCommand() {
    return "nick";
  }

  @Override
  public List<String> getParameterNames() {
    return List.of("nickname");
  }

  @Override
  public String getPermissionNode() {
    return "essentials.nick";
  }
}
