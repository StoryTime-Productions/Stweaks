package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.models.ItemConsumable;
import com.storytimeproductions.stweaks.consumables.AnvilConsumable;
import com.storytimeproductions.stweaks.consumables.BackConsumable;
import com.storytimeproductions.stweaks.consumables.BoostConsumable;
import com.storytimeproductions.stweaks.consumables.EnderchestConsumable;
import com.storytimeproductions.stweaks.consumables.EntitySizeConsumable;
import com.storytimeproductions.stweaks.consumables.NickConsumable;
import com.storytimeproductions.stweaks.consumables.PlayerSizeConsumable;
import com.storytimeproductions.stweaks.consumables.SetWarpConsumable;
import com.storytimeproductions.stweaks.consumables.SmithingTableConsumable;
import com.storytimeproductions.stweaks.consumables.WorkbenchConsumable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listens for item consumption events and handles custom consumable items. If the consumed item
 * matches a registered consumable, it executes the associated command. If parameters are required,
 * it prompts the player for input via chat.
 */
public class ItemConsumableListener implements Listener {
  // Static list of all consumables
  private static final List<ItemConsumable> CONSUMABLES =
      List.of(
          new BackConsumable(),
          new SetWarpConsumable(),
          new WorkbenchConsumable(),
          new SmithingTableConsumable(),
          new NickConsumable(),
          new EnderchestConsumable(),
          new AnvilConsumable(),
          new EntitySizeConsumable(),
          new PlayerSizeConsumable(),
          new BoostConsumable());

  // Map to track players waiting for parameters
  private final Map<UUID, PendingCommand> pendingCommands = new HashMap<>();

  /**
   * Handles the PlayerItemConsumeEvent.
   *
   * @param event the event triggered when a player consumes an item.
   */
  @EventHandler
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();
    String itemModel = getItemModel(item);
    if (itemModel == null) {
      return;
    }

    for (ItemConsumable consumable : CONSUMABLES) {
      if (itemModel.equals(consumable.getItemModel())) {
        if (consumable
            instanceof com.storytimeproductions.stweaks.consumables.EntitySizeConsumable) {
          // Remove only one item from the stack (from player's inventory)
          ItemStack[] contents = player.getInventory().getContents();
          for (int i = 0; i < contents.length; i++) {
            ItemStack invItem = contents[i];
            if (invItem != null && invItem.isSimilar(item)) {
              if (invItem.getAmount() > 1) {
                invItem.setAmount(invItem.getAmount() - 1);
              } else {
                player.getInventory().setItem(i, null);
              }
              break;
            }
          }
          // Grant permission for 30 seconds
          String permission = consumable.getPermissionNode();
          if (permission != null && !permission.isBlank()) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "lp user " + player.getName() + " permission set " + permission + " true");
            Bukkit.getScheduler()
                .runTaskLater(
                    Bukkit.getPluginManager().getPlugin("stweaks"),
                    () ->
                        Bukkit.dispatchCommand(
                            Bukkit.getConsoleSender(),
                            "lp user " + player.getName() + " permission unset " + permission),
                    20L * 30);
          }
          player.sendMessage(
              Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                  .append(
                      Component.text(
                          "You can now use '/entitysize entity looking <size>' for 30 seconds!",
                          NamedTextColor.YELLOW)));
          event.setCancelled(true);
        } else if (!consumable.getParameterNames().isEmpty()) {
          // Remove only one item from the stack (from player's inventory)
          ItemStack[] contents = player.getInventory().getContents();
          for (int i = 0; i < contents.length; i++) {
            ItemStack invItem = contents[i];
            if (invItem != null && invItem.isSimilar(item)) {
              if (invItem.getAmount() > 1) {
                invItem.setAmount(invItem.getAmount() - 1);
              } else {
                player.getInventory().setItem(i, null);
              }
              break;
            }
          }
          // Ask for parameters via chat
          pendingCommands.put(
              player.getUniqueId(), new PendingCommand(consumable, new ArrayList<>()));
          player.sendMessage(
              Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                  .append(Component.text("Please enter: ", NamedTextColor.YELLOW))
                  .append(
                      Component.text(
                          String.join(", ", consumable.getParameterNames()),
                          NamedTextColor.YELLOW)));
          event.setCancelled(true);
        } else {
          runConsumableCommand(player, consumable, List.of());
          event.setCancelled(true);
          // Remove only one item from the stack (from player's inventory)
          ItemStack[] contents = player.getInventory().getContents();
          for (int i = 0; i < contents.length; i++) {
            ItemStack invItem = contents[i];
            if (invItem != null && invItem.isSimilar(item)) {
              if (invItem.getAmount() > 1) {
                invItem.setAmount(invItem.getAmount() - 1);
              } else {
                player.getInventory().setItem(i, null);
              }
              break;
            }
          }
        }
        break;
      }
    }
  }

  /**
   * Handles the AsyncChatEvent to collect parameters for consumable commands.
   *
   * @param event the event triggered when a player sends a chat message.
   */
  @EventHandler
  public void onPlayerChat(io.papermc.paper.event.player.AsyncChatEvent event) {
    Player player = event.getPlayer();
    PendingCommand pending = pendingCommands.get(player.getUniqueId());
    if (pending != null) {
      event.setCancelled(true);
      String message = PlainTextComponentSerializer.plainText().serialize(event.message());
      int paramIndex = pending.parameters.size();
      String paramType = "alphanumeric";
      if (paramIndex < pending.consumable.getParameterTypes().size()) {
        paramType = pending.consumable.getParameterTypes().get(paramIndex);
      }
      boolean valid;
      if ("numeric".equals(paramType)) {
        valid = message.matches("\\d+");
      } else if ("alpha".equals(paramType)) {
        valid = message.matches("[a-zA-Z]+$");
      } else {
        valid = message.matches("[a-zA-Z0-9_]+$");
      }
      if (!valid) {
        player.sendMessage(
            Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                .append(Component.text("Invalid input for parameter '", NamedTextColor.YELLOW))
                .append(
                    Component.text(
                        pending.consumable.getParameterNames().get(paramIndex),
                        NamedTextColor.YELLOW))
                .append(Component.text("'. Please enter a ", NamedTextColor.YELLOW))
                .append(Component.text(paramType, NamedTextColor.YELLOW))
                .append(Component.text(" value.", NamedTextColor.YELLOW)));
        return;
      }
      pending.parameters.add(message);
      if (pending.parameters.size() >= pending.consumable.getParameterNames().size()) {
        Bukkit.getScheduler()
            .runTask(
                Bukkit.getPluginManager().getPlugin("stweaks"),
                () -> {
                  String permission = pending.consumable.getPermissionNode();
                  if (permission != null && !permission.isBlank()) {
                    Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "lp user " + player.getName() + " permission set " + permission + " true");
                  }
                  runConsumableCommand(player, pending.consumable, pending.parameters);
                  if (permission != null && !permission.isBlank()) {
                    Bukkit.getScheduler()
                        .runTaskLater(
                            Bukkit.getPluginManager().getPlugin("stweaks"),
                            () ->
                                Bukkit.dispatchCommand(
                                    Bukkit.getConsoleSender(),
                                    "lp user "
                                        + player.getName()
                                        + " permission unset "
                                        + permission),
                            2L);
                  }
                  // Remove only one item from the stack (from player's inventory)
                  ItemStack[] contents = player.getInventory().getContents();
                  for (int i = 0; i < contents.length; i++) {
                    ItemStack invItem = contents[i];
                    if (invItem != null
                        && invItem.hasItemMeta()
                        && invItem.isSimilar(player.getInventory().getItemInMainHand())) {
                      if (invItem.getAmount() > 1) {
                        invItem.setAmount(invItem.getAmount() - 1);
                      } else {
                        player.getInventory().setItem(i, null);
                      }
                      break;
                    }
                  }
                });
        pendingCommands.remove(player.getUniqueId());
      } else {
        player.sendMessage(
            Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                .append(Component.text("Please enter: ", NamedTextColor.YELLOW))
                .append(
                    Component.text(
                        pending.consumable.getParameterNames().get(pending.parameters.size()),
                        NamedTextColor.YELLOW)));
      }
    }
  }

  private void runConsumableCommand(Player player, ItemConsumable consumable, List<String> params) {
    String permission = consumable.getPermissionNode();
    String command = consumable.getCommand();
    String fullCommand;
    // Special handling for PlayerSizeConsumable and EntitySizeConsumable
    if (consumable instanceof PlayerSizeConsumable || consumable instanceof EntitySizeConsumable) {
      fullCommand = command + (params.isEmpty() ? "" : " " + String.join(" ", params)) + " 5";
    } else {
      fullCommand = command + (params.isEmpty() ? "" : " " + String.join(" ", params));
    }
    var plugin = Bukkit.getPluginManager().getPlugin("stweaks");

    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              // 1. Set permission if needed
              if (permission != null && !permission.isBlank()) {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "lp user " + player.getName() + " permission set " + permission + " true");
              }

              // 2. Wait 2 ticks to ensure LuckPerms updates
              Bukkit.getScheduler()
                  .runTaskLater(
                      plugin,
                      () -> {
                        player.performCommand(fullCommand);

                        // 3. Wait 2 more ticks, then unset permission
                        if (permission != null && !permission.isBlank()) {
                          Bukkit.getScheduler()
                              .runTaskLater(
                                  plugin,
                                  () -> {
                                    Bukkit.dispatchCommand(
                                        Bukkit.getConsoleSender(),
                                        "lp user "
                                            + player.getName()
                                            + " permission unset "
                                            + permission);
                                  },
                                  2L);
                        }
                      },
                      2L);
            });
  }

  private String getItemModel(ItemStack item) {
    if (item == null || !item.hasItemMeta()) {
      return null;
    }
    ItemMeta meta = item.getItemMeta();
    NamespacedKey itemModel = meta.getItemModel();
    if (itemModel != null && itemModel.getNamespace().equals("storytime")) {
      return itemModel.getNamespace() + ":" + itemModel.getKey();
    }
    return null;
  }

  private static class PendingCommand {
    final ItemConsumable consumable;
    final List<String> parameters;

    PendingCommand(ItemConsumable c, List<String> p) {
      this.consumable = c;
      this.parameters = p;
    }
  }
}
