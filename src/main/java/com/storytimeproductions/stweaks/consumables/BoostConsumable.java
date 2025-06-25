package com.storytimeproductions.stweaks.consumables;

import com.storytimeproductions.models.ItemConsumable;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;

/**
 * Represents a consumable item that allows players to add a small boost to the server multiplier.
 */
public class BoostConsumable implements ItemConsumable {
  @Override
  public Material getBaseMaterial() {
    return Material.APPLE;
  }

  @Override
  public String getItemModel() {
    return "storytime:boost_token";
  }

  @Override
  public Component getName() {
    return Component.text("Boost Consumable");
  }

  @Override
  public List<Component> getLore() {
    return List.of(Component.text("Use to add a small boost to the server multiplier."));
  }

  @Override
  public String getCommand() {
    return "status boost 0.1";
  }

  @Override
  public List<String> getParameterNames() {
    return List.of();
  }

  @Override
  public String getPermissionNode() {
    return "";
  }

  /** Executes the command as if it were run by the console. */
  public void executeAsConsole() {
    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "status boost 0.01");
  }
}
