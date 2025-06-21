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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
  private final Map<Location, Integer> treadmillCounts = new HashMap<>();
  private final Map<Location, Integer> trampolineCounts = new HashMap<>();
  private final int goal = 350;
  private final Map<Location, String> punchingBagHologramNames = new HashMap<>();
  private final Map<Location, String> treadmillHologramNames = new HashMap<>();
  private final Map<Location, String> trampolineHologramNames = new HashMap<>();
  private final List<Cuboid> treadmillCuboids = new ArrayList<>();
  private final Map<UUID, Location> lastTrampoline = new HashMap<>();
  private final Map<Location, Integer> squatCounts = new HashMap<>();
  private final Map<Location, String> squatHologramNames = new HashMap<>();
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
    List<String> punchingBagHolograms = new ArrayList<>();
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

      // Use center of cuboid for tracking/hologram
      int centerX = (cuboid.x1 + cuboid.x2) / 2;
      int minY = cuboid.y1;
      int centerZ = (cuboid.z1 + cuboid.z2) / 2;
      Location base = new Location(world, centerX, minY, centerZ);
      punchingBagCuboidToBase.put(cuboid, base);

      String holoName = (String) config.getGameProperties().get("punchingBagHologram" + i);
      if (holoName != null) {
        punchingBagHolograms.add(holoName);
      }
    }

    int treadmillCount =
        Integer.parseInt(config.getGameProperties().get("treadmillCount").toString());
    treadmillCuboids.clear();
    List<String> treadmillHolograms = new ArrayList<>();
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
      treadmillCuboids.add(new Cuboid(world, x1, y1, z1, x2, y2, z2));

      String holoName = (String) config.getGameProperties().get("treadmillHologram" + i);
      if (holoName != null) {
        treadmillHolograms.add(holoName);
      }
    }

    // --- Trampolines ---
    int trampolineCount =
        Integer.parseInt(config.getGameProperties().get("trampolineCount").toString());
    List<Cuboid> trampolineCuboids = new ArrayList<>();
    List<String> trampolineHolograms = new ArrayList<>();
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
      trampolineCuboids.add(new Cuboid(world, x1, y1, z1, x2, y2, z2));

      String holoName = (String) config.getGameProperties().get("trampolineHologram" + i);
      if (holoName != null) {
        trampolineHolograms.add(holoName);
      }
    }

    // --- Squat Racks ---
    int squatCount =
        config.getGameProperties().containsKey("squatCount")
            ? Integer.parseInt(config.getGameProperties().get("squatCount").toString())
            : 0;
    List<String> squatHolograms = new ArrayList<>();
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

      // Use center of cuboid for hologram and tracking
      int centerX = (cuboid.x1 + cuboid.x2) / 2;
      int minY = cuboid.y1;
      int centerZ = (cuboid.z1 + cuboid.z2) / 2;
      Location squatLoc = new Location(world, centerX, minY, centerZ);
      squatCounts.put(squatLoc, goal);

      String holoName = (String) config.getGameProperties().get("squatHologram" + i);
      if (holoName != null) {
        squatHolograms.add(holoName);
      } else {
        holoName = "gym-squat-" + i;
      }
      squatHologramNames.put(squatLoc, holoName);
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &eSquats left: " + goal);
    }

    // --- Place blocks and holograms as before using these lists ---
    for (int i = 0; i < punchingBagCuboids.size(); i++) {
      Cuboid cuboid = punchingBagCuboids.get(i);
      Location base = punchingBagCuboidToBase.get(cuboid);
      punchingBagCounts.put(base, goal);
      String holoName =
          punchingBagHolograms.size() > i ? punchingBagHolograms.get(i) : "gym-punchingbag-" + i;
      punchingBagHologramNames.put(base, holoName);
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &ePunches left: " + goal);
    }

    // --- Treadmills ---
    for (int i = 0; i < treadmillCuboids.size(); i++) {
      Cuboid cuboid = treadmillCuboids.get(i);
      int backX = cuboid.x1;
      int backY = cuboid.y1;
      int backZ = cuboid.z1;
      World world = cuboid.world;
      Location back = new Location(world, backX, backY, backZ);
      treadmillCounts.put(back, 0);
      String holoName =
          treadmillHolograms.size() > i ? treadmillHolograms.get(i) : "gym-treadmill-" + i;
      treadmillHologramNames.put(back, holoName);
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &eMeters: 0/" + goal);
    }

    // --- Trampolines ---
    for (int i = 0; i < trampolineCuboids.size(); i++) {
      Cuboid cuboid = trampolineCuboids.get(i);
      int centerX = (cuboid.x1 + cuboid.x2) / 2;
      int minY = cuboid.y1;
      int centerZ = (cuboid.z1 + cuboid.z2) / 2;
      World world = cuboid.world;
      Location loc = new Location(world, centerX, minY, centerZ);
      trampolineCounts.put(loc, goal);
      String holoName =
          trampolineHolograms.size() > i ? trampolineHolograms.get(i) : "gym-trampoline-" + i;
      trampolineHologramNames.put(loc, holoName);
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &eJumps left: " + goal);
    }

    // --- Squat Racks ---
    for (int i = 0; i < squatCuboids.size(); i++) {
      Cuboid cuboid = squatCuboids.get(i);
      int centerX = (cuboid.x1 + cuboid.x2) / 2;
      int minY = cuboid.y1;
      int centerZ = (cuboid.z1 + cuboid.z2) / 2;
      World world = cuboid.world;
      Location loc = new Location(world, centerX, minY, centerZ);
      squatCounts.put(loc, goal);
      String holoName = squatHolograms.size() > i ? squatHolograms.get(i) : "gym-squat-" + i;
      squatHologramNames.put(loc, holoName);
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &eSquats left: " + goal);
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
    resetAllCounts();
    squatCounts.clear();
    squatHologramNames.clear();
    squatCuboids.clear();
    lastSneakState.clear();
  }

  /**
   * Handles player interactions with punching bags.
   *
   * @param event the PlayerInteractEvent triggered by a player
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
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
        String holoName = punchingBagHologramNames.get(base);
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            "dh l set " + holoName + " 1 1 &ePunches left: " + (left > 0 ? left : 0));
        if (left <= 0) {
          Player player = event.getPlayer();
          giveTimeTicket(player);
          Bukkit.dispatchCommand(
              Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &ePunches left: " + goal);
          punchingBagCounts.put(base, goal);
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
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
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
        int meters = treadmillCounts.getOrDefault(back, 0) + 1;
        treadmillCounts.put(back, meters);
        String holoName =
            treadmillHologramNames.containsKey(back)
                ? treadmillHologramNames.get(back)
                : "gym-treadmill-" + i;
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            "dh l set " + holoName + " 1 1 &eMeters: " + meters + "/" + goal);
        // Teleport player to back block, preserving yaw and pitch
        Location tp = back.clone().add(0.5, 0.5, 0.5);
        tp.setYaw(to.getYaw());
        tp.setPitch(to.getPitch());
        player.teleport(tp);
        if (meters >= goal) {
          giveTimeTicket(player);
          treadmillCounts.put(back, 0);
          Bukkit.dispatchCommand(
              Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &eMeters: 0/" + goal);
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
          String holoName = trampolineHologramNames.get(tramp);
          Bukkit.dispatchCommand(
              Bukkit.getConsoleSender(),
              "dh l set " + holoName + " 1 1 &eJumps left: " + (jumps > 0 ? jumps : 0));
          if (jumps <= 0) {
            giveTimeTicket(player);
            trampolineCounts.put(tramp, goal);
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &eJumps left: " + goal);
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

  /** Resets all gym station counts and holograms back to the goal value. */
  public void resetAllCounts() {
    // Reset punching bags
    for (Map.Entry<Location, Integer> entry : punchingBagCounts.entrySet()) {
      punchingBagCounts.put(entry.getKey(), goal);
      String holoName = punchingBagHologramNames.get(entry.getKey());
      if (holoName != null) {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &ePunches left: " + goal);
      }
    }
    // Reset treadmills
    for (Map.Entry<Location, Integer> entry : treadmillCounts.entrySet()) {
      treadmillCounts.put(entry.getKey(), 0);
      String holoName = treadmillHologramNames.get(entry.getKey());
      if (holoName != null) {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &eMeters: 0/" + goal);
      }
    }
    // Reset trampolines
    for (Map.Entry<Location, Integer> entry : trampolineCounts.entrySet()) {
      trampolineCounts.put(entry.getKey(), goal);
      String holoName = trampolineHologramNames.get(entry.getKey());
      if (holoName != null) {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &eJumps left: " + goal);
      }
    }
    // Reset squats
    for (Map.Entry<Location, Integer> entry : squatCounts.entrySet()) {
      squatCounts.put(entry.getKey(), goal);
      String holoName = squatHologramNames.get(entry.getKey());
      if (holoName != null) {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(), "dh l set " + holoName + " 1 1 &eSquats left: " + goal);
      }
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
  @EventHandler
  public void onPlayerToggleSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
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
              String holoName = squatHologramNames.get(squatLoc);
              Bukkit.dispatchCommand(
                  Bukkit.getConsoleSender(),
                  "dh l set " + holoName + " 1 1 &eSquats left: " + (squats > 0 ? squats : 0));
              if (squats <= 0) {
                giveTimeTicket(player);
                squatCounts.put(squatLoc, goal);
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "dh l set " + holoName + " 1 1 &eSquats left: " + goal);
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
