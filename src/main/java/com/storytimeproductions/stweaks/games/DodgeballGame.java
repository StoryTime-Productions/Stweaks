package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.Cuboid;
import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Represents a Dodgeball game where players are split into two teams and can throw rockets at each
 * other. Implements the Minigame interface for game lifecycle management.
 */
public class DodgeballGame implements Minigame {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Map<Player, String> teamMap = new HashMap<>();
  private final List<Player> redTeam = new ArrayList<>();
  private final List<Player> blueTeam = new ArrayList<>();
  private final List<Location> redSpawns = new ArrayList<>();
  private final List<Location> blueSpawns = new ArrayList<>();
  private final List<Location> rocketSpawners = new ArrayList<>();
  private int rocketTick = 0;
  private final int rocketInterval = 5;
  private int nextRocketIn = rocketInterval;
  private boolean gameInProgress = false;

  /**
   * Constructs a new Dodgeball game with the specified configuration.
   *
   * @param config the game configuration
   */
  public DodgeballGame(GameConfig config) {
    this.config = config;
  }

  /** Initializes the Battleship game. */
  @Override
  public void onInit() {
    Collections.shuffle(players);
    redTeam.clear();
    blueTeam.clear();
    teamMap.clear();

    for (int i = 0; i < players.size(); i++) {
      Player p = players.get(i);
      if (i % 2 == 0) {
        redTeam.add(p);
        teamMap.put(p, "red");
      } else {
        blueTeam.add(p);
        teamMap.put(p, "blue");
      }
    }

    // Load cuboid spawn regions from config
    // Example config: redSpawnCuboid=world,x1,y1,z1,x2,y2,z2
    // blueSpawnCuboid=world,x1,y1,z1,x2,y2,z2
    // rocketSpawnersCuboid=world,x1,y1,z1,x2,y2,z2

    redSpawns.clear();
    blueSpawns.clear();
    rocketSpawners.clear();

    Cuboid redCuboid = getCuboidFromConfig("redSpawnCuboid");
    if (redCuboid != null) {
      List<Location> allRed = redCuboid.getLocations();
      Collections.shuffle(allRed);
      for (int i = 0; i < Math.min(2, allRed.size()); i++) {
        redSpawns.add(allRed.get(i));
      }
    }

    Cuboid blueCuboid = getCuboidFromConfig("blueSpawnCuboid");
    if (blueCuboid != null) {
      List<Location> allBlue = blueCuboid.getLocations();
      Collections.shuffle(allBlue);
      for (int i = 0; i < Math.min(2, allBlue.size()); i++) {
        blueSpawns.add(allBlue.get(i));
      }
    }

    Cuboid rocketCuboid = getCuboidFromConfig("rocketSpawnersCuboid");
    if (rocketCuboid != null) {
      rocketSpawners.addAll(rocketCuboid.getLocations());
    }

    // Teleport players to random spawn in their region and equip them
    for (Player p : redTeam) {
      Location spawn = redSpawns.get(ThreadLocalRandom.current().nextInt(redSpawns.size()));
      p.teleport(spawn);
      equipPlayer(p);
    }
    for (Player p : blueTeam) {
      Location spawn = blueSpawns.get(ThreadLocalRandom.current().nextInt(blueSpawns.size()));
      p.teleport(spawn);
      equipPlayer(p);
    }

    gameInProgress = true;
    rocketTick = 0;
    nextRocketIn = rocketInterval;
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {
    // Announce to each player which team they are on
    for (Player p : players) {
      String team = teamMap.get(p);
      if (team != null) {
        p.sendMessage(
            Component.text("You are on the ", NamedTextColor.GRAY)
                .append(
                    net.kyori.adventure.text.Component.text(
                        team.equals("red") ? "Red Team" : "Blue Team",
                        team.equals("red") ? NamedTextColor.RED : NamedTextColor.BLUE))
                .append(Component.text("!", NamedTextColor.GRAY)));
      }
    }
  }

  /**
   * Handles player death events to announce which team the player belonged to.
   *
   * @param event the PlayerDeathEvent
   */
  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player dead = event.getEntity();
    if (players.contains(dead)) {
      String team = teamMap.get(dead);
      Component msg =
          Component.text(dead.getName() + " has died for the ", NamedTextColor.GRAY)
              .append(
                  Component.text(
                      team != null && team.equals("red") ? "Red Team" : "Blue Team",
                      team != null && team.equals("red")
                          ? NamedTextColor.RED
                          : NamedTextColor.BLUE))
              .append(Component.text("!", NamedTextColor.GRAY));
      for (Player p : players) {
        p.sendMessage(msg);
      }

      dead.setHealth(dead.getAttribute(Attribute.MAX_HEALTH).getValue());
      dead.teleport(config.getExitArea());

      // Remove items and armor
      removeItems(dead);

      if ("red".equals(team)) {
        redTeam.remove(dead);
      } else if ("blue".equals(team)) {
        blueTeam.remove(dead);
      }

      // Prevent actual death
      event.setCancelled(true);

      event.setKeepInventory(true);
      event.setKeepLevel(true);
    }
  }

  /** Updates the game state. */
  @Override
  public void update() {
    if (!gameInProgress) {
      return;
    }

    rocketTick++;
    nextRocketIn--;

    if (rocketTick >= rocketInterval) {
      rocketTick = 0;
      nextRocketIn = rocketInterval;
      spawnRocket();
    }

    // Check for team elimination
    if (isTeamDead(redTeam)) {
      endGame("blue");
    } else if (isTeamDead(blueTeam)) {
      endGame("red");
    }
  }

  /** Renders the game state. */
  @Override
  public void render() {
    int secondsLeft = Math.max(1, nextRocketIn);
    for (Player p : players) {
      p.sendActionBar(
          net.kyori.adventure.text.Component.text(
              "Next rocket spawns in " + secondsLeft + "s",
              net.kyori.adventure.text.format.NamedTextColor.RED));
    }
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    List<Player> winners = new ArrayList<>();
    if (!redTeam.isEmpty() && blueTeam.isEmpty()) {
      winners.addAll(redTeam);
    } else if (!blueTeam.isEmpty() && redTeam.isEmpty()) {
      winners.addAll(blueTeam);
    }
    if (!winners.isEmpty()) {
      rewardWinners(winners, players.size());
    }
    for (Player p : players) {
      removeItems(p);
    }
    players.clear();
    redTeam.clear();
    blueTeam.clear();
    teamMap.clear();
    redSpawns.clear();
    blueSpawns.clear();
    rocketSpawners.clear();
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
    redTeam.remove(player);
    blueTeam.remove(player);
    teamMap.remove(player);
  }

  /**
   * Gets the list of players currently in the game.
   *
   * @return a list of players in the game
   */
  @Override
  public List<Player> getPlayers() {
    return players;
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

  /**
   * Removes any items related to the game from the player's inventory.
   *
   * @param player the player whose items should be removed
   */
  @Override
  public void removeItems(Player player) {
    // Remove any item named "Rocket Launcher" or "Rockets"
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.hasItemMeta()) {
        // log the item name
        if (item.getType() == Material.CARROT_ON_A_STICK
            || item.getType() == Material.WARPED_FUNGUS_ON_A_STICK
            || item.getType() == Material.CLOCK) {
          player.getInventory().remove(item);
        }
      }
    }
    // Remove all armor
    player.getInventory().setHelmet(null);
    player.getInventory().setChestplate(null);
    player.getInventory().setLeggings(null);
    player.getInventory().setBoots(null);
  }

  // --- Helper methods ---

  private void equipPlayer(Player player) {
    // Equip with blast protection 1 armor
    ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
    ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
    ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
    ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
    for (ItemStack armor : new ItemStack[] {helmet, chest, legs, boots}) {
      armor.addEnchantment(Enchantment.BLAST_PROTECTION, 1);
    }
    EntityEquipment eq = player.getEquipment();
    eq.setHelmet(helmet);
    eq.setChestplate(chest);
    eq.setLeggings(legs);
    eq.setBoots(boots);

    // Give rocket launcher via function
    boolean wasOp = player.isOp();
    player.setOp(true);
    Bukkit.dispatchCommand(player, "function thepa:give/grenade_launcher");
    if (!wasOp) {
      player.setOp(false);
    }
  }

  private void spawnRocket() {
    if (rocketSpawners.isEmpty()) {
      return;
    }
    Location loc = rocketSpawners.get(ThreadLocalRandom.current().nextInt(rocketSpawners.size()));
    Block block = loc.getBlock();
    block.setType(Material.REDSTONE_BLOCK);

    // Remove after 2 seconds (40 ticks)
    new BukkitRunnable() {
      @Override
      public void run() {
        if (block.getType() == Material.REDSTONE_BLOCK) {
          block.setType(Material.AIR);
        }
      }
    }.runTaskLater(Bukkit.getPluginManager().getPlugin("stweaks"), 40L);
  }

  private void rewardWinners(List<Player> winners, int ticketCount) {
    ItemStack tickets = new ItemStack(org.bukkit.Material.NAME_TAG, ticketCount);
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);

    for (Player p : winners) {
      p.getInventory().addItem(tickets);
    }
  }

  private boolean isTeamDead(List<Player> team) {
    for (Player p : team) {
      if (p.isOnline() && !p.isDead()) {
        return false;
      }
    }
    return true;
  }

  private void endGame(String winningTeam) {
    gameInProgress = false;
    for (Player p : players) {
      p.sendMessage(
          net.kyori.adventure.text.Component.text(
              "Team " + winningTeam.toUpperCase() + " wins Dodgeball!",
              winningTeam.equals("red")
                  ? net.kyori.adventure.text.format.NamedTextColor.RED
                  : net.kyori.adventure.text.format.NamedTextColor.BLUE));
    }
    gameInProgress = false;
  }

  private Cuboid getCuboidFromConfig(String key) {
    String val = config.getGameProperties().get(key);
    if (val == null) {
      return null;
    }
    String[] parts = val.split(",");
    if (parts.length < 7) {
      return null;
    }
    World world = Bukkit.getWorld(parts[0]);
    int x1 = Integer.parseInt(parts[1]);
    int y1 = Integer.parseInt(parts[2]);
    int z1 = Integer.parseInt(parts[3]);
    int x2 = Integer.parseInt(parts[4]);
    int y2 = Integer.parseInt(parts[5]);
    int z2 = Integer.parseInt(parts[6]);
    return new Cuboid(world, x1, y1, z1, x2, y2, z2);
  }
}
