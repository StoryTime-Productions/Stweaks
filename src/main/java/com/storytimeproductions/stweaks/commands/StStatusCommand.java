package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.stweaks.playtime.PlaytimeData;
import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import java.util.Arrays;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Command for checking the player's current playtime status.
 *
 * <p>This command retrieves and displays the amount of time a player has played for the current day
 * and compares it against the required playtime as set in the plugin settings for that specific
 * player.
 */
public class StStatusCommand implements CommandExecutor {

  private final JavaPlugin plugin;

  /**
   * Constructor for the StStatusCommand class.
   *
   * @param plugin The JavaPlugin instance.
   */
  public StStatusCommand(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // /ststatus reset <player>
    if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
      if (!sender.hasPermission("stweaks.ststatus.reset")) {
        sender.sendMessage("§cYou don't have permission to reset playtime.");
        return true;
      }

      String targetName = args[1];
      OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
      UUID targetIdentifer = target.getUniqueId();

      if (targetIdentifer == null) {
        sender.sendMessage("§cCould not find player: " + targetName);
        return true;
      }

      if (PlaytimeTracker.getData(targetIdentifer) != null) {
        PlaytimeTracker.resetPlaytime(targetIdentifer);
        sender.sendMessage("§aReset playtime for §f" + targetName + "§a.");
      } else {
        sender.sendMessage("§eNo playtime data found for " + targetName + ".");
      }
      return true;
    }

    // /ststatus add <player> <seconds>
    if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
      if (!sender.hasPermission("stweaks.ststatus.add")) {
        sender.sendMessage("You don't have permission to add playtime.");
        return true;
      }
      String targetName = args[1];
      OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
      UUID targetIdentifer = target.getUniqueId();

      if (targetIdentifer == null) {
        sender.sendMessage("Could not find player: " + targetName);
        return true;
      }

      PlaytimeData data = PlaytimeTracker.getData(targetIdentifer);
      if (data == null) {
        data = new PlaytimeData();
        PlaytimeTracker.setPlaytime(targetIdentifer, data);
      }

      try {
        long secondsToAdd = Long.parseLong(args[2]);
        data.addAvailableSeconds(secondsToAdd);
        sender.sendMessage("Added " + secondsToAdd + " seconds to " + targetName + ".");
      } catch (NumberFormatException e) {
        sender.sendMessage("Invalid number of seconds: " + args[2]);
      }
      return true;
    }

    if (sender instanceof Player player) {
      openStatusInventory(player);
      return true;
    }
    return false;
  }

  /**
   * Opens a 54-slot inventory for the player, with black panes on the edges and information items
   * in the center.
   */
  private void openStatusInventory(Player player) {
    Inventory inv =
        Bukkit.createInventory(
            null, 54, net.kyori.adventure.text.Component.text("Your Playtime Status"));

    // Fill edges with black stained glass panes
    ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta paneMeta = blackPane.getItemMeta();
    paneMeta.displayName(net.kyori.adventure.text.Component.text(" "));
    blackPane.setItemMeta(paneMeta);

    // Top and bottom rows
    for (int i = 0; i < 9; i++) {
      inv.setItem(i, blackPane);
      inv.setItem(45 + i, blackPane);
    }
    // Left and right columns
    for (int i = 1; i < 5; i++) {
      inv.setItem(i * 9, blackPane);
      inv.setItem(i * 9 + 8, blackPane);
    }

    // Get playtime data
    PlaytimeData data = PlaytimeTracker.getData(player.getUniqueId());
    long secondsLeft = data != null ? data.getAvailableSeconds() : 0;

    // Info item: Seconds left
    ItemStack timeLeft = new ItemStack(Material.CLOCK);
    ItemMeta timeMeta = timeLeft.getItemMeta();
    timeMeta.displayName(Component.text("Seconds Left"));
    timeMeta.lore(
        Arrays.asList(
            Component.text("You have"), Component.text(secondsLeft + " seconds remaining.")));
    timeLeft.setItemMeta(timeMeta);
    inv.setItem(22, timeLeft);

    // Info item: 5-minute tickets (minus 10 minutes)
    int tickets = 0;
    if (secondsLeft > 600) {
      tickets = (int) ((secondsLeft - 600) / 300);
    }
    ItemStack ticketItem = new ItemStack(Material.PAPER);
    ItemMeta ticketMeta = ticketItem.getItemMeta();
    ticketMeta.displayName(Component.text("5-Minute Tickets"));
    ticketMeta.lore(
        Arrays.asList(
            Component.text("You can convert your remaining time"),
            Component.text("(minus 10 minutes) into tickets."),
            Component.text(tickets + " tickets available.")));
    ticketItem.setItemMeta(ticketMeta);
    inv.setItem(24, ticketItem);

    player.openInventory(inv);

    // Pseudocode: update slot 22 every second
    new BukkitRunnable() {
      @Override
      public void run() {
        Component inventoryTitle = player.getOpenInventory().title();
        if (!Component.text("Your Playtime Status").equals(inventoryTitle)) {
          cancel();
          return;
        }
        PlaytimeData data = PlaytimeTracker.getData(player.getUniqueId());
        long secondsLeft = data != null ? data.getAvailableSeconds() : 0;
        ItemStack timeLeft = new ItemStack(Material.CLOCK);
        ItemMeta timeMeta = timeLeft.getItemMeta();
        timeMeta.displayName(Component.text("Seconds Left"));
        timeMeta.lore(
            Arrays.asList(
                Component.text("You have"), Component.text(secondsLeft + " seconds remaining.")));
        timeLeft.setItemMeta(timeMeta);
        player.getOpenInventory().setItem(22, timeLeft);
      }
    }.runTaskTimer(plugin, 20L, 20L);
  }
}
