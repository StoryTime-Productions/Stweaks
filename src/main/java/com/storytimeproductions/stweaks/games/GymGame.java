package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.Cuboid;
import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Represents a gym-themed minigame where players can interact with punching bags, treadmills, and
 * trampolines to earn time tickets.
 *
 * <p>This class implements the Minigame interface and handles game initialization, player
 * interactions, and game logic.
 */
public class GymGame implements Minigame, Listener {
  private final GameConfig config;
  private final Plugin plugin;
  private final List<Player> players = new ArrayList<>();
  private final Map<Location, Integer> punchingBagCounts = new HashMap<>();
  private final Map<Integer, Integer> treadmillCounts = new HashMap<>();
  private final Map<Location, Integer> trampolineCounts = new HashMap<>();
  private final int goal = 350;
  private final Map<Location, TextDisplay> punchingBagDisplays = new HashMap<>();
  private final Map<Integer, TextDisplay> treadmillDisplays = new HashMap<>();
  private final Map<Location, TextDisplay> trampolineDisplays = new HashMap<>();
  private final List<Cuboid> treadmillCuboids = new ArrayList<>();
  private final Map<UUID, Location> lastTrampoline = new HashMap<>();
  private final Map<Location, Integer> squatCounts = new HashMap<>();
  private final Map<Location, TextDisplay> squatDisplays = new HashMap<>();
  private final Map<UUID, Boolean> lastSneakState = new HashMap<>();
  private final List<Cuboid> squatCuboids = new ArrayList<>();
  private Block leaveBlock;
  private final Map<Cuboid, Location> punchingBagCuboidToBase = new HashMap<>();
  private final List<Cuboid> punchingBagCuboids = new ArrayList<>();

  /**
   * Constructs a new GymGame with the specified configuration and plugin.
   *
   * @param config the game configuration
   */
  public GymGame(GameConfig config) {
    this.config = config;
    this.plugin = Bukkit.getPluginManager().getPlugin("stweaks");
  }

  @Override
  public void onInit() {
    // --- Punching Bags ---
    int punchingBagCount =
        Integer.parseInt(config.getGameProperties().get("punchingBagCount").toString());
    punchingBagCuboids.clear();
    for (int i = 1; i <= punchingBagCount; i++) {
      String[] regionParts =
          ((String) config.getGameProperties().get("punchingBagRegion" + i)).split(",");
      World world = Bukkit.getWorld(regionParts[0]);
      int x1 = Integer.parseInt(regionParts[1]);
      int y1 = Integer.parseInt(regionParts[2]);
      int z1 = Integer.parseInt(regionParts[3]);
      int x2 = Integer.parseInt(regionParts[4]);
      int y2 = Integer.parseInt(regionParts[5]);
      int z2 = Integer.parseInt(regionParts[6]);
      Cuboid cuboid = new Cuboid(world, x1, y1, z1, x2, y2, z2);
      punchingBagCuboids.add(cuboid);

      int centerX = (cuboid.x1 + cuboid.x2) / 2;
      int minY = cuboid.y1;
      int centerZ = (cuboid.z1 + cuboid.z2) / 2;
      Location base = new Location(world, centerX, minY, centerZ);
      punchingBagCuboidToBase.put(cuboid, base);
      punchingBagCounts.put(base, goal);

      TextDisplay display =
          spawnDisplay(
              world,
              base.clone().add(0.5, 2.5, 0.5),
              Component.text("Punches left: " + goal, NamedTextColor.YELLOW));
      punchingBagDisplays.put(base, display);
    }

    // --- Treadmills ---
    int treadmillCount =
        Integer.parseInt(config.getGameProperties().get("treadmillCount").toString());
    treadmillCuboids.clear();
    for (int i = 1; i <= treadmillCount; i++) {
      String[] regionParts =
          ((String) config.getGameProperties().get("treadmillRegion" + i)).split(",");
      World world = Bukkit.getWorld(regionParts[0]);
      int x1 = Integer.parseInt(regionParts[1]);
      int y1 = Integer.parseInt(regionParts[2]);
      int z1 = Integer.parseInt(regionParts[3]);
      int x2 = Integer.parseInt(regionParts[4]);
      int y2 = Integer.parseInt(regionParts[5]);
      int z2 = Integer.parseInt(regionParts[6]);
      Cuboid cuboid = new Cuboid(world, x1, y1, z1, x2, y2, z2);
      treadmillCuboids.add(cuboid);

      Location back = new Location(world, x1, y1, z1);
      treadmillCounts.put(i - 1, 0);

      TextDisplay display =
          spawnDisplay(
              world,
              back.clone().add(0.5, 2.5, 0.5),
              Component.text("Meters: 0/" + goal, NamedTextColor.YELLOW));
      treadmillDisplays.put(i - 1, display);
    }

    // --- Trampolines ---
    int trampolineCount =
        Integer.parseInt(config.getGameProperties().get("trampolineCount").toString());
    List<Cuboid> trampolineCuboids = new ArrayList<>();
    for (int i = 1; i <= trampolineCount; i++) {
      String[] regionParts =
          ((String) config.getGameProperties().get("trampolineRegion" + i)).split(",");
      World world = Bukkit.getWorld(regionParts[0]);
      int x1 = Integer.parseInt(regionParts[1]);
      int y1 = Integer.parseInt(regionParts[2]);
      int z1 = Integer.parseInt(regionParts[3]);
      int x2 = Integer.parseInt(regionParts[4]);
      int y2 = Integer.parseInt(regionParts[5]);
      int z2 = Integer.parseInt(regionParts[6]);
      Cuboid cuboid = new Cuboid(world, x1, y1, z1, x2, y2, z2);
      trampolineCuboids.add(cuboid);

      int centerX = (cuboid.x1 + cuboid.x2) / 2;
      int minY = cuboid.y1;
      int centerZ = (cuboid.z1 + cuboid.z2) / 2;
      Location loc = new Location(world, centerX, minY, centerZ);
      trampolineCounts.put(loc, goal);

      TextDisplay display =
          spawnDisplay(
              world,
              loc.clone().add(0.5, 2.5, 0.5),
              Component.text("Jumps left: " + goal, NamedTextColor.YELLOW));
      trampolineDisplays.put(loc, display);
    }

    // --- Squat Racks ---
    int squatCount =
        config.getGameProperties().containsKey("squatCount")
            ? Integer.parseInt(config.getGameProperties().get("squatCount").toString())
            : 0;
    for (int i = 1; i <= squatCount; i++) {
      String[] regionParts =
          ((String) config.getGameProperties().get("squatRegion" + i)).split(",");
      World world = Bukkit.getWorld(regionParts[0]);
      int x1 = Integer.parseInt(regionParts[1]);
      int y1 = Integer.parseInt(regionParts[2]);
      int z1 = Integer.parseInt(regionParts[3]);
      int x2 = Integer.parseInt(regionParts[4]);
      int y2 = Integer.parseInt(regionParts[5]);
      int z2 = Integer.parseInt(regionParts[6]);
      Cuboid cuboid = new Cuboid(world, x1, y1, z1, x2, y2, z2);
      squatCuboids.add(cuboid);

      int centerX = (cuboid.x1 + cuboid.x2) / 2;
      int minY = cuboid.y1;
      int centerZ = (cuboid.z1 + cuboid.z2) / 2;
      Location squatLoc = new Location(world, centerX, minY, centerZ);
      squatCounts.put(squatLoc, goal);

      TextDisplay display =
          spawnDisplay(
              world,
              squatLoc.clone().add(0.5, 2.5, 0.5),
              Component.text("Squats left: " + goal, NamedTextColor.YELLOW));
      squatDisplays.put(squatLoc, display);
    }

    // --- Leave Block ---
    String leaveLocString = (String) config.getGameProperties().get("leaveBlock");
    if (leaveLocString != null) {
      String[] parts = leaveLocString.split(",");
      if (parts.length == 4) {
        Location loc =
            new Location(
                Bukkit.getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]));
        leaveBlock = loc.getBlock();
      }
    }

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  private TextDisplay spawnDisplay(World world, Location loc, Component text) {
    return world.spawn(
        loc,
        TextDisplay.class,
        d -> {
          d.setBillboard(Display.Billboard.VERTICAL);
          d.setAlignment(TextDisplay.TextAlignment.CENTER);
          d.setDefaultBackground(false);
          d.setSeeThrough(false);
          d.setPersistent(false);
          d.text(text);
        });
  }

  private void setDisplayText(TextDisplay display, Component text) {
    if (display != null && display.isValid()) {
      display.text(text);
    }
  }

  /**
   * Adds the player to the game.
   *
   * @param player the player to add
   */
  @Override
  public void join(Player player) {
    if (!players.contains(player)) {
      players.add(player);
      player.teleport(config.getGameArea());
    }
  }

  /**
   * Removes the player from the game.
   *
   * @param player the player to remove
   */
  @Override
  public void leave(Player player) {
    players.remove(player);
  }

  /**
   * Returns the list of players currently in the game.
   *
   * @return a list of players
   */
  @Override
  public List<Player> getPlayers() {
    return new ArrayList<>(players);
  }

  /**
   * Returns the game configuration.
   *
   * @return the game configuration
   */
  @Override
  public GameConfig getConfig() {
    return config;
  }

  /** Destroys the game, unregistering all events and cleaning up resources. */
  @Override
  public void onDestroy() {
    HandlerList.unregisterAll(this);

    for (TextDisplay d : punchingBagDisplays.values()) {
      if (d != null && d.isValid()) {
        d.remove();
      }
    }
    for (TextDisplay d : treadmillDisplays.values()) {
      if (d != null && d.isValid()) {
        d.remove();
      }
    }
    for (TextDisplay d : trampolineDisplays.values()) {
      if (d != null && d.isValid()) {
        d.remove();
      }
    }
    for (TextDisplay d : squatDisplays.values()) {
      if (d != null && d.isValid()) {
        d.remove();
      }
    }

    punchingBagDisplays.clear();
    treadmillDisplays.clear();
    trampolineDisplays.clear();
    squatDisplays.clear();
    squatCounts.clear();
    squatCuboids.clear();
    lastSneakState.clear();
  }

  @Override
  public boolean allowsConcurrentJoins() {
    return true;
  }

  @Override
  public boolean shouldTeleportOnExit() {
    return false;
  }

  /**
   * Handles player interactions with punching bags.
   *
   * @param event the PlayerInteractEvent triggered by a player
   */
  @Override
  public void onInteract(PlayerInteractEvent event) {
    // Allow leaving the gym by clicking the leave block
    Block block = event.getClickedBlock();
    if (block != null
        && leaveBlock != null
        && block.getLocation().equals(leaveBlock.getLocation())) {
      event.getPlayer().performCommand("casino leave");
      event.setCancelled(true);
      return;
    }

    if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
      return;
    }
    Block clickedBlock = event.getClickedBlock();
    if (clickedBlock == null) {
      return;
    }
    Location hitLoc = clickedBlock.getLocation();

    // Check if the clicked block is inside any punching bag cuboid
    for (Cuboid cuboid : punchingBagCuboids) {
      if (cuboid.contains(hitLoc)) {
        Location base = punchingBagCuboidToBase.get(cuboid);
        int left = punchingBagCounts.get(base) - 1;
        punchingBagCounts.put(base, left);
        setDisplayText(
            punchingBagDisplays.get(base),
            Component.text("Punches left: " + (left > 0 ? left : 0), NamedTextColor.YELLOW));
        if (left <= 0) {
          giveTimeTicket(event.getPlayer());
          punchingBagCounts.put(base, goal);
          setDisplayText(
              punchingBagDisplays.get(base),
              Component.text("Punches left: " + goal, NamedTextColor.YELLOW));
        }
        event.setCancelled(true);
        return;
      }
    }
  }

  /**
   * Handles player movement to detect treadmill and trampoline interactions.
   *
   * @param event the PlayerMoveEvent triggered by a player moving
   */
  @Override
  public void onMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Location to = event.getTo();
    Location from = event.getFrom();

    // --- Treadmill logic ---
    for (int i = 0; i < treadmillCuboids.size(); i++) {
      Cuboid treadmillCuboid = treadmillCuboids.get(i);
      Location front = getTreadmillFront(treadmillCuboid);
      if (front == null) {
        continue;
      }

      // The "back" is the other block in the cuboid
      List<Location> treadmillBlocks = new ArrayList<>();
      World world = treadmillCuboid.world;
      for (int x = Math.min(treadmillCuboid.x1, treadmillCuboid.x2);
          x <= Math.max(treadmillCuboid.x1, treadmillCuboid.x2);
          x++) {
        for (int y = Math.min(treadmillCuboid.y1, treadmillCuboid.y2);
            y <= Math.max(treadmillCuboid.y1, treadmillCuboid.y2);
            y++) {
          for (int z = Math.min(treadmillCuboid.z1, treadmillCuboid.z2);
              z <= Math.max(treadmillCuboid.z1, treadmillCuboid.z2);
              z++) {
            treadmillBlocks.add(new Location(world, x, y, z));
          }
        }
      }
      treadmillBlocks.removeIf(loc -> loc.equals(front));
      Location back = treadmillBlocks.isEmpty() ? null : treadmillBlocks.get(0);

      if (back != null
          && from.getBlock().equals(back.getBlock())
          && to.getBlock().equals(front.getBlock())) {
        int meters = treadmillCounts.getOrDefault(i, 0) + 1;
        treadmillCounts.put(i, meters);
        setDisplayText(
            treadmillDisplays.get(i),
            Component.text("Meters: " + meters + "/" + goal, NamedTextColor.YELLOW));
        // Teleport player to back block, preserving yaw and pitch
        Location tp = back.clone().add(0.5, 0.5, 0.5);
        tp.setYaw(to.getYaw());
        tp.setPitch(to.getPitch());
        player.teleport(tp);
        if (meters >= goal) {
          giveTimeTicket(player);
          treadmillCounts.put(i, 0);
          setDisplayText(
              treadmillDisplays.get(i), Component.text("Meters: 0/" + goal, NamedTextColor.YELLOW));
        }
        break;
      }
    }

    // --- Trampoline logic ---
    for (Location tramp : trampolineCounts.keySet()) {
      // Check if player is standing on the trampoline slime block
      if (to.getBlockX() == tramp.getBlockX()
          && to.getBlockY() - 1 == tramp.getBlockY()
          && to.getBlockZ() == tramp.getBlockZ()
          && tramp.getBlock().getType() == Material.SLIME_BLOCK) {
        // Only decrement if the player was NOT just on this trampoline
        if (!tramp.equals(lastTrampoline.get(player.getUniqueId()))) {
          int jumps = trampolineCounts.get(tramp) - 1;
          trampolineCounts.put(tramp, jumps);
          setDisplayText(
              trampolineDisplays.get(tramp),
              Component.text("Jumps left: " + (jumps > 0 ? jumps : 0), NamedTextColor.YELLOW));
          if (jumps <= 0) {
            giveTimeTicket(player);
            trampolineCounts.put(tramp, goal);
            setDisplayText(
                trampolineDisplays.get(tramp),
                Component.text("Jumps left: " + goal, NamedTextColor.YELLOW));
          }
        }
        lastTrampoline.put(player.getUniqueId(), tramp);
        break;
      }
    }
    // Clean up when player leaves the trampoline
    if (lastTrampoline.containsKey(player.getUniqueId())) {
      boolean onAnyTramp = false;
      for (Location tramp : trampolineCounts.keySet()) {
        if (to.getBlockX() == tramp.getBlockX()
            && to.getBlockY() - 1 == tramp.getBlockY()
            && to.getBlockZ() == tramp.getBlockZ()) {
          onAnyTramp = true;
          break;
        }
      }
      if (!onAnyTramp) {
        lastTrampoline.remove(player.getUniqueId());
      }
    }
  }

  private Location getTreadmillFront(Cuboid treadmillCuboid) {
    World world = treadmillCuboid.world;
    // Iterate over both blocks in the cuboid (since treadmill is 2 blocks)
    for (int x = Math.min(treadmillCuboid.x1, treadmillCuboid.x2);
        x <= Math.max(treadmillCuboid.x1, treadmillCuboid.x2);
        x++) {
      for (int y = Math.min(treadmillCuboid.y1, treadmillCuboid.y2);
          y <= Math.max(treadmillCuboid.y1, treadmillCuboid.y2);
          y++) {
        for (int z = Math.min(treadmillCuboid.z1, treadmillCuboid.z2);
            z <= Math.max(treadmillCuboid.z1, treadmillCuboid.z2);
            z++) {
          Location loc = new Location(world, x, y, z);
          // Check all 6 adjacent blocks for quartz
          for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
              for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) {
                  continue;
                }
                Location adj = loc.clone().add(dx, dy, dz);
                if (adj.getBlock().getType() == Material.QUARTZ_BLOCK) {
                  return loc;
                }
              }
            }
          }
        }
      }
    }
    return null; // Not found
  }

  private void giveTimeTicket(Player player) {
    ItemStack ticket = new ItemStack(Material.NAME_TAG, 1);
    ItemMeta meta = ticket.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    ticket.setItemMeta(meta);
    player.getInventory().addItem(ticket);
    player.sendMessage(Component.text("You earned a time ticket!").color(NamedTextColor.GOLD));
  }

  /** Resets all gym station counts and displays back to the goal value. */
  public void resetAllCounts() {
    for (Map.Entry<Location, Integer> entry : punchingBagCounts.entrySet()) {
      punchingBagCounts.put(entry.getKey(), goal);
      setDisplayText(
          punchingBagDisplays.get(entry.getKey()),
          Component.text("Punches left: " + goal, NamedTextColor.YELLOW));
    }
    for (Map.Entry<Integer, Integer> entry : treadmillCounts.entrySet()) {
      treadmillCounts.put(entry.getKey(), 0);
      setDisplayText(
          treadmillDisplays.get(entry.getKey()),
          Component.text("Meters: 0/" + goal, NamedTextColor.YELLOW));
    }
    for (Map.Entry<Location, Integer> entry : trampolineCounts.entrySet()) {
      trampolineCounts.put(entry.getKey(), goal);
      setDisplayText(
          trampolineDisplays.get(entry.getKey()),
          Component.text("Jumps left: " + goal, NamedTextColor.YELLOW));
    }
    for (Map.Entry<Location, Integer> entry : squatCounts.entrySet()) {
      squatCounts.put(entry.getKey(), goal);
      setDisplayText(
          squatDisplays.get(entry.getKey()),
          Component.text("Squats left: " + goal, NamedTextColor.YELLOW));
    }
  }

  /**
   * Called after the game has been initialized. Use this method to perform any post-initialization
   * logic required by the minigame.
   */
  @Override
  public void afterInit() {}

  /**
   * Updates the game state. This method should be called periodically to handle game logic, timers,
   * or state changes.
   */
  @Override
  public void update() {}

  /**
   * Renders any visual elements or updates required by the minigame. This method can be used to
   * update scoreboards, holograms, or other UI elements.
   */
  @Override
  public void render() {}

  /**
   * Determines whether the game should end or quit.
   *
   * @return true if the game should quit, false otherwise
   */
  @Override
  public boolean shouldQuit() {
    return players.size() == 0;
  }

  /**
   * Removes specific items from the player's inventory as required by the minigame.
   *
   * @param player the player from whom to remove items
   */
  @Override
  public void removeItems(Player player) {}

  /**
   * Handles player crouch (sneak) actions to detect squat interactions.
   *
   * @param event the PlayerToggleSneakEvent triggered by a player
   */
  @Override
  public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    boolean isSneaking = event.isSneaking();
    UUID uuid = player.getUniqueId();

    // Only count crouch DOWN (not up)
    if (isSneaking) {
      Location playerLoc = player.getLocation();
      for (Location squatLoc : squatCounts.keySet()) {
        // Check if player is inside the squat cuboid
        for (Cuboid cuboid : squatCuboids) {
          if (cuboid.contains(playerLoc)) {
            // Only count if last state was not sneaking
            if (!lastSneakState.getOrDefault(uuid, false)) {
              int squats = squatCounts.get(squatLoc) - 1;
              squatCounts.put(squatLoc, squats);
              setDisplayText(
                  squatDisplays.get(squatLoc),
                  Component.text(
                      "Squats left: " + (squats > 0 ? squats : 0), NamedTextColor.YELLOW));
              if (squats <= 0) {
                giveTimeTicket(player);
                squatCounts.put(squatLoc, goal);
                setDisplayText(
                    squatDisplays.get(squatLoc),
                    Component.text("Squats left: " + goal, NamedTextColor.YELLOW));
              }
            }
            break;
          }
        }
      }
    }
    lastSneakState.put(uuid, isSneaking);
  }
}
