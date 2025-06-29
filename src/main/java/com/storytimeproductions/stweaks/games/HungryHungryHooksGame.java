package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.Cuboid;
import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Minigame: Hungry Hungry Hooks. Players are split into colored teams and must fish pigs into their
 * zone.
 */
public class HungryHungryHooksGame implements Minigame, Listener {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Map<Player, Integer> teamMap = new HashMap<>();
  private final Map<Integer, Integer> teamScores = new HashMap<>();
  private final List<Location> baseLocations = new ArrayList<>();
  private final List<Location> pigSpawnLocations = new ArrayList<>();
  private Scoreboard scoreboard;
  private Objective objective;
  private boolean gameInProgress = false;
  private final int maxScore = 7;
  private final int teamCount = 4;

  private final int pigTargetCount = 14;
  private int pigSpawnTick = 0;
  private final int pigSpawnInterval = 10;
  private boolean pigsInitiallySpawned = false;

  private static final NamedTextColor[] teamColors = {
    NamedTextColor.GREEN, NamedTextColor.YELLOW, NamedTextColor.BLUE, NamedTextColor.RED
  };
  private static final String[] teamNames = {"Green", "Yellow", "Blue", "Red"};
  private static final String[] teamHolograms = {
    "team1-holo", "team2-holo", "team3-holo", "team4-holo"
  };
  private final List<Cuboid> teamPens = new ArrayList<>();

  /**
   * Constructs a new HungryHungryHooksGame game with the specified configuration.
   *
   * @param config the game configuration
   */
  public HungryHungryHooksGame(GameConfig config) {
    this.config = config;
  }

  /** Initializes the Battleship game. */
  @Override
  public void onInit() {
    // Clear and load base locations from config
    baseLocations.clear();
    for (int i = 0; i < teamCount; i++) {
      String key = "team" + (i + 1) + "Base";
      String locStr = config.getGameProperties().get(key);
      if (locStr != null) {
        String[] parts = locStr.split(",");
        World world = Bukkit.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        baseLocations.add(new Location(world, x, y, z));
      }
    }

    // Clear and load team pens from config
    teamPens.clear();
    for (int i = 0; i < teamCount; i++) {
      String key = "team" + (i + 1) + "Pen";
      String penStr = config.getGameProperties().get(key);
      if (penStr != null) {
        String[] parts = penStr.split(",");
        World world = Bukkit.getWorld(parts[0]);
        int x1 = Integer.parseInt(parts[1]);
        int y1 = Integer.parseInt(parts[2]);
        int z1 = Integer.parseInt(parts[3]);
        int x2 = Integer.parseInt(parts[4]);
        int y2 = Integer.parseInt(parts[5]);
        int z2 = Integer.parseInt(parts[6]);
        teamPens.add(new Cuboid(world, x1, y1, z1, x2, y2, z2));
      }
    }

    // Clear and load pig spawn locations from config
    pigSpawnLocations.clear();
    int pigSpawnCount =
        Integer.parseInt(config.getGameProperties().getOrDefault("pigSpawnCount", "0"));
    for (int i = 1; i <= pigSpawnCount; i++) {
      String key = "pigSpawn" + i;
      String locStr = config.getGameProperties().get(key);
      if (locStr != null) {
        String[] parts = locStr.split(",");
        World world = Bukkit.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        pigSpawnLocations.add(new Location(world, x, y, z));
      }
    }

    // Assign players to teams
    Collections.shuffle(players);
    for (int i = 0; i < players.size(); i++) {
      int team = i % teamCount;
      teamMap.put(players.get(i), team);
      players.get(i).teleport(baseLocations.get(team));
      removeItems(players.get(i));
      ItemStack rod = new ItemStack(Material.FISHING_ROD);
      rod.addUnsafeEnchantment(Enchantment.UNBREAKING, 32767);
      players.get(i).getInventory().addItem(rod);
      players
          .get(i)
          .showTitle(
              Title.title(
                  Component.text("You are on Team " + teamNames[team] + "!", teamColors[team]),
                  Component.text("Hook pigs into your zone!", NamedTextColor.YELLOW)));
    }
    // Initialize scores
    for (int i = 0; i < teamCount; i++) {
      teamScores.put(i, 0);
    }
    setupScoreboard();
    gameInProgress = true;
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

    for (Player p : players) {
      Integer team = teamMap.get(p);
      if (team != null && team < baseLocations.size()) {
        Location base = baseLocations.get(team);
        if (p.getLocation().getY() < base.getY()) {
          p.teleport(base);
        }
      }
    }

    // Spawn all pigs immediately at game start
    if (!pigsInitiallySpawned) {
      int toSpawn = pigTargetCount;
      for (int i = 0; i < toSpawn && i < pigSpawnLocations.size(); i++) {
        Location spawnLoc = pigSpawnLocations.get(i % pigSpawnLocations.size());
        spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.PIG);
      }
      pigsInitiallySpawned = true;
      pigSpawnTick = 0;
    }

    // Pig spawn timer: every 10 seconds (200 ticks)
    pigSpawnTick++;

    if (pigSpawnTick >= pigSpawnInterval) {
      pigSpawnTick = 0;
      int currentPigCount = 0;
      for (Location loc : pigSpawnLocations) {
        currentPigCount += loc.getWorld().getEntitiesByClass(Pig.class).size();
      }
      int toSpawn = pigTargetCount - currentPigCount;
      for (int i = 0; i < toSpawn && i < pigSpawnLocations.size(); i++) {
        Location spawnLoc = pigSpawnLocations.get(i % pigSpawnLocations.size());
        spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.PIG);
      }
    }

    // Check each team's pen for pigs and score
    for (int team = 0; team < teamPens.size(); team++) {
      Cuboid pen = teamPens.get(team);
      List<Pig> pigsInPen = new ArrayList<>();
      for (Pig pig : pen.world.getEntitiesByClass(Pig.class)) {
        if (pen.contains(pig.getLocation())) {
          pigsInPen.add(pig);
        }
      }
      if (!pigsInPen.isEmpty() && teamScores.get(team) < maxScore) {
        pigsInPen.get(0).remove();

        for (Player p : players) {
          p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        addPointToTeam(team);
      }
    }
  }

  /**
   * Renders the game state to all players. This method is called periodically to update the game
   * display.
   */
  @Override
  public void render() {
    // Always update the scoreboard display for all players
    updateScoreboard();
    for (Player p : players) {
      // Also send the action bar countdown in render
      int ticksLeft = pigSpawnInterval - pigSpawnTick;
      int secondsLeft = Math.max(1, ticksLeft);
      Component actionBar =
          Component.text("Next pig wave in " + secondsLeft + "s", NamedTextColor.YELLOW);
      p.sendActionBar(actionBar);
    }
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    // Find the winning team (the one with MAX_SCORE)
    int winningTeam = -1;
    for (Map.Entry<Integer, Integer> entry : teamScores.entrySet()) {
      if (entry.getValue() >= maxScore) {
        winningTeam = entry.getKey();
        break;
      }
    }
    if (winningTeam != -1) {
      rewardWinners(winningTeam);
    }
    for (Player p : players) {
      removeItems(p);
    }
    // Reset all team holograms to seven red squares
    StringBuilder resetBar = new StringBuilder();
    for (int i = 0; i < maxScore; i++) {
      // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
      resetBar.append("&c\u25a0");
      // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
    }
    for (String holoName : teamHolograms) {
      String cmd = "dh l set " + holoName + " 1 1 " + resetBar;
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
    players.clear();
    teamMap.clear();
    teamScores.clear();
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

  @Override
  public void removeItems(Player player) {
    ItemStack[] contents = player.getInventory().getContents();
    for (int i = 0; i < contents.length; i++) {
      ItemStack item = contents[i];
      if (item != null && item.getType() == Material.FISHING_ROD) {
        player.getInventory().setItem(i, null);
      }
    }
  }

  // --- Scoreboard setup and update ---
  private void setupScoreboard() {
    scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    if (scoreboard.getObjective("hooks") != null) {
      scoreboard.getObjective("hooks").unregister();
    }
    objective =
        scoreboard.registerNewObjective(
            "hooks", Criteria.DUMMY, Component.text("Pig Points", NamedTextColor.YELLOW));
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    for (Player p : players) {
      p.setScoreboard(scoreboard);
    }
  }

  private void updateScoreboard() {
    if (objective == null) {
      return;
    }

    for (int i = 0; i < teamCount; i++) {
      String teamName = teamNames[i] + " Team";
      objective.getScore(teamName).setScore(teamScores.getOrDefault(i, 0));
    }
  }

  private void addPointToTeam(int team) {
    int score = teamScores.getOrDefault(team, 0) + 1;
    teamScores.put(team, score);
    updateScoreboard();
    updateTeamHologram(team, score);
    if (score >= maxScore) {
      endGame(team);
    }
  }

  // Update the hologram with colored squares using ampersand color codes and
  // \u25a0
  private void updateTeamHologram(int team, int score) {
    StringBuilder bar = new StringBuilder();
    for (int i = 0; i < maxScore; i++) {
      // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
      if (i < score) {
        bar.append("&a\u25a0");
      } else {
        bar.append("&c\u25a0");
      }
      // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
    }
    String holoName = teamHolograms[team];
    String cmd = "dh l set " + holoName + " 1 1 " + bar;
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
  }

  private void endGame(int winningTeam) {
    Component winMsg =
        Component.text(
            "Team " + teamNames[winningTeam] + " wins Hungry Hungry Hooks!",
            teamColors[winningTeam]);
    // Show title and send message to winners
    for (Player p : players) {
      Integer team = teamMap.get(p);
      if (team != null && team.equals(winningTeam)) {
        p.sendMessage(winMsg);
        p.showTitle(
            Title.title(
                Component.text("VICTORY!", teamColors[winningTeam]),
                Component.text("Your team wins Hungry Hungry Hooks!", NamedTextColor.GOLD)));
      } else {
        p.sendMessage(winMsg);
      }
      // Remove scoreboard for all players (set to a new/empty scoreboard)
      p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    // Unregister the sidebar objective to fully clear the sidebar
    if (scoreboard != null && objective != null) {
      scoreboard.getObjective("hooks").unregister();
      scoreboard = null;
      objective = null;
    }

    // Remove all pigs in the game world(s)
    for (Location spawnLoc : pigSpawnLocations) {
      World world = spawnLoc.getWorld();
      for (Pig pig : world.getEntitiesByClass(Pig.class)) {
        pig.remove();
      }
    }

    // Launch fireworks for all winners (must be after scoreboard removal)
    for (Player p : players) {
      Integer team = teamMap.get(p);
      if (team != null && team.equals(winningTeam)) {
        launchFireworks(p, teamColors[winningTeam]);
      }
    }

    new BukkitRunnable() {
      @Override
      public void run() {
        gameInProgress = false;
      }
    }.runTaskLater(Bukkit.getPluginManager().getPlugin("stweaks"), 50L);
  }

  // Launch colored fireworks near the player for 5 seconds
  private void launchFireworks(Player player, NamedTextColor color) {
    Color fireworkColor = getBukkitColor(color);
    new BukkitRunnable() {
      private int ticks = 0; // Make ticks an instance variable

      @Override
      public void run() {
        if (ticks >= 100) { // 5 seconds (20 ticks per second)
          this.cancel();
          return;
        }
        Firework firework =
            player.getWorld().spawn(player.getLocation().add(0, 1, 0), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(
            FireworkEffect.builder()
                .withColor(fireworkColor)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        firework.setSilent(true);
        firework.detonate();
        ticks += 10; // Now this will increment properly
      }
    }.runTaskTimer(Bukkit.getPluginManager().getPlugin("stweaks"), 0L, 10L);
  }

  // Helper to convert NamedTextColor to Bukkit Color
  private Color getBukkitColor(NamedTextColor color) {
    if (NamedTextColor.RED.equals(color)) {
      return Color.RED;
    } else if (NamedTextColor.BLUE.equals(color)) {
      return Color.BLUE;
    } else if (NamedTextColor.GREEN.equals(color)) {
      return Color.LIME;
    } else if (NamedTextColor.YELLOW.equals(color)) {
      return Color.YELLOW;
    } else {
      return Color.WHITE;
    }
  }

  private void rewardWinners(int winningTeam) {
    int ticketCount = 0;
    // Count the number of players NOT on the winning team
    for (Player p : players) {
      Integer team = teamMap.get(p);
      if (team == null || team != winningTeam) {
        ticketCount++;
      }
    }
    if (ticketCount <= 0) {
      return;
    }

    ItemStack tickets = new ItemStack(org.bukkit.Material.NAME_TAG, ticketCount);
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);

    // Give tickets to all players on the winning team
    for (Player p : players) {
      Integer team = teamMap.get(p);
      if (team != null && team.equals(winningTeam)) {
        p.getInventory().addItem(tickets);
      }
    }
  }
}
