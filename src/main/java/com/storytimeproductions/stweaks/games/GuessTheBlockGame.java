package com.storytimeproductions.stweaks.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** Guess The Block: a two-player deduction game with physical board tiles in the arena. */
public class GuessTheBlockGame implements Minigame, Listener {
  private static final int BOARD_ROWS = 4;
  private static final int BOARD_COLS = 6;
  private static final int BOARD_SIZE = BOARD_ROWS * BOARD_COLS;
  private static final int DEFAULT_BOARD_SPACING = 16;

  private static final List<Material> ALL_BLOCKS =
      List.copyOf(
          Arrays.stream(Material.values())
              .filter(Material::isBlock)
              .filter(Material::isItem)
              .filter(material -> material != Material.AIR)
              .filter(material -> material != Material.CAVE_AIR)
              .filter(material -> material != Material.VOID_AIR)
              .toList());

  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Random random = new Random();
  private final Board[] boards = new Board[2];
  private final Map<UUID, Integer> pendingGuess = new HashMap<>();

  private List<Material> boardBlocks;
  private Material[] secretBlocks;
  private boolean[] skipNextTurn;
  private int currentTurn;
  private boolean gameEnded;
  private boolean listenersRegistered;

  public GuessTheBlockGame(GameConfig config) {
    this.config = config;
  }

  @Override
  public void onInit() {
    registerListeners();
    gameEnded = false;
    currentTurn = 0;
    skipNextTurn = new boolean[] {false, false};
    pendingGuess.clear();

    initializeBoardsFromConfig();
    if (!boardsConfigured()) {
      gameEnded = true;
      return;
    }

    for (Board board : boards) {
      board.restore();
    }

    if (boardBlocks == null || boardBlocks.size() != BOARD_SIZE) {
      boardBlocks = drawMatchPool(random, BOARD_SIZE);
    }

    int firstSecret = random.nextInt(BOARD_SIZE);
    int secondSecret;
    do {
      secondSecret = random.nextInt(BOARD_SIZE);
    } while (secondSecret == firstSecret);
    secretBlocks = new Material[] {boardBlocks.get(firstSecret), boardBlocks.get(secondSecret)};
  }

  @Override
  public void afterInit() {
    if (gameEnded || players.size() < 2 || secretBlocks == null) {
      return;
    }

    for (int i = 0; i < 2; i++) {
      revealSecret(players.get(i), i);
    }

    broadcast(Component.text("=== Guess The Block ===", NamedTextColor.GOLD));
    broadcast(
        Component.text(
            "Ask yes/no questions, eliminate candidates, then right-click your lectern to guess.",
            NamedTextColor.YELLOW));
    notifyTurn();
  }

  @Override
  public void update() {}

  @Override
  public void render() {
    if (gameEnded || players.size() < 2) {
      return;
    }

    for (int i = 0; i < 2; i++) {
      Player player = players.get(i);
      if (i == currentTurn) {
        player.sendActionBar(
            Component.text(
                "Your turn: ask, eliminate tiles, or use your lectern to guess.",
                NamedTextColor.GREEN));
      } else {
        player.sendActionBar(
            Component.text(
                players.get(currentTurn).getName() + "'s turn: answer honestly.", NamedTextColor.YELLOW));
      }
    }
  }

  @Override
  public void onDestroy() {
    for (Player player : new ArrayList<>(players)) {
      removeItems(player);
    }
    players.clear();
    pendingGuess.clear();
    secretBlocks = null;
    boardBlocks = null;
    skipNextTurn = null;
    currentTurn = 0;
    gameEnded = false;

    clearBoards();
    unregisterListeners();
  }

  @Override
  public boolean shouldQuit() {
    return gameEnded;
  }

  @Override
  public void join(Player player) {
    if (players.contains(player)) {
      return;
    }

    if (players.size() >= config.getPlayerLimit() || players.size() >= 2) {
      player.sendMessage(Component.text("This game supports two players only.", NamedTextColor.RED));
      return;
    }

    players.add(player);
    player.teleport(resolvePlayerSpawn(players.size() - 1));
  }

  @Override
  public void leave(Player player) {
    pendingGuess.remove(player.getUniqueId());
    removeItems(player);
    players.remove(player);

    if (!gameEnded && players.size() < 2) {
      gameEnded = true;
    }
  }

  @Override
  public List<Player> getPlayers() {
    return players;
  }

  @Override
  public GameConfig getConfig() {
    return config;
  }

  @Override
  public void removeItems(Player player) {
    player.getInventory().remove(Material.WRITTEN_BOOK);
  }

  @EventHandler
  public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
    if (gameEnded || !boardsConfigured()) {
      return;
    }
    if (!(event.getRightClicked() instanceof ItemFrame frame)) {
      return;
    }

    int boardIndex = -1;
    int slotIndex = -1;
    for (int i = 0; i < boards.length; i++) {
      int candidate = boards[i].getSlotForFrame(frame.getUniqueId());
      if (candidate != -1) {
        boardIndex = i;
        slotIndex = candidate;
        break;
      }
    }
    if (boardIndex == -1) {
      return;
    }

    event.setCancelled(true);
    Player player = event.getPlayer();
    if (players.size() < 2 || !players.get(boardIndex).equals(player)) {
      player.sendMessage(
          Component.text("You can only toggle blocks on your own board.", NamedTextColor.RED));
      return;
    }

    boards[boardIndex].toggleSlot(slotIndex);
    Material target = boardBlocks.get(slotIndex);
    if (boards[boardIndex].isEliminated(slotIndex)) {
      player.sendMessage(Component.text("Eliminated: " + formatName(target), NamedTextColor.RED));
    } else {
      player.sendMessage(Component.text("Restored: " + formatName(target), NamedTextColor.GREEN));
    }
  }

  @EventHandler
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof ItemFrame frame)) {
      return;
    }
    for (Board board : boards) {
      if (board != null && board.getSlotForFrame(frame.getUniqueId()) != -1) {
        event.setCancelled(true);
        return;
      }
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (gameEnded || !boardsConfigured()) {
      return;
    }
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Block clicked = event.getClickedBlock();
    if (clicked == null) {
      return;
    }

    int boardIndex = -1;
    int slotIndex = -1;
    for (int i = 0; i < boards.length; i++) {
      int candidate = boards[i].getSlotForTrapdoor(clicked.getLocation());
      if (candidate != -1) {
        boardIndex = i;
        slotIndex = candidate;
        break;
      }
    }

    if (boardIndex != -1) {
      event.setCancelled(true);
      Player player = event.getPlayer();
      if (players.size() < 2 || !players.get(boardIndex).equals(player)) {
        player.sendMessage(
            Component.text("You can only toggle blocks on your own board.", NamedTextColor.RED));
        return;
      }

      boards[boardIndex].toggleSlot(slotIndex);
      Material target = boardBlocks.get(slotIndex);
      if (boards[boardIndex].isEliminated(slotIndex)) {
        player.sendMessage(Component.text("Eliminated: " + formatName(target), NamedTextColor.RED));
      } else {
        player.sendMessage(Component.text("Restored: " + formatName(target), NamedTextColor.GREEN));
      }
      return;
    }

    if (clicked.getType() != Material.LECTERN || players.size() < 2) {
      return;
    }

    int ownerIndex = -1;
    for (int i = 0; i < boards.length; i++) {
      if (boards[i] != null && clicked.getLocation().equals(boards[i].getLecternLocation())) {
        ownerIndex = i;
        break;
      }
    }
    if (ownerIndex == -1) {
      return;
    }

    event.setCancelled(true);
    Player player = event.getPlayer();
    if (!players.get(ownerIndex).equals(player)) {
      player.sendMessage(Component.text("That is not your guess lectern.", NamedTextColor.RED));
      return;
    }
    if (currentTurn != ownerIndex) {
      player.sendMessage(Component.text("It is not your turn.", NamedTextColor.RED));
      return;
    }

    openGuessInventory(player, ownerIndex);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    Integer playerIndex = pendingGuess.get(player.getUniqueId());
    if (playerIndex == null) {
      return;
    }

    event.setCancelled(true);
    if (event.getClickedInventory() == null
        || event.getClickedInventory() != event.getView().getTopInventory()) {
      return;
    }

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || clicked.getType() == Material.AIR) {
      return;
    }
    if (event.getSlot() < 0 || event.getSlot() >= boardBlocks.size()) {
      return;
    }

    player.closeInventory();
    pendingGuess.remove(player.getUniqueId());

    int opponentIndex = 1 - playerIndex;
    if (secretBlocks == null || opponentIndex < 0 || opponentIndex >= secretBlocks.length) {
      return;
    }

    Material guess = clicked.getType();
    if (guess == secretBlocks[opponentIndex]) {
      endGame(player);
      return;
    }

    player.sendMessage(Component.text("Wrong guess. You lose your next turn.", NamedTextColor.RED));
    if (players.size() > opponentIndex) {
      players
          .get(opponentIndex)
          .sendMessage(
              Component.text(player.getName() + " guessed wrong.", NamedTextColor.GREEN));
    }
    skipNextTurn[playerIndex] = true;
    advanceTurn();
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    pendingGuess.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    if (pendingGuess.containsKey(player.getUniqueId())) {
      event.setCancelled(true);
    }
  }

  private void endGame(Player winner) {
    gameEnded = true;

    for (Player player : players) {
      removeItems(player);
      player.sendMessage(Component.text("=== GAME OVER ===", NamedTextColor.GOLD));
      player.sendMessage(Component.text(winner.getName() + " wins!", NamedTextColor.YELLOW));
      if (secretBlocks != null && secretBlocks.length == 2 && players.size() >= 2) {
        player.sendMessage(
            Component.text(
                players.get(0).getName() + "'s block: " + formatName(secretBlocks[0]).toUpperCase(),
                NamedTextColor.AQUA));
        player.sendMessage(
            Component.text(
                players.get(1).getName() + "'s block: " + formatName(secretBlocks[1]).toUpperCase(),
                NamedTextColor.AQUA));
      }
    }
  }

  private void openGuessInventory(Player player, int playerIndex) {
    Inventory inventory =
        Bukkit.createInventory(
            null, 27, Component.text("Guess The Block", NamedTextColor.GOLD));

    for (int i = 0; i < boardBlocks.size(); i++) {
      Material material = boardBlocks.get(i);
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(Component.text(formatName(material), NamedTextColor.WHITE));
      item.setItemMeta(meta);
      inventory.setItem(i, item);
    }

    pendingGuess.put(player.getUniqueId(), playerIndex);
    player.openInventory(inventory);
  }

  private void advanceTurn() {
    if (players.size() < 2) {
      return;
    }

    currentTurn = 1 - currentTurn;
    if (skipNextTurn[currentTurn]) {
      skipNextTurn[currentTurn] = false;
      players
          .get(currentTurn)
          .sendMessage(Component.text("Your turn is skipped.", NamedTextColor.RED));
      currentTurn = 1 - currentTurn;
    }
    notifyTurn();
  }

  private void notifyTurn() {
    if (players.size() < 2) {
      return;
    }

    for (int i = 0; i < 2; i++) {
      if (i == currentTurn) {
        players
            .get(i)
            .sendMessage(
                Component.text(
                    "Your turn. Ask, eliminate, or guess using your lectern.",
                    NamedTextColor.GREEN));
      } else {
        players
            .get(i)
            .sendMessage(Component.text("Opponent turn.", NamedTextColor.YELLOW));
      }
    }
  }

  private void revealSecret(Player player, int playerIndex) {
    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta meta = (BookMeta) book.getItemMeta();
    meta.setTitle("Your Secret Block");
    meta.setAuthor("STweaks");
    meta.pages(
        Component.text("Your secret block is:\n\n")
            .append(Component.text(formatName(secretBlocks[playerIndex]).toUpperCase(), NamedTextColor.GOLD))
            .append(
                Component.text(
                    "\n\nKeep it hidden. Right-click your lectern when you are ready to guess.",
                    NamedTextColor.GRAY)));
    book.setItemMeta(meta);
    player.getInventory().addItem(book);
    player.sendMessage(
        Component.text("A secret block book was added to your inventory.", NamedTextColor.AQUA));
  }

  private void initializeBoardsFromConfig() {
    clearBoards();

    Location boardOneOrigin = parseLocation(config.getGameProperties().get("board1-origin"));
    if (boardOneOrigin == null) {
      boardOneOrigin = config.getGameArea();
    }

    if (boardOneOrigin == null || boardOneOrigin.getWorld() == null) {
      Bukkit.getLogger().warning("[STweaks] GuessTheBlock missing board origin configuration.");
      return;
    }

    int spacing =
        parseInt(config.getGameProperties().get("board-spacing"), DEFAULT_BOARD_SPACING);
    Location boardTwoOrigin = parseLocation(config.getGameProperties().get("board2-origin"));
    if (boardTwoOrigin == null) {
      boardTwoOrigin = boardOneOrigin.clone().add(spacing, 0, 0);
    }

    boardBlocks = drawMatchPool(random, BOARD_SIZE);
    boards[0] = new Board(boardOneOrigin, boardBlocks);
    boards[1] = new Board(boardTwoOrigin, boardBlocks);
    boards[0].build();
    boards[1].build();
  }

  private Location resolvePlayerSpawn(int playerIndex) {
    String key = playerIndex == 0 ? "player1-spawn" : "player2-spawn";
    Location configured = parseLocation(config.getGameProperties().get(key));
    if (configured != null) {
      return configured;
    }

    if (boardsConfigured()) {
      return boards[playerIndex].getOrigin().clone().add(0, 1, 2);
    }

    Location gameArea = config.getGameArea();
    if (gameArea != null) {
      return gameArea.clone().add(0, 1, 0);
    }

    return playerIndex == 0
        ? Bukkit.getWorlds().get(0).getSpawnLocation()
        : Bukkit.getWorlds().get(0).getSpawnLocation().clone().add(2, 0, 0);
  }

  private boolean boardsConfigured() {
    return boards[0] != null && boards[1] != null;
  }

  private void clearBoards() {
    for (int i = 0; i < boards.length; i++) {
      if (boards[i] != null) {
        boards[i].clear();
      }
      boards[i] = null;
    }
  }

  private void registerListeners() {
    if (listenersRegistered) {
      return;
    }

    Plugin plugin = Bukkit.getPluginManager().getPlugin("Stweaks");
    if (plugin == null) {
      plugin = Bukkit.getPluginManager().getPlugin("stweaks");
    }
    if (plugin == null) {
      Bukkit.getLogger().warning("[STweaks] Could not register GuessTheBlock listeners.");
      return;
    }

    Bukkit.getPluginManager().registerEvents(this, plugin);
    listenersRegistered = true;
  }

  private void unregisterListeners() {
    if (!listenersRegistered) {
      return;
    }
    HandlerList.unregisterAll(this);
    listenersRegistered = false;
  }

  private static List<Material> drawMatchPool(Random random, int size) {
    if (size > ALL_BLOCKS.size()) {
      throw new IllegalArgumentException("Requested block pool is larger than the catalog.");
    }

    List<Material> draw = new ArrayList<>(ALL_BLOCKS);
    Collections.shuffle(draw, random);
    return List.copyOf(draw.subList(0, size));
  }

  private static int parseInt(String value, int defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }

  private static Location parseLocation(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    String[] parts = value.split(",");
    if (parts.length < 4) {
      return null;
    }

    World world = Bukkit.getWorld(parts[0]);
    if (world == null) {
      return null;
    }

    try {
      double x = Double.parseDouble(parts[1]);
      double y = Double.parseDouble(parts[2]);
      double z = Double.parseDouble(parts[3]);
      return new Location(world, x, y, z);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private void broadcast(Component message) {
    for (Player player : players) {
      player.sendMessage(message);
    }
  }

  private static String formatName(Material material) {
    return material.name().toLowerCase().replace('_', ' ');
  }

  private static final class Board {
    private static final Material BOARD_BORDER = Material.POLISHED_BLACKSTONE_BRICKS;
    private static final Material BOARD_PANEL = Material.SMOOTH_QUARTZ;
    private static final Material BOARD_SEPARATOR = Material.CUT_COPPER;
    private static final Material BOARD_LIGHT = Material.SEA_LANTERN;
    private static final Material BOARD_COVER = Material.OAK_TRAPDOOR;

    private final Location origin;
    private final List<Material> boardBlocks;
    private final List<ItemFrame> itemFrames = new ArrayList<>();
    private final List<Block> frameBackingBlocks = new ArrayList<>();
    private final List<Block> decorativeBlocks = new ArrayList<>();
    private final List<Block> trapdoorBlocks = new ArrayList<>();
    private final Map<UUID, Integer> frameRegistry = new HashMap<>();
    private final Map<Location, Integer> trapdoorRegistry = new HashMap<>();
    private final boolean[] eliminated;

    private Location lecternLocation;

    private Board(Location origin, List<Material> boardBlocks) {
      this.origin = origin.clone();
      this.boardBlocks = List.copyOf(boardBlocks);
      this.eliminated = new boolean[boardBlocks.size()];
    }

    private Location getOrigin() {
      return origin;
    }

    private Location getLecternLocation() {
      return lecternLocation;
    }

    private void build() {
      World world = origin.getWorld();
      if (world == null) {
        return;
      }

      decorateBoardSurface(world);

      for (int row = 0; row < BOARD_ROWS; row++) {
        for (int col = 0; col < BOARD_COLS; col++) {
          int slot = row * BOARD_COLS + col;
          int x = origin.getBlockX() + col * 2;
          int y = origin.getBlockY() + row * 2;
          int z = origin.getBlockZ();

          Block backing = world.getBlockAt(x, y, z - 1);
          backing.setType(BOARD_PANEL);
          frameBackingBlocks.add(backing);

          Block trapdoor = world.getBlockAt(x, y, z);
          trapdoor.setType(BOARD_COVER);
          TrapDoor trapDoorData = (TrapDoor) trapdoor.getBlockData();
          trapDoorData.setFacing(BlockFace.NORTH);
          trapDoorData.setHalf(Bisected.Half.BOTTOM);
          trapDoorData.setOpen(true);
          trapdoor.setBlockData(trapDoorData);
          trapdoorBlocks.add(trapdoor);
          trapdoorRegistry.put(trapdoor.getLocation(), slot);

          Location frameLocation = new Location(world, x + 0.5, y + 0.5, z);
          ItemFrame frame =
              world.spawn(
                  frameLocation,
                  ItemFrame.class,
                  spawned -> {
                    spawned.setFacingDirection(BlockFace.SOUTH);
                    spawned.setVisible(true);
                    spawned.setFixed(true);
                    spawned.setItem(new ItemStack(boardBlocks.get(slot)));
                  });
          itemFrames.add(frame);
          frameRegistry.put(frame.getUniqueId(), slot);
        }
      }

      Block lectern =
          world.getBlockAt(
              origin.getBlockX() + BOARD_COLS * 2, origin.getBlockY(), origin.getBlockZ() + 1);
      lectern.setType(Material.LECTERN);
      lecternLocation = lectern.getLocation();
    }

    private void restore() {
      for (int i = 0; i < trapdoorBlocks.size(); i++) {
        eliminated[i] = false;
        setTrapdoorOpen(i, true);
      }
    }

    private void toggleSlot(int slot) {
      eliminated[slot] = !eliminated[slot];
      setTrapdoorOpen(slot, !eliminated[slot]);
    }

    private boolean isEliminated(int slot) {
      return eliminated[slot];
    }

    private int getSlotForFrame(UUID frameId) {
      return frameRegistry.getOrDefault(frameId, -1);
    }

    private int getSlotForTrapdoor(Location location) {
      return trapdoorRegistry.getOrDefault(location, -1);
    }

    private void clear() {
      itemFrames.forEach(Entity::remove);
      frameBackingBlocks.forEach(block -> block.setType(Material.AIR));
      decorativeBlocks.forEach(block -> block.setType(Material.AIR));
      trapdoorBlocks.forEach(block -> block.setType(Material.AIR));
      if (lecternLocation != null) {
        lecternLocation.getBlock().setType(Material.AIR);
      }
    }

    private void decorateBoardSurface(World world) {
      int minX = origin.getBlockX() - 1;
      int maxX = origin.getBlockX() + BOARD_COLS * 2 - 1;
      int minY = origin.getBlockY() - 1;
      int maxY = origin.getBlockY() + BOARD_ROWS * 2 - 1;
      int z = origin.getBlockZ();

      for (int x = minX; x <= maxX; x++) {
        for (int y = minY; y <= maxY; y++) {
          boolean border = x == minX || x == maxX || y == minY || y == maxY;
          boolean slotPosition =
              x >= origin.getBlockX()
                  && y >= origin.getBlockY()
                  && (x - origin.getBlockX()) % 2 == 0
                  && (y - origin.getBlockY()) % 2 == 0;

          if (slotPosition) {
            continue;
          }

          Block block = world.getBlockAt(x, y, z);
          if (border) {
            block.setType(BOARD_BORDER);
          } else if ((x - origin.getBlockX()) % 2 == 1 && (y - origin.getBlockY()) % 2 == 1) {
            block.setType(BOARD_LIGHT);
          } else {
            block.setType(BOARD_SEPARATOR);
          }
          decorativeBlocks.add(block);
        }
      }
    }

    private void setTrapdoorOpen(int slot, boolean open) {
      Block block = trapdoorBlocks.get(slot);
      TrapDoor trapDoor = (TrapDoor) block.getBlockData();
      trapDoor.setOpen(open);
      block.setBlockData(trapDoor);
    }
  }
}