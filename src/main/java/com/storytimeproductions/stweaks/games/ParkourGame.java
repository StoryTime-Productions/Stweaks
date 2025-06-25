package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * ParkourGame: Players are teleported to the start area. If they fall to the floor level, they are
 * out. If they right-click the win block, they win and get 5 time tickets.
 */
public class ParkourGame implements Minigame, Listener {
  private final GameConfig config;
  private final Set<UUID> players = new HashSet<>();
  private int floorLevel;
  private Location winBlockLoc;
  private Location startArea;

  /**
   * Constructs a ParkourGame instance.
   *
   * @param config The game configuration.
   */
  public ParkourGame(GameConfig config) {
    this.config = config;

    // Parse floorLevel from game properties
    this.floorLevel = Integer.parseInt(config.getGameProperties().get("floorLevel").toString());

    // Parse win-block location from game properties (format: world,x,y,z)
    String[] winBlockParts = config.getGameProperties().get("win-block").toString().split(",");
    World winWorld = Bukkit.getWorld(winBlockParts[0]);
    int winX = Integer.parseInt(winBlockParts[1]);
    int winY = Integer.parseInt(winBlockParts[2]);
    int winZ = Integer.parseInt(winBlockParts[3]);
    this.winBlockLoc = new Location(winWorld, winX, winY, winZ);

    // Get start area from config
    this.startArea = config.getGameArea();
  }

  @Override
  public void onInit() {
    Bukkit.getPluginManager().registerEvents(this, Bukkit.getPluginManager().getPlugin("stweaks"));
  }

  @Override
  public void afterInit() {}

  @Override
  public void update() {}

  @Override
  public void render() {}

  @Override
  public void join(Player player) {
    players.add(player.getUniqueId());
    player.teleport(startArea);
  }

  @Override
  public void leave(Player player) {
    players.remove(player.getUniqueId());
  }

  @Override
  public List<Player> getPlayers() {
    List<Player> result = new ArrayList<>();
    for (UUID uuid : players) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null && p.isOnline()) {
        result.add(p);
      }
    }
    return result;
  }

  @Override
  public GameConfig getConfig() {
    return config;
  }

  @Override
  public boolean shouldQuit() {
    return players.size() == 0;
  }

  @Override
  public void removeItems(Player player) {}

  /** If a player in the game lands on the floor level, remove them from the game. */
  // ...existing code...

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (!players.contains(player.getUniqueId())) {
      return;
    }
    if (event.getTo() == null) {
      return;
    }
    if (event.getTo().getBlockY() == floorLevel) {
      player.sendMessage(
          Component.text("[Stweaks] ", NamedTextColor.YELLOW)
              .append(
                  Component.text(
                      "You fell! You are out of the parkour game.", NamedTextColor.WHITE)));
      leave(player);
    }
  }

  /** If a player in the game right-clicks the win block, reward and remove them. */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (!players.contains(player.getUniqueId())) {
      return;
    }
    if (event.getClickedBlock() == null) {
      return;
    }
    Location clicked = event.getClickedBlock().getLocation();
    if (clicked.getWorld().equals(winBlockLoc.getWorld())
        && clicked.getBlockX() == winBlockLoc.getBlockX()
        && clicked.getBlockY() == winBlockLoc.getBlockY()
        && clicked.getBlockZ() == winBlockLoc.getBlockZ()) {
      // Give 5 time tickets (implement your own giveTimeTicket logic)
      giveTimeTickets(player, 5);
      player.sendMessage(
          Component.text("[Stweaks] ", NamedTextColor.YELLOW)
              .append(
                  Component.text(
                      "Congratulations! You completed the parkour and earned 5 time tickets.",
                      NamedTextColor.WHITE)));
      // Launch a single firework at the win block
      Firework firework =
          winBlockLoc.getWorld().spawn(winBlockLoc.clone().add(0.5, 1, 0.5), Firework.class);
      FireworkMeta meta = firework.getFireworkMeta();
      meta.addEffect(
          FireworkEffect.builder()
              .withColor(org.bukkit.Color.ORANGE)
              .withFade(org.bukkit.Color.YELLOW)
              .with(FireworkEffect.Type.BALL_LARGE)
              .trail(true)
              .flicker(true)
              .build());
      meta.setPower(1);
      firework.setFireworkMeta(meta);
      leave(player);
    }
  }

  /**
   * Prevents players from using TPA commands while in the parkour game.
   *
   * <p>This is to ensure that players cannot teleport out of the parkour area during gameplay.
   */
  @EventHandler
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    Player player = event.getPlayer();
    if (!players.contains(player.getUniqueId())) {
      return;
    }
    String msg = event.getMessage().toLowerCase();
    if (msg.startsWith("/tpa")
        || msg.startsWith("/tpahere")
        || msg.startsWith("/tpaccept")
        || msg.startsWith("/tpdeny")) {
      event.setCancelled(true);
      player.sendMessage(
          Component.text("[Stweaks] ", NamedTextColor.YELLOW)
              .append(
                  Component.text(
                      "TPA commands are disabled during parkour!", NamedTextColor.WHITE)));
    }
  }

  /** Utility to give time tickets to a player. */
  private void giveTimeTickets(Player player, int amount) {
    ItemStack tickets = new ItemStack(org.bukkit.Material.NAME_TAG, amount);
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);
    player.getInventory().addItem(tickets);
  }

  @Override
  public void onDestroy() {
    HandlerList.unregisterAll(this);
  }
}
