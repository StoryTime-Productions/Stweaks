package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.Cuboid;
import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import com.storytimeproductions.models.stgames.challenges.BellRingChallenge;
import com.storytimeproductions.models.stgames.challenges.BreakBlockChallenge;
import com.storytimeproductions.models.stgames.challenges.CraftItemChallenge;
import com.storytimeproductions.models.stgames.challenges.EmptyInventoryChallenge;
import com.storytimeproductions.models.stgames.challenges.HatSwitchChallenge;
import com.storytimeproductions.models.stgames.challenges.KillPlayerChallenge;
import com.storytimeproductions.models.stgames.challenges.LadderClimbChallenge;
import com.storytimeproductions.models.stgames.challenges.PunchPigChallenge;
import com.storytimeproductions.models.stgames.challenges.RidePigChallenge;
import com.storytimeproductions.models.stgames.challenges.ShearSheepChallenge;
import com.storytimeproductions.models.stgames.challenges.SnowballChallenge;
import com.storytimeproductions.models.stgames.challenges.StoryBlitzChallenge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents a StoryBlitz game. Implements the Minigame interface for game lifecycle management.
 */
public class StoryBlitz implements Minigame {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Map<Player, Integer> lives = new HashMap<>();
  private final List<StoryBlitzChallenge> challenges = new ArrayList<>();
  private StoryBlitzChallenge currentChallenge = null;
  private int cooldown = 5;
  private int cooldownTicks = 0;
  private final int minCooldown = 1;
  private boolean gameInProgress = false;
  private final Map<Player, BossBar> bossBars = new HashMap<>();

  private int challengeTimerTicks = 0;
  private final int maxChallengeTicks = 30;
  private boolean challengeTimeoutWarning = false;
  private int challengeIndex = 0;
  private List<StoryBlitzChallenge> challengeOrder = new ArrayList<>();
  private BossBar timerBar = null;

  /**
   * Constructs a new StoryBlitz game with the specified configuration.
   *
   * @param config the game configuration
   */
  public StoryBlitz(GameConfig config) {
    this.config = config;
  }

  /** Initializes the Battleship game. */
  @Override
  public void onInit() {
    // Initialize lives and challenges
    for (Player p : players) {
      lives.put(p, 5);
      p.teleport(config.getGameArea());
    }

    List<Cuboid> spawnRegions = new ArrayList<>();
    Object regionCountObj = config.getGameProperties().get("regionCount");
    int regionCount = 1;
    if (regionCountObj instanceof Number) {
      regionCount = ((Number) regionCountObj).intValue();
    }

    for (int i = 1; i <= regionCount; i++) {
      Object regionObj = config.getGameProperties().get("spawnRegion" + i);
      if (regionObj instanceof String) {
        try {
          String[] parts = ((String) regionObj).split(",");
          if (parts.length == 7) {
            String worldName = parts[0];
            int x1 = Integer.parseInt(parts[1]);
            int y1 = Integer.parseInt(parts[2]);
            int z1 = Integer.parseInt(parts[3]);
            int x2 = Integer.parseInt(parts[4]);
            int y2 = Integer.parseInt(parts[5]);
            int z2 = Integer.parseInt(parts[6]);
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
              Cuboid cuboid = new Cuboid(world, x1, y1, z1, x2, y2, z2);
              spawnRegions.add(cuboid);
              // Clear the region to air on startup
              for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                  for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                  }
                }
              }
            }
          }
        } catch (Exception e) {
          Bukkit.getLogger()
              .warning("[StoryBlitz] Failed to parse spawnRegion" + i + ": " + regionObj);
        }
      }
    }

    challenges.add(new BreakBlockChallenge(spawnRegions));
    challenges.add(new CraftItemChallenge(spawnRegions));
    challenges.add(new KillPlayerChallenge(spawnRegions));
    challenges.add(new EmptyInventoryChallenge(spawnRegions));
    challenges.add(new RidePigChallenge(spawnRegions));
    challenges.add(new PunchPigChallenge(spawnRegions));
    challenges.add(new BellRingChallenge(spawnRegions));
    challenges.add(new HatSwitchChallenge(spawnRegions));
    challenges.add(new LadderClimbChallenge(spawnRegions));
    challenges.add(new ShearSheepChallenge(spawnRegions));
    challenges.add(new SnowballChallenge(spawnRegions));
    Collections.shuffle(challenges);

    gameInProgress = true;
    cooldown = 5;
    cooldownTicks = cooldown;
    nextChallenge();
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {}

  /** Updates the game state. */
  @Override
  public void update() {
    if (!gameInProgress) {
      return;
    }
    // Only count down when no challenge is active
    if (cooldownTicks > 0 && currentChallenge == null) {
      cooldownTicks--;
      // Hide timer bar if present
      if (timerBar != null) {
        timerBar.setVisible(false);
      }
      // Play a tick sound to all players during the countdown
      for (Player p : players) {
        if (lives.getOrDefault(p, 0) > 0) {
          p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
      }
      return;
    }
    if (currentChallenge != null) {
      // Check completion
      List<Player> incomplete = new ArrayList<>();
      for (Player p : players) {
        if (lives.getOrDefault(p, 0) > 0 && !currentChallenge.isCompleted(p)) {
          incomplete.add(p);
        }
      }

      // --- Challenge timer logic ---
      challengeTimerTicks++;

      // Show bossbar timer
      if (timerBar == null) {
        timerBar = Bukkit.createBossBar("Time left: 30s", BarColor.RED, BarStyle.SOLID);
        for (Player p : players) {
          timerBar.addPlayer(p);
        }
      }
      timerBar.setVisible(true);
      int secondsLeft = Math.max(0, 30 - challengeTimerTicks);
      timerBar.setTitle("Time left: " + secondsLeft + "s");
      timerBar.setProgress(Math.max(0.0, Math.min(1.0, (30.0 - challengeTimerTicks) / 30.0)));

      // Play note sound for last 3 seconds
      if (secondsLeft <= 3 && secondsLeft > 0) {
        for (Player p : players) {
          if (lives.getOrDefault(p, 0) > 0) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
          }
        }
      }

      if (!challengeTimeoutWarning && challengeTimerTicks >= maxChallengeTicks) {
        // 30 seconds are up, announce 3-second warning
        for (Player p : players) {
          if (lives.getOrDefault(p, 0) > 0) {
            p.sendMessage(Component.text("Time's up!", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
          }
        }
        challengeTimeoutWarning = true;
      }
      if (challengeTimeoutWarning && challengeTimerTicks >= maxChallengeTicks + 3) {
        // Hide timer bar
        if (timerBar != null) {
          timerBar.setVisible(false);
        }
        // Force next challenge, deduct a life from everyone
        for (Player p : players) {
          if (lives.getOrDefault(p, 0) > 0) {
            int remaining = lives.getOrDefault(p, 1) - 1;
            lives.put(p, remaining);
            p.sendMessage(
                Component.text(
                    "You lost a life due to timeout! Lives left: " + remaining,
                    NamedTextColor.RED));
            if (remaining <= 0) {
              p.teleport(config.getExitArea());
              p.sendMessage(Component.text("You are out!", NamedTextColor.GRAY));
              // Play a sound effect to all players when a player dies
              for (Player all : players) {
                all.playSound(all.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
              }
            }
          }
        }
        currentChallenge.cleanup(players);
        currentChallenge = null;
        cooldownTicks = cooldown;
        return;
      }
      // --- End challenge timer logic ---

      if (incomplete.size() == 1) {
        Player loser = incomplete.get(0);
        int remaining = lives.getOrDefault(loser, 1) - 1;
        lives.put(loser, remaining);
        loser.sendMessage(
            Component.text("You lost a life! Lives left: " + remaining, NamedTextColor.RED));
        if (remaining <= 0) {
          loser.teleport(config.getExitArea());
          loser.sendMessage(Component.text("You are out!", NamedTextColor.GRAY));
          cooldown = Math.max(minCooldown, cooldown - 1);
          // Play a sound effect to all players when a player dies
          for (Player all : players) {
            all.playSound(all.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
          }
        }
        // End challenge and go to next
        if (timerBar != null) {
          timerBar.setVisible(false);
        }
        currentChallenge.cleanup(players);
        currentChallenge = null;
        cooldownTicks = cooldown;
        return;
      }
      // If all players have completed the challenge at the same time, just move on to
      // the next challenge
      if (incomplete.isEmpty()) {
        if (timerBar != null) {
          timerBar.setVisible(false);
        }
        currentChallenge.cleanup(players);
        currentChallenge = null;
        cooldownTicks = cooldown;
        return;
      }
      return;
    }
    // Start next challenge if countdown is done and no challenge is active
    if (cooldownTicks <= 0 && currentChallenge == null) {
      nextChallenge();
    }
  }

  /** Renders the game state. */
  @Override
  public void render() {
    for (Player p : players) {
      int l = lives.getOrDefault(p, 0);

      // BossBar for lives
      BossBar bar = bossBars.get(p);
      if (bar == null) {
        bar = Bukkit.createBossBar("Lives: " + l, BarColor.YELLOW, BarStyle.SEGMENTED_6);
        bossBars.put(p, bar);
        bar.addPlayer(p);
      }
      bar.setTitle("Lives: " + l);
      bar.setProgress(Math.max(0.0, Math.min(1.0, l / 5.0)));
      bar.setVisible(l > 0);

      // Only show action bar for next challenge if no challenge is active AND
      // countdown is running
      if (cooldownTicks > 0 && currentChallenge == null) {
        p.sendActionBar(
            Component.text("Next challenge in " + (cooldownTicks) + "s", NamedTextColor.YELLOW));
      } else {
        // Do not display anything during a challenge or if no countdown
        p.sendActionBar(Component.empty());
      }
    }
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    // Reward winner(s)
    List<Player> alive = new ArrayList<>();
    for (Player p : players) {
      if (lives.getOrDefault(p, 0) > 0) {
        alive.add(p);
      }
    }
    if (alive.size() == 1) {
      rewardWinner(alive.get(0), players.size());
    }
    for (Player p : players) {
      removeItems(p);
    }
    for (BossBar bar : bossBars.values()) {
      bar.removeAll();
    }
    bossBars.clear();

    // Clear out all spawn regions and set them to air
    Object regionCountObj = config.getGameProperties().get("regionCount");
    int regionCount = 1;
    if (regionCountObj instanceof Number) {
      regionCount = ((Number) regionCountObj).intValue();
    }
    for (int i = 1; i <= regionCount; i++) {
      Object regionObj = config.getGameProperties().get("spawnRegion" + i);
      if (regionObj instanceof String) {
        try {
          String[] parts = ((String) regionObj).split(",");
          if (parts.length == 7) {
            String worldName = parts[0];
            int x1 = Integer.parseInt(parts[1]);
            int y1 = Integer.parseInt(parts[2]);
            int z1 = Integer.parseInt(parts[3]);
            int x2 = Integer.parseInt(parts[4]);
            int y2 = Integer.parseInt(parts[5]);
            int z2 = Integer.parseInt(parts[6]);
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
              for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                  for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                  }
                }
              }
            }
          }
        } catch (Exception e) {
          Bukkit.getLogger()
              .warning("[StoryBlitz] Failed to clear spawnRegion" + i + ": " + regionObj);
        }
      }
    }

    players.clear();
    lives.clear();
    challenges.clear();
    currentChallenge = null;
    gameInProgress = false;
  }

  /**
   * Adds the specified player to the game.
   *
   * @param player the player to add
   */
  @Override
  public void join(Player player) {
    if (!players.contains(player)) {
      players.add(player);
      lives.put(player, 5);
    }
  }

  /**
   * Removes the specified player from the game.
   *
   * @param player the player to remove
   */
  @Override
  public void leave(Player player) {
    players.remove(player);
    lives.remove(player);
  }

  /**
   * Gets the list of players currently in the game.
   *
   * @return a list of players in the game
   */
  @Override
  public List<Player> getPlayers() {
    return new ArrayList<>(players);
  }

  /**
   * Gets the game configuration.
   *
   * @return the game configuration
   */
  @Override
  public GameConfig getConfig() {
    return config;
  }

  /**
   * Determines if the game should quit.
   *
   * @return true if the game should quit, false otherwise
   */
  @Override
  public boolean shouldQuit() {
    return !gameInProgress;
  }

  @Override
  public void removeItems(Player player) {
    // Remove all challenge-related items if needed
  }

  // --- Helper methods ---

  private void nextChallenge() {
    if (challenges.isEmpty()
        || players.stream().filter(p -> lives.getOrDefault(p, 0) > 0).count() <= 1) {
      gameInProgress = false;
      return;
    }
    // Shuffle the collection and go through it one by one
    if (challengeOrder.isEmpty() || challengeIndex >= challengeOrder.size()) {
      challengeOrder = new ArrayList<>(challenges);
      Collections.shuffle(challengeOrder);
      challengeIndex = 0;
    }
    StoryBlitzChallenge next = challengeOrder.get(challengeIndex++);
    currentChallenge = next;
    currentChallenge.start(players);
    cooldownTicks = cooldown;
    challengeTimerTicks = 0;
    challengeTimeoutWarning = false;
    for (Player p : players) {
      if (lives.getOrDefault(p, 0) > 0) {
        p.sendMessage(
            Component.text("Challenge: " + currentChallenge.getDescription(), NamedTextColor.AQUA));
      }
    }
  }

  private void rewardWinner(Player winner, int ticketCount) {
    ItemStack tickets = new ItemStack(Material.NAME_TAG, ticketCount);
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);
    winner.getInventory().addItem(tickets);

    // Announce the winner to all players
    for (Player p : players) {
      p.sendMessage(Component.text(winner.getName() + " has won StoryBlitz!", NamedTextColor.GOLD));
    }

    // Spawn non-harming fireworks at the winner's location for 3 seconds
    Location loc = winner.getLocation();
    World world = loc.getWorld();
    // Spawn non-harming fireworks at the winner's current location for 3 seconds
    if (world != null) {
      new org.bukkit.scheduler.BukkitRunnable() {
        int ticks = 0;

        @Override
        public void run() {
          if (ticks >= 60) { // 3 seconds at 20 ticks per second
            this.cancel();
            return;
          }
          // Always use the winner's current location
          Location currentLoc = winner.getLocation();
          Firework fw =
              world.spawn(
                  currentLoc,
                  Firework.class,
                  firework -> {
                    FireworkEffect effect =
                        FireworkEffect.builder()
                            .withColor(Color.AQUA, Color.LIME, Color.YELLOW)
                            .withFade(org.bukkit.Color.WHITE)
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .trail(true)
                            .flicker(true)
                            .build();
                    FireworkMeta fwm = firework.getFireworkMeta();
                    fwm.addEffect(effect);
                    fwm.setPower(0);
                    firework.setFireworkMeta(fwm);
                  });
          // Remove the firework instantly to prevent damage
          Bukkit.getScheduler()
              .runTaskLater(Bukkit.getPluginManager().getPlugin("stweaks"), fw::detonate, 2L);
          ticks += 10;
        }
      }.runTaskTimer(Bukkit.getPluginManager().getPlugin("stweaks"), 0L, 10L); // every 0.5s
    }
  }
}
