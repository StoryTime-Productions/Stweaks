package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.stweaks.games.hunt.HiderClass;
import com.storytimeproductions.stweaks.games.hunt.HiderUtilityListener;
import com.storytimeproductions.stweaks.games.hunt.HuntDisguiseManager;
import com.storytimeproductions.stweaks.games.hunt.HuntHologramManager;
import com.storytimeproductions.stweaks.games.hunt.HuntKitManager;
import com.storytimeproductions.stweaks.games.hunt.HuntLobbyManager;
import com.storytimeproductions.stweaks.games.hunt.HuntMap;
import com.storytimeproductions.stweaks.games.hunt.HuntPlayerData;
import com.storytimeproductions.stweaks.games.hunt.HuntPrepPhaseManager;
import com.storytimeproductions.stweaks.games.hunt.HuntTeam;
import com.storytimeproductions.stweaks.games.hunt.HunterClass;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.libraryaddict.disguise.DisguiseAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

/** Command to open the Hunt game lobby and handle class/map selection. */
public class HuntCommand implements CommandExecutor {

  private final HuntLobbyManager lobbyManager;
  private final HuntHologramManager hologramManager;
  private final HuntKitManager kitManager;
  private final HuntDisguiseManager disguiseManager;
  private final HuntPrepPhaseManager prepPhaseManager;
  private final HiderUtilityListener hiderUtilityListener;
  private final JavaPlugin plugin;

  // Cooldown tracking for class switching (500ms cooldown)
  private final Map<UUID, Long> classJoinCooldowns = new HashMap<>();
  private static final long CLASS_JOIN_COOLDOWN_MS = 500;

  // Cooldown tracking for map voting (500ms cooldown)
  private final Map<UUID, Long> voteJoinCooldowns = new HashMap<>();
  private static final long VOTE_COOLDOWN_MS = 500;

  /**
   * Constructs a new HuntCommand with the given managers.
   *
   * @param plugin The JavaPlugin instance for accessing configuration
   * @param lobbyManager The HuntLobbyManager instance to use for lobby operations
   * @param hologramManager The HuntHologramManager instance for hologram operations
   * @param kitManager The HuntKitManager instance for kit operations
   * @param disguiseManager The HuntDisguiseManager instance for disguise operations
   * @param prepPhaseManager The HuntPrepPhaseManager instance for prep phase operations
   * @param hiderUtilityListener The HiderUtilityListener instance for clearing hider data
   */
  public HuntCommand(
      JavaPlugin plugin,
      HuntLobbyManager lobbyManager,
      HuntHologramManager hologramManager,
      HuntKitManager kitManager,
      HuntDisguiseManager disguiseManager,
      HuntPrepPhaseManager prepPhaseManager,
      HiderUtilityListener hiderUtilityListener) {
    this.plugin = plugin;
    this.lobbyManager = lobbyManager;
    this.hologramManager = hologramManager;
    this.kitManager = kitManager;
    this.disguiseManager = disguiseManager;
    this.prepPhaseManager = prepPhaseManager;
    this.hiderUtilityListener = hiderUtilityListener;
  }

  /**
   * Handles the /hunt command and its subcommands.
   *
   * @param sender The command sender
   * @param command The command
   * @param label The command label
   * @param args The command arguments
   * @return true if the command was handled, false otherwise
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      return true;
    }

    if (args.length == 0) {
      // Auto-start prep phase if not already active
      if (!prepPhaseManager.isPrepPhaseActive() && !prepPhaseManager.isGameStarting()) {
        prepPhaseManager.startPrepPhase();
        player.sendMessage(Component.text("Started Hunt prep phase!", NamedTextColor.GREEN));
      }

      teleportToHuntSpawn(player);
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "lobby" -> {
        lobbyManager.openMainMenu(player);
        player.sendMessage(Component.text("Opening Hunt game lobby...", NamedTextColor.GREEN));
      }
      case "join" -> {
        if (args.length < 2) {
          player.sendMessage(Component.text("Usage: /hunt join <className>", NamedTextColor.RED));
          player.sendMessage(Component.text("Available classes:", NamedTextColor.YELLOW));
          player.sendMessage(
              Component.text("Hunters: brute, nimble, saboteur", NamedTextColor.WHITE));
          player.sendMessage(
              Component.text("Hiders: trickster, phaser, cloaker", NamedTextColor.WHITE));
          return true;
        }

        String className = args[1].toLowerCase();
        handleClassJoin(player, className);
      }
      case "map" -> {
        if (args.length < 2) {
          player.sendMessage(Component.text("Usage: /hunt map <mapName>", NamedTextColor.RED));
          player.sendMessage(Component.text("Available maps:", NamedTextColor.YELLOW));
          for (HuntMap map : HuntMap.values()) {
            player.sendMessage(
                Component.text("• " + map.name().toLowerCase(), NamedTextColor.WHITE));
          }
          return true;
        }

        String mapName = args[1].toLowerCase();
        handleMapVote(player, mapName);
      }
      case "prep" -> {
        if (args.length < 2) {
          player.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW));
          player.sendMessage(
              Component.text("/hunt prep start - Start prep phase", NamedTextColor.WHITE));
          player.sendMessage(
              Component.text("/hunt prep end - End prep phase", NamedTextColor.WHITE));
          return true;
        }

        switch (args[1].toLowerCase()) {
          case "start" -> {
            prepPhaseManager.startPrepPhase();
            player.sendMessage(Component.text("Started Hunt prep phase!", NamedTextColor.GREEN));
          }
          case "end" -> {
            prepPhaseManager.endPrepPhase();
            player.sendMessage(Component.text("Ended Hunt prep phase!", NamedTextColor.YELLOW));
          }
          default -> {
            player.sendMessage(Component.text("Invalid prep command!", NamedTextColor.RED));
            player.sendMessage(Component.text("Use /hunt prep for help", NamedTextColor.YELLOW));
          }
        }
      }
      case "ready" -> {
        if (!prepPhaseManager.isPrepPhaseActive()) {
          player.sendMessage(
              Component.text("No prep phase is currently active!", NamedTextColor.RED));
          return true;
        }

        boolean ready = true;
        if (args.length > 1 && "false".equalsIgnoreCase(args[1])) {
          ready = false;
        }

        prepPhaseManager.setPlayerReady(player, ready);
      }
      case "vote" -> {
        if (!prepPhaseManager.isPrepPhaseActive()) {
          player.sendMessage(
              Component.text("No prep phase is currently active!", NamedTextColor.RED));
          return true;
        }

        if (args.length < 2) {
          player.sendMessage(Component.text("Usage: /hunt vote <mapName>", NamedTextColor.RED));
          return true;
        }

        String mapName = args[1].toLowerCase();
        for (HuntMap map : HuntMap.values()) {
          if (map.name().toLowerCase().equals(mapName)) {
            prepPhaseManager.handleMapVote(player, map);
            return true;
          }
        }

        player.sendMessage(Component.text("Unknown map: " + mapName, NamedTextColor.RED));
      }
      case "start" -> {
        if (!prepPhaseManager.isPrepPhaseActive()) {
          sender.sendMessage(
              Component.text("No prep phase is currently active!", NamedTextColor.RED));
          return true;
        }

        prepPhaseManager.attemptGameStart();
      }
      case "leave" -> {
        if (args.length < 2) {
          player.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW));
          player.sendMessage(
              Component.text("/hunt leave lobby - Leave the Hunt lobby", NamedTextColor.WHITE));
          player.sendMessage(
              Component.text("/hunt leave class - Leave your current class", NamedTextColor.WHITE));
          player.sendMessage(
              Component.text("/hunt leave map - Remove your map vote", NamedTextColor.WHITE));
          return true;
        }

        switch (args[1].toLowerCase()) {
          case "lobby" -> {
            lobbyManager.removePlayer(player.getUniqueId());
            player.closeInventory();
            // Clean up cooldowns
            classJoinCooldowns.remove(player.getUniqueId());
            voteJoinCooldowns.remove(player.getUniqueId());
            player.sendMessage(Component.text("Left the Hunt game lobby.", NamedTextColor.YELLOW));
          }
          case "class" -> {
            handleLeaveClass(player);
          }
          case "map" -> {
            handleLeaveMap(player);
          }
          default -> {
            player.sendMessage(Component.text("Invalid leave command!", NamedTextColor.RED));
            player.sendMessage(Component.text("Use /hunt leave for help", NamedTextColor.YELLOW));
          }
        }
      }
      case "status" -> {
        var data = lobbyManager.getPlayerData(player.getUniqueId());
        if (data == null) {
          player.sendMessage(Component.text("You are not in the Hunt lobby.", NamedTextColor.RED));
        } else {
          player.sendMessage(Component.text("=== Hunt Lobby Status ===", NamedTextColor.GOLD));
          player.sendMessage(
              Component.text(
                  "Team: "
                      + (data.getSelectedTeam() != null
                          ? data.getSelectedTeam().getDisplayName()
                          : "None"),
                  NamedTextColor.WHITE));

          if (data.getSelectedTeam() != null) {
            String className = "None";
            if (data.getSelectedTeam().name().equals("HUNTERS")
                && data.getSelectedHunterClass() != null) {
              className = data.getSelectedHunterClass().getDisplayName();
            } else if (data.getSelectedTeam().name().equals("HIDERS")
                && data.getSelectedHiderClass() != null) {
              className = data.getSelectedHiderClass().getDisplayName();
            }
            player.sendMessage(Component.text("Class: " + className, NamedTextColor.WHITE));
          }

          player.sendMessage(
              Component.text(
                  "Map: "
                      + (data.getPreferredMap() != null
                          ? data.getPreferredMap().getDisplayName()
                          : "None"),
                  NamedTextColor.WHITE));
          player.sendMessage(
              Component.text(
                  "Game Mode: "
                      + (data.getPreferredGameMode() != null
                          ? data.getPreferredGameMode().getDisplayName()
                          : "None"),
                  NamedTextColor.WHITE));
          player.sendMessage(
              Component.text(
                  "Ready: " + (data.isReady() ? "Yes" : "No"),
                  data.isReady() ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
      }
      case "disguise" -> {
        if (args.length < 2) {
          player.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW));
          player.sendMessage(
              Component.text("/hunt disguise spawn - Spawn disguise stands", NamedTextColor.WHITE));
          player.sendMessage(
              Component.text("/hunt disguise clear - Clear disguise stands", NamedTextColor.WHITE));
          player.sendMessage(
              Component.text("/hunt disguise remove - Remove your disguise", NamedTextColor.WHITE));
          player.sendMessage(
              Component.text(
                  "/hunt disguise reload - Reload disguise config", NamedTextColor.WHITE));
          return true;
        }

        switch (args[1].toLowerCase()) {
          case "spawn" -> {
            disguiseManager.spawnDisguiseStands();
            player.sendMessage(
                Component.text("Spawned disguise armor stands!", NamedTextColor.GREEN));
          }
          case "clear" -> {
            disguiseManager.clearDisguiseStands();
            player.sendMessage(
                Component.text("Cleared all disguise armor stands!", NamedTextColor.YELLOW));
          }
          case "remove" -> {
            disguiseManager.removeDisguise(player);
            player.sendMessage(Component.text("Removed your disguise!", NamedTextColor.YELLOW));
          }
          case "reload" -> {
            disguiseManager.reload();
            player.sendMessage(Component.text("Reloaded disguise system!", NamedTextColor.GREEN));
          }
          default -> {
            player.sendMessage(Component.text("Invalid disguise command!", NamedTextColor.RED));
            player.sendMessage(
                Component.text("Use /hunt disguise for help", NamedTextColor.YELLOW));
          }
        }
      }
      default -> {
        player.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW));
        player.sendMessage(
            Component.text("/hunt - Teleport to hunt world spawn", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt lobby - Open the Hunt lobby", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt join <className> - Join a class", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt map <mapName> - Vote for a map", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt prep start/end - Manage prep phase", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt ready [true/false] - Set ready status", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text(
                "/hunt vote <mapName> - Vote for map (prep phase)", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt start - Start game (prep phase)", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt leave lobby - Leave the Hunt lobby", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt leave class - Leave your current class", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt leave map - Remove your map vote", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt status - Check your lobby status", NamedTextColor.WHITE));
        player.sendMessage(
            Component.text("/hunt disguise - Manage disguise system", NamedTextColor.WHITE));
      }
    }

    return true;
  }

  /**
   * Handles a player joining a specific class.
   *
   * @param player The player joining the class
   * @param className The name of the class to join
   */
  private void handleClassJoin(Player player, String className) {
    // Check cooldown first
    UUID playerId = player.getUniqueId();
    long currentTime = System.currentTimeMillis();

    if (classJoinCooldowns.containsKey(playerId)) {
      long lastJoinTime = classJoinCooldowns.get(playerId);
      long timeSinceLastJoin = currentTime - lastJoinTime;

      if (timeSinceLastJoin < CLASS_JOIN_COOLDOWN_MS) {
        return;
      }
    }

    // Try to match hunter classes first
    for (HunterClass hunterClass : HunterClass.values()) {
      if (hunterClass.name().toLowerCase().equals(className)) {
        String hologramId = "hunter_" + className.toLowerCase();

        // Store the current state before calling the hologram manager
        String classBefore = hologramManager.getPlayerClassSelection(player.getUniqueId());

        // Add to class (or remove if already in it) - hologram manager handles the
        // logic
        hologramManager.addPlayerToClass(player.getUniqueId(), player.getName(), className, true);

        // Check if the state actually changed to determine what message to send
        String classAfter = hologramManager.getPlayerClassSelection(player.getUniqueId());

        // Only send a message if the state actually changed
        if (!java.util.Objects.equals(classBefore, classAfter)) {
          // Update cooldown since the state changed
          classJoinCooldowns.put(playerId, currentTime);

          if (hologramId.equals(classAfter)) {
            // Player joined the hunter class - update lobby manager and give kit
            HuntPlayerData playerData = lobbyManager.getOrCreatePlayerData(player.getUniqueId());

            // Check if player was previously a hider and remove any block disguises
            if (playerData.getSelectedTeam() == HuntTeam.HIDERS) {
              // Remove LibsDisguises block disguise if present
              if (DisguiseAPI.isDisguised(player)) {
                DisguiseAPI.undisguiseToAll(player);
                player.sendMessage(
                    Component.text("Removed your block disguise!", NamedTextColor.YELLOW));
              }

              // Clear hider cooldowns and stored data
              hiderUtilityListener.clearPlayerCooldowns(player.getUniqueId());
            }

            playerData.setSelectedTeam(HuntTeam.HUNTERS);
            playerData.setSelectedHunterClass(hunterClass);

            // Remove any existing kit first, then give new kit
            kitManager.removePlayerKit(player);
            kitManager.giveHunterKit(player, hunterClass);
            player.sendMessage(
                Component.text(
                    "Joined " + hunterClass.getDisplayName() + " (Hunter)", NamedTextColor.GREEN));

            // Update prep phase holograms if prep phase is active
            prepPhaseManager.updatePlayerClassSelection(
                player.getUniqueId(), HuntTeam.HUNTERS, hunterClass);
          } else {
            // Player left the class - update lobby manager and remove kit
            HuntPlayerData playerData = lobbyManager.getPlayerData(player.getUniqueId());
            if (playerData != null) {
              playerData.setSelectedTeam(null);
              playerData.setSelectedHunterClass(null);
            }

            kitManager.removePlayerKit(player);
            player.sendMessage(
                Component.text(
                    "Left " + hunterClass.getDisplayName() + " (Hunter)", NamedTextColor.YELLOW));

            // Update prep phase holograms if prep phase is active
            prepPhaseManager.updatePlayerClassSelection(player.getUniqueId(), null, null);
          }
        }
        return;
      }
    }

    // Try to match hider classes
    for (HiderClass hiderClass : HiderClass.values()) {
      if (hiderClass.name().toLowerCase().equals(className)) {
        String hologramId = "hider_" + className.toLowerCase();

        // Store the current state before calling the hologram manager
        String classBefore = hologramManager.getPlayerClassSelection(player.getUniqueId());

        // Add to class (or remove if already in it) - hologram manager handles the
        // logic
        hologramManager.addPlayerToClass(player.getUniqueId(), player.getName(), className, false);

        // Check if the state actually changed to determine what message to send
        String classAfter = hologramManager.getPlayerClassSelection(player.getUniqueId());

        // Only send a message if the state actually changed
        if (!java.util.Objects.equals(classBefore, classAfter)) {
          // Update cooldown since the state changed
          classJoinCooldowns.put(playerId, currentTime);

          if (hologramId.equals(classAfter)) {
            // Player joined the hider class - update lobby manager and give kit
            HuntPlayerData playerData = lobbyManager.getOrCreatePlayerData(player.getUniqueId());

            // Check if player was previously a hunter and remove any hunter disguises
            if (playerData.getSelectedTeam() == HuntTeam.HUNTERS) {
              // Remove hunter disguise (using disguise manager)
              disguiseManager.removeDisguise(player);
              player.sendMessage(
                  Component.text("Removed your hunter disguise!", NamedTextColor.YELLOW));
            }

            playerData.setSelectedTeam(HuntTeam.HIDERS);
            playerData.setSelectedHiderClass(hiderClass);

            // Remove any existing kit first, then give new kit
            kitManager.removePlayerKit(player);
            kitManager.giveHiderKit(player, hiderClass);
            player.sendMessage(
                Component.text(
                    "Joined " + hiderClass.getDisplayName() + " (Hider)", NamedTextColor.GREEN));

            // Update prep phase holograms if prep phase is active
            prepPhaseManager.updatePlayerClassSelection(
                player.getUniqueId(), HuntTeam.HIDERS, hiderClass);
          } else {
            // Player left the class - update lobby manager and remove kit
            HuntPlayerData playerData = lobbyManager.getPlayerData(player.getUniqueId());
            if (playerData != null) {
              playerData.setSelectedTeam(null);
              playerData.setSelectedHiderClass(null);
            }

            kitManager.removePlayerKit(player);
            player.sendMessage(
                Component.text(
                    "Left " + hiderClass.getDisplayName() + " (Hider)", NamedTextColor.YELLOW));

            // Update prep phase holograms if prep phase is active
            prepPhaseManager.updatePlayerClassSelection(player.getUniqueId(), null, null);
          }
        }
        return;
      }
    }

    // Class not found
    player.sendMessage(Component.text("Unknown class: " + className, NamedTextColor.RED));
    player.sendMessage(Component.text("Available classes:", NamedTextColor.YELLOW));
    player.sendMessage(Component.text("Hunters: brute, nimble, saboteur", NamedTextColor.WHITE));
    player.sendMessage(Component.text("Hiders: trickster, phaser, cloaker", NamedTextColor.WHITE));
  }

  /**
   * Handles a player voting for a map.
   *
   * @param player The player voting
   * @param mapName The name of the map to vote for
   */
  private void handleMapVote(Player player, String mapName) {
    // Check vote cooldown
    UUID playerId = player.getUniqueId();
    long currentTime = System.currentTimeMillis();

    if (voteJoinCooldowns.containsKey(playerId)) {
      long lastVoteTime = voteJoinCooldowns.get(playerId);
      if (currentTime - lastVoteTime < VOTE_COOLDOWN_MS) {
        // Still on cooldown, ignore the vote
        return;
      }
    }

    // Update cooldown
    voteJoinCooldowns.put(playerId, currentTime);

    // Try to match the map
    for (HuntMap map : HuntMap.values()) {
      if (map.name().toLowerCase().equals(mapName)) {
        String hologramId = "map_" + mapName.toLowerCase();

        // Store the current state before calling the hologram manager
        String mapBefore = hologramManager.getPlayerMapVote(player.getUniqueId());

        // Always call the hologram manager - it will handle the toggle logic internally
        hologramManager.addPlayerToMap(player.getUniqueId(), player.getName(), mapName);

        // Check if the state actually changed to determine what message to send
        String mapAfter = hologramManager.getPlayerMapVote(player.getUniqueId());

        // Update prep phase manager as well
        if (hologramId.equals(mapAfter)) {
          // Player voted for the map - notify prep phase manager
          prepPhaseManager.handleMapVote(player, map);
        } else if (mapBefore != null && mapBefore.equals(hologramId)) {
          // Player removed their vote for this map - notify prep phase manager
          prepPhaseManager.removeMapVote(player);
        }

        // Only send a message if the state actually changed
        if (!java.util.Objects.equals(mapBefore, mapAfter)) {
          if (hologramId.equals(mapAfter)) {
            // Player voted for the map (message already sent by prep phase manager)
            // player.sendMessage already called by prepPhaseManager.handleMapVote
          } else {
            // Player removed their vote for the map (message already sent by prep phase
            // manager)
            // player.sendMessage already called by prepPhaseManager.removeMapVote
          }
        }
        return;
      }
    }

    // Map not found
    player.sendMessage(Component.text("Unknown map: " + mapName, NamedTextColor.RED));
    player.sendMessage(Component.text("Available maps:", NamedTextColor.YELLOW));
    for (HuntMap map : HuntMap.values()) {
      player.sendMessage(Component.text("• " + map.name().toLowerCase(), NamedTextColor.WHITE));
    }
  }

  /**
   * Handles a player leaving their current class.
   *
   * @param player The player leaving their class
   */
  private void handleLeaveClass(Player player) {
    String currentClass = hologramManager.getPlayerClassSelection(player.getUniqueId());
    if (currentClass == null) {
      player.sendMessage(Component.text("You are not in any class!", NamedTextColor.RED));
      return;
    }

    // Remove player from their current class
    boolean removed = hologramManager.removePlayerFromClass(player.getUniqueId());
    if (removed) {
      // Remove their kit and handle disguise removal
      kitManager.removePlayerKit(player);

      // Handle disguise removal if player was in a hunter class
      if (disguiseManager != null && currentClass.startsWith("hunter_")) {
        disguiseManager.handleClassChange(player, currentClass, null);
      }

      // Determine class name for message
      String className = "Unknown";
      String teamType = "Unknown";
      if (currentClass.startsWith("hunter_")) {
        String classKey = currentClass.substring("hunter_".length()).toUpperCase();
        try {
          HunterClass hunterClass = HunterClass.valueOf(classKey);
          className = hunterClass.getDisplayName();
          teamType = "Hunter";
        } catch (IllegalArgumentException e) {
          // Fallback
        }
      } else if (currentClass.startsWith("hider_")) {
        String classKey = currentClass.substring("hider_".length()).toUpperCase();
        try {
          HiderClass hiderClass = HiderClass.valueOf(classKey);
          className = hiderClass.getDisplayName();
          teamType = "Hider";
        } catch (IllegalArgumentException e) {
          // Fallback
        }
      }

      player.sendMessage(
          Component.text("Left " + className + " (" + teamType + ")", NamedTextColor.YELLOW));
    } else {
      player.sendMessage(Component.text("Failed to leave class!", NamedTextColor.RED));
    }
  }

  /**
   * Handles a player removing their map vote.
   *
   * @param player The player removing their map vote
   */
  private void handleLeaveMap(Player player) {
    String currentMap = hologramManager.getPlayerMapVote(player.getUniqueId());
    if (currentMap == null) {
      player.sendMessage(Component.text("You haven't voted for any map!", NamedTextColor.RED));
      return;
    }

    // Remove player from their current map vote (hologram system)
    boolean removed = hologramManager.removePlayerFromMapVote(player.getUniqueId());

    // Also remove from prep phase manager
    prepPhaseManager.removeMapVote(player);

    if (!removed) {
      player.sendMessage(Component.text("Failed to remove map vote!", NamedTextColor.RED));
    }
  }

  /**
   * Teleports a player to the hunt world spawn location as defined in hunt.yml.
   *
   * @param player The player to teleport
   */
  private void teleportToHuntSpawn(Player player) {
    try {
      // Load hunt configuration
      File huntConfigFile = new File(plugin.getDataFolder(), "hunt.yml");
      if (!huntConfigFile.exists()) {
        player.sendMessage(Component.text("Hunt configuration not found!", NamedTextColor.RED));
        return;
      }

      FileConfiguration huntConfig = YamlConfiguration.loadConfiguration(huntConfigFile);

      // Get world name
      String worldName = huntConfig.getString("hunt.world", "world");

      // First, use Multiverse to teleport player to the world
      String mvTpCommand = "mv tp " + player.getName() + " " + worldName;
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), mvTpCommand);

      // Schedule the coordinate teleport to happen after the world teleport
      Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                try {
                  World world = Bukkit.getWorld(worldName);
                  if (world == null) {
                    player.sendMessage(
                        Component.text(
                            "Hunt world '" + worldName + "' not found!", NamedTextColor.RED));
                    return;
                  }

                  // Get spawn coordinates
                  double x = huntConfig.getDouble("hunt.spawn.x", 0.0);
                  double y = huntConfig.getDouble("hunt.spawn.y", 65.0);
                  double z = huntConfig.getDouble("hunt.spawn.z", 0.0);
                  float yaw = (float) huntConfig.getDouble("hunt.spawn.yaw", 0.0);
                  float pitch = (float) huntConfig.getDouble("hunt.spawn.pitch", 0.0);

                  Location spawnLocation = new Location(world, x, y, z, yaw, pitch);

                  // Clear any existing kits, disguises, and effects before entering Hunt
                  clearPlayerForHuntLobby(player);

                  // Teleport player to specific coordinates
                  player.teleport(spawnLocation);
                  player.sendMessage(Component.text("Welcome to the Hunt!", NamedTextColor.GREEN));

                } catch (Exception e) {
                  player.sendMessage(
                      Component.text(
                          "Failed to teleport to hunt spawn coordinates!", NamedTextColor.RED));
                  plugin
                      .getLogger()
                      .warning(
                          "Failed to teleport player to hunt spawn coordinates: " + e.getMessage());
                }
              },
              10L); // Wait 10 ticks (0.5 seconds) for the world teleport to complete

    } catch (Exception e) {
      player.sendMessage(Component.text("Failed to teleport to hunt spawn!", NamedTextColor.RED));
      plugin.getLogger().warning("Failed to teleport player to hunt spawn: " + e.getMessage());
    }
  }

  /**
   * Clears all kits, disguises, potion effects, and resets player state for entering Hunt lobby.
   *
   * @param player The player to clean up
   */
  private void clearPlayerForHuntLobby(Player player) {
    try {
      // Remove any active disguises
      if (DisguiseAPI.isDisguised(player)) {
        DisguiseAPI.undisguiseToAll(player);
      }

      // Remove all potion effects
      for (PotionEffect effect : player.getActivePotionEffects()) {
        player.removePotionEffect(effect.getType());
      }

      // Reset entity size back to normal if it was changed
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), String.format("entitysize player %s 1.0", player.getName()));

      // Clear inventory items from other games (keep player's normal items)
      // Note: We don't clear the entire inventory as players should keep their
      // personal items
      kitManager.removePlayerKit(player);

      // Clear any hider utility data if the player had hider abilities active
      if (hiderUtilityListener != null) {
        hiderUtilityListener.clearPlayerCooldowns(player.getUniqueId());
      }

      plugin.getLogger().info("Cleared " + player.getName() + " for Hunt lobby entry");

    } catch (Exception e) {
      plugin
          .getLogger()
          .warning(
              "Failed to clear player " + player.getName() + " for Hunt lobby: " + e.getMessage());
    }
  }
}
