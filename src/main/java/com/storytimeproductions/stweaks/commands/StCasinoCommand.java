package com.storytimeproductions.stweaks.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /casino command, teleporting the player to the "casino" world using Multiverse.
 *
 * <p>This command can only be executed by players. When run, it dispatches the Multiverse teleport
 * command as the console to move the player to the "casino" world.
 */
public class StCasinoCommand implements CommandExecutor {

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
    // Teleport the player to the casino world using Multiverse
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv tp " + player.getName() + " casino");
    return true;
  }
}
