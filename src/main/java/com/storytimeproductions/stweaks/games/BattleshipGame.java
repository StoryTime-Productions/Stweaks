package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/** Represents a Battleship game where players place ships on a board and take turns. */
public class BattleshipGame implements Minigame, Listener {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Map<Player, Integer> terracottaPlaced = new HashMap<>();
  private final Map<Player, Boolean> boardReady = new HashMap<>();
  private final Map<Player, List<List<Location>>> playerShipsCoords = new HashMap<>();
  private final Map<Player, List<List<int[]>>> playerShipsCoordsRelative = new HashMap<>();
  private final Material shipBlock = Material.GREEN_GLAZED_TERRACOTTA;
  private final Material boardBlock = Material.LAPIS_BLOCK;
  private final Material missBlock = Material.DIORITE_WALL;
  private final Material hitBlock = Material.RED_SANDSTONE_WALL;
  private BukkitTask startTimerTask = null;
  private boolean gameInProgress = false;
  private int currentPlayerIndex = 0;

  // Add this field to your class to store the public board center
  private Location publicBoardCenter;

  // Add this field to your class:
  private String boardDirection;

  // Track the number of hits for each player
  private final Map<Player, Integer> playerHitCount = new HashMap<>();

  // Track if a winner has been determined
  private boolean winnerDetermined = false;

  /**
   * Constructs a new Battleship game with the specified configuration.
   *
   * @param config the game configuration
   */
  public BattleshipGame(GameConfig config) {
    this.config = config;
  }

  /** Initializes the Battleship game. */
  @Override
  public void onInit() {
    terracottaPlaced.clear();
    boardReady.clear();
    playerShipsCoords.clear();
    gameInProgress = false;
    currentPlayerIndex = 0;
    playerHitCount.clear(); // Initialize hit count
    if (startTimerTask != null) {
      startTimerTask.cancel();
      startTimerTask = null;
    }
  }

  private void clearAllWalls() {
    // Clear walls from both personal boards
    for (Player p : players) {
      Location boardCenter = getPlayerBoardCenter(p);
      int centerX = boardCenter.getBlockX();
      int centerZ = boardCenter.getBlockZ();
      int y = boardCenter.getBlockY() + 1; // walls are one above the board
      for (int dx = -3; dx <= 3; dx++) {
        for (int dz = -3; dz <= 3; dz++) {
          Block b = boardCenter.getWorld().getBlockAt(centerX + dx, y, centerZ + dz);
          if (b.getType() == hitBlock || b.getType() == missBlock) {
            b.setType(Material.AIR);
          }
        }
      }
    }

    // Clear walls from both sides of the public board
    int pubX = publicBoardCenter.getBlockX();
    int pubY = publicBoardCenter.getBlockY();
    int pubZ = publicBoardCenter.getBlockZ();
    if (boardDirection.equals("north") || boardDirection.equals("south")) {
      // X is width, Y is height, Z is fixed
      for (int dx = -3; dx <= 3; dx++) {
        for (int dy = -3; dy <= 3; dy++) {
          // South side (lower Z)
          Block b1 = publicBoardCenter.getWorld().getBlockAt(pubX + dx, pubY + dy, pubZ - 1);
          if (b1.getType() == hitBlock || b1.getType() == missBlock) {
            b1.setType(Material.AIR);
          }
          // North side (higher Z)
          Block b2 = publicBoardCenter.getWorld().getBlockAt(pubX + dx, pubY + dy, pubZ + 1);
          if (b2.getType() == hitBlock || b2.getType() == missBlock) {
            b2.setType(Material.AIR);
          }
        }
      }
    } else {
      // Z is width, Y is height, X is fixed
      for (int dz = -3; dz <= 3; dz++) {
        for (int dy = -3; dy <= 3; dy++) {
          // West side (lower X)
          Block b1 = publicBoardCenter.getWorld().getBlockAt(pubX - 1, pubY + dy, pubZ + dz);
          if (b1.getType() == hitBlock || b1.getType() == missBlock) {
            b1.setType(Material.AIR);
          }
          // East side (higher X)
          Block b2 = publicBoardCenter.getWorld().getBlockAt(pubX + 1, pubY + dy, pubZ + dz);
          if (b2.getType() == hitBlock || b2.getType() == missBlock) {
            b2.setType(Material.AIR);
          }
        }
      }
    }
  }

  // Call this in afterInit() or your setup code to initialize the public board
  // center
  private void setupPublicBoardCenter() {
    String[] parts = config.getGameProperties().get("public-board-center").split(",");
    publicBoardCenter =
        new Location(
            Bukkit.getWorld(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3]));
    boardDirection = config.getGameProperties().getOrDefault("direction", "north").toLowerCase();
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {
    setupPublicBoardCenter();
    for (Player p : players) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fly " + p.getName());

      Location boardCenter = getPlayerBoardCenter(p);
      int centerX = boardCenter.getBlockX();
      int centerZ = boardCenter.getBlockZ();
      int y = boardCenter.getBlockY();
      for (int dx = -3; dx <= 3; dx++) {
        for (int dz = -3; dz <= 3; dz++) {
          Block b = boardCenter.getWorld().getBlockAt(centerX + dx, y, centerZ + dz);
          b.setType(boardBlock);
        }
      }
      p.getInventory().addItem(new ItemStack(shipBlock, 16));
      terracottaPlaced.put(p, 0);
      boardReady.put(p, false);
      p.sendMessage("Place your ships by right-clicking your board with green glazed terracotta!");
      p.sendMessage(
          Component.text(
              "Required ships: 5, 4, 3, 2, 2 blocks (no touching, only straight lines).",
              NamedTextColor.YELLOW));
    }
    clearAllWalls();
  }

  /** Updates the game state. */
  @Override
  public void update() {}

  /** Renders the game state. */
  @Override
  public void render() {}

  /** Called when the game is destroyed or stopped. */
  @Override
  public void onDestroy() {
    // Cancel any running timer tasks
    if (startTimerTask != null) {
      startTimerTask.cancel();
      startTimerTask = null;
    }
    // Clear all wall blocks from boards
    clearAllWalls();

    // Clear all game state and references
    players.clear();
    terracottaPlaced.clear();
    boardReady.clear();
    playerShipsCoords.clear();
    playerShipsCoordsRelative.clear();
    playerHitCount.clear();
    winnerDetermined = false;
    gameInProgress = false;
    currentPlayerIndex = 0;
    publicBoardCenter = null;
    boardDirection = null;
  }

  /** Allows a player to join the game. */
  @Override
  public void join(Player player) {
    if (players.size() >= config.getPlayerLimit()) {
      return;
    }
    if (!players.contains(player)) {
      players.add(player);
      terracottaPlaced.put(player, 0);
      String key = (players.size() == 1) ? "player1-join-area" : "player2-join-area";
      String[] parts = config.getGameProperties().get(key).split(",");
      Location joinLoc =
          new Location(
              Bukkit.getWorld(parts[0]),
              Double.parseDouble(parts[1]),
              Double.parseDouble(parts[2]) + 1,
              Double.parseDouble(parts[3]));
      player.teleport(joinLoc);
    } else {
      player.sendMessage("You are already in the game.");
    }
  }

  /** Allows a player to leave the game. */
  @Override
  public void leave(Player player) {
    players.remove(player);
    terracottaPlaced.remove(player);
    boardReady.remove(player);
    playerShipsCoords.remove(player);
    playerHitCount.remove(player); // Remove player from hit count map
    player.sendMessage("You have left the game.");
  }

  /** Gets the list of players currently in the game. */
  @Override
  public List<Player> getPlayers() {
    return players;
  }

  /** Gets the game configuration. */
  @Override
  public GameConfig getConfig() {
    return config;
  }

  /** Determines if the game should quit. */
  @Override
  public boolean shouldQuit() {
    return winnerDetermined;
  }

  private Location getPlayerBoardCenter(Player player) {
    String key = players.indexOf(player) == 0 ? "player1-join-area" : "player2-join-area";
    String[] parts = config.getGameProperties().get(key).split(",");
    return new Location(
        Bukkit.getWorld(parts[0]),
        Double.parseDouble(parts[1]),
        Double.parseDouble(parts[2]),
        Double.parseDouble(parts[3]));
  }

  // --- Board Placement and Validation ---

  /**
   * Handles player interactions to place or remove ship blocks on the board.
   *
   * @param event the PlayerInteractEvent triggered by the player
   */
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (!players.contains(player)) {
      return;
    }
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Location boardCenter = getPlayerBoardCenter(player);
    Block clicked = event.getClickedBlock();
    if (clicked == null) {
      return;
    }
    ItemStack hand = player.getInventory().getItemInMainHand();

    // Remove ship block
    if (clicked.getType() == shipBlock) {
      clicked.setType(boardBlock);
      player.getInventory().addItem(new ItemStack(shipBlock, 1));
      terracottaPlaced.put(player, terracottaPlaced.get(player) - 1);
      boardReady.put(player, false);
      checkAndStartGameCountdown();
      event.setCancelled(true);
      return;
    }

    // Place ship block
    if (hand.getType() == shipBlock && clicked.getType() == boardBlock) {
      if (terracottaPlaced.get(player) >= 16) {
        player.sendMessage("You have already placed all your ships.");
        return;
      }

      clicked.setType(shipBlock);
      hand.setAmount(hand.getAmount() - 1);
      terracottaPlaced.put(player, terracottaPlaced.get(player) + 1);
      event.setCancelled(true);

      // Check if all 16 placed
      if (terracottaPlaced.get(player) == 16) {
        String error = validateBoard(player, boardCenter);
        if (error == null) {
          boardReady.put(player, true);
          playerShipsCoords.put(player, extractShipsFromBoard(boardCenter));
          playerShipsCoordsRelative.put(player, extractShipsFromBoardRelative(boardCenter));
          player.sendMessage("Your board is ready!");
          player.showTitle(
              Title.title(
                  Component.text("Setup Valid!", NamedTextColor.GREEN),
                  Component.text("All ships placed correctly.", NamedTextColor.GRAY),
                  Title.Times.times(
                      Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000))));
          player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else {
          boardReady.put(player, false);
          player.showTitle(
              Title.title(
                  Component.text("Setup Invalid!", NamedTextColor.RED),
                  Component.text(error, NamedTextColor.GRAY),
                  Title.Times.times(
                      Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000))));
          player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
        checkAndStartGameCountdown();
      }
      return;
    }

    // --- Game phase: Diorite wall placement ---
    if (gameInProgress && player.equals(players.get(currentPlayerIndex))) {
      if (!player.equals(players.get(currentPlayerIndex))) {
        player.sendMessage(Component.text("It's not your turn!", NamedTextColor.RED));
        event.setCancelled(true);
        return;
      }

      if (hand.getType() == missBlock && isOnPublicBoard(clicked.getLocation())) {
        int[] rel = getRelativePublicBoardCoords(clicked.getLocation());

        if (players.indexOf(player) == 1) {
          if (boardDirection.equals("north") || boardDirection.equals("south")) {
            rel[1] = 6 - rel[1];
            rel[0] = 6 - rel[0];
          }
        } else {
          // Player 1's coordinates are already in the correct orientation
          if (boardDirection.equals("north") || boardDirection.equals("south")) {
            rel[0] = 6 - rel[0];
          }
        }

        Player opponent = players.get(1 - currentPlayerIndex);
        boolean hit = false;
        for (int i = 0; i < playerShipsCoordsRelative.get(opponent).size(); i++) {
          List<int[]> ship = playerShipsCoordsRelative.get(opponent).get(i);
          for (int[] coord : ship) {
            if (coord[0] == rel[0] && coord[1] == rel[1]) {
              hit = true;
              break;
            }
          }
          if (hit) {
            break;
          }
        }

        Location wallLoc = clicked.getLocation().clone();
        int offset = 1;
        if (boardDirection.equals("north")) {
          // Player 1 is south, Player 2 is north
          if (players.indexOf(player) == 0) {
            wallLoc.add(0, 0, -offset); // south side (lower Z)
          } else {
            wallLoc.add(0, 0, offset); // north side (higher Z)
          }
        } else if (boardDirection.equals("south")) {
          // Player 1 is north, Player 2 is south
          if (players.indexOf(player) == 0) {
            wallLoc.add(0, 0, offset); // north side (higher Z)
          } else {
            wallLoc.add(0, 0, -offset); // south side (lower Z)
          }
        } else if (boardDirection.equals("east")) {
          // Player 1 is west, Player 2 is east
          if (players.indexOf(player) == 0) {
            wallLoc.add(offset, 0, 0); // east side (higher X)
          } else {
            wallLoc.add(-offset, 0, 0); // west side (lower X)
          }
        } else if (boardDirection.equals("west")) {
          // Player 1 is east, Player 2 is west
          if (players.indexOf(player) == 0) {
            wallLoc.add(-offset, 0, 0); // west side (lower X)
          } else {
            wallLoc.add(offset, 0, 0); // east side (higher X)
          }
        }

        // After placing the wall (hit or miss) on the public board and opponent's
        // board:
        Block wallBlock =
            wallLoc
                .getWorld()
                .getBlockAt(wallLoc.getBlockX(), wallLoc.getBlockY(), wallLoc.getBlockZ());
        wallBlock.setType(hit ? hitBlock : missBlock);

        // Remove one diorite wall from the player's inventory after placing
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == missBlock) {
          handItem.setAmount(handItem.getAmount() - 1);
        } else {
          // Fallback: remove one from inventory if not in main hand
          player.getInventory().removeItem(new ItemStack(missBlock, 1));
        }

        // Place a wall (hit or miss) on the opponent's personal board, one level above
        // their board
        int mappedX = rel[0];
        int mappedZ = rel[1];

        if (boardDirection.equals("north") || boardDirection.equals("south")) {
          if (players.indexOf(player) == 0) { // Player 1
            mappedX = 6 - mappedX;
          }
        } else {
          if (players.indexOf(player) == 0) { // Player 1
            mappedZ = 6 - mappedZ;
          }
        }

        Location opponentBoardCenter = getPlayerBoardCenter(opponent);
        int oppX;
        int oppZ;
        if (boardDirection.equals("north") || boardDirection.equals("south")) {
          oppX = opponentBoardCenter.getBlockX() + mappedX - 3;
          oppZ = opponentBoardCenter.getBlockZ() + mappedZ - 3;
        } else {
          // For east/west, swap axes
          oppX = opponentBoardCenter.getBlockX() + mappedZ - 3;
          oppZ = opponentBoardCenter.getBlockZ() + mappedX - 3;
        }
        int oppY = opponentBoardCenter.getBlockY() + 1; // one level above
        Block oppBlock = opponentBoardCenter.getWorld().getBlockAt(oppX, oppY, oppZ);
        oppBlock.setType(hit ? hitBlock : missBlock);

        if (hit) {
          // Increment hit count for the current player
          playerHitCount.put(player, playerHitCount.getOrDefault(player, 0) + 1);

          player.showTitle(
              Title.title(
                  Component.text("Hit!", NamedTextColor.RED),
                  Component.text(""),
                  Title.Times.times(
                      Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000))));

          // Check for win (16 hits)
          if (playerHitCount.get(player) >= 16) {
            broadcastToPlayers(player.getName() + " wins!");
            rewardWinner(player);
            winnerDetermined = true; // <-- Set winner flag
            quitGame();
            return;
          }
        } else {
          player.showTitle(
              Title.title(
                  Component.text("Miss!", NamedTextColor.GRAY),
                  Component.text("", NamedTextColor.GRAY),
                  Title.Times.times(
                      Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000))));
        }
        // Next turn
        currentPlayerIndex = 1 - currentPlayerIndex;
        promptPlayerForMove(players.get(currentPlayerIndex));
      }
    }
  }

  // --- Validation and Ship Extraction ---
  private String validateBoard(Player player, Location boardCenter) {
    int size = 7;
    int[][] board = new int[size][size];
    int centerX = boardCenter.getBlockX();
    int centerZ = boardCenter.getBlockZ();
    int y = boardCenter.getBlockY();

    for (int dx = -3; dx <= 3; dx++) {
      for (int dz = -3; dz <= 3; dz++) {
        Block b = boardCenter.getWorld().getBlockAt(centerX + dx, y, centerZ + dz);
        board[dx + 3][dz + 3] = (b.getType() == shipBlock) ? 1 : 0;
      }
    }

    List<Integer> foundShips = new ArrayList<>();
    boolean[][] visited = new boolean[size][size];

    for (int x = 0; x < size; x++) {
      for (int z = 0; z < size; z++) {
        if (board[x][z] == 1 && !visited[x][z]) {
          int lenH = 1;
          int zz = z + 1;
          while (zz < size && board[x][zz] == 1 && !visited[x][zz]) {
            lenH++;
            zz++;
          }
          int lenV = 1;
          int xx = x + 1;
          while (xx < size && board[xx][z] == 1 && !visited[xx][z]) {
            lenV++;
            xx++;
          }
          if (lenH > 1 && lenV > 1) {
            return "L-shaped ship detected.";
          }
          int len = 1;
          boolean isHorizontal = false;
          if (lenH > 1) {
            len = lenH;
            isHorizontal = true;
          } else if (lenV > 1) {
            len = lenV;
          }
          // Mark visited first
          for (int k = 0; k < len; k++) {
            int cx = isHorizontal ? x : x + k;
            int cz = isHorizontal ? z + k : z;
            visited[cx][cz] = true;
          }
          // Now check for touching
          for (int k = 0; k < len; k++) {
            int cx = isHorizontal ? x : x + k;
            int cz = isHorizontal ? z + k : z;
            for (int dx = -1; dx <= 1; dx++) {
              for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                  continue;
                }
                int nx = cx + dx;
                int nz = cz + dz;
                if (nx >= 0 && nx < size && nz >= 0 && nz < size) {
                  if (board[nx][nz] == 1 && !visited[nx][nz]) {
                    return "Ships touching!";
                  }
                }
              }
            }
          }
          if (len < 2) {
            return "Found a ship that is too short (must be at least 2 blocks)";
          }
          if (len > 5) {
            return "Found a ship that is too long (max 5 blocks)";
          }
          foundShips.add(len);
          if (isHorizontal) {
            z += len - 1;
          }
        }
      }
    }

    List<Integer> required = Arrays.asList(5, 4, 3, 2, 2);
    Collections.sort(foundShips);
    Collections.sort(required);
    if (!foundShips.equals(required)) {
      List<Integer> missing = new ArrayList<>(required);
      List<Integer> excess = new ArrayList<>(foundShips);
      for (Integer shipLen : foundShips) {
        missing.remove(shipLen);
      }
      for (Integer reqLen : required) {
        excess.remove(reqLen);
      }
      StringBuilder sb = new StringBuilder("Incorrect ships: ");
      if (!missing.isEmpty()) {
        sb.append("Missing sizes ").append(missing).append(". ");
      }
      if (!excess.isEmpty()) {
        sb.append("Excess sizes ").append(excess).append(". ");
      }
      return sb.toString().trim();
    }
    return null; // valid
  }

  private List<List<Location>> extractShipsFromBoard(Location boardCenter) {
    int size = 7;
    int[][] board = new int[size][size];
    int centerX = boardCenter.getBlockX();
    int centerZ = boardCenter.getBlockZ();
    int y = boardCenter.getBlockY();

    for (int dx = -3; dx <= 3; dx++) {
      for (int dz = -3; dz <= 3; dz++) {
        Block b = boardCenter.getWorld().getBlockAt(centerX + dx, y, centerZ + dz);
        board[dx + 3][dz + 3] = (b.getType() == shipBlock) ? 1 : 0;
      }
    }

    boolean[][] visited = new boolean[size][size];
    List<List<Location>> ships = new ArrayList<>();

    int playerIdx = players.indexOf(getPlayerByBoardCenter(boardCenter));
    String dir = boardDirection;

    // Helper to map logical (row,col) to world (x,z) for each player/direction
    BiFunction<Integer, Integer, int[]> logicalToWorld =
        (row, col) -> {
          int wx = centerX;
          int wz = centerZ;
          if (dir.equals("north")) {
            // Player 1: top=north, Player 2: top=south (mirror Z)
            if (playerIdx == 0) {
              wx += col - 3;
              wz += row - 3;
            } else {
              wx += 6 - col - 3;
              wz += 6 - row - 3;
            }
          } else if (dir.equals("east")) {
            // Player 1: top=east (mirror X), Player 2: top=west
            if (playerIdx == 0) {
              wx += 6 - row - 3;
              wz += col - 3;
            } else {
              wx += row - 3;
              wz += 6 - col - 3;
            }
          }
          return new int[] {wx, wz};
        };

    for (int row = 0; row < size; row++) {
      for (int col = 0; col < size; col++) {
        if (board[row][col] == 1 && !visited[row][col]) {
          List<Location> ship = new ArrayList<>();
          int lenH = 1;
          int cc = col + 1;
          while (cc < size && board[row][cc] == 1 && !visited[row][cc]) {
            lenH++;
            cc++;
          }
          int lenV = 1;
          int rr = row + 1;
          while (rr < size && board[rr][col] == 1 && !visited[rr][col]) {
            lenV++;
            rr++;
          }
          boolean isHorizontal = lenH > 1;
          int len = isHorizontal ? lenH : lenV;
          for (int k = 0; k < len; k++) {
            int r = isHorizontal ? row : row + k;
            int c = isHorizontal ? col + k : col;
            visited[r][c] = true;
            int[] wxz = logicalToWorld.apply(r, c);
            ship.add(new Location(boardCenter.getWorld(), wxz[0], y, wxz[1]));
          }
          ships.add(ship);
          if (isHorizontal) {
            col += len - 1;
          }
        }
      }
    }
    return ships;
  }

  // Helper to get the player by their board center
  private Player getPlayerByBoardCenter(Location boardCenter) {
    for (Player p : players) {
      Location c = getPlayerBoardCenter(p);
      if (c.getWorld().equals(boardCenter.getWorld())
          && c.getBlockX() == boardCenter.getBlockX()
          && c.getBlockY() == boardCenter.getBlockY()
          && c.getBlockZ() == boardCenter.getBlockZ()) {
        return p;
      }
    }
    return null;
  }

  private List<List<int[]>> extractShipsFromBoardRelative(Location boardCenter) {
    int size = 7;
    int[][] board = new int[size][size];
    int centerX = boardCenter.getBlockX();
    int centerZ = boardCenter.getBlockZ();
    int y = boardCenter.getBlockY();

    for (int dx = -3; dx <= 3; dx++) {
      for (int dz = -3; dz <= 3; dz++) {
        Block b = boardCenter.getWorld().getBlockAt(centerX + dx, y, centerZ + dz);
        board[dx + 3][dz + 3] = (b.getType() == shipBlock) ? 1 : 0;
      }
    }

    boolean[][] visited = new boolean[size][size];
    List<List<int[]>> ships = new ArrayList<>();

    int playerIdx = players.indexOf(getPlayerByBoardCenter(boardCenter));
    String dir = boardDirection;

    // Helper to map logical (row,col) to stored (relative) coordinates for each
    // player/direction
    BiFunction<Integer, Integer, int[]> logicalToRelative =
        (row, col) -> {
          int relRow = row;
          int relCol = col;
          if ((dir.equals("north") || dir.equals("south")) && playerIdx == 1) {
            // Mirror Z axis for player 2 if board is north/south
            relRow = size - 1 - row;
          }
          // For east/west, you can add similar logic if you want to mirror X for player 2
          return new int[] {relRow, relCol};
        };

    for (int row = 0; row < size; row++) {
      for (int col = 0; col < size; col++) {
        if (board[row][col] == 1 && !visited[row][col]) {
          List<int[]> ship = new ArrayList<>();
          int lenH = 1;
          int cc = col + 1;
          while (cc < size && board[row][cc] == 1 && !visited[row][cc]) {
            lenH++;
            cc++;
          }
          int lenV = 1;
          int rr = row + 1;
          while (rr < size && board[rr][col] == 1 && !visited[rr][col]) {
            lenV++;
            rr++;
          }
          boolean isHorizontal = lenH > 1;
          int len = isHorizontal ? lenH : lenV;
          for (int k = 0; k < len; k++) {
            int r = isHorizontal ? row : row + k;
            int c = isHorizontal ? col + k : col;
            visited[r][c] = true;
            ship.add(logicalToRelative.apply(r, c));
          }
          ships.add(ship);
          if (isHorizontal) {
            col += len - 1;
          }
        }
      }
    }
    return ships;
  }

  // --- Game Start and Turn Management ---
  private void checkAndStartGameCountdown() {
    if (players.size() < 2) {
      return;
    }
    if (Boolean.TRUE.equals(boardReady.get(players.get(0)))
        && Boolean.TRUE.equals(boardReady.get(players.get(1)))) {
      if (startTimerTask == null) {
        broadcastToPlayers("Both boards are valid! Game will start in 5 seconds...");
        startTimerTask =
            new BukkitRunnable() {
              @Override
              public void run() {
                if (Boolean.TRUE.equals(boardReady.get(players.get(0)))
                    && Boolean.TRUE.equals(boardReady.get(players.get(1)))) {
                  startGame();
                } else {
                  broadcastToPlayers("A board became invalid. Countdown cancelled.");
                }
                startTimerTask = null;
              }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("stweaks"), 100L);
      }
    } else if (startTimerTask != null) {
      broadcastToPlayers("A board became invalid. Countdown cancelled.");
      startTimerTask.cancel();
      startTimerTask = null;
    }
  }

  private void startGame() {
    gameInProgress = true;
    broadcastToPlayers("Game started! Player 1 goes first.");
    currentPlayerIndex = 0;
    promptPlayerForMove(players.get(currentPlayerIndex));
  }

  private void promptPlayerForMove(Player player) {
    // Give the player a diorite wall for their move
    player.getInventory().addItem(new ItemStack(missBlock, 1));

    player.sendActionBar(
        Component.text(
            "It's your turn! Place a diorite wall on your side of the public board.",
            NamedTextColor.AQUA));
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);

    new BukkitRunnable() {
      @Override
      public void run() {
        if (gameInProgress && players.get(currentPlayerIndex).equals(player)) {
          player.sendActionBar(
              Component.text(
                  "It's your turn! Place a diorite wall on your side of the public board.",
                  NamedTextColor.AQUA));
          player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        } else {
          player.sendActionBar(Component.empty());
          cancel();
        }
      }
    }.runTaskTimer(Bukkit.getPluginManager().getPlugin("stweaks"), 0L, 40L); // every 2 seconds
  }

  private void broadcastToPlayers(String msg) {
    for (Player p : players) {
      p.sendMessage(Component.text(msg, NamedTextColor.YELLOW));
    }
  }

  // Utility: Get relative (0-6, 0-6) coordinate from a block location on the
  // public board
  private int[] getRelativePublicBoardCoords(Location loc) {
    int relX = loc.getBlockX() - publicBoardCenter.getBlockX();
    int relY = loc.getBlockY() - publicBoardCenter.getBlockY();
    int relZ = loc.getBlockZ() - publicBoardCenter.getBlockZ();
    int[] result;
    if (boardDirection.equals("north") || boardDirection.equals("south")) {
      // Board is vertical in X/Y, Z is fixed
      // X is width (0-6), Y is height (0-6)
      result = new int[] {relX + 3, relY + 3};
    } else {
      // Board is vertical in Z/Y, X is fixed
      // Z is width (0-6), Y is height (0-6)
      result = new int[] {relZ + 3, relY + 3};
    }
    return result;
  }

  // Utility: Check if a block is on the public 7x7 board, accounting for
  // direction
  private boolean isOnPublicBoard(Location loc) {
    int relX = loc.getBlockX() - publicBoardCenter.getBlockX();
    int relY = loc.getBlockY() - publicBoardCenter.getBlockY();
    int relZ = loc.getBlockZ() - publicBoardCenter.getBlockZ();
    boolean result;

    // For a vertical board perpendicular to direction:
    // - For north/south: board is X (width) by Y (height), Z is fixed
    // - For east/west: board is Z (width) by Y (height), X is fixed
    if (boardDirection.equals("north") || boardDirection.equals("south")) {
      // Board is vertical in X/Y, Z is fixed
      result = relX >= -3 && relX <= 3 && relY >= -3 && relY <= 3 && relZ == 0;
    } else {
      // Board is vertical in Z/Y, X is fixed
      result = relZ >= -3 && relZ <= 3 && relY >= -3 && relY <= 3 && relX == 0;
    }

    return result;
  }

  private void rewardWinner(Player winner) {
    winner.sendMessage(Component.text("Congratulations! You win!", NamedTextColor.GOLD));
  }

  private void quitGame() {
    gameInProgress = false;
    broadcastToPlayers("Game over!");
    for (Player p : players) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fly " + p.getName());
    }
  }
}
