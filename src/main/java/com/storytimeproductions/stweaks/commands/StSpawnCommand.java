package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.eden.Eden;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Command that teleports a player to a configured spawn location in the "world" world.
 *
 * <p>This command can only be executed by players who are currently in the world specified in the
 * config file. The destination coordinates (x, y, z) and the world name must be defined in
 * config.yml under the "spawn" section.
 *
 * <p>Example config section:
 *
 * <pre>
 * spawn:
 *   world: world
 *   x: -1939
 *   y: 166
 *   z: 862
 * </pre>
 */
public class StSpawnCommand implements CommandExecutor {

  private final FileConfiguration config;

  /**
   * Constructs a new StSpawnCommand using the provided configuration file.
   *
   * @param config The plugin's configuration file used to retrieve spawn coordinates and world.
   */
  public StSpawnCommand(FileConfiguration config) {
    this.config = config;
  }

  /**
   * Executes the /spawn command.
   *
   * <p>This command checks if the sender is a player and ensures they are in the allowed world (as
   * specified in config). If the checks pass, the player is teleported to the coordinates specified
   * in the config file.
   *
   * @param sender The source of the command.
   * @param command The command object that was executed.
   * @param label The alias of the command used.
   * @param args Any additional arguments (ignored).
   * @return true if the command was successfully handled; false otherwise.
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Only players can use this command.");
      return true;
    }

    Player player = (Player) sender;
    String currentWorld = player.getWorld().getName();
    String spawnWorld = config.getString("spawn.world");
    List<String> allowedWorlds = config.getStringList("spawn.allowed-worlds");
    if (allowedWorlds.isEmpty()) {
      allowedWorlds = List.of(spawnWorld);
    }

    boolean allowed = allowedWorlds.stream().anyMatch(w -> w.equalsIgnoreCase(currentWorld));
    if (!allowed) {
      player.sendMessage("You cannot teleport to spawn from this world.");
      return true;
    }

    // Lobby players must complete the EDEN puzzle before they can use /spawn
    String lobbyWorld = config.getString("lobby.world", "lobby");
    if (currentWorld.equalsIgnoreCase(lobbyWorld)) {
      Plugin edenPlugin = Bukkit.getPluginManager().getPlugin("EDEN");
      if (edenPlugin instanceof Eden eden && !eden.getPuzzleManager().isPortalUnlocked(player)) {
        player.sendMessage("You must complete the puzzle before you can leave the lobby.");
        return true;
      }
    }

    double x = config.getDouble("spawn.x");
    double y = config.getDouble("spawn.y");
    double z = config.getDouble("spawn.z");

    World world = Bukkit.getWorld(spawnWorld);
    if (world == null) {
      player.sendMessage("Spawn world '" + spawnWorld + "' not found.");
      return true;
    }

    Location spawnLocation = new Location(world, x, y, z);
    player.teleport(spawnLocation);
    player.sendMessage("Teleported to spawn.");
    return true;
  }
}
