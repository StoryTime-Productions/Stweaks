package com.storytimeproductions.stweaks.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command that teleports a player to the lobby world and then to specific coordinates.
 *
 * <p>When a player executes the /lobby command, this class performs two actions in sequence:
 *
 * <ol>
 *   <li>Executes the Multiverse command to teleport the player to the "lobby" world.
 *   <li>Teleports the player to the exact coordinates (-117, 223, 184) within that world, after a
 *       short delay to ensure the world is fully loaded.
 * </ol>
 */
public class StLobbyCommand implements CommandExecutor {

  /**
   * Handles the /lobby command.
   *
   * <p>Checks if the command sender is a player, executes the Multiverse teleport command to move
   * the player to the "lobby" world, and after a short delay, teleports the player to a fixed
   * coordinate location within that world.
   *
   * @param sender The source of the command, typically a player.
   * @param command The command object associated with this execution.
   * @param label The alias of the command used.
   * @param args Additional arguments passed with the command.
   * @return true if the command was executed successfully; false otherwise.
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Only players can use this command.");
      return true;
    }

    Player player = (Player) sender;

    // First, run the multiverse command to switch worlds
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv tp " + player.getName() + " lobby");

    // Then, teleport the player to the exact coordinates
    Bukkit.getScheduler()
        .runTaskLater(
            Bukkit.getPluginManager().getPlugin("Stweaks"),
            () -> player.teleport(new org.bukkit.Location(player.getWorld(), -117, 223, 184)),
            20L);

    player.sendMessage("Teleporting you to the lobby...");
    return true;
  }
}
