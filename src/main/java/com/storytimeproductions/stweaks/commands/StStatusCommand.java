package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.stweaks.config.SettingsManager;
import com.storytimeproductions.stweaks.playtime.PlaytimeData;
import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command for checking the player's current playtime status.
 *
 * <p>This command retrieves and displays the amount of time a player has played for the current day
 * and compares it against the required playtime as set in the plugin settings for that specific
 * player.
 */
public class StStatusCommand implements CommandExecutor {

  /**
   * Executes the STStatus command.
   *
   * <p>This method checks if the command sender is a player. If so, it retrieves the player's
   * playtime data and compares it with the required playtime specified in the settings for that
   * player. The player is informed of their current playtime and the required playtime for the day.
   *
   * @param sender The entity that issued the command (player or console).
   * @param command The command that was executed.
   * @param label The alias of the command.
   * @param args The arguments passed with the command.
   * @return true if the command was successfully executed, false if the sender is not a player.
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player) {
      // Get the playtime data for the player using their unique ID
      PlaytimeData data = PlaytimeTracker.getData(player.getUniqueId());

      if (data != null) {
        // Retrieve the player's current playtime
        double played = data.getMinutesPlayed();

        // Get the required playtime for the player (per-player customization)
        int requiredMinutes = SettingsManager.getRequiredMinutes(player.getUniqueId());

        // Display the player's playtime and the required playtime for the day
        sender.sendMessage(
            "You have played " + (int) played + " minutes today. Required: " + requiredMinutes);
      } else {
        sender.sendMessage("No playtime data available.");
      }
      return true;
    }
    return false;
  }
}
