package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.models.stgames.Minigame;
import com.storytimeproductions.stweaks.listeners.GameManagerListener;
import java.io.File;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Handles the /casino command, teleporting the player to the "casino" world using Multiverse.
 *
 * <p>This command can only be executed by players. When run, it dispatches the Multiverse teleport
 * command as the console to move the player to the "casino" world.
 */
public class StCasinoCommand implements CommandExecutor {

  private FileConfiguration config;

  /**
   * Constructs a new StCasinoCommand with access to the plugin's configuration.
   *
   * @param config The {@link FileConfiguration} instance used to retrieve casino coordinates and
   *     settings.
   */
  public StCasinoCommand(FileConfiguration config) {
    this.config = config;
  }

  /**
   * Executes the /casino command.
   *
   * @param sender The source of the command.
   * @param command The command that was executed.
   * @param label The alias of the command used.
   * @param args The command arguments.
   * @return true if the command was handled, false otherwise.
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("This command can only be used by players.");
      return true;
    }

    if (label.equalsIgnoreCase("casino")
        && args.length == 1
        && args[0].equalsIgnoreCase("reload")) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("stweaks");

      if (!sender.isOp()) {
        sender.sendMessage("You do not have permission to use this command.");
        return true;
      }

      File gamesFile = new File(plugin.getDataFolder(), "games.yml");
      if (!gamesFile.exists()) {
        sender.sendMessage("games.yml not found!");
        return true;
      }

      FileConfiguration gamesConfig = YamlConfiguration.loadConfiguration(gamesFile);
      GameManagerListener.loadGamesFromConfig(gamesConfig);

      sender.sendMessage("Casino games configuration reloaded from games.yml.");
      return true;
    }

    if (label.equalsIgnoreCase("casino") && args.length == 1 && args[0].equalsIgnoreCase("leave")) {
      // Find the game the player is in and teleport to its exit area
      for (Minigame minigame : GameManagerListener.activeGames.values()) {
        if (minigame.getPlayers().contains(player)) {
          Location exit = minigame.getConfig().getExitArea();
          if (exit != null) {
            player.teleport(exit);
            minigame.leave(player);
            refundTicket(player, minigame.getConfig().getTicketCost());
            player.sendMessage("You have left the game.");
          } else {
            player.sendMessage("No exit area set for this game.");
          }
          return true;
        }
      }
      player.sendMessage("You are not currently in a game.");
      return true;
    }

    // Teleport the player to the casino world using Multiverse
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv tp " + player.getName() + " casino");

    // Get location from config (example path: casino-spawn.x, y, z, yaw, pitch)
    double x = config.getDouble("casino.x", -117);
    double y = config.getDouble("casino.y", 223);
    double z = config.getDouble("casino.z", 184);
    float yaw = (float) config.getDouble("casino.yaw", 0.0);
    float pitch = (float) config.getDouble("casino.pitch", 0.0);

    // Delay the teleport to ensure Multiverse has moved the player to the correct
    // world
    Bukkit.getScheduler()
        .runTaskLater(
            Bukkit.getPluginManager().getPlugin("stweaks"),
            () -> {
              if (player.isOnline() && player.getWorld().getName().equalsIgnoreCase("casino")) {
                player.teleport(new org.bukkit.Location(player.getWorld(), x, y, z, yaw, pitch));
              }
            },
            10L); // 10 ticks = 0.5 seconds

    return true;
  }

  private void refundTicket(Player player, int amount) {
    ItemStack tickets = new ItemStack(org.bukkit.Material.NAME_TAG, amount);
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);
    player.getInventory().addItem(tickets);
    player.sendMessage(
        Component.text("Your Time Ticket(s) have been refunded.", NamedTextColor.YELLOW));
  }
}
