package com.storytimeproductions.stweaks.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Handles the /casino command, teleporting the player to the "casino" world using Multiverse.
 *
 * <p>This command can only be executed by players. When run, it dispatches the Multiverse teleport
 * command as the console to move the player to the "casino" world.
 */
public class StCasinoCommand implements CommandExecutor {

  private FileConfiguration config;

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

    // Get location from config (example path: casino-spawn.x, y, z, yaw, pitch)
    double x = config.getDouble("casino-spawn.x", 0.5);
    double y = config.getDouble("casino-spawn.y", 64.0);
    double z = config.getDouble("casino-spawn.z", 0.5);
    float yaw = (float) config.getDouble("casino-spawn.yaw", 0.0);
    float pitch = (float) config.getDouble("casino-spawn.pitch", 0.0);

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
}
