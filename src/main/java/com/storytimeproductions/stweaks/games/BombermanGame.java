package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Represents a Bomberman game, implementing the Minigame interface and handling player
 * interactions, game state updates, and rendering.
 */
public class BombermanGame implements Minigame {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final List<Location> spawnPoints = new ArrayList<>();
  private final int maxPlayers = 6;
  private final Map<UUID, Integer> playerSpawnIndex = new HashMap<>();
  private Scoreboard gameScoreboard;
  private Objective gameObjective;
  private boolean gameEnded = false;
  private final List<Player> alivePlayers = new ArrayList<>();

  /**
   * Constructs a new Bomberman game with the specified configuration.
   *
   * @param config the game configuration
   */
  public BombermanGame(GameConfig config) {
    this.config = config;

    // Example: load spawn points from config (assume keys: spawn1, spawn2, ...,
    // spawn6)
    for (int i = 1; i <= maxPlayers; i++) {
      String key = "spawn" + i;
      String locString = config.getGameProperties().get(key);
      if (locString != null) {
        String[] parts = locString.split(",");
        if (parts.length == 4) {
          Location loc =
              new Location(
                  Bukkit.getWorld(parts[0]),
                  Double.parseDouble(parts[1]),
                  Double.parseDouble(parts[2]),
                  Double.parseDouble(parts[3]));
          spawnPoints.add(loc);
        }
      }
    }
  }

  /** Initializes the Bomberman game. */
  @Override
  public void onInit() {
    // Give all players a TNT on game start
    for (Player player : players) {
      giveTnt(player);
      // Add player to the "bomber" region in world "casino"
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(),
          String.format("rg addmember -w \"casino\" bomber %s", player.getName()));
    }

    alivePlayers.clear();
    alivePlayers.addAll(players);

    // Get regions from game properties (comma-separated list)
    String regionsStr = config.getGameProperties().get("regions");
    if (regionsStr != null && !regionsStr.isEmpty()) {
      regionsStr = regionsStr.replace("[", "").replace("]", "");
      String[] regions = regionsStr.split(",");
      for (String region : regions) {
        region = region.trim();
        if (!region.isEmpty()) {
          String regionData = config.getGameProperties().get(region);
          if (regionData != null && !regionData.isEmpty()) {
            String[] parts = regionData.split(",");
            if (parts.length == 7) {
              World world = Bukkit.getWorld(parts[0]);
              int x1 = Integer.parseInt(parts[1]);
              int y1 = Integer.parseInt(parts[2]);
              int z1 = Integer.parseInt(parts[3]);
              int x2 = Integer.parseInt(parts[4]);
              int y2 = Integer.parseInt(parts[5]);
              int z2 = Integer.parseInt(parts[6]);
              fillRegionRandomly(world, x1, y1, z1, x2, y2, z2);
            }
          }
        }
      }
    }

    // Initialize scoreboard and objective only once
    gameScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    if (gameScoreboard.getObjective("bomberman") == null) {
      gameObjective =
          gameScoreboard.registerNewObjective(
              "bomberman", Criteria.DUMMY, Component.text("Bomberman", NamedTextColor.GOLD));
      gameObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
    } else {
      gameObjective = gameScoreboard.getObjective("bomberman");
    }
  }

  private void fillRegionRandomly(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
    if (world == null) {
      return;
    }
    int minX = Math.min(x1, x2);
    int maxX = Math.max(x1, x2);
    int minY = Math.min(y1, y2);
    int maxY = Math.max(y1, y2);
    int minZ = Math.min(z1, z2);
    int maxZ = Math.max(z1, z2);
    Material[] choices = {Material.NETHERRACK, Material.OAK_PLANKS, Material.DIRT};
    Random random = new Random();

    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        for (int z = minZ; z <= maxZ; z++) {
          Location loc = new Location(world, x, y, z);
          Block block = world.getBlockAt(loc);
          if (block.getType() == Material.AIR) {
            block.setType(choices[random.nextInt(choices.length)]);
          }
        }
      }
    }
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {
    // Teleport each player to their assigned spawn point based on join order
    for (int i = 0; i < players.size(); i++) {
      Player player = players.get(i);
      if (i < spawnPoints.size()) {
        player.teleport(spawnPoints.get(i));
      }
    }
  }

  /** Updates the game state. */
  @Override
  public void update() {
    Location exit = config.getExitArea();
    // If only one player remains, end the game and announce the winner
    if (alivePlayers.size() == 1 && !gameEnded) {
      Player winner = alivePlayers.get(0);
      Bukkit.broadcast(Component.text(winner.getName() + " wins Bomberman!", NamedTextColor.GOLD));
      winner.getInventory().remove(Material.TNT);
      if (exit != null) {
        winner.teleport(exit);
        rewardWinner(winner, players.size());
      }
      gameEnded = true;
    }
  }

  private void rewardWinner(Player winner, int playerCount) {
    ItemStack tickets = new ItemStack(org.bukkit.Material.NAME_TAG, playerCount);
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);
    winner.getInventory().addItem(tickets);
  }

  /** Renders the game state. */
  @Override
  public void render() {
    Location exit = config.getExitArea();
    List<String> alive = new ArrayList<>();
    List<String> eliminated = new ArrayList<>();

    for (Player player : players) {
      if (exit == null
          || player.getWorld() != exit.getWorld()
          || player.getLocation().distance(exit) > 2.0) {
        alive.add(player.getName());
      } else {
        eliminated.add(player.getName());
      }
    }

    // Clear previous scores (optional, to avoid ghost entries)
    for (String entry : gameScoreboard.getEntries()) {
      gameScoreboard.resetScores(entry);
    }

    int score = alive.size() + eliminated.size() + 2;
    gameObjective.getScore("Alive:").setScore(score--);
    for (String name : alive) {
      gameObjective.getScore("  " + name).setScore(score--);
    }
    gameObjective.getScore("Eliminated:").setScore(score--);
    for (String name : eliminated) {
      gameObjective.getScore("  " + name).setScore(score--);
    }

    // Set the scoreboard for all players
    for (Player player : players) {
      player.setScoreboard(gameScoreboard);
    }
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    for (Player player : players) {
      player.getInventory().remove(Material.TNT);
      // Remove player from the "bomber" region in world "casino"
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(),
          String.format("rg removemember -w \"casino\" bomber %s", player.getName()));
    }
    players.clear();
    playerSpawnIndex.clear();
    gameEnded = false;
    alivePlayers.clear();
  }

  /**
   * Adds the specified player to the game.
   *
   * @param player the player to add
   */
  @Override
  public void join(Player player) {
    if (players.size() >= maxPlayers) {
      player.sendMessage(Component.text("The game is full!", NamedTextColor.RED));
      return;
    }
    if (players.contains(player)) {
      return;
    }
    players.add(player);
    alivePlayers.add(player); // Add to alivePlayers

    // Assign spawn point
    int spawnIdx = players.size() - 1;
    playerSpawnIndex.put(player.getUniqueId(), spawnIdx);
    if (spawnIdx < spawnPoints.size()) {
      player.teleport(spawnPoints.get(spawnIdx));
    }
    giveTnt(player);
    player.sendMessage(
        Component.text("You have joined Bomberman! Use TNT to play.", NamedTextColor.GREEN));
  }

  /**
   * Removes the specified player from the game.
   *
   * @param player the player to remove
   */
  @Override
  public void leave(Player player) {
    players.remove(player);
    playerSpawnIndex.remove(player.getUniqueId());
    player.getInventory().remove(Material.TNT);
    Location exit = config.getExitArea();
    if (exit != null) {
      player.teleport(exit);
    }
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
    return gameEnded;
  }

  /**
   * Removes items from the player's inventory when they leave the game.
   *
   * @param player the player whose items should be removed
   */
  @Override
  public void removeItems(Player player) {
    player.getInventory().remove(Material.TNT);
  }

  private void giveTnt(Player player) {
    ItemStack tnt = new ItemStack(Material.TNT, 1);
    ItemMeta meta = tnt.getItemMeta();
    meta.displayName(Component.text("Bomberman TNT", NamedTextColor.RED));
    tnt.setItemMeta(meta);
    player.getInventory().addItem(tnt);
  }

  /**
   * Handles player interactions with TNT items.
   *
   * @param event the PlayerInteractEvent
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (!players.contains(player)) {
      return;
    }
    ItemStack item = event.getItem();
    if (item != null && item.getType() == Material.TNT) {
      // Delay to next tick to ensure TNT is consumed
      Bukkit.getScheduler()
          .runTaskLater(
              Bukkit.getPluginManager().getPlugin("stweaks"),
              () -> {
                if (player.isOnline()) {
                  giveTnt(player);
                }
              },
              1L);
    }
  }

  /**
   * Handles player death events to eliminate players.
   *
   * @param event the PlayerDeathEvent
   */
  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    if (!players.contains(player)) {
      return;
    }

    // Remove from alivePlayers
    alivePlayers.remove(player);

    // Cancel the death, restore health, and teleport to exit
    event.setCancelled(true);
    Bukkit.getScheduler()
        .runTaskLater(
            JavaPlugin.getProvidingPlugin(getClass()),
            () -> {
              double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
              player.setHealth(maxHealth);
              player.setFoodLevel(20);
              player.setFireTicks(0);
              Location exit = config.getExitArea();
              player.getInventory().remove(Material.TNT);
              if (exit != null) {
                player.teleport(exit);
              }
              player.sendMessage(Component.text("You have been eliminated!", NamedTextColor.RED));
            },
            1L);
  }
}
