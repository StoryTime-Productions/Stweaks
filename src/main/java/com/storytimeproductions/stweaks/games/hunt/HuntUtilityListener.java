package com.storytimeproductions.stweaks.games.hunt;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Listener that handles the logic for hunter utility item abilities. Each hunter class has a unique
 * utility item with special abilities and cooldowns.
 */
public class HuntUtilityListener implements Listener {

  private final FileConfiguration config;
  private final Map<UUID, Long> resilienceCooldowns;
  private final Map<UUID, Long> dashCooldowns;
  private final Map<UUID, Long> scannerCooldowns;
  private final HuntLobbyManager lobbyManager;

  /**
   * Constructs a new HuntUtilityListener.
   *
   * @param config The hunt configuration
   * @param lobbyManager The lobby manager for checking player states
   */
  public HuntUtilityListener(FileConfiguration config, HuntLobbyManager lobbyManager) {
    this.config = config;
    this.lobbyManager = lobbyManager;
    this.resilienceCooldowns = new HashMap<>();
    this.dashCooldowns = new HashMap<>();
    this.scannerCooldowns = new HashMap<>();
  }

  /**
   * Handles player interactions with utility items.
   *
   * @param event The PlayerInteractEvent
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();

    if (item == null || !item.hasItemMeta()) {
      return;
    }

    // Only handle right-click interactions
    if (!event.getAction().toString().contains("RIGHT_CLICK")) {
      return;
    }

    // Check if player is in hunt world and is a hunter
    boolean inHuntWorld = isPlayerInHuntWorld(player);
    boolean isHunter = isPlayerHunter(player);

    if (!inHuntWorld) {
      // Debug: Player not in hunt world
      org.bukkit.Bukkit.getLogger()
          .info(
              "[DEBUG] "
                  + player.getName()
                  + " tried to use utility in non-hunt world: "
                  + player.getWorld().getName());
      return;
    }

    if (!isHunter) {
      // Debug: Player not a hunter
      org.bukkit.Bukkit.getLogger()
          .info("[DEBUG] " + player.getName() + " tried to use utility but is not a hunter");
      return;
    }

    Material material = item.getType();
    UUID playerId = player.getUniqueId();

    org.bukkit.Bukkit.getLogger()
        .info("[DEBUG] " + player.getName() + " is using utility item: " + material.name());

    switch (material) {
      case TOTEM_OF_UNDYING:
        handleResilienceTotem(player, playerId, event);
        break;
      case FEATHER:
        handleDash(player, playerId, event);
        break;
      case ENDER_EYE:
        handleScanner(player, playerId, event);
        break;
      default:
        break;
    }
  }

  /**
   * Handles the Resilience Totem ability for the Brute class. Provides temporary resistance and
   * regeneration.
   *
   * @param player The player using the ability
   * @param playerId The player's UUID
   * @param event The interaction event
   */
  private void handleResilienceTotem(Player player, UUID playerId, PlayerInteractEvent event) {
    int cooldownSeconds = config.getInt("hunt.utility-cooldowns.resilience-totem", 30);

    if (isOnCooldown(playerId, resilienceCooldowns, cooldownSeconds)) {
      event.setCancelled(true);
      return;
    }

    // Apply resilience effects
    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1)); // 10s
    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1)); // 5s

    // Visual and audio effects
    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 20);
    player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

    // Set cooldown
    resilienceCooldowns.put(playerId, System.currentTimeMillis());
    event.setCancelled(true);
  }

  /**
   * Handles the Dash ability for the Nimble class. Provides a quick speed boost.
   *
   * @param player The player using the ability
   * @param playerId The player's UUID
   * @param event The interaction event
   */
  private void handleDash(Player player, UUID playerId, PlayerInteractEvent event) {
    int cooldownSeconds = config.getInt("hunt.utility-cooldowns.dash", 15);

    if (isOnCooldown(playerId, dashCooldowns, cooldownSeconds)) {
      event.setCancelled(true);
      return;
    }

    // Apply dash effect
    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 3)); // 3s of Speed IV

    // Visual and audio effects
    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.5, 0.1, 0.5, 0.1);
    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

    // Set cooldown
    dashCooldowns.put(playerId, System.currentTimeMillis());

    event.setCancelled(true);
  }

  /**
   * Handles the Scanner ability for the Saboteur class. Reveals nearby hiders temporarily.
   *
   * @param player The player using the ability
   * @param playerId The player's UUID
   * @param event The interaction event
   */
  private void handleScanner(Player player, UUID playerId, PlayerInteractEvent event) {
    int cooldownSeconds = config.getInt("hunt.utility-cooldowns.scanner", 25);

    if (isOnCooldown(playerId, scannerCooldowns, cooldownSeconds)) {
      event.setCancelled(true);
      return;
    }

    // Scan for nearby players (hiders) within 20 blocks
    int scanRadius = 20;

    for (Player nearbyPlayer : player.getWorld().getPlayers()) {
      if (!nearbyPlayer.equals(player)
          && nearbyPlayer.getLocation().distance(player.getLocation()) <= scanRadius) {
        // Check if the nearby player is a hider
        if (isPlayerHider(nearbyPlayer)) {
          // Give the hider a glowing effect for 5 seconds
          nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
        }
      }
    }

    // Visual and audio effects
    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 30, 3, 3, 3, 0.1);
    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

    // Set cooldown
    scannerCooldowns.put(playerId, System.currentTimeMillis());

    event.setCancelled(true);
  }

  /**
   * Checks if a player is on cooldown for a specific ability.
   *
   * @param playerId The player's UUID
   * @param cooldownMap The cooldown map for the ability
   * @param cooldownSeconds The cooldown duration in seconds
   * @return true if the player is on cooldown, false otherwise
   */
  private boolean isOnCooldown(UUID playerId, Map<UUID, Long> cooldownMap, int cooldownSeconds) {
    Long lastUsed = cooldownMap.get(playerId);
    if (lastUsed == null) {
      return false;
    }
    long currentTime = System.currentTimeMillis();
    long cooldownMillis = cooldownSeconds * 1000L;
    return (currentTime - lastUsed) < cooldownMillis;
  }

  /**
   * Checks if a player is in the hunt world.
   *
   * @param player The player to check
   * @return true if the player is in the hunt world, false otherwise
   */
  private boolean isPlayerInHuntWorld(Player player) {
    String huntWorldName = config.getString("hunt.world", "hunt");
    String playerWorldName = player.getWorld().getName();

    // Check if it matches the configured world exactly
    if (playerWorldName.equals(huntWorldName)) {
      return true;
    }

    // Check if it's any hunt world by looking for "hunt" in the name (case
    // insensitive)
    if (playerWorldName.toLowerCase().contains("hunt")) {
      return true;
    }

    return false;
  }

  /**
   * Checks if a player is a hunter (has selected a hunter class).
   *
   * @param player The player to check
   * @return true if the player is a hunter, false otherwise
   */
  private boolean isPlayerHunter(Player player) {
    // Don't allow spectators to use abilities
    if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
      org.bukkit.Bukkit.getLogger()
          .info("[DEBUG] " + player.getName() + " is spectator, not hunter");
      return false;
    }

    HuntPlayerData playerData = lobbyManager.getPlayerData(player.getUniqueId());

    if (playerData == null) {
      org.bukkit.Bukkit.getLogger().info("[DEBUG] " + player.getName() + " has no player data");
      return false;
    }

    HuntTeam selectedTeam = playerData.getSelectedTeam();
    HunterClass hunterClass = playerData.getSelectedHunterClass();

    if (selectedTeam != HuntTeam.HUNTERS) {
      org.bukkit.Bukkit.getLogger()
          .info(
              "[DEBUG] "
                  + player.getName()
                  + " is not on hunters team: "
                  + (selectedTeam != null ? selectedTeam.name() : "null"));
      return false;
    }

    boolean hasHunterClass = hunterClass != null;
    org.bukkit.Bukkit.getLogger()
        .info(
            "[DEBUG] "
                + player.getName()
                + " hunter class: "
                + (hunterClass != null ? hunterClass.name() : "null"));
    return hasHunterClass;
  }

  /**
   * Checks if a player is a hider (has selected a hider class).
   *
   * @param player The player to check
   * @return true if the player is a hider, false otherwise
   */
  private boolean isPlayerHider(Player player) {
    HuntPlayerData playerData = lobbyManager.getPlayerData(player.getUniqueId());
    if (playerData == null || playerData.getSelectedTeam() != HuntTeam.HIDERS) {
      return false;
    }
    return playerData.getSelectedHiderClass() != null;
  }

  /**
   * Clears all cooldowns for a specific player. This should be called when a player disconnects or
   * leaves the hunt.
   *
   * @param playerId The UUID of the player
   */
  public void clearPlayerCooldowns(UUID playerId) {
    resilienceCooldowns.remove(playerId);
    dashCooldowns.remove(playerId);
    scannerCooldowns.remove(playerId);
  }

  /** Clears all cooldowns for all players. This should be called during server shutdown. */
  public void clearAllCooldowns() {
    resilienceCooldowns.clear();
    dashCooldowns.clear();
    scannerCooldowns.clear();
  }
}
