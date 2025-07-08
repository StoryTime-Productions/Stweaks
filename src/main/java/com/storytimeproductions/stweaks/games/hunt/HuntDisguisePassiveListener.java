package com.storytimeproductions.stweaks.games.hunt;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/** New implementation of passive abilities for hunter disguises with advanced mechanics. */
public class HuntDisguisePassiveListener implements Listener {

  private final JavaPlugin plugin;
  private final HuntDisguiseManager disguiseManager;
  private final HuntLobbyManager lobbyManager;
  private final Map<UUID, Location> lastPlayerLocations;
  private final Map<UUID, Long> playerStillTimes;
  private final Map<UUID, BukkitTask> passiveEffectTasks;

  // New tracking maps for advanced passive abilities
  private final Map<UUID, Entity> scarecrowClones; // Track Scarecrow clones
  private final Map<UUID, Map<UUID, Long>>
      slendermanLookTimes; // Track how long hiders look at Slenderman
  private final Map<UUID, Long> jigsawVanishTimes; // Track Jigsaw vanish start times
  private final Map<UUID, BukkitTask> springtrapAuraTasks; // Track Springtrap aura tasks
  private final Map<UUID, BukkitTask> herobrineGlowTasks; // Track Herobrine glow tasks
  private final Map<UUID, BukkitTask>
      slendermanParanoiaBarTasks; // Track Slenderman paranoia bar countdown tasks
  private final Map<UUID, Map<UUID, Long>>
      slendermanParanoiaCooldowns; // Track when hiders can be affected by paranoia
  // again

  // Permanent hotbar tracking for idle-based abilities
  private final Map<UUID, BossBar> permanentIdleHotbars; // Track permanent idle progress bars

  // Hider title tracking for "RUN" warnings
  private final Map<UUID, BukkitTask> hiderRunTitleTasks; // Track "RUN" title countdown tasks
  private final Map<UUID, Long> hiderSeenCooldowns; // Track cooldown for hiders being seen

  // Ability cooldown tracking
  private final Map<UUID, Long> abilityCooldowns; // Track when abilities can be used again

  // Track which players have active abilities to prevent multiple simultaneous
  // passives
  private final Map<UUID, Boolean> activeAbilities; // Track if a player has an active ability

  // Track abilities that are fully charged and ready to use
  private final Map<UUID, Boolean>
      chargedAbilities; // Track if a player has a charged ability ready

  // Slenderman paranoia task
  private BukkitTask slendermanParanoiaTask;

  // Global tick-based update task for all hunters
  private BukkitTask globalTickTask;

  /**
   * Constructs a new HuntDisguisePassiveListenerNew.
   *
   * @param plugin The JavaPlugin instance
   * @param disguiseManager The disguise manager to check player disguises
   * @param lobbyManager The lobby manager to check if players are hunters
   */
  public HuntDisguisePassiveListener(
      JavaPlugin plugin, HuntDisguiseManager disguiseManager, HuntLobbyManager lobbyManager) {
    this.plugin = plugin;
    this.disguiseManager = disguiseManager;
    this.lobbyManager = lobbyManager;
    this.lastPlayerLocations = new HashMap<>();
    this.playerStillTimes = new HashMap<>();
    this.passiveEffectTasks = new HashMap<>();
    this.scarecrowClones = new HashMap<>();
    this.slendermanLookTimes = new HashMap<>();
    this.jigsawVanishTimes = new HashMap<>();
    this.springtrapAuraTasks = new HashMap<>();
    this.herobrineGlowTasks = new HashMap<>();
    this.slendermanParanoiaBarTasks = new HashMap<>();
    this.slendermanParanoiaCooldowns = new HashMap<>();
    this.permanentIdleHotbars = new HashMap<>();
    this.hiderRunTitleTasks = new HashMap<>();
    this.hiderSeenCooldowns = new HashMap<>();
    this.abilityCooldowns = new HashMap<>();
    this.activeAbilities = new HashMap<>();
    this.chargedAbilities = new HashMap<>();

    // Start Slenderman paranoia detection task (runs independently every second)
    startSlendermanParanoiaTask();

    // Start global tick-based update system for all hunters
    startGlobalTickTask();
  }

  /**
   * Handles player movement to track location changes and clean up non-hunters. Most logic is now
   * handled by the global tick task for better performance.
   *
   * @param event The PlayerMoveEvent
   */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    // Log world info for debugging
    String worldName = player.getWorld().getName();
    if (worldName.toLowerCase().contains("hunt")) {
      plugin.getLogger().fine("Player " + player.getName() + " moved in hunt world: " + worldName);
    }

    // Only handle hunters with disguises
    boolean isHunter = isDisguisedHunter(player);
    if (!isHunter) {
      // Clean up tracking data if player is not a disguised hunter
      if (lastPlayerLocations.containsKey(playerId)) {
        plugin
            .getLogger()
            .info("Removing passive effects from " + player.getName() + " in world " + worldName);
        removePassiveEffects(player);
        removePermanentIdleHotbar(player); // Only remove when no longer a hunter
        lastPlayerLocations.remove(playerId);
        playerStillTimes.remove(playerId);
        abilityCooldowns.remove(playerId);
        activeAbilities.remove(playerId);
        chargedAbilities.remove(playerId);
        cancelPassiveTask(playerId);
      }
      return;
    }

    // Log when we find a valid hunter with disguise
    if (worldName.toLowerCase().contains("hunt")) {
      plugin
          .getLogger()
          .info("Found disguised hunter " + player.getName() + " in hunt world: " + worldName);
    }

    // Simply update the last location - the global tick task handles the rest
    Location currentLocation = player.getLocation();
    lastPlayerLocations.put(playerId, currentLocation.clone());
  }

  /**
   * Applies passive abilities based on the disguise type and how long the player has been still.
   */
  private void applyPassiveAbility(
      Player player, HunterDisguiseType disguiseType, long stillDuration) {
    UUID playerId = player.getUniqueId();

    // Check if player already has an active ability to prevent multiple
    // simultaneous passives
    if (activeAbilities.getOrDefault(playerId, false)) {
      return;
    }

    switch (disguiseType) {
      case SPRINGTRAP:
        // Vengeful Echo: Slight slowness to hiders in a 5-block radius
        if (stillDuration >= 1) {
          applySpringtrapVengefulEcho(player);
        }
        break;

      case HEROBRINE:
        // Glitch Sight: Brief glowing effect on nearest hider within 10 blocks
        if (stillDuration >= 2) {
          applyHerobrineGlitchSight(player);
        }
        break;

      case SLENDERMAN:
        // Paranoia Aura: Handled in movement detection
        // No standing still requirement
        break;

      case CRYPTID:
        // Shadow Phase: Moves silently and leaves no footstep particles
        // Plus conditional long-range teleport when ability is charged and manually
        // activated
        if (stillDuration >= 3) {
          applyCryptidShadowPhase(player);
        }
        break;

      case SCARECROW:
        // Field of Fear: Create clone when standing still for 4+ seconds
        if (stillDuration >= 4) {
          applyScarecrowFieldOfFear(player);
        }
        break;

      case JIGSAW:
        // Puzzle Field: Vanish for 5 seconds when standing still
        if (stillDuration >= 3) {
          applyJigsawPuzzleField(player);
        }
        break;

      default:
        break;
    }

    // Note: Boss bar management is now handled in the global tick system
  }

  /** Springtrap - Vengeful Echo: Apply slowness to hiders in 5-block radius. */
  private void applySpringtrapVengefulEcho(Player springtrap) {
    UUID springtrapId = springtrap.getUniqueId();

    // Cancel existing task if any
    BukkitTask existingTask = springtrapAuraTasks.get(springtrapId);
    if (existingTask != null) {
      return; // Already running
    }

    // Mark player as having an active ability
    activeAbilities.put(springtrapId, true);

    BukkitTask auraTask =
        new BukkitRunnable() {
          @Override
          public void run() {
            if (!springtrap.isOnline() || !isDisguisedHunter(springtrap)) {
              springtrapAuraTasks.remove(springtrapId);
              activeAbilities.remove(springtrapId); // Clear active ability flag
              cancel();
              return;
            }

            boolean affectedAny = false;
            for (Player nearbyPlayer : springtrap.getWorld().getPlayers()) {
              if (!nearbyPlayer.equals(springtrap) && isPlayerHider(nearbyPlayer)) {
                double distance = nearbyPlayer.getLocation().distance(springtrap.getLocation());
                if (distance <= 5.0) {
                  nearbyPlayer.addPotionEffect(
                      new PotionEffect(PotionEffectType.SLOWNESS, 60, 0)); // 3
                  // seconds

                  // Play hunter screech sound when effect is applied to hider
                  disguiseManager.playCharacterScreech(springtrap);

                  affectedAny = true;
                }
              }
            }
            if (affectedAny) {
              // Show radius particle effect when ability activates
              showRadiusParticleEffect(springtrap, 5.0);

              // Use the permanent idle hotbar for ability duration display
              showAbilityUseDuration(springtrap, HunterDisguiseType.SPRINGTRAP, 60);
              triggerAbilityUseEffect(springtrap, HunterDisguiseType.SPRINGTRAP);

              // Cancel this task since the ability duration is now being managed
              springtrapAuraTasks.remove(springtrapId);
              // Note: activeAbilities flag will be cleared by showAbilityUseDuration when the
              // ability ends
              cancel();
            }
          }
        }.runTaskTimer(plugin, 0L, 40L); // Every 2 seconds

    springtrapAuraTasks.put(springtrapId, auraTask);
  }

  /** Herobrine - Glitch Sight: Brief glowing effect on nearest hider within 10 blocks. */
  private void applyHerobrineGlitchSight(Player herobrine) {
    UUID herobrineId = herobrine.getUniqueId();

    // Cancel existing task if any
    BukkitTask existingTask = herobrineGlowTasks.get(herobrineId);
    if (existingTask != null) {
      return; // Already running
    }

    // Mark player as having an active ability
    activeAbilities.put(herobrineId, true);

    BukkitTask glowTask =
        new BukkitRunnable() {
          @Override
          public void run() {
            if (!herobrine.isOnline() || !isDisguisedHunter(herobrine)) {
              herobrineGlowTasks.remove(herobrineId);
              activeAbilities.remove(herobrineId); // Clear active ability flag
              cancel();
              return;
            }

            Player nearestHider = null;
            double nearestDistance = 10.0;

            for (Player nearbyPlayer : herobrine.getWorld().getPlayers()) {
              if (!nearbyPlayer.equals(herobrine) && isPlayerHider(nearbyPlayer)) {
                double distance = nearbyPlayer.getLocation().distance(herobrine.getLocation());
                if (distance <= 10.0 && distance < nearestDistance) {
                  nearestHider = nearbyPlayer;
                  nearestDistance = distance;
                }
              }
            }
            if (nearestHider != null) {
              // Show radius particle effect when ability activates
              showRadiusParticleEffect(herobrine, 10.0);

              // Apply glowing effect visible only to Herobrine
              nearestHider.addPotionEffect(
                  new PotionEffect(PotionEffectType.GLOWING, 100, 0)); // 5 seconds

              // Play hunter screech sound when effect is applied to hider
              disguiseManager.playCharacterScreech(herobrine);

              // Use the permanent idle hotbar for ability duration display
              showAbilityUseDuration(herobrine, HunterDisguiseType.HEROBRINE, 100);
              triggerAbilityUseEffect(herobrine, HunterDisguiseType.HEROBRINE);

              // Cancel this task since the ability duration is now being managed
              herobrineGlowTasks.remove(herobrineId);
              // Note: activeAbilities flag will be cleared by showAbilityUseDuration when the
              // ability ends
              cancel();
            }
          }
        }.runTaskTimer(plugin, 0L, 120L); // Every 6 seconds

    herobrineGlowTasks.put(herobrineId, glowTask);
  }

  /** Slenderman - Paranoia Aura: Detect when Slenderman looks at hiders. */
  private void startSlendermanParanoiaTask() {
    slendermanParanoiaTask =
        new BukkitRunnable() {
          @Override
          public void run() {
            handleSlendermanParanoia();
          }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
  }

  /** Handles Slenderman paranoia detection - Slenderman looking at hiders causes effects. */
  private void handleSlendermanParanoia() {
    long currentTime = System.currentTimeMillis();

    for (Player slenderman : Bukkit.getOnlinePlayers()) {
      if (!isDisguisedHunter(slenderman)) {
        continue;
      }

      HunterDisguiseType disguiseType = getPlayerDisguiseType(slenderman);
      if (disguiseType != HunterDisguiseType.SLENDERMAN) {
        continue;
      }

      UUID slendermanId = slenderman.getUniqueId();
      Map<UUID, Long> lookTimes =
          slendermanLookTimes.computeIfAbsent(slendermanId, k -> new HashMap<>());
      Map<UUID, Long> paranoiaCooldowns =
          slendermanParanoiaCooldowns.computeIfAbsent(slendermanId, k -> new HashMap<>());

      // Clean up expired cooldowns to prevent memory leaks
      paranoiaCooldowns.entrySet().removeIf(entry -> currentTime >= entry.getValue());

      for (Player hider : slenderman.getWorld().getPlayers()) {
        if (!hider.equals(slenderman) && isPlayerHider(hider)) {
          // Check if SLENDERMAN is looking at the HIDER (reversed logic)
          if (isPlayerLookingAt(slenderman, hider)) {
            UUID hiderId = hider.getUniqueId();

            // Check if hider is still on cooldown from a recent paranoia effect
            Long cooldownEnd = paranoiaCooldowns.get(hiderId);
            if (cooldownEnd != null && currentTime < cooldownEnd) {
              // Hider is still affected by paranoia, skip this hider
              continue;
            }

            // Check if Slenderman is currently showing a paranoia effect (bar is active)
            BukkitTask activeParanoiaBarTask = slendermanParanoiaBarTasks.get(slendermanId);
            if (activeParanoiaBarTask != null && !activeParanoiaBarTask.isCancelled()) {
              // Paranoia effect is already active, don't trigger again
              continue;
            }

            if (!lookTimes.containsKey(hiderId)) {
              lookTimes.put(hiderId, currentTime);
            } else {
              long lookDuration = currentTime - lookTimes.get(hiderId);
              if (lookDuration >= 1000) { // 1 second of Slenderman staring at hider
                // Apply paranoia effects to the hider
                hider.addPotionEffect(
                    new PotionEffect(PotionEffectType.NAUSEA, 100, 0)); // 5 seconds

                // Play hunter screech sound when effect is applied to hider
                disguiseManager.playCharacterScreech(slenderman);

                hider.playSound(hider.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.0f);

                // Set cooldown for this hider (5 seconds to match effect duration)
                paranoiaCooldowns.put(hiderId, currentTime + 5000L);

                // Show "RUN" title to the hider
                showRunTitleToHider(hider, slenderman);

                // Fill Slenderman's boss bar on successful paranoia effect
                fillSlendermanParanoiaBar(slenderman);

                lookTimes.remove(hiderId); // Reset to prevent spam

                // Only affect one hider per trigger to prevent multiple simultaneous effects
                break;
              }
            }
          } else {
            lookTimes.remove(hider.getUniqueId());
          }
        }
      }
    }
  }

  /**
   * Fills Slenderman's boss bar when a paranoia effect is successfully applied to a hider and then
   * smoothly decreases it over the paranoia effect duration.
   */
  private void fillSlendermanParanoiaBar(Player slenderman) {
    UUID slendermanId = slenderman.getUniqueId();
    BossBar hotbar = permanentIdleHotbars.get(slendermanId);

    if (hotbar != null) {
      // Cancel any existing paranoia bar task to prevent overlap
      BukkitTask existingTask = slendermanParanoiaBarTasks.get(slendermanId);
      if (existingTask != null && !existingTask.isCancelled()) {
        existingTask.cancel();
      }

      // Fill the bar completely and set static title to prevent flickering
      hotbar.setProgress(1.0);
      hotbar.setColor(BarColor.PURPLE);
      hotbar.setTitle("Paranoia Aura: Active");

      // Start smooth countdown over 5 seconds (100 ticks) to match paranoia effect
      // duration
      final int totalTicks = 100; // 5 seconds in ticks
      final int updateInterval = 2; // Update every 2 ticks for smooth animation

      BukkitTask countdownTask =
          new BukkitRunnable() {
            private int ticksElapsed = 0;

            @Override
            public void run() {
              if (hotbar == null || !permanentIdleHotbars.containsKey(slendermanId)) {
                slendermanParanoiaBarTasks.remove(slendermanId);
                this.cancel();
                return;
              }

              ticksElapsed += updateInterval;

              // Calculate progress (starts at 1.0, decreases to 0.0)
              double progress = Math.max(0.0, 1.0 - ((double) ticksElapsed / totalTicks));
              hotbar.setProgress(progress);

              // Only change title when effect is completely finished
              if (progress <= 0.0) {
                // Effect finished - reset to idle state
                hotbar.setProgress(0.0);
                hotbar.setColor(BarColor.BLUE);
                hotbar.setTitle("Paranoia Aura");
                slendermanParanoiaBarTasks.remove(slendermanId);
                this.cancel();
              }
            }
          }.runTaskTimer(plugin, updateInterval, updateInterval);

      // Store the task for potential cancellation
      slendermanParanoiaBarTasks.put(slendermanId, countdownTask);
    }
  }

  /** Cryptid - Shadow Phase: Silent movement and conditional teleport. */
  private void handleCryptidSilentMovement(Player cryptid) {
    HunterDisguiseType disguiseType = getPlayerDisguiseType(cryptid);
    if (disguiseType == HunterDisguiseType.CRYPTID) {
      // Apply silent movement (no particles, reduced sound)
      cryptid.addPotionEffect(
          new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false)); // Silent speed
    }
  }

  private void applyCryptidShadowPhase(Player cryptid) {
    // Check for nearby hiders and play screech if any are found within 5 blocks
    playScreechIfHidersNearby(cryptid, 5.0);

    // Long-range teleport when ability is activated
    Location currentLoc = cryptid.getLocation();
    Vector direction =
        currentLoc.getDirection().multiply(7); // Increased from 3 to 7 blocks forward
    Location teleportLoc = currentLoc.clone().add(direction);

    // Ensure safe teleport location
    if (teleportLoc.getBlock().getType().isSolid()) {
      teleportLoc = teleportLoc.add(0, 1, 0);
    }

    // Additional safety check - make sure we don't teleport into another solid
    // block
    for (int i = 0; i < 3; i++) {
      if (teleportLoc.clone().add(0, i, 0).getBlock().getType().isSolid()) {
        teleportLoc = teleportLoc.add(0, 1, 0);
      } else {
        break;
      }
    }

    cryptid.teleport(teleportLoc);
    cryptid.playSound(cryptid.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
    // Use the permanent idle hotbar for ability duration display
    showAbilityUseDuration(cryptid, HunterDisguiseType.CRYPTID, 100);
    triggerAbilityUseEffect(cryptid, HunterDisguiseType.CRYPTID);
  }

  /** Scarecrow - Field of Fear: Create clone that detects nearby hiders. */
  private void applyScarecrowFieldOfFear(Player scarecrow) {
    UUID scarecrowId = scarecrow.getUniqueId();

    // Remove existing clone if any
    Entity existingClone = scarecrowClones.get(scarecrowId);
    if (existingClone != null && !existingClone.isDead()) {
      return; // Clone already exists
    }

    // Show radius particle effect when ability activates (10-block detection range)
    showRadiusParticleEffect(scarecrow, 10.0);

    // Create creeper clone disguised as scarecrow
    Location cloneLocation = scarecrow.getLocation().clone();
    Creeper clone = (Creeper) scarecrow.getWorld().spawnEntity(cloneLocation, EntityType.CREEPER);
    clone.setAI(false);
    clone.setInvulnerable(true);
    clone.setPowered(false);
    clone.setCustomNameVisible(false); // Hide nametag
    clone.setSilent(true); // Make clone silent

    // Disguise the creeper with the scarecrow's disguise skin
    try {
      String scarecrowSkin = disguiseManager.getPlayerDisguiseSkin(scarecrowId);
      if (scarecrowSkin != null) {
        // Use the skin name directly from the disguise manager
        PlayerDisguise disguise = new PlayerDisguise(scarecrowSkin);
        disguise.setReplaceSounds(false);
        disguise.setModifyBoundingBox(false);
        disguise.setNameVisible(false); // Hide disguise name

        // Set the clone's scale to match the scarecrow's scale from config
        try {
          // Get scale from hunt config (same as used for players)
          double scarecrowScale = 1.3; // Default scale
          try {
            // Try to access the config through the plugin
            if (plugin.getConfig().contains("hunt.disguise.player-scale")) {
              scarecrowScale = plugin.getConfig().getDouble("hunt.disguise.player-scale", 1.3);
            }
          } catch (Exception configEx) {
            System.out.println(
                "[HuntPassive] Could not read scale from config, using default: "
                    + configEx.getMessage());
          }

          disguise.getWatcher().setScale(scarecrowScale);
        } catch (Exception scaleEx) {
          System.out.println("[HuntPassive] Failed to set clone scale: " + scaleEx.getMessage());
        }

        // Hide armor on clone (same as original player)
        try {
          disguise
              .getWatcher()
              .setArmor(new org.bukkit.inventory.ItemStack[4]); // Empty armor array
          disguise
              .getWatcher()
              .setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
          disguise
              .getWatcher()
              .setItemInOffHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
        } catch (Exception armorEx) {
          System.out.println("[HuntPassive] Failed to hide clone armor: " + armorEx.getMessage());
        }

        DisguiseAPI.disguiseEntity(clone, disguise);
      } else {
        System.out.println("[HuntPassive] No disguise skin found for scarecrow player");
      }
    } catch (Exception e) {
      System.out.println("[HuntPassive] Failed to disguise scarecrow clone: " + e.getMessage());
    }

    scarecrowClones.put(scarecrowId, clone);

    // Schedule clone monitoring and removal
    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        ticks++;

        if (clone.isDead() || ticks >= 200) { // 10 seconds
          if (!clone.isDead()) {
            clone.remove();
          }
          scarecrowClones.remove(scarecrowId);
          cancel();
          return;
        }

        // Check for nearby hiders and move towards them every 5 ticks (0.25 seconds)
        if (ticks % 5 == 0) {
          Player nearestHider = null;
          double nearestDistance = Double.MAX_VALUE; // Initialize to max value
          final double detectionRange = 10.0; // Search within 10 blocks

          // Debug: List all players and their status
          for (Player nearbyPlayer : clone.getWorld().getPlayers()) {
            double distance = nearbyPlayer.getLocation().distance(clone.getLocation());
            boolean isHider = isPlayerHider(nearbyPlayer);

            if (isHider) {
              if (distance <= detectionRange && distance < nearestDistance) {
                nearestHider = nearbyPlayer;
                nearestDistance = distance;
              }
            }
          }

          // Move clone towards nearest hider
          if (nearestHider != null) {

            // Safety check - ensure clone is still valid
            if (clone.isDead()) {
              scarecrowClones.remove(scarecrowId);
              cancel();
              return;
            }

            Location cloneLocation = clone.getLocation();
            Location hiderLocation = nearestHider.getLocation();

            // Calculate direction vector (only X and Z, keep Y stable)
            Vector direction = hiderLocation.toVector().subtract(cloneLocation.toVector());
            direction.setY(0); // Remove Y component to prevent flying

            // Safety check for zero vector
            if (direction.lengthSquared() == 0) {
              return;
            }

            direction = direction.normalize();

            // Move towards hider (0.3 blocks per movement = 6 blocks per second)
            Vector movement = direction.multiply(0.3);
            Location newLocation = cloneLocation.add(movement);

            // Keep clone at roughly the same Y level as before, with minor ground
            // adjustment
            double targetY = cloneLocation.getY();

            // Only adjust Y if the block at the new location is solid
            if (newLocation.getBlock().getType().isSolid()) {
              targetY = newLocation.getY() + 1; // Move up one block if there's a solid block
            } else if (newLocation.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
              // If there's air below, lower the clone slightly (max 2 blocks down)
              for (int i = 1; i <= 2; i++) {
                Location checkLocation = newLocation.clone().subtract(0, i, 0);
                if (!checkLocation.getBlock().getType().isAir()) {
                  targetY = checkLocation.getY() + 1;
                  break;
                }
              }
            }

            newLocation.setY(targetY);

            // Safety check - make sure new location is in the same world
            if (newLocation.getWorld().equals(cloneLocation.getWorld())) {
              try {
                // Calculate yaw and pitch to look at the hider
                Vector lookDirection =
                    hiderLocation.toVector().subtract(newLocation.toVector()).normalize();
                float yaw =
                    (float) Math.toDegrees(Math.atan2(-lookDirection.getX(), lookDirection.getZ()));
                float pitch = (float) Math.toDegrees(Math.asin(-lookDirection.getY()));

                // Set the clone's rotation to face the hider
                newLocation.setYaw(yaw);
                newLocation.setPitch(pitch);

                clone.teleport(newLocation);

                // Check if clone is now very close to the hider (within 1.5 blocks)
                double newDistance = newLocation.distance(hiderLocation);
                if (newDistance <= 1.5) {

                  // Play dramatic sound and apply effects
                  clone
                      .getWorld()
                      .playSound(clone.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 2.0f, 0.5f);

                  // Play faint explosion sound when clone catches hider
                  nearestHider
                      .getWorld()
                      .playSound(
                          nearestHider.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 0.8f);

                  nearestHider.addPotionEffect(
                      new PotionEffect(PotionEffectType.GLOWING, 100, 0)); // 5 seconds

                  // Play character-specific screech for the scarecrow
                  disguiseManager.playCharacterScreech(scarecrow);

                  // Flash-glitch "FEAR ME" title effect
                  showFearMeTitleFlash(nearestHider);

                  // Remove the clone
                  clone.remove();
                  scarecrowClones.remove(scarecrowId);
                  cancel();
                  return;
                }
              } catch (Exception e) {
                System.out.println("[HuntPassive] Failed to teleport clone: " + e.getMessage());
              }
            }
          } else {
            System.out.println(
                "[HuntPassive] No valid hiders found within " + detectionRange + " blocks");
          }
        }

        // Check for detection every second
        if (ticks % 20 == 0) {
          for (Player nearbyPlayer : clone.getWorld().getPlayers()) {
            if (isPlayerHider(nearbyPlayer)) {
              double distance = nearbyPlayer.getLocation().distance(clone.getLocation());
              if (distance <= 2.0) {
                // Play screech and highlight hider
                clone
                    .getWorld()
                    .playSound(clone.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 2.0f, 0.5f);
                nearbyPlayer.addPotionEffect(
                    new PotionEffect(PotionEffectType.GLOWING, 60, 0)); // 3
                // seconds

                // Play hunter screech sound when effect is applied to hider
                disguiseManager.playCharacterScreech(scarecrow);
              }
            }
          }
        }
      }
    }.runTaskTimer(plugin, 0L, 1L);

    // Use the permanent idle hotbar for ability duration display
    showAbilityUseDuration(scarecrow, HunterDisguiseType.SCARECROW, 200);
    triggerAbilityUseEffect(scarecrow, HunterDisguiseType.SCARECROW);
  }

  /** Jigsaw - Puzzle Field: Vanish for 5 seconds. */
  private void applyJigsawPuzzleField(Player jigsaw) {
    UUID jigsawId = jigsaw.getUniqueId();

    // Check if already vanished
    if (jigsawVanishTimes.containsKey(jigsawId)) {
      return;
    }

    jigsawVanishTimes.put(jigsawId, System.currentTimeMillis());

    // Check for nearby hiders and play screech if any are found within 5 blocks
    playScreechIfHidersNearby(jigsaw, 5.0);

    // Apply invisibility
    jigsaw.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0)); // 5 seconds
    jigsaw.playSound(jigsaw.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.1f);

    // Use the permanent idle hotbar for ability duration display
    showAbilityUseDuration(jigsaw, HunterDisguiseType.JIGSAW, 100);
    triggerAbilityUseEffect(jigsaw, HunterDisguiseType.JIGSAW);

    // Schedule removal of vanish state
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              jigsawVanishTimes.remove(jigsawId);
              jigsaw.playSound(jigsaw.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
            },
            100L);
  }

  /**
   * Applies armor hiding to hunters to maintain disguise immersion while keeping items in
   * inventory.
   */
  private void applyArmorHiding(Player player) {
    try {
      // Check if player has an active disguise
      if (DisguiseAPI.isDisguised(player)) {
        PlayerDisguise disguise = (PlayerDisguise) DisguiseAPI.getDisguise(player);
        if (disguise != null) {
          // Hide all armor visually while keeping it in inventory
          disguise
              .getWatcher()
              .setArmor(new org.bukkit.inventory.ItemStack[4]); // Empty armor slots
          disguise
              .getWatcher()
              .setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
          disguise
              .getWatcher()
              .setItemInOffHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
        }
      }
    } catch (Exception e) {
      System.out.println(
          "[HuntPassive] Failed to hide armor for " + player.getName() + ": " + e.getMessage());
    }
  }

  /** Uses the permanent idle hotbar to show ability duration (consumption). */
  private void showAbilityUseDuration(
      Player player, HunterDisguiseType disguiseType, int durationTicks) {
    UUID playerId = player.getUniqueId();
    BossBar hotbar = permanentIdleHotbars.get(playerId);

    // Mark player as having an active ability
    activeAbilities.put(playerId, true);

    if (hotbar != null) {
      // Change title to show ability is active
      String abilityName = getIdleAbilityName(disguiseType);
      hotbar.setTitle(abilityName + " Active");
      hotbar.setColor(BarColor.PURPLE);
      hotbar.setProgress(1.0); // Start at full

      // Create countdown task for ability duration
      new BukkitRunnable() {
        int remainingTicks = durationTicks;

        @Override
        public void run() {
          // Get fresh reference to the hotbar each tick to ensure we have the current one
          BossBar currentHotbar = permanentIdleHotbars.get(playerId);

          if (remainingTicks <= 0 || !player.isOnline() || currentHotbar == null) {
            // Ability finished - immediately reset to charge mode (KEEP BAR VISIBLE)
            if (currentHotbar != null && permanentIdleHotbars.containsKey(playerId)) {
              currentHotbar.setTitle(abilityName + " Charge");
              currentHotbar.setColor(BarColor.BLUE);
              currentHotbar.setProgress(0.0); // Reset to empty for next charge
              // Clear active ability flag and charged ability flag
              activeAbilities.remove(playerId);
              chargedAbilities.remove(playerId);
            }
            cancel();
            return;
          }

          // Update progress bar during ability use - use fresh hotbar reference
          double progress = (double) remainingTicks / durationTicks;
          currentHotbar.setProgress(Math.max(0.0, progress));
          remainingTicks--;
        }
      }.runTaskTimer(plugin, 0L, 1L);
    }
  }

  /**
   * Ensures a hunter always has a boss bar for their current disguise, creating or updating as
   * needed.
   */
  private void ensurePermanentIdleHotbar(Player player, HunterDisguiseType disguiseType) {
    UUID playerId = player.getUniqueId();
    BossBar existingHotbar = permanentIdleHotbars.get(playerId);
    String expectedAbilityName = getIdleAbilityName(disguiseType);

    // Check if we need to create a new bar or update the existing one for a
    // different disguise
    if (existingHotbar == null) {
      // Create new boss bar
      String title = expectedAbilityName + " Charge";
      BossBar hotbar = Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SEGMENTED_10);
      hotbar.addPlayer(player);
      hotbar.setProgress(0.0);
      permanentIdleHotbars.put(playerId, hotbar);
    } else {
      // Update existing bar for current disguise if it's different
      String currentTitle = existingHotbar.getTitle();
      if (!currentTitle.contains(expectedAbilityName)) {
        // Player changed disguise - update the bar
        existingHotbar.setTitle(expectedAbilityName + " Charge");
        existingHotbar.setColor(BarColor.BLUE);
        existingHotbar.setProgress(0.0);
      }
    }
  }

  /** Updates idle progress on the boss bar without removing it. */
  private void updateIdleProgress(
      Player player, HunterDisguiseType disguiseType, long currentIdleTime) {
    UUID playerId = player.getUniqueId();
    BossBar hotbar = permanentIdleHotbars.get(playerId);

    if (hotbar != null) {
      // Slenderman's bar works differently - it doesn't charge based on idle time
      if (disguiseType == HunterDisguiseType.SLENDERMAN) {
        // Only update title and color if no ability is active AND no paranoia effect is
        // running
        BukkitTask activeParanoiaTask = slendermanParanoiaBarTasks.get(playerId);
        boolean paranoiaEffectActive =
            activeParanoiaTask != null && !activeParanoiaTask.isCancelled();

        if (!activeAbilities.getOrDefault(playerId, false) && !paranoiaEffectActive) {
          hotbar.setProgress(0.0); // Always empty unless filled by paranoia effect
          hotbar.setColor(BarColor.BLUE);
          hotbar.setTitle("Paranoia Aura");
        }
        return;
      }

      // For other hunters, use the normal idle-based charging system
      long requiredIdleTime = getRequiredIdleTime(disguiseType);
      double progress = Math.min(1.0, (double) currentIdleTime / requiredIdleTime);
      String abilityName = getIdleAbilityName(disguiseType);

      // Mark ability as charged when it reaches 100%
      if (progress >= 1.0 && !chargedAbilities.getOrDefault(playerId, false)) {
        chargedAbilities.put(playerId, true);
      }

      // Update progress
      hotbar.setProgress(progress);

      // Change color and title based on progress, but only if no ability is active
      if (!activeAbilities.getOrDefault(playerId, false)) {
        if (progress >= 1.0) {
          hotbar.setColor(BarColor.GREEN); // Ready to use
          hotbar.setTitle(abilityName + " Ready! (Shift to use)");
        } else if (progress >= 0.5) {
          hotbar.setColor(BarColor.YELLOW); // Charging
          hotbar.setTitle(abilityName + " Charging...");
        } else {
          hotbar.setColor(BarColor.BLUE); // Building up
          hotbar.setTitle(abilityName + " Charge");
        }
      }
    }
  }

  /** Triggers ability use effect on the permanent hotbar (decreases and resets). */
  private void triggerAbilityUseEffect(Player player, HunterDisguiseType disguiseType) {
    UUID playerId = player.getUniqueId();
    BossBar hotbar = permanentIdleHotbars.get(playerId);

    if (hotbar != null) {
      // Flash red briefly to indicate ability use
      hotbar.setColor(BarColor.RED);
      hotbar.setProgress(0.0);

      // Reset to blue after a brief moment
      Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                if (hotbar != null && permanentIdleHotbars.containsKey(playerId)) {
                  hotbar.setColor(BarColor.BLUE);
                }
              },
              10L); // 0.5 seconds
    }
  }

  /** Removes permanent idle hotbar for a player. */
  private void removePermanentIdleHotbar(Player player) {
    UUID playerId = player.getUniqueId();
    BossBar hotbar = permanentIdleHotbars.remove(playerId);
    if (hotbar != null) {
      hotbar.removeAll();
    }
  }

  /** Checks if a disguise type has idle-time related abilities. */
  private boolean hasIdleTimeAbility(HunterDisguiseType disguiseType) {
    return disguiseType == HunterDisguiseType.SPRINGTRAP
        || disguiseType == HunterDisguiseType.HEROBRINE
        || disguiseType == HunterDisguiseType.SLENDERMAN
        || disguiseType == HunterDisguiseType.CRYPTID
        || disguiseType == HunterDisguiseType.SCARECROW
        || disguiseType == HunterDisguiseType.JIGSAW;
  }

  /** Gets the required idle time for a disguise type's ability (in seconds). */
  private long getRequiredIdleTime(HunterDisguiseType disguiseType) {
    switch (disguiseType) {
      case SPRINGTRAP:
        return 1; // Vengeful Echo: 1 second
      case HEROBRINE:
        return 2; // Glitch Sight: 2 seconds
      case SLENDERMAN:
        return 999999; // Paranoia Aura: Always present, fills on effect, not idle time
      case CRYPTID:
        return 3; // Shadow Phase: 3 seconds
      case SCARECROW:
        return 4; // Field of Fear: 4 seconds
      case JIGSAW:
        return 3; // Puzzle Field: 3 seconds
      default:
        return 5; // Default fallback
    }
  }

  /** Gets the ability name for display in the hotbar. */
  private String getIdleAbilityName(HunterDisguiseType disguiseType) {
    switch (disguiseType) {
      case SPRINGTRAP:
        return "Vengeful Echo";
      case HEROBRINE:
        return "Glitch Sight";
      case SLENDERMAN:
        return "Paranoia Aura";
      case CRYPTID:
        return "Shadow Phase";
      case SCARECROW:
        return "Field of Fear";
      case JIGSAW:
        return "Puzzle Field";
      default:
        return "Passive Ability";
    }
  }

  /** Checks if a player is looking directly at another player. */
  private boolean isPlayerLookingAt(Player observer, Player target) {
    Vector observerDirection = observer.getEyeLocation().getDirection();
    Vector toTarget =
        target
            .getEyeLocation()
            .toVector()
            .subtract(observer.getEyeLocation().toVector())
            .normalize();

    double dot = observerDirection.dot(toTarget);
    return dot > 0.9; // Looking within ~25 degrees
  }

  /** Removes passive effects from a player. */
  private void removePassiveEffects(Player player) {
    UUID playerId = player.getUniqueId();

    // Cancel any running tasks
    BukkitTask springtrapTask = springtrapAuraTasks.remove(playerId);
    if (springtrapTask != null) {
      springtrapTask.cancel();
    }

    BukkitTask herobrineTask = herobrineGlowTasks.remove(playerId);
    if (herobrineTask != null) {
      herobrineTask.cancel();
    }

    // Cancel any Slenderman paranoia bar task
    BukkitTask paranoiaBarTask = slendermanParanoiaBarTasks.remove(playerId);
    if (paranoiaBarTask != null) {
      paranoiaBarTask.cancel();
    }

    // Cancel any hider "RUN" title task
    BukkitTask hiderRunTask = hiderRunTitleTasks.remove(playerId);
    if (hiderRunTask != null) {
      hiderRunTask.cancel();
    }

    // Clear any remaining title for the hider
    if (player != null) {
      player.clearTitle();
    }

    // Remove permanent idle hotbar
    BossBar permanentHotbar = permanentIdleHotbars.remove(playerId);
    if (permanentHotbar != null) {
      permanentHotbar.removeAll();
    }

    // Remove scarecrow clone
    Entity clone = scarecrowClones.remove(playerId);
    if (clone != null && !clone.isDead()) {
      clone.remove();
    }

    // Clear tracking data
    slendermanLookTimes.remove(playerId);
    slendermanParanoiaCooldowns.remove(playerId);
    jigsawVanishTimes.remove(playerId);
    activeAbilities.remove(playerId);
  }

  /** Gets the disguise type for a player based on their current disguise. */
  private HunterDisguiseType getPlayerDisguiseType(Player player) {
    String disguiseDisplayName = disguiseManager.getPlayerDisguiseSkin(player.getUniqueId());
    if (disguiseDisplayName != null) {
      return HunterDisguiseType.fromDisplayName(disguiseDisplayName);
    }
    return null;
  }

  /** Checks if a player is a disguised hunter. */
  private boolean isDisguisedHunter(Player player) {
    // Don't allow spectators to use abilities
    if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
      return false;
    }

    // Debug to help diagnose issues
    plugin
        .getLogger()
        .fine(
            "Checking if player "
                + player.getName()
                + " is a disguised hunter in world: "
                + player.getWorld().getName());

    // Check if player is a hunter
    HuntPlayerData playerData = lobbyManager.getPlayerData(player.getUniqueId());
    if (playerData == null) {
      plugin.getLogger().fine("Player data is null for " + player.getName());
      return false;
    }

    if (playerData.getSelectedTeam() != HuntTeam.HUNTERS) {
      plugin.getLogger().fine("Player " + player.getName() + " is not on HUNTERS team");
      return false;
    }

    if (playerData.getSelectedHunterClass() == null) {
      plugin.getLogger().fine("Player " + player.getName() + " has no hunter class selected");
      return false;
    }

    // Check if player has an active disguise
    boolean isDisguised = disguiseManager.isPlayerDisguised(player.getUniqueId());
    if (!isDisguised) {
      plugin.getLogger().fine("Player " + player.getName() + " is not disguised");
    }

    return isDisguised;
  }

  /** Checks if a player is a hider. */
  private boolean isPlayerHider(Player player) {
    // Don't allow spectators to be affected by or use abilities
    if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
      return false;
    }

    // Debug to help diagnose issues
    plugin
        .getLogger()
        .fine(
            "Checking if player "
                + player.getName()
                + " is a hider in world: "
                + player.getWorld().getName());

    HuntPlayerData playerData = lobbyManager.getPlayerData(player.getUniqueId());
    if (playerData == null) {
      plugin.getLogger().fine("Player data is null for " + player.getName());
      return false;
    }

    if (playerData.getSelectedTeam() != HuntTeam.HIDERS) {
      plugin.getLogger().fine("Player " + player.getName() + " is not on HIDERS team");
      return false;
    }
    return playerData.getSelectedHiderClass() != null;
  }

  /** Checks if a player has moved significantly. */
  private boolean hasMovedSignificantly(Location lastLocation, Location currentLocation) {
    if (!lastLocation.getWorld().equals(currentLocation.getWorld())) {
      return true;
    }
    return lastLocation.distance(currentLocation) > 0.1;
  }

  /** Cancels any passive effect task for a player. */
  private void cancelPassiveTask(UUID playerId) {
    BukkitTask task = passiveEffectTasks.remove(playerId);
    if (task != null) {
      task.cancel();
    }
  }

  /**
   * Starts the global tick-based update system that checks all hunters every tick. This ensures
   * idle progress and passive abilities are updated smoothly regardless of movement.
   */
  private void startGlobalTickTask() {
    globalTickTask =
        new BukkitRunnable() {
          @Override
          public void run() {
            // Check all online players every tick
            for (Player player : Bukkit.getOnlinePlayers()) {
              if (isDisguisedHunter(player)) {
                updateHunterState(player);
              } else {
                // Clean up any tracking data for players who are no longer disguised hunters
                UUID playerId = player.getUniqueId();
                if (lastPlayerLocations.containsKey(playerId)
                    || playerStillTimes.containsKey(playerId)
                    || abilityCooldowns.containsKey(playerId)
                    || activeAbilities.containsKey(playerId)) {
                  lastPlayerLocations.remove(playerId);
                  playerStillTimes.remove(playerId);
                  abilityCooldowns.remove(playerId);
                  activeAbilities.remove(playerId);
                  chargedAbilities.remove(playerId);
                  removePermanentIdleHotbar(player); // Only remove when no longer a hunter
                }
              }
            }
          }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick (20 times per second)
  }

  /**
   * Updates a single hunter's state including idle progress and passive abilities. Called every
   * tick for all disguised hunters.
   */
  private void updateHunterState(Player player) {
    UUID playerId = player.getUniqueId();
    Location currentLocation = player.getLocation();
    Location lastLocation = lastPlayerLocations.get(playerId);

    // Apply armor hiding for all disguised hunters
    applyArmorHiding(player);

    // Always ensure the hunter has a boss bar for their current disguise
    HunterDisguiseType currentDisguiseType = getPlayerDisguiseType(player);
    if (currentDisguiseType != null && hasIdleTimeAbility(currentDisguiseType)) {
      ensurePermanentIdleHotbar(player, currentDisguiseType);
    }

    // Check if player has moved significantly (more than 0.1 blocks)
    boolean hasMovedSignificantly =
        lastLocation == null || hasMovedSignificantly(lastLocation, currentLocation);

    if (hasMovedSignificantly) {
      // Player has moved - update location
      lastPlayerLocations.put(playerId, currentLocation.clone());

      // Only reset still time if the ability is not already fully charged
      if (!chargedAbilities.getOrDefault(playerId, false)) {
        playerStillTimes.remove(playerId);
      }
      // DO NOT remove the boss bar when moving - keep it visible always

      // Handle movement-based abilities
      if (currentDisguiseType == HunterDisguiseType.CRYPTID) {
        handleCryptidSilentMovement(player);
      }

    } else {
      // Player hasn't moved significantly - track idle time
      if (!playerStillTimes.containsKey(playerId)) {
        playerStillTimes.put(playerId, System.currentTimeMillis());
      }

      // Update idle progress every tick only if no ability is currently active
      Long stillStartTime = playerStillTimes.get(playerId);
      if (stillStartTime != null && !activeAbilities.getOrDefault(playerId, false)) {
        long currentIdleTime = (System.currentTimeMillis() - stillStartTime) / 1000;

        if (currentDisguiseType != null && hasIdleTimeAbility(currentDisguiseType)) {
          updateIdleProgress(player, currentDisguiseType, currentIdleTime);
        }
      } else if (chargedAbilities.getOrDefault(playerId, false)
          && !activeAbilities.getOrDefault(playerId, false)) {
        // If ability is charged but player is moving, maintain the "Ready" state
        if (currentDisguiseType != null && hasIdleTimeAbility(currentDisguiseType)) {
          maintainChargedState(player, currentDisguiseType);
        }
      }
    }
  }

  /** Clears all passive effects and tasks for a specific player. */
  public void clearPlayerPassiveEffects(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) {
      removePassiveEffects(player);
    }

    lastPlayerLocations.remove(playerId);
    playerStillTimes.remove(playerId);
    abilityCooldowns.remove(playerId);
    activeAbilities.remove(playerId);
    chargedAbilities.remove(playerId);
    cancelPassiveTask(playerId);
  }

  /** Clears all passive effects and tasks for all players. */
  public void clearAllPassiveEffects() {
    // Remove passive effects from all tracked players
    for (UUID playerId : lastPlayerLocations.keySet()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        removePassiveEffects(player);
      }
    }

    // Clear all tracking data
    lastPlayerLocations.clear();
    playerStillTimes.clear();
    passiveEffectTasks.values().forEach(BukkitTask::cancel);
    passiveEffectTasks.clear();
    springtrapAuraTasks.values().forEach(BukkitTask::cancel);
    springtrapAuraTasks.clear();
    herobrineGlowTasks.values().forEach(BukkitTask::cancel);
    herobrineGlowTasks.clear();

    // Remove all clones
    for (Entity clone : scarecrowClones.values()) {
      if (!clone.isDead()) {
        clone.remove();
      }
    }
    scarecrowClones.clear();

    slendermanLookTimes.clear();
    slendermanParanoiaCooldowns.clear();
    jigsawVanishTimes.clear();

    // Remove all permanent hotbars
    for (BossBar hotbar : permanentIdleHotbars.values()) {
      hotbar.removeAll();
    }
    permanentIdleHotbars.clear();

    // Clear ability cooldowns
    abilityCooldowns.clear();

    // Clear active abilities
    activeAbilities.clear();

    // Clear charged abilities
    chargedAbilities.clear();

    // Cancel Slenderman paranoia task
    if (slendermanParanoiaTask != null) {
      slendermanParanoiaTask.cancel();
    }

    // Cancel and clear all Slenderman paranoia bar tasks
    slendermanParanoiaBarTasks.values().forEach(BukkitTask::cancel);
    slendermanParanoiaBarTasks.clear();

    // Cancel and clear all hider "RUN" title tasks
    hiderRunTitleTasks.values().forEach(BukkitTask::cancel);
    hiderRunTitleTasks.clear();

    // Clear hider seen cooldowns
    hiderSeenCooldowns.clear();

    // Cancel global tick task
    if (globalTickTask != null) {
      globalTickTask.cancel();
      globalTickTask = null;
    }
  }

  /** Stops the Slenderman paranoia detection task. */
  public void stopSlendermanParanoiaTask() {
    if (slendermanParanoiaTask != null) {
      slendermanParanoiaTask.cancel();
      slendermanParanoiaTask = null;
    }
  }

  /** Stops the global tick task. */
  public void stopGlobalTickTask() {
    if (globalTickTask != null) {
      globalTickTask.cancel();
      globalTickTask = null;
    }
  }

  /**
   * Handles player shift detection to trigger passive abilities when the bar is full.
   *
   * @param event The PlayerToggleSneakEvent
   */
  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    // Only handle shift down (start sneaking) for disguised hunters
    if (!event.isSneaking() || !isDisguisedHunter(player)) {
      return;
    }

    // Check if player has a cooldown
    Long cooldownEnd = abilityCooldowns.get(playerId);
    if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
      return;
    }

    HunterDisguiseType disguiseType = getPlayerDisguiseType(player);
    if (disguiseType == null || !hasIdleTimeAbility(disguiseType)) {
      return;
    }

    // Check if player has a charged ability ready to use
    if (!chargedAbilities.getOrDefault(playerId, false)) {
      return;
    }

    applyPassiveAbility(player, disguiseType, getRequiredIdleTime(disguiseType));

    // Set cooldown (2 seconds)
    abilityCooldowns.put(playerId, System.currentTimeMillis() + 2000L);

    // Clear charged ability and reset idle time to start charging again
    chargedAbilities.remove(playerId);
    playerStillTimes.put(playerId, System.currentTimeMillis());
  }

  /**
   * Shows a bold red "RUN" title message to a hider when they are spotted by a Slenderman.
   *
   * @param hider The hider who has been spotted
   * @param slenderman The Slenderman player who spotted the hider
   */
  private void showRunTitleToHider(Player hider, Player slenderman) {
    UUID hiderId = hider.getUniqueId();
    long currentTime = System.currentTimeMillis();

    // Check if hider is on cooldown (to prevent spam)
    Long cooldownEnd = hiderSeenCooldowns.get(hiderId);
    if (cooldownEnd != null && currentTime < cooldownEnd) {
      return; // Still on cooldown
    }

    // Cancel any existing title task
    BukkitTask existingTask = hiderRunTitleTasks.get(hiderId);
    if (existingTask != null) {
      existingTask.cancel();
      hiderRunTitleTasks.remove(hiderId);
    }

    // Set cooldown (5 seconds to match paranoia effect duration)
    hiderSeenCooldowns.put(hiderId, currentTime + 5000L);

    // Apply darkness effect to the hider for the paranoia duration
    hider.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0)); // 5 seconds

    // Play hunter screech sound when effect is applied to hider
    disguiseManager.playCharacterScreech(slenderman);

    // Create a blinking title task that shows/hides the title randomly
    BukkitTask blinkingTask =
        new BukkitRunnable() {
          private int ticksRemaining = 100; // 5 seconds total (100 ticks)
          private boolean titleVisible = false;
          private final Random random = new Random();

          @Override
          public void run() {
            if (ticksRemaining <= 0) {
              // Time's up, clear the title and finish
              hider.clearTitle();
              hiderRunTitleTasks.remove(hiderId);
              cancel();
              return;
            }

            // Randomly decide if title should be visible (70% chance to show when hidden,
            // 30% chance to hide when shown)
            boolean shouldShow;
            if (titleVisible) {
              shouldShow = random.nextDouble() > 0.3; // 70% chance to keep showing
            } else {
              shouldShow = random.nextDouble() < 0.7; // 70% chance to start showing
            }

            if (shouldShow && !titleVisible) {
              // Show the title with no fade (instant appearance)
              Title runTitle =
                  Title.title(
                      Component.text("HE'S HERE")
                          .color(NamedTextColor.DARK_RED)
                          .decorate(TextDecoration.BOLD),
                      Component.empty(), // No subtitle
                      Title.Times.times(
                          Duration.ofMillis(0), // No fade in
                          Duration.ofMillis(1000), // Stay for 1 second max
                          Duration.ofMillis(0) // No fade out
                          ));
              hider.showTitle(runTitle);
              titleVisible = true;
            } else if (!shouldShow && titleVisible) {
              // Hide the title
              hider.clearTitle();
              titleVisible = false;
            }

            ticksRemaining--;
          }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick for smooth blinking

    // Store the task for potential cancellation
    hiderRunTitleTasks.put(hiderId, blinkingTask);
  }

  /**
   * Shows a flash-glitch "FEAR ME" title effect when a scarecrow clone catches a hider.
   *
   * @param hider The hider who was caught by the clone
   */
  private void showFearMeTitleFlash(Player hider) {
    // Create a rapid flash-glitch effect for "FEAR ME" title
    new BukkitRunnable() {
      private int flashCount = 0;
      private final int totalFlashes = 8; // Total number of flashes (4 on/off cycles)

      @Override
      public void run() {
        if (flashCount >= totalFlashes) {
          // Clear title at the end
          hider.clearTitle();
          cancel();
          return;
        }

        boolean shouldShow = (flashCount % 2 == 0); // Show on even counts, hide on odd

        if (shouldShow) {
          // Show the "FEAR ME" title with dramatic styling
          Title fearTitle =
              Title.title(
                  Component.text("FEAR ME")
                      .color(NamedTextColor.DARK_RED)
                      .decorate(TextDecoration.BOLD),
                  Component.empty(), // No subtitle
                  Title.Times.times(
                      Duration.ofMillis(0), // No fade in
                      Duration.ofMillis(150), // Stay for 150ms
                      Duration.ofMillis(0) // No fade out
                      ));
          hider.showTitle(fearTitle);
        } else {
          // Hide the title for glitch effect
          hider.clearTitle();
        }

        flashCount++;
      }
    }.runTaskTimer(plugin, 0L, 3L); // Run every 3 ticks (150ms) for rapid flashing
  }

  /** Creates a circular red particle effect around a player to show ability radius. */
  private void showRadiusParticleEffect(Player player, double radius) {
    Location center = player.getLocation().add(0, 0.1, 0); // Slightly above ground

    // Create a circle of red particles
    for (int i = 0; i < 50; i++) { // 50 particles for a smooth circle
      double angle = 2 * Math.PI * i / 50;
      double x = center.getX() + radius * Math.cos(angle);
      double z = center.getZ() + radius * Math.sin(angle);
      Location particleLocation = new Location(center.getWorld(), x, center.getY(), z);

      // Spawn red particle (using DUST with red color)
      center
          .getWorld()
          .spawnParticle(
              Particle.DUST,
              particleLocation,
              1,
              new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
    }
  }

  /** Checks for nearby hiders within a radius and plays screech if any are found. */
  private void playScreechIfHidersNearby(Player hunter, double radius) {
    for (Player nearbyPlayer : hunter.getWorld().getPlayers()) {
      if (!nearbyPlayer.equals(hunter) && isPlayerHider(nearbyPlayer)) {
        double distance = nearbyPlayer.getLocation().distance(hunter.getLocation());
        if (distance <= radius) {
          // Play character-specific screech if hiders are nearby
          disguiseManager.playCharacterScreech(hunter);
          return; // Only play screech once per ability use
        }
      }
    }
  }

  /** Maintains the charged state visual for abilities that are ready to use. */
  private void maintainChargedState(Player player, HunterDisguiseType disguiseType) {
    UUID playerId = player.getUniqueId();
    BossBar hotbar = permanentIdleHotbars.get(playerId);

    if (hotbar != null && !activeAbilities.getOrDefault(playerId, false)) {
      String abilityName = getIdleAbilityName(disguiseType);
      hotbar.setProgress(1.0); // Keep at full charge
      hotbar.setColor(BarColor.GREEN); // Ready to use
      hotbar.setTitle(abilityName + " Ready! (Shift to use)");
    }
  }
}
