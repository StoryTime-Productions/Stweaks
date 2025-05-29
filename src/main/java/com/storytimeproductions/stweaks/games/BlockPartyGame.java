package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents a Block Party game where players must stand on the correct colored blocks. Implements
 * the Minigame interface for game lifecycle management.
 */
public class BlockPartyGame implements Minigame {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final List<Location> platformBlocks = new ArrayList<>();
  private final List<Location> originalPlatformBlocks = new ArrayList<>();

  private boolean inGracePeriod = false;

  private Material currentTargetColor;

  private boolean roundActive = false;

  private int initialPlayerCount = 0;
  private Player winner = null;

  private boolean inPostRemovalDelay = false;
  private int roundNumber = 1;

  private int graceSeconds = 6; // Initial grace period in seconds
  private int graceSecondsCurrent = graceSeconds;
  private int postRemovalDelaySeconds = 3;
  private int postRemovalDelayCurrent = 0;

  /**
   * Constructs a new Block Party game with the specified configuration.
   *
   * @param config the game configuration
   */
  public BlockPartyGame(GameConfig config) {
    this.config = config;
  }

  /** Initializes the BlockParty game. */
  @Override
  public void onInit() {
    initialPlayerCount = players.size();
    roundActive = false;
    inGracePeriod = false;
    inPostRemovalDelay = false;
    roundNumber = 1;
    graceSeconds = 6;
    graceSecondsCurrent = graceSeconds;
    postRemovalDelaySeconds = 3;
    postRemovalDelayCurrent = 0;
    winner = null;
    platformBlocks.clear();
    originalPlatformBlocks.clear();

    Location area = config.getGameArea();
    if (area == null) {
      return;
    }

    World world = area.getWorld();
    if (world == null) {
      return;
    }

    int minX = area.getBlockX() - 4;
    int maxX = area.getBlockX() + 4;
    int minZ = area.getBlockZ() - 4;
    int maxZ = area.getBlockZ() + 4;
    int y = area.getBlockY();

    Material[] colors = {
      Material.WHITE_CONCRETE,
      Material.ORANGE_CONCRETE,
      Material.MAGENTA_CONCRETE,
      Material.LIGHT_BLUE_CONCRETE,
      Material.YELLOW_CONCRETE,
      Material.LIME_CONCRETE,
      Material.PINK_CONCRETE,
      Material.GRAY_CONCRETE,
      Material.LIGHT_GRAY_CONCRETE,
      Material.CYAN_CONCRETE,
      Material.PURPLE_CONCRETE,
      Material.BLUE_CONCRETE,
      Material.BROWN_CONCRETE,
      Material.GREEN_CONCRETE,
      Material.RED_CONCRETE,
      Material.BLACK_CONCRETE
    };
    Random rand = new Random();

    platformBlocks.clear();
    originalPlatformBlocks.clear(); // Only clear here, never again!
    for (int x = minX; x <= maxX; x += 2) {
      for (int z = minZ; z <= maxZ; z += 2) {
        Material color = colors[rand.nextInt(colors.length)];
        for (int dx = 0; dx < 2; dx++) {
          for (int dz = 0; dz < 2; dz++) {
            int bx = x + dx;
            int bz = z + dz;
            Block block = world.getBlockAt(bx, y, bz);
            if (block.getType() != Material.AIR && block.getType() != Material.BARRIER) {
              Location loc = block.getLocation().clone();
              if (!originalPlatformBlocks.contains(loc)) {
                originalPlatformBlocks.add(loc);
              }
              block.setType(color);
              platformBlocks.add(loc);
            }
          }
        }
      }
    }
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {
    // Send welcome and instruction titles to all players in the game area
    for (Player player : players) {
      Location area = config.getGameArea();

      if (player.getLocation().distance(area) < 10) {
        player.showTitle(
            Title.title(
                Component.text("Block Party!").color(NamedTextColor.AQUA),
                Component.text("Stand on the correct color!"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));
        // Delay for 2 seconds, then show the main instruction
        Bukkit.getScheduler()
            .runTaskLater(
                Bukkit.getPluginManager().getPlugin("STweaks"),
                () ->
                    player.showTitle(
                        Title.title(
                            Component.text("Get Ready!").color(NamedTextColor.YELLOW),
                            Component.text(""),
                            Title.Times.times(
                                Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500)))),
                40L);
      }
    }

    startNextRound();
  }

  @Override
  public void update() {
    if (!roundActive) {
      return;
    }

    // Grace period countdown: show action bar, decrement every second
    if (inGracePeriod) {
      if (graceSecondsCurrent > 0) {
        // Action bar handled in render()
        graceSecondsCurrent--;
        return;
      } else {
        // Grace period over, remove non-target blocks and start post-removal delay
        inGracePeriod = false;
        removeNonTargetBlocks();
        postRemovalDelayCurrent = postRemovalDelaySeconds;
        inPostRemovalDelay = true;
        return;
      }
    }

    // Post-removal delay: let players fall before elimination
    if (inPostRemovalDelay) {
      if (postRemovalDelayCurrent > 0) {
        postRemovalDelayCurrent--;
        checkPlayers();
        return;
      }
      inPostRemovalDelay = false;
      startNextRound();
      return;
    }
  }

  // Add this method to regenerate the floor with random colors
  private void regenerateFloor() {
    if (originalPlatformBlocks.isEmpty()) {
      return;
    }

    Material[] colors = {
      Material.WHITE_CONCRETE,
      Material.ORANGE_CONCRETE,
      Material.MAGENTA_CONCRETE,
      Material.LIGHT_BLUE_CONCRETE,
      Material.YELLOW_CONCRETE,
      Material.LIME_CONCRETE,
      Material.PINK_CONCRETE,
      Material.GRAY_CONCRETE,
      Material.LIGHT_GRAY_CONCRETE,
      Material.CYAN_CONCRETE,
      Material.PURPLE_CONCRETE,
      Material.BLUE_CONCRETE,
      Material.BROWN_CONCRETE,
      Material.GREEN_CONCRETE,
      Material.RED_CONCRETE,
      Material.BLACK_CONCRETE
    };
    Random rand = new Random();
    platformBlocks.clear();

    // 1. Pick a random location to be the target block
    int targetIndex = rand.nextInt(originalPlatformBlocks.size());
    Location targetLoc = originalPlatformBlocks.get(targetIndex);

    // 2. Pick a random color for the target
    Material targetColor = colors[rand.nextInt(colors.length)];

    // 3. Assign colors to all blocks, ensuring only one block has the target color
    // and no two adjacent blocks have the same color
    for (Location loc : originalPlatformBlocks) {
      Block block = loc.getWorld().getBlockAt(loc);
      Material color;
      if (loc.equals(targetLoc)) {
        color = targetColor;
      } else {
        // Pick a color that is not the target color and not the same as any adjacent
        // block
        List<Material> possibleColors = new ArrayList<>(List.of(colors));
        possibleColors.remove(targetColor);

        // Check adjacent blocks (N, S, E, W)
        for (int[] offset : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
          Location adj = loc.clone().add(offset[0], 0, offset[1]);
          if (platformBlocks.contains(adj)) {
            Block adjBlock = adj.getWorld().getBlockAt(adj);
            possibleColors.remove(adjBlock.getType());
          }
        }
        // Fallback in case all colors are removed (shouldn't happen with 15+ colors)
        if (possibleColors.isEmpty()) {
          possibleColors.addAll(List.of(colors));
          possibleColors.remove(targetColor);
        }
        color = possibleColors.get(rand.nextInt(possibleColors.size()));
      }
      block.setType(color);
      platformBlocks.add(loc.clone());
    }

    // Set the current target color and location for this round
    currentTargetColor = targetColor;
  }

  // In your startNextRound() or update() method, decrease graceTicks every 3
  // rounds:

  private void startNextRound() {
    regenerateFloor();

    // Pick a random color from the platform
    List<Material> presentColors =
        platformBlocks.stream()
            .map(loc -> loc.getBlock().getType())
            .distinct()
            .filter(mat -> mat.name().endsWith("_CONCRETE"))
            .toList();

    if (presentColors.isEmpty()) {
      roundActive = false;
      Bukkit.getLogger()
          .warning("[BlockParty] No concrete colors left on the platform. Ending game.");
      return;
    }

    currentTargetColor = presentColors.get(new Random().nextInt(presentColors.size()));
    givePlayersTargetBlock();
    roundActive = true;
    inGracePeriod = true;
    inPostRemovalDelay = false;

    // Every 3rd round, decrease grace period by 2 seconds, min 1
    if (roundNumber % 3 == 0 && graceSeconds > 1) {
      graceSeconds = Math.max(1, graceSeconds - 2);
    }
    graceSecondsCurrent = graceSeconds;
    roundNumber++;

    // Play round start sound for all players
    for (Player player : players) {
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
    }
  }

  private void givePlayersTargetBlock() {
    for (Player player : players) {
      ItemStack item = new ItemStack(currentTargetColor, 1);
      player.getInventory().setItem(4, item);
      player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f);
    }
  }

  private void removeNonTargetBlocks() {
    for (Location loc : platformBlocks) {
      Block block = loc.getBlock();
      if (block.getType() != currentTargetColor) {
        block.setType(Material.AIR);
      }
    }
    // Play block removal sound for all players
    for (Player player : players) {
      player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.7f, 1.1f);
    }
  }

  private void checkPlayers() {
    Location exit = config.getExitArea();
    int platformY = config.getGameArea().getBlockY();
    for (Player player : new ArrayList<>(players)) {
      if (player.getLocation().getBlockY() < platformY) {
        player.teleport(exit);
        player.sendMessage(Component.text("You lost!", NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        removeTargetBlockFromPlayer(player);
        players.remove(player);
      }
    }
    // If only one player remains, set as winner
    if (players.size() == 1) {
      winner = players.get(0);
      removeTargetBlockFromPlayer(winner);
      roundActive = false;
    }
  }

  // Called every tick by the game manager
  @Override
  public void render() {
    if (!roundActive) {
      return;
    }
    String colorName = currentTargetColor.name().replace("_CONCRETE", "").replace("_", " ");
    NamedTextColor color = getNamedTextColor(currentTargetColor);

    for (Player player : players) {
      if (inGracePeriod) {
        player.sendActionBar(
            Component.text("Stand on: ", NamedTextColor.GRAY)
                .append(Component.text(colorName, color, TextDecoration.BOLD))
                .append(Component.text(" (" + graceSecondsCurrent + "s)", NamedTextColor.YELLOW)));
      } else if (inPostRemovalDelay) {
        player.sendActionBar(
            Component.text(
                "Blocks vanished! Next round in " + postRemovalDelayCurrent + "s",
                NamedTextColor.RED));
      } else {
        player.sendActionBar(Component.text("", NamedTextColor.GRAY));
      }
    }
  }

  // Helper to get NamedTextColor from Material
  private NamedTextColor getNamedTextColor(Material mat) {
    switch (mat) {
      case WHITE_CONCRETE:
        return NamedTextColor.WHITE;
      case ORANGE_CONCRETE:
        return NamedTextColor.GOLD;
      case MAGENTA_CONCRETE:
        return NamedTextColor.LIGHT_PURPLE;
      case LIGHT_BLUE_CONCRETE:
        return NamedTextColor.AQUA;
      case YELLOW_CONCRETE:
        return NamedTextColor.YELLOW;
      case LIME_CONCRETE:
        return NamedTextColor.GREEN;
      case PINK_CONCRETE:
        return NamedTextColor.LIGHT_PURPLE;
      case GRAY_CONCRETE:
        return NamedTextColor.DARK_GRAY;
      case LIGHT_GRAY_CONCRETE:
        return NamedTextColor.GRAY;
      case CYAN_CONCRETE:
        return NamedTextColor.DARK_AQUA;
      case PURPLE_CONCRETE:
        return NamedTextColor.DARK_PURPLE;
      case BLUE_CONCRETE:
        return NamedTextColor.BLUE;
      case BROWN_CONCRETE:
        return NamedTextColor.GOLD;
      case GREEN_CONCRETE:
        return NamedTextColor.DARK_GREEN;
      case RED_CONCRETE:
        return NamedTextColor.RED;
      case BLACK_CONCRETE:
        return NamedTextColor.BLACK;
      default:
        return NamedTextColor.WHITE;
    }
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
    // Game should quit if no players or only one winner left
    if (players.size() <= 1) {
      return true;
    }
    return false;
  }

  /**
   * Adds the specified player to the game.
   *
   * @param player the player to add
   */
  @Override
  public void join(Player player) {
    players.add(player);
  }

  /**
   * Removes the specified player from the game.
   *
   * @param player the player to remove
   */
  @Override
  public void leave(Player player) {
    players.remove(player);
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    regenerateFloor();

    if (winner != null) {
      ItemStack tickets = new ItemStack(Material.NAME_TAG, initialPlayerCount);
      tickets.getItemMeta().displayName(Component.text("Time Ticket").color(NamedTextColor.GOLD));
      ItemMeta meta = tickets.getItemMeta();
      meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
      meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
      meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      tickets.setItemMeta(meta);
      winner.getInventory().addItem(tickets);
      winner.sendMessage(
          Component.text(
              "You win! +" + initialPlayerCount + " Time Tickets!", NamedTextColor.GOLD));
      winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

      Location exit = config.getExitArea();
      if (exit != null) {
        winner.teleport(exit);
      }
      winner = null;
    }
    players.clear();
  }

  private void removeTargetBlockFromPlayer(Player player) {
    for (int i = 0; i < player.getInventory().getSize(); i++) {
      ItemStack item = player.getInventory().getItem(i);
      if (item != null && item.getType() == currentTargetColor) {
        player.getInventory().setItem(i, null);
      }
    }
  }
}
