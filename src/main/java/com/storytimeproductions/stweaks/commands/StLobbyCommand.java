package com.storytimeproductions.stweaks.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Command that teleports a player to the lobby world and then to specific coordinates.
 *
 * <p>Coordinates and orientation are loaded from the plugin config.yml under the 'lobby' section.
 */
public class StLobbyCommand implements CommandExecutor {

  private final FileConfiguration config;

  /**
   * Constructs a new StLobbyCommand with access to the plugin's configuration.
   *
   * @param config The {@link FileConfiguration} instance used to retrieve lobby coordinates and
   *     settings.
   */
  public StLobbyCommand(FileConfiguration config) {
    this.config = config;
  }

  /**
   * Handles the /lobby command.
   *
   * <p>Executes the Multiverse teleport and then teleports the player to coordinates defined in
   * config.yml under 'lobby'.
   *
   * @param sender The source of the command.
   * @param command The command object.
   * @param label The command label.
   * @param args Additional arguments.
   * @return true if handled successfully.
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Only players can use this command.");
      return true;
    }

    Player player = (Player) sender;

    String worldName = config.getString("lobby.world", "lobby");
    double x = config.getDouble("lobby.x", -117);
    double y = config.getDouble("lobby.y", 223);
    double z = config.getDouble("lobby.z", 184);
    float yaw = (float) config.getDouble("lobby.yaw", -135.7298);
    float pitch = (float) config.getDouble("lobby.pitch", -4.836296);

    // Run the multiverse command to switch worlds
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(), "mv tp " + player.getName() + " " + worldName);

    // Then teleport to exact location after short delay
    Bukkit.getScheduler()
        .runTaskLater(
            Bukkit.getPluginManager().getPlugin("Stweaks"),
            () -> {
              World world = Bukkit.getWorld(worldName);
              if (world == null) {
                player.sendMessage("Lobby world '" + worldName + "' not found.");
                return;
              }

              Location location = new Location(world, x, y, z, yaw, pitch);
              player.teleport(location);
            },
            5L);

    return true;
  }
}
