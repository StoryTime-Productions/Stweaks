package com.storytimeproductions.stweaks.games.hunt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import me.libraryaddict.disguise.DisguiseAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages the prep phase of Hunt games including map voting, player readiness tracking, countdown,
 * and game initialization.
 */
public class HuntPrepPhaseManager {
  private final JavaPlugin plugin;
  private final HuntKitManager kitManager;
  private final HuntLobbyManager lobbyManager;
  private final HuntDisguiseManager disguiseManager;
  private final HuntHologramManager hologramManager;
  private final FileConfiguration config;
  private HuntDeathHandler deathHandler; // Reference to death handler for counting hiders

  // Voting and ready status tracking
  private final Map<UUID, HuntMap> playerMapVotes;
  private final Map<HuntMap, Integer> mapVoteCounts;
  private final Map<UUID, Boolean> playerReadyStatus;

  // Phase state management
  private boolean prepPhaseActive;
  private boolean gameStarting;
  private boolean gameEnded;

  private HuntMap selectedMap;
  private BukkitTask countdownTask;
  private BukkitTask gameTimer;
  private BukkitTask heartbeatTask;
  private BossBar gameTimerBar;

  // Game participants (only ready players)
  private final Map<UUID, HuntTeam> gameParticipants;
  private final Map<UUID, Object> gameClassSelections; // HunterClass or HiderClass

  // All players who have selected classes (regardless of ready status)
  private final Map<UUID, HuntTeam> allPlayerSelections;

  /**
   * Constructs a new HuntPrepPhaseManager.
   *
   * @param plugin The JavaPlugin instance
   * @param lobbyManager The lobby manager
   * @param hologramManager The hologram manager
   * @param disguiseManager The disguise manager
   * @param config The configuration
   */
  public HuntPrepPhaseManager(
      JavaPlugin plugin,
      HuntLobbyManager lobbyManager,
      HuntHologramManager hologramManager,
      HuntDisguiseManager disguiseManager,
      HuntKitManager kitManager,
      FileConfiguration config) {
    this.plugin = plugin;
    this.lobbyManager = lobbyManager;
    this.hologramManager = hologramManager;
    this.disguiseManager = disguiseManager;
    this.kitManager = kitManager;
    this.config = config;
    this.playerMapVotes = new HashMap<>();
    this.mapVoteCounts = new HashMap<>();
    this.playerReadyStatus = new HashMap<>();
    this.gameParticipants = new HashMap<>();
    this.gameClassSelections = new HashMap<>();
    this.allPlayerSelections = new HashMap<>();
    this.prepPhaseActive = false;
    this.gameStarting = false;
    this.gameEnded = false;

    // Initialize map vote counts
    for (HuntMap map : HuntMap.values()) {
      mapVoteCounts.put(map, 0);
    }
  }

  /** Starts the prep phase and creates all necessary holograms. */
  public void startPrepPhase() {
    if (prepPhaseActive) {
      return;
    }

    prepPhaseActive = true;
    gameStarting = false;
    gameEnded = false;

    plugin.getLogger().info("Starting Hunt prep phase");

    // Clear previous state
    playerMapVotes.clear();
    playerReadyStatus.clear();
    gameParticipants.clear();
    gameClassSelections.clear();
    allPlayerSelections.clear();

    for (HuntMap map : HuntMap.values()) {
      mapVoteCounts.put(map, 0);
    }

    // Initialize start game hologram
    initializeStartGameHologram();

    // Initialize ready status hologram
    initializeReadyStatusHologram();

    // Update start game hologram to initial state
    updateStartGameHologram();

    // Notify all players
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendMessage(
          Component.text(
              "Hunt prep phase started! Vote for a map and get ready!", NamedTextColor.GREEN));
    }
  }

  /** Ends the prep phase and removes players from holograms. */
  public void endPrepPhase() {
    if (!prepPhaseActive) {
      return;
    }

    prepPhaseActive = false;

    // Cancel any running tasks
    if (countdownTask != null) {
      countdownTask.cancel();
      countdownTask = null;
    }

    if (heartbeatTask != null) {
      heartbeatTask.cancel();
      heartbeatTask = null;
    }

    // Remove players from holograms instead of clearing them
    removePlayersFromStartGameHologram();
    removePlayersFromReadyStatusHologram();
    removePlayersFromAllHolograms();

    plugin.getLogger().info("Hunt prep phase ended");
  }

  /**
   * Handles a player voting for a map.
   *
   * @param player The player voting
   * @param map The map they're voting for
   */
  public void handleMapVote(Player player, HuntMap map) {
    if (!prepPhaseActive || gameStarting) {
      return;
    }

    UUID playerId = player.getUniqueId();
    HuntMap previousVote = playerMapVotes.get(playerId);

    // Remove previous vote
    if (previousVote != null) {
      mapVoteCounts.put(previousVote, mapVoteCounts.get(previousVote) - 1);
    }

    // Add new vote
    playerMapVotes.put(playerId, map);
    mapVoteCounts.put(map, mapVoteCounts.get(map) + 1);

    player.sendMessage(
        Component.text("Voted for " + map.getDisplayName() + "!", NamedTextColor.YELLOW));

    // Update game participants since map vote is now required
    HuntPlayerData data = lobbyManager.getPlayerData(playerId);
    if (data != null && data.getSelectedTeam() != null) {
      HuntTeam team = data.getSelectedTeam();
      Object classSelection =
          (team == HuntTeam.HUNTERS) ? data.getSelectedHunterClass() : data.getSelectedHiderClass();

      if (classSelection != null) {
        // Player now has team, class, and map vote - add to participants
        gameParticipants.put(playerId, team);
        gameClassSelections.put(playerId, classSelection);
        plugin
            .getLogger()
            .info("Added " + player.getName() + " to gameParticipants after map vote");
      }
    }

    // Update holograms to reflect new vote status
    updateReadyStatusHologram();
  }

  /**
   * Handles a player marking themselves as ready or not ready.
   *
   * @param player The player
   * @param ready Whether they are ready
   */
  public void setPlayerReady(Player player, boolean ready) {
    if (!prepPhaseActive || gameStarting) {
      return;
    }

    UUID playerId = player.getUniqueId();
    HuntPlayerData data = lobbyManager.getPlayerData(playerId);

    // Debug logging
    Bukkit.getLogger()
        .info(
            "Hunt setPlayerReady: "
                + player.getName()
                + " - Ready: "
                + ready
                + ", Has data: "
                + (data != null)
                + ", Has team/class: "
                + (data != null
                    && data.getSelectedTeam() != null
                    && ((data.getSelectedTeam() == HuntTeam.HUNTERS
                            && data.getSelectedHunterClass() != null)
                        || (data.getSelectedTeam() == HuntTeam.HIDERS
                            && data.getSelectedHiderClass() != null)))
                + ", Has map vote: "
                + playerMapVotes.containsKey(playerId));

    // Require team, class selection, AND map vote
    if (data == null
        || data.getSelectedTeam() == null
        || (data.getSelectedTeam() == HuntTeam.HUNTERS && data.getSelectedHunterClass() == null)
        || (data.getSelectedTeam() == HuntTeam.HIDERS && data.getSelectedHiderClass() == null)
        || !playerMapVotes.containsKey(playerId)) {
      player.sendMessage(
          Component.text(
              "Please select a team, class, and vote for a map before readying up!",
              NamedTextColor.RED));
      return;
    }

    playerReadyStatus.put(playerId, ready);

    if (ready) {
      // Store player's selections for the game (they should already be in
      // gameParticipants from class selection)
      if (!gameParticipants.containsKey(playerId)) {
        gameParticipants.put(playerId, data.getSelectedTeam());
        if (data.getSelectedTeam() == HuntTeam.HUNTERS) {
          gameClassSelections.put(playerId, data.getSelectedHunterClass());
        } else {
          gameClassSelections.put(playerId, data.getSelectedHiderClass());
        }

        // Debug logging
        Bukkit.getLogger()
            .info(
                "Hunt added player to gameParticipants: "
                    + player.getName()
                    + " - Team: "
                    + data.getSelectedTeam()
                    + ", Class: "
                    + (data.getSelectedTeam() == HuntTeam.HUNTERS
                        ? data.getSelectedHunterClass()
                        : data.getSelectedHiderClass()));
      }
    }
    // Note: We don't remove from gameParticipants when unreadying - they're still
    // in the game

    player.sendMessage(
        Component.text(
            ready ? "You are now ready!" : "You are no longer ready",
            ready ? NamedTextColor.GREEN : NamedTextColor.YELLOW));

    // Update ready status hologram
    updateReadyStatusHologram();

    // Check if all players are ready
    checkAllPlayersReady();
  }

  /** Attempts to start the game if conditions are met. */
  public void attemptGameStart() {
    if (!prepPhaseActive || gameStarting) {
      return;
    }

    // Debug logging for game start attempt
    Bukkit.getLogger()
        .info(
            "Hunt game start attempt - gameParticipants size: "
                + gameParticipants.size()
                + ", playerReadyStatus size: "
                + playerReadyStatus.size()
                + ", gameClassSelections size: "
                + gameClassSelections.size());

    // Create copy to avoid ConcurrentModificationException
    Set<UUID> participantsCopy = new HashSet<>(gameParticipants.keySet());
    for (UUID playerId : participantsCopy) {
      Player player = Bukkit.getPlayer(playerId);
      String playerName = (player != null) ? player.getName() : "Unknown";
      boolean isReady = Boolean.TRUE.equals(playerReadyStatus.get(playerId));
      boolean hasClass = gameClassSelections.containsKey(playerId);
      Bukkit.getLogger()
          .info(
              "Hunt participant: "
                  + playerName
                  + " - Ready: "
                  + isReady
                  + ", Has class: "
                  + hasClass
                  + ", Team: "
                  + gameParticipants.get(playerId));
    }

    if (!canStartGame()) {
      // Send specific error messages
      if (gameParticipants.isEmpty()) {
        broadcastMessage("Cannot start game: No players ready!", NamedTextColor.RED);
      } else {
        boolean hasHunter =
            gameParticipants.values().stream().anyMatch(team -> team == HuntTeam.HUNTERS);
        if (!hasHunter) {
          broadcastMessage("Cannot start game: Need at least one hunter!", NamedTextColor.RED);
        } else {
          broadcastMessage("Cannot start game: Not all players are ready!", NamedTextColor.RED);
        }
      }
      return;
    }

    // Start countdown
    startGameCountdown();
  }

  /** Starts the 5-second countdown to game start. */
  private void startGameCountdown() {
    gameStarting = true;

    countdownTask =
        new BukkitRunnable() {
          int countdown = config.getInt("hunt.prep-phase.countdown-duration", 5);

          @Override
          public void run() {
            if (countdown > 0) {
              // Show countdown title to all participants
              Title countdownTitle =
                  Title.title(
                      Component.text(countdown, NamedTextColor.RED),
                      Component.text("Game starting...", NamedTextColor.YELLOW));

              for (UUID playerId : new HashSet<>(gameParticipants.keySet())) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                  player.showTitle(countdownTitle);
                }
              }

              countdown--;
            } else {
              // Start the game
              startGame();
              cancel();
            }
          }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second
  }

  /** Starts the actual game after countdown. */
  private void startGame() {
    // Select map (random if no clear winner)
    selectedMap = selectWinningMap();

    // Show map selection title
    Title mapTitle =
        Title.title(
            Component.text("MAP: " + selectedMap.getDisplayName(), NamedTextColor.GOLD),
            Component.text("Good luck!", NamedTextColor.YELLOW));

    for (UUID playerId : new HashSet<>(gameParticipants.keySet())) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        player.showTitle(mapTitle);
      }
    }

    // Teleport players to the selected map
    Bukkit.getScheduler()
        .runTaskLater(plugin, this::teleportPlayersToMap, 40L); // 2 seconds after title

    // End prep phase
    endPrepPhase();

    plugin.getLogger().info("Hunt game started on map: " + selectedMap.getDisplayName());
  }

  /**
   * Selects the winning map based on votes, with random selection for ties.
   *
   * @return The selected map
   */
  private HuntMap selectWinningMap() {
    // Find the map with the most votes
    HuntMap winningMap = null;
    int maxVotes = -1;

    for (Map.Entry<HuntMap, Integer> entry : mapVoteCounts.entrySet()) {
      if (entry.getValue() > maxVotes) {
        maxVotes = entry.getValue();
        winningMap = entry.getKey();
      }
    }

    // If no votes or tie, select randomly
    if (winningMap == null || maxVotes == 0) {
      HuntMap[] maps = HuntMap.values();
      winningMap = maps[new Random().nextInt(maps.length)];
    }

    return winningMap;
  }

  /** Teleports all players to their appropriate spawn locations on the selected map. */
  private void teleportPlayersToMap() {
    ConfigurationSection mapConfig =
        config.getConfigurationSection("hunt.maps." + selectedMap.name().toLowerCase());
    if (mapConfig == null) {
      plugin.getLogger().warning("No configuration found for map: " + selectedMap.name());
      return;
    }

    String worldName = mapConfig.getString("world", "hunt");
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      plugin.getLogger().warning("World not found: " + worldName);
      return;
    }

    // Create copies of participant lists to avoid ConcurrentModificationException
    Map<UUID, HuntTeam> participantsCopy = new HashMap<>(gameParticipants);

    int teleportedCount = 0;
    int hunterCount = 0;
    int hiderCount = 0;

    // Teleport hunters to hunter spawn
    ConfigurationSection hunterSpawnConfig = mapConfig.getConfigurationSection("hunter-spawn");
    if (hunterSpawnConfig != null) {
      Location hunterSpawn =
          new Location(
              world,
              hunterSpawnConfig.getDouble("x"),
              hunterSpawnConfig.getDouble("y"),
              hunterSpawnConfig.getDouble("z"),
              (float) hunterSpawnConfig.getDouble("yaw"),
              (float) hunterSpawnConfig.getDouble("pitch"));

      for (Map.Entry<UUID, HuntTeam> entry : participantsCopy.entrySet()) {
        if (entry.getValue() == HuntTeam.HUNTERS) {
          Player player = Bukkit.getPlayer(entry.getKey());
          if (player != null && player.isOnline()) {
            // Teleport hunter without removing kit or disguise
            player.teleport(hunterSpawn);
            teleportedCount++;
            hunterCount++;

            player.sendMessage(
                Component.text("You have been locked in the hunter area!", NamedTextColor.RED));

            plugin.getLogger().info("Teleported hunter " + player.getName() + " to hunter spawn");
          } else {
            plugin.getLogger().warning("Hunter player is null or offline: " + entry.getKey());
          }
        }
      }
    } else {
      plugin
          .getLogger()
          .warning("No hunter spawn configuration found for map: " + selectedMap.name());
    }

    // Teleport hiders to random hider spawns
    List<Map<?, ?>> hiderSpawns = mapConfig.getMapList("hider-spawns");
    if (!hiderSpawns.isEmpty()) {
      Random random = new Random();

      for (Map.Entry<UUID, HuntTeam> entry : participantsCopy.entrySet()) {
        if (entry.getValue() == HuntTeam.HIDERS) {
          Player player = Bukkit.getPlayer(entry.getKey());
          if (player != null && player.isOnline()) {
            // Select random spawn point
            Map<?, ?> spawnData = hiderSpawns.get(random.nextInt(hiderSpawns.size()));
            Location hiderSpawn =
                new Location(
                    world,
                    ((Number) spawnData.get("x")).doubleValue(),
                    ((Number) spawnData.get("y")).doubleValue(),
                    ((Number) spawnData.get("z")).doubleValue(),
                    ((Number) spawnData.get("yaw")).floatValue(),
                    ((Number) spawnData.get("pitch")).floatValue());

            // Teleport hider without removing kit
            player.teleport(hiderSpawn);
            teleportedCount++;
            hiderCount++;

            player.sendMessage(Component.text("Find a hiding spot quickly!", NamedTextColor.BLUE));

            plugin.getLogger().info("Teleported hider " + player.getName() + " to hider spawn");
          } else {
            plugin.getLogger().warning("Hider player is null or offline: " + entry.getKey());
          }
        }
      }
    } else {
      plugin
          .getLogger()
          .warning("No hider spawn configuration found for map: " + selectedMap.name());
    }

    plugin
        .getLogger()
        .info(
            "Teleported "
                + teleportedCount
                + " players ("
                + hunterCount
                + " hunters, "
                + hiderCount
                + " hiders) to map: "
                + selectedMap.name());

    // After teleporting all players, re-apply kits, disguises, and
    // passives/abilities - add a short delay to ensure teleport completes first
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              // Use a copy of participants to avoid ConcurrentModificationException
              Map<UUID, HuntTeam> participantsCopyInner = new HashMap<>(gameParticipants);

              plugin
                  .getLogger()
                  .info("Re-applying kits, abilities, and disguises after teleport...");

              for (Map.Entry<UUID, HuntTeam> entry : participantsCopyInner.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                  HuntPlayerData data = lobbyManager.getPlayerData(entry.getKey());
                  if (data == null) {
                    plugin
                        .getLogger()
                        .warning(
                            "[DEBUG] No player data found for "
                                + player.getName()
                                + " during teleport");
                    continue;
                  }

                  // Log current world for debugging
                  plugin
                      .getLogger()
                      .info(
                          "[DEBUG] Player "
                              + player.getName()
                              + " is in world: "
                              + player.getWorld().getName());

                  // Heal all players to full health after teleport
                  player.setHealth(20.0); // Standard max health for players
                  player.setFoodLevel(20);
                  player.setSaturation(20.0f);

                  if (entry.getValue() == HuntTeam.HUNTERS) {
                    // Re-apply hunter kit, abilities, and disguise
                    if (data.getSelectedHunterClass() != null) {
                      // First apply the kit (only if not spectator)
                      if (player.getGameMode() != GameMode.SPECTATOR) {
                        kitManager.giveHunterKit(player, data.getSelectedHunterClass());

                        // Then apply abilities (these are important for gameplay)
                        kitManager.applyHunterAbilities(player, data.getSelectedHunterClass());

                        plugin
                            .getLogger()
                            .info(
                                "[DEBUG] Applied hunter abilities for "
                                    + player.getName()
                                    + " as "
                                    + data.getSelectedHunterClass().name());
                      } else {
                        plugin
                            .getLogger()
                            .info(
                                "[DEBUG] Skipped applying hunter abilities to spectator: "
                                    + player.getName());
                      }
                    }

                    // Re-apply disguise last - with additional debug logging
                    if (disguiseManager != null) {
                      String disguiseSkin = data.getSelectedDisguise();
                      String disguiseActualSkin = data.getSelectedDisguiseSkin();
                      if (disguiseSkin != null && !disguiseSkin.isEmpty()) {
                        plugin
                            .getLogger()
                            .info(
                                "[DEBUG] Found saved disguise '"
                                    + disguiseSkin
                                    + "' with skin '"
                                    + (disguiseActualSkin != null ? disguiseActualSkin : "null")
                                    + "' for "
                                    + player.getName());
                      } else {
                        plugin
                            .getLogger()
                            .warning("[DEBUG] No saved disguise found for " + player.getName());
                      }

                      plugin
                          .getLogger()
                          .info("[DEBUG] Attempted to re-apply disguise for " + player.getName());
                    } else {
                      plugin
                          .getLogger()
                          .warning(
                              "[DEBUG] disguiseManager is null when trying to apply disguises");
                    }
                  } else if (entry.getValue() == HuntTeam.HIDERS) {
                    // Re-apply hider kit and passives
                    if (data.getSelectedHiderClass() != null) {
                      // First apply the kit (only if not spectator)
                      if (player.getGameMode() != GameMode.SPECTATOR) {
                        kitManager.giveHiderKit(player, data.getSelectedHiderClass());

                        // Then apply passives (these are important for gameplay)
                        kitManager.applyHiderPassives(player, data.getSelectedHiderClass());
                        plugin
                            .getLogger()
                            .info(
                                "Applied hider passives for "
                                    + player.getName()
                                    + " as "
                                    + data.getSelectedHiderClass().name());
                      } else {
                        plugin
                            .getLogger()
                            .info(
                                "Skipped applying hider passives to spectator: "
                                    + player.getName());
                      }
                    }
                  }
                }
              }

              // Initialize hider count in death handler AFTER players are teleported and
              // setup
              if (deathHandler != null) {
                deathHandler.initializeAliveHidersCount();
                plugin.getLogger().info("Initialized alive hiders count after teleport and setup");
              }
            },
            10L); // 0.5 second delay after teleport

    // Start hunter lock-in timer with boss bar countdown
    startHunterLockIn();

    // Start game timer (after lock-in period)
    int lockDuration = config.getInt("hunt.prep-phase.hunter-lock-duration", 30);
    Bukkit.getScheduler().runTaskLater(plugin, this::startGameTimer, (lockDuration + 2) * 20L);
  }

  /** Starts the hunter lock-in period where hunters cannot move and are blinded. */
  private void startHunterLockIn() {
    int lockDuration = config.getInt("hunt.prep-phase.hunter-lock-duration", 30);

    // Create copies of participant lists to avoid ConcurrentModificationException
    Map<UUID, HuntTeam> participantsCopy = new HashMap<>(gameParticipants);

    // Create boss bar for lock-in countdown
    BossBar lockInBar =
        Bukkit.createBossBar(
            "Hunters locked in: " + lockDuration + " seconds", BarColor.YELLOW, BarStyle.SOLID);

    // Add all game participants to boss bar
    for (UUID playerId : participantsCopy.keySet()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null && player.isOnline()) {
        lockInBar.addPlayer(player);
      }
    }
    lockInBar.setVisible(true);

    // Apply blindness and slowness to hunters (movement disabled)
    for (Map.Entry<UUID, HuntTeam> entry : participantsCopy.entrySet()) {
      if (entry.getValue() == HuntTeam.HUNTERS) {
        Player hunter = Bukkit.getPlayer(entry.getKey());
        if (hunter != null && hunter.isOnline()) {
          // Clear any existing potion effects first to prevent conflicts
          for (PotionEffectType effectType :
              new PotionEffectType[] {
                PotionEffectType.BLINDNESS,
                PotionEffectType.SLOWNESS,
                PotionEffectType.JUMP_BOOST,
                PotionEffectType.WEAKNESS,
                PotionEffectType.DARKNESS
              }) {
            hunter.removePotionEffect(effectType);
          }

          // Apply extreme effects to fully disable movement
          hunter.addPotionEffect(
              new PotionEffect(
                  PotionEffectType.BLINDNESS, lockDuration * 20, 1, false, false, true));
          hunter.addPotionEffect(
              new PotionEffect(
                  PotionEffectType.DARKNESS, lockDuration * 20, 1, false, false, true));
          hunter.addPotionEffect(
              new PotionEffect(
                  PotionEffectType.SLOWNESS,
                  lockDuration * 20,
                  255,
                  false,
                  false,
                  true)); // Max slowness (level 255)
          hunter.addPotionEffect(
              new PotionEffect(
                  PotionEffectType.WEAKNESS,
                  lockDuration * 20,
                  100,
                  false,
                  false,
                  true)); // Make them very weak
          hunter.addPotionEffect(
              new PotionEffect(
                  PotionEffectType.JUMP_BOOST,
                  lockDuration * 20,
                  128, // Negative jump boost prevents jumping
                  false,
                  false,
                  true));

          // Send title to hunter indicating they're locked
          Title lockedTitle =
              Title.title(
                  Component.text("LOCKED", NamedTextColor.RED),
                  Component.text("Wait for hiders to hide", NamedTextColor.YELLOW));
          hunter.showTitle(lockedTitle);

          plugin
              .getLogger()
              .info("Applied blindness and movement restrictions to hunter: " + hunter.getName());
        }
      }
    }

    // Start countdown timer
    BukkitRunnable lockInCountdown =
        new BukkitRunnable() {
          int timeLeft = lockDuration;

          @Override
          public void run() {
            if (timeLeft <= 0) {
              // Release hunters
              releaseHunters();

              // Remove lock-in boss bar
              lockInBar.removeAll();

              cancel();
              return;
            }

            // Update boss bar
            lockInBar.setTitle("Hunters locked in: " + timeLeft + " seconds");
            lockInBar.setProgress((double) timeLeft / lockDuration);

            // Change color based on time remaining
            if (timeLeft <= 10) {
              lockInBar.setColor(BarColor.RED);
            } else if (timeLeft <= 20) {
              lockInBar.setColor(BarColor.YELLOW);
            }

            timeLeft--;
          }
        };

    lockInCountdown.runTaskTimer(plugin, 0L, 20L); // Run every second
  }

  /** Releases hunters from lock-in by removing effects and announcing. */
  private void releaseHunters() {
    // Create copies of participant lists to avoid ConcurrentModificationException
    Map<UUID, HuntTeam> participantsCopy = new HashMap<>(gameParticipants);

    // Remove effects from hunters
    for (Map.Entry<UUID, HuntTeam> entry : participantsCopy.entrySet()) {
      if (entry.getValue() == HuntTeam.HUNTERS) {
        Player hunter = Bukkit.getPlayer(entry.getKey());
        if (hunter != null && hunter.isOnline()) {
          // Remove ALL movement restriction effects
          for (PotionEffectType effectType :
              new PotionEffectType[] {
                PotionEffectType.BLINDNESS,
                PotionEffectType.SLOWNESS,
                PotionEffectType.JUMP_BOOST,
                PotionEffectType.WEAKNESS,
                PotionEffectType.DARKNESS
              }) {
            hunter.removePotionEffect(effectType);
          }

          // Re-apply hunter abilities to ensure they're active after restrictions are
          // removed
          HuntPlayerData data = lobbyManager.getPlayerData(hunter.getUniqueId());
          if (data != null && data.getSelectedHunterClass() != null) {
            // Only apply abilities if still not in spectator mode
            if (hunter.getGameMode() != GameMode.SPECTATOR) {
              kitManager.applyHunterAbilities(hunter, data.getSelectedHunterClass());
              plugin
                  .getLogger()
                  .info("Re-applied abilities for " + hunter.getName() + " after release");
            } else {
              plugin
                  .getLogger()
                  .info("Skipped re-applying abilities to spectator hunter: " + hunter.getName());
            }
          }

          // Show dramatic "HUNT!" title
          Title huntTitle =
              Title.title(
                  Component.text("HUNT!", NamedTextColor.DARK_RED),
                  Component.text("Find the hiders!", NamedTextColor.YELLOW));
          hunter.showTitle(huntTitle);

          hunter.sendMessage(Component.text("You are now free to hunt!", NamedTextColor.GREEN));
          plugin.getLogger().info("Released hunter from lock-in: " + hunter.getName());
        }
      }
    }

    broadcastMessage("Hunters are now released! The hunt begins!", NamedTextColor.RED);
  }

  /** Starts the main game timer displayed on the action bar. */
  private void startGameTimer() {
    int gameDuration = config.getInt("hunt.prep-phase.game-duration", 5); // minutes

    // Create copies of participant lists to avoid ConcurrentModificationException
    Map<UUID, HuntTeam> participantsCopy = new HashMap<>(gameParticipants);

    // Create boss bar for game timer
    gameTimerBar =
        Bukkit.createBossBar(
            "Game Time Remaining: " + gameDuration + ":00", BarColor.GREEN, BarStyle.SOLID);

    // Add all game participants to boss bar
    for (UUID playerId : participantsCopy.keySet()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        gameTimerBar.addPlayer(player);
      }
    }

    gameTimerBar.setVisible(true);

    // Start countdown timer
    gameTimer =
        new BukkitRunnable() {
          int timeLeft = gameDuration * 60; // Convert to seconds

          @Override
          public void run() {
            if (timeLeft <= 0) {
              // Game over - hiders win
              endGame(HuntTeam.HIDERS);
              cancel();
              return;
            }

            // Update boss bar
            int minutes = timeLeft / 60;
            int seconds = timeLeft % 60;
            String timeDisplay = String.format("%d:%02d", minutes, seconds);

            // Count remaining hiders
            long hidersAlive =
                gameParticipants.entrySet().stream()
                    .filter(entry -> entry.getValue() == HuntTeam.HIDERS)
                    .map(entry -> Bukkit.getPlayer(entry.getKey()))
                    .filter(player -> player != null && player.isOnline())
                    .count();

            gameTimerBar.setTitle("Time: " + timeDisplay + " | Hiders: " + hidersAlive);
            gameTimerBar.setProgress((double) timeLeft / (gameDuration * 60));

            // Change color based on time remaining
            if (timeLeft <= 60) {
              gameTimerBar.setColor(BarColor.RED);
            } else if (timeLeft <= 120) {
              gameTimerBar.setColor(BarColor.YELLOW);
            }

            timeLeft--;
          }
        }.runTaskTimer(plugin, 0L, 20L);

    // Start heartbeat task for hiders when hunters are nearby
    startHeartbeatTask();
  }

  /**
   * Ends the current Hunt game and announces the winning team.
   *
   * @param winningTeam The team that won the game
   */
  public void endGame(HuntTeam winningTeam) {
    // Prevent duplicate execution
    if (gameEnded) {
      return;
    }
    gameEnded = true;

    // Cancel game timer
    if (gameTimer != null) {
      gameTimer.cancel();
      gameTimer = null;
    }

    // Cancel heartbeat task
    if (heartbeatTask != null) {
      heartbeatTask.cancel();
      heartbeatTask = null;
    }

    // Remove boss bar
    if (gameTimerBar != null) {
      gameTimerBar.removeAll();
      gameTimerBar = null;
    }

    // Announce winner
    Title winTitle =
        Title.title(
            Component.text(winningTeam.getDisplayName() + " WIN!", NamedTextColor.GOLD),
            Component.text("Game Over", NamedTextColor.YELLOW));

    for (UUID playerId : gameParticipants.keySet()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        player.showTitle(winTitle);
      }
    }

    broadcastMessage(
        winningTeam.getDisplayName() + " have won the Hunt game!", NamedTextColor.GOLD);

    // Spawn fireworks for the winning team
    spawnWinningTeamFireworks(winningTeam);

    // Remove players from all holograms (map, class, prep phase)
    removePlayersFromAllHolograms();

    // Clear game state
    gameParticipants.clear();
    gameClassSelections.clear();
    allPlayerSelections.clear();

    plugin.getLogger().info("Hunt game ended. Winner: " + winningTeam.getDisplayName());
  }

  /**
   * Spawns fireworks for the winning team for 3 seconds.
   *
   * @param winningTeam The team that won the game
   */
  private void spawnWinningTeamFireworks(HuntTeam winningTeam) {
    // Get alive players on the winning team
    List<Player> winningPlayers = new ArrayList<>();

    for (UUID playerId : gameParticipants.keySet()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null && player.isOnline() && player.getGameMode() != GameMode.SPECTATOR) {
        HuntPlayerData playerData = lobbyManager.getPlayerData(playerId);
        if (playerData != null && playerData.getSelectedTeam() == winningTeam) {
          winningPlayers.add(player);
        }
      }
    }

    // Use the death handler's fireworks method if available
    if (deathHandler != null) {
      deathHandler.spawnVictoryFireworks(winningPlayers);
      plugin
          .getLogger()
          .info(
              "Spawning victory fireworks for "
                  + winningTeam.getDisplayName()
                  + " ("
                  + winningPlayers.size()
                  + " players)");
    } else {
      plugin.getLogger().warning("Death handler not available for spawning victory fireworks");
      // Fallback - teleport players to lobby after a short delay
      Bukkit.getScheduler().runTaskLater(plugin, this::teleportAllPlayersToLobby, 60L); // 3 seconds
    }
  }

  // Hologram management methods
  private void clearAllPages(String hologramName) {
    // Remove all lines from page 1, keeping line 1 with placeholder content to
    // preserve the page
    for (int line = 6; line >= 2; line--) {
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh l remove " + hologramName + " 1 " + line);
    }

    // Add blank lines back to ensure subsequent set commands work
    for (int line = 2; line <= 6; line++) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add " + hologramName + " 1 &7");
    }
  }

  private void initializeStartGameHologram() {
    // Clear existing content
    clearAllPages("start_game");

    // Set initial content (line 1 already exists from clearAllPages)
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l set start_game 1 1 &c&lSTART GAME");
    // Add line 2 before setting
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add start_game 1 &7");
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(), "dh l set start_game 1 2 &7Wait for all players to be ready");
  }

  private void updateStartGameHologram() {
    // Check if all conditions are met for starting the game
    boolean canStart = canStartGame();

    if (canStart) {
      // Green start game - ready to start
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l set start_game 1 1 &a&lSTART GAME");
      // Add line 2 before setting (in case it doesn't exist)
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add start_game 1 &7");
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh l set start_game 1 2 &7Click to start the hunt!");
    } else {
      // Red start game - not ready
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l set start_game 1 1 &c&lSTART GAME");
      // Add line 2 before setting (in case it doesn't exist)
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add start_game 1 &7");
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh l set start_game 1 2 &7Wait for all players to be ready");
    }
  }

  private boolean canStartGame() {
    // Check if we have at least one hunter
    boolean hasHunter =
        gameParticipants.values().stream().anyMatch(team -> team == HuntTeam.HUNTERS);
    if (!hasHunter) {
      return false;
    }

    // Check if all participants are ready and have valid selections
    for (UUID playerId : gameParticipants.keySet()) {
      if (!Boolean.TRUE.equals(playerReadyStatus.get(playerId))) {
        return false;
      }

      if (!gameClassSelections.containsKey(playerId)) {
        return false;
      }
    }

    // Must have at least one participant
    return !gameParticipants.isEmpty();
  }

  private void initializeReadyStatusHologram() {
    // Update with current status - this will clear and rebuild the content
    updateReadyStatusHologram();
  }

  private void updateReadyStatusHologram() {
    // Clear existing content and rebuild (but keep page 1)
    clearAllPages("ready_status");

    // Set title on the first page (line 1 already exists from clearAllPages)
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(), "dh l set ready_status 1 1 &b&lREADY STATUS CHECKLIST");

    int lineNumber = 2;

    // Check if all hunters have disguises (from all players, not just ready ones)
    Set<UUID> hunterIds =
        allPlayerSelections.entrySet().stream()
            .filter(entry -> entry.getValue() == HuntTeam.HUNTERS)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toSet());

    Set<UUID> disguisedPlayers = disguiseManager.getDisguisedPlayers();
    boolean allHuntersHaveDisguises =
        !hunterIds.isEmpty() && disguisedPlayers.containsAll(hunterIds);

    plugin
        .getLogger()
        .info(
            "Hunter check: hunterIds="
                + hunterIds.size()
                + ", disguisedPlayers="
                + disguisedPlayers.size()
                + ", allHuntersHaveDisguises="
                + allHuntersHaveDisguises
                + ", hunterIds="
                + hunterIds
                + ", disguisedPlayers="
                + disguisedPlayers);

    // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
    String hunterCheck = allHuntersHaveDisguises ? "&a\u2713" : "&c\u2717";
    // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
    // Add line before setting
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add ready_status 1 &7");
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(),
        String.format(
            "dh l set ready_status 1 %d %s &7All Hunters have disguises",
            lineNumber++, hunterCheck));

    // Check if we have at least one hider (from all players, not just ready ones)
    boolean hasHider =
        allPlayerSelections.values().stream().anyMatch(team -> team == HuntTeam.HIDERS);
    // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
    String hiderCheck = hasHider ? "&a\u2713" : "&c\u2717";
    // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
    // Add line before setting
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add ready_status 1 &7");
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(),
        String.format(
            "dh l set ready_status 1 %d %s &7At least one Hider", lineNumber++, hiderCheck));

    // Check if all players with class selections have voted for maps
    long playersWithClassSelections = allPlayerSelections.size();
    long playersWithMapVotes = playerMapVotes.size();
    boolean allPlayersVoted =
        (playersWithClassSelections > 0) && (playersWithMapVotes >= playersWithClassSelections);

    // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
    String mapVoteCheck = allPlayersVoted ? "&a\u2713" : "&c\u2717";
    // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
    // Add line before setting
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add ready_status 1 &7");
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(),
        String.format(
            "dh l set ready_status 1 %d %s &7All players voted for maps (%d/%d)",
            lineNumber++, mapVoteCheck, playersWithMapVotes, playersWithClassSelections));

    // Add empty line for separation
    // Add line before setting
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add ready_status 1 &7");
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(), String.format("dh l set ready_status 1 %d &7", lineNumber++));

    // Count ready players by team
    long readyHunters =
        gameParticipants.entrySet().stream()
            .filter(entry -> entry.getValue() == HuntTeam.HUNTERS)
            .filter(entry -> Boolean.TRUE.equals(playerReadyStatus.get(entry.getKey())))
            .count();

    long totalHunters =
        gameParticipants.values().stream().filter(team -> team == HuntTeam.HUNTERS).count();

    long readyHiders =
        gameParticipants.entrySet().stream()
            .filter(entry -> entry.getValue() == HuntTeam.HIDERS)
            .filter(entry -> Boolean.TRUE.equals(playerReadyStatus.get(entry.getKey())))
            .count();

    long totalHiders =
        gameParticipants.values().stream().filter(team -> team == HuntTeam.HIDERS).count();

    // Show team readiness status
    if (totalHunters > 0) {
      // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
      String hunterReadyCheck = (readyHunters == totalHunters) ? "&a\u2713" : "&e\u2022";
      // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
      // Add line before setting
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add ready_status 1 &7");
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(),
          String.format(
              "dh l set ready_status 1 %d %s &7Hunters Ready: &f%d/%d",
              lineNumber++, hunterReadyCheck, readyHunters, totalHunters));
    }

    if (totalHiders > 0) {
      // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
      String hiderReadyCheck = (readyHiders == totalHiders) ? "&a\u2713" : "&e\u2022";
      // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
      // Add line before setting
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh l add ready_status 1 &7");
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(),
          String.format(
              "dh l set ready_status 1 %d %s &7Hiders Ready: &f%d/%d",
              lineNumber++, hiderReadyCheck, readyHiders, totalHiders));
    }

    // Update start game hologram based on readiness
    updateStartGameHologram();
  }

  private void checkAllPlayersReady() {
    boolean allReady = true;
    int readyCount = 0;

    for (Player player : Bukkit.getOnlinePlayers()) {
      UUID playerId = player.getUniqueId();
      HuntPlayerData data = lobbyManager.getPlayerData(playerId);

      // Check if player has team, class selection, AND map vote
      if (data != null
          && data.getSelectedTeam() != null
          && ((data.getSelectedTeam() == HuntTeam.HUNTERS && data.getSelectedHunterClass() != null)
              || (data.getSelectedTeam() == HuntTeam.HIDERS
                  && data.getSelectedHiderClass() != null))
          && playerMapVotes.containsKey(playerId)) {
        if (!Boolean.TRUE.equals(playerReadyStatus.get(playerId))) {
          allReady = false;
        } else {
          readyCount++;
        }
      }
    }

    if (allReady && readyCount > 0) {
      broadcastMessage("All players are ready! Game can now be started!", NamedTextColor.GREEN);
    }
  }

  private void broadcastMessage(String message, NamedTextColor color) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendMessage(Component.text(message, color));
    }
  }

  /**
   * Checks if the prep phase is currently active.
   *
   * @return true if prep phase is active, false otherwise
   */
  public boolean isPrepPhaseActive() {
    return prepPhaseActive;
  }

  /**
   * Checks if a game is currently starting (in countdown).
   *
   * @return true if game is starting, false otherwise
   */
  public boolean isGameStarting() {
    return gameStarting;
  }

  /**
   * Gets the currently selected map (only valid during/after game start).
   *
   * @return the selected map, or null if no game is running
   */
  public HuntMap getSelectedMap() {
    return selectedMap;
  }

  /**
   * Gets a copy of the current game participants.
   *
   * @return map of player UUIDs to their teams
   */
  public Map<UUID, HuntTeam> getGameParticipants() {
    return new HashMap<>(gameParticipants);
  }

  /**
   * Gets a copy of the current game class selections.
   *
   * @return map of player UUIDs to their class selections
   */
  public Map<UUID, Object> getGameClassSelections() {
    return new HashMap<>(gameClassSelections);
  }

  /**
   * Removes a player from the prep phase when they leave.
   *
   * @param playerId The UUID of the player leaving
   */
  public void removePlayer(UUID playerId) {
    HuntMap previousVote = playerMapVotes.remove(playerId);
    if (previousVote != null) {
      mapVoteCounts.put(previousVote, mapVoteCounts.get(previousVote) - 1);
    }

    playerReadyStatus.remove(playerId);
    gameParticipants.remove(playerId);
    gameClassSelections.remove(playerId);
    allPlayerSelections.remove(playerId);

    if (prepPhaseActive) {
      updateReadyStatusHologram();
    }

    // Remove from game timer if active
    if (gameTimerBar != null) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        gameTimerBar.removePlayer(player);
      }
    }
  }

  /**
   * Updates the ready status hologram when players change their class selections. This should be
   * called from HuntCommand when players join or leave classes.
   *
   * @param playerId The UUID of the player whose class selection changed
   * @param newTeam The player's new team (null if they left)
   * @param newClass The player's new class (null if they left)
   */
  public void updatePlayerClassSelection(UUID playerId, HuntTeam newTeam, Object newClass) {
    // Auto-start prep phase if not active and someone joins a class
    if (!prepPhaseActive && newTeam != null) {
      startPrepPhase();
      return; // startPrepPhase will call updateReadyStatusHologram
    }

    if (!prepPhaseActive) {
      return;
    }

    // Update game participants if they have valid selections
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) {
      // Track all player selections (regardless of other selections - just need team)
      if (newTeam != null) {
        allPlayerSelections.put(playerId, newTeam);
        plugin
            .getLogger()
            .info(
                "Added player "
                    + player.getName()
                    + " to allPlayerSelections as "
                    + newTeam
                    + ". Total in allPlayerSelections: "
                    + allPlayerSelections.size());
      } else {
        allPlayerSelections.remove(playerId);
        plugin
            .getLogger()
            .info(
                "Removed player "
                    + player.getName()
                    + " from allPlayerSelections. Total remaining: "
                    + allPlayerSelections.size());
      }

      // Add to gameParticipants if they have a valid team, class selection, AND map
      // vote
      if (newTeam != null && newClass != null && playerMapVotes.containsKey(playerId)) {
        gameParticipants.put(playerId, newTeam);
        gameClassSelections.put(playerId, newClass);
        plugin
            .getLogger()
            .info(
                "Added player "
                    + player.getName()
                    + " to gameParticipants as "
                    + newTeam
                    + " with class "
                    + newClass
                    + " and map vote "
                    + playerMapVotes.get(playerId)
                    + ". Total participants: "
                    + gameParticipants.size());
      } else {
        // Remove from participants if they left their class or don't have map vote
        gameParticipants.remove(playerId);
        gameClassSelections.remove(playerId);
        playerReadyStatus.remove(playerId); // Also remove ready status if they left
        plugin
            .getLogger()
            .info(
                "Removed player "
                    + player.getName()
                    + " from gameParticipants. Total remaining: "
                    + gameParticipants.size()
                    + " (missing: team="
                    + (newTeam != null)
                    + ", class="
                    + (newClass != null)
                    + ", mapVote="
                    + playerMapVotes.containsKey(playerId)
                    + ")");
      }
    }

    // Update holograms
    updateReadyStatusHologram();
  }

  /**
   * Updates the ready status hologram when players change their disguise selections. This should be
   * called from the disguise system when hunters select or deselect disguises.
   */
  public void updateDisguiseSelections() {
    plugin.getLogger().info("updateDisguiseSelections called, prepPhaseActive: " + prepPhaseActive);
    if (prepPhaseActive) {
      plugin.getLogger().info("Updating ready status hologram due to disguise selection change");
      updateReadyStatusHologram();
    }
  }

  /**
   * Removes a player's map vote.
   *
   * @param player The player whose vote to remove
   */
  public void removeMapVote(Player player) {
    if (!prepPhaseActive || gameStarting) {
      return;
    }

    UUID playerId = player.getUniqueId();
    HuntMap previousVote = playerMapVotes.remove(playerId);

    if (previousVote != null) {
      // Remove vote count
      mapVoteCounts.put(previousVote, mapVoteCounts.get(previousVote) - 1);

      // Remove from game participants since map vote is required
      gameParticipants.remove(playerId);
      gameClassSelections.remove(playerId);
      playerReadyStatus.remove(playerId);

      player.sendMessage(
          Component.text(
              "Removed vote for " + previousVote.getDisplayName() + "!", NamedTextColor.YELLOW));

      plugin
          .getLogger()
          .info("Removed " + player.getName() + " from gameParticipants after removing map vote");

      // Update holograms
      updateReadyStatusHologram();
    } else {
      player.sendMessage(Component.text("You haven't voted for any map!", NamedTextColor.RED));
    }
  }

  /**
   * Teleports all players in hunt worlds back to the lobby location. Used after a game ends and
   * fireworks finish.
   */
  public void teleportAllPlayersToLobby() {
    // Get the lobby location from config
    String worldName = config.getString("lobby.world", "world");
    double x = config.getDouble("lobby.x", 0);
    double y = config.getDouble("lobby.y", 64);
    double z = config.getDouble("lobby.z", 0);
    float yaw = (float) config.getDouble("lobby.yaw", 0);
    float pitch = (float) config.getDouble("lobby.pitch", 0);

    Location lobbyLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);

    // If lobby world doesn't exist, use the server's default world
    if (lobbyLocation.getWorld() == null) {
      lobbyLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
      plugin.getLogger().warning("Lobby world not found. Using default world spawn instead.");
    }

    // Teleport all players in hunt worlds to the lobby
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.getWorld().getName().toLowerCase().contains("hunt")) {
        // Reset game mode to adventure
        player.setGameMode(GameMode.ADVENTURE);

        // Remove any potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
          player.removePotionEffect(effect.getType());
        }

        // Remove disguises if any
        if (DisguiseAPI.isDisguised(player)) {
          DisguiseAPI.undisguiseToAll(player);
        }

        // Reset entity size back to normal if it was changed
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(), String.format("entitysize player %s 1.0", player.getName()));

        // Auto-run hunt command to bring them back to Hunt lobby after a short delay
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  if (player.isOnline()) {
                    player.performCommand("hunt");
                    player.sendMessage(
                        Component.text("Returning you to Hunt lobby...", NamedTextColor.YELLOW));
                  }
                },
                40L); // 2 seconds delay
      }
    }
  }

  /**
   * Sets the death handler for this prep phase manager.
   *
   * @param deathHandler The death handler
   */
  public void setDeathHandler(HuntDeathHandler deathHandler) {
    this.deathHandler = deathHandler;
    plugin.getLogger().info("Death handler set for HuntPrepPhaseManager");
  }

  /** Starts the heartbeat task that plays sounds to hiders when hunters are nearby. */
  private void startHeartbeatTask() {
    // Configuration for heartbeat detection
    double detectionRadius = config.getDouble("hunt.heartbeat.detection-radius", 15.0);
    int heartbeatInterval = config.getInt("hunt.heartbeat.interval-ticks", 40); // 2 seconds

    heartbeatTask =
        new BukkitRunnable() {
          @Override
          public void run() {
            // Create copies to avoid ConcurrentModificationException
            Map<UUID, HuntTeam> participantsCopy = new HashMap<>(gameParticipants);

            for (Map.Entry<UUID, HuntTeam> entry : participantsCopy.entrySet()) {
              UUID playerId = entry.getKey();
              HuntTeam team = entry.getValue();

              // Only check hiders
              if (team != HuntTeam.HIDERS) {
                continue;
              }

              Player hider = Bukkit.getPlayer(playerId);
              if (hider == null || !hider.isOnline()) {
                continue;
              }

              // Check if any hunters are nearby
              boolean hunterNearby = false;
              Location hiderLocation = hider.getLocation();

              for (Map.Entry<UUID, HuntTeam> otherEntry : participantsCopy.entrySet()) {
                UUID otherPlayerId = otherEntry.getKey();
                HuntTeam otherTeam = otherEntry.getValue();

                // Only check hunters
                if (otherTeam != HuntTeam.HUNTERS) {
                  continue;
                }

                Player hunter = Bukkit.getPlayer(otherPlayerId);
                if (hunter == null || !hunter.isOnline()) {
                  continue;
                }

                // Check distance
                Location hunterLocation = hunter.getLocation();
                if (hiderLocation.getWorld().equals(hunterLocation.getWorld())
                    && hiderLocation.distance(hunterLocation) <= detectionRadius) {
                  hunterNearby = true;
                  break;
                }
              }

              // Play heartbeat sound if hunter is nearby
              if (hunterNearby) {
                hider.playSound(hider.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 0.5f);

                // Optional: Send action bar message
                Component heartbeatMessage =
                    Component.text("♥ Hunter nearby ♥", NamedTextColor.RED);
                hider.sendActionBar(heartbeatMessage);
              }
            }
          }
        }.runTaskTimer(plugin, 0L, heartbeatInterval);
  }

  private void removePlayersFromStartGameHologram() {
    // Remove all players from the start game hologram
    for (Player player : Bukkit.getOnlinePlayers()) {
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh removePlayer start_game " + player.getName());
    }
  }

  private void removePlayersFromReadyStatusHologram() {
    // Remove all players from the ready status hologram
    for (Player player : Bukkit.getOnlinePlayers()) {
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh removePlayer ready_status " + player.getName());
    }
  }

  private void removePlayersFromAllHolograms() {
    // Remove all players from map and class holograms
    for (Player player : Bukkit.getOnlinePlayers()) {
      hologramManager.removePlayerFromAllHolograms(player.getUniqueId());
    }
  }
}
