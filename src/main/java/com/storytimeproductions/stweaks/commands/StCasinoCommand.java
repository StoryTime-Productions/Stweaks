package com.storytimeproductions.stweaks.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StCasinoCommand implements CommandExecutor {

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
