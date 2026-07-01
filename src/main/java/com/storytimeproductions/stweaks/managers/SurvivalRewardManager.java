package com.storytimeproductions.stweaks.managers;

import com.storytimeproductions.stweaks.Stweaks;
import java.io.File;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Gives portal-unlocked players the StoryTime Guide (slot 3) and Threshold Key (slot 5) when they
 * enter the survival world, and handles right-click interactions for both items.
 *
 * <p>Portal-unlock state is read directly from EDEN's per-player YAML so that STweaks has no
 * compile-time dependency on the EDEN plugin.
 */
public class SurvivalRewardManager implements Listener {

  private static final String VAL_GUIDE = "GUIDE_BOOK";
  private static final String VAL_KEY = "THRESHOLD_KEY";
  private static final int SLOT_GUIDE = 3;
  private static final int SLOT_KEY = 5;

  private final Stweaks plugin;
  private final NamespacedKey itemKey;
  private final NamespacedKey menuActionKey;

  /**
   * Constructs a new SurvivalRewardManager.
   *
   * @param plugin the Stweaks plugin instance
   */
  public SurvivalRewardManager(Stweaks plugin) {
    this.plugin = plugin;
    this.itemKey = new NamespacedKey(plugin, "stweaks_item");
    this.menuActionKey = new NamespacedKey(plugin, "menu_action");
  }

  /**
   * Gives items to portal-unlocked players who join (they are auto-sent to the lobby).
   *
   * @param event the join event
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    // Delay past PlayerActivityListener's 10-tick lobby send so the world change has settled.
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              if (!player.isOnline()) {
                return;
              }
              if (isInLobbyWorld(player) && isPortalUnlocked(player)) {
                giveItems(player);
              }
            },
            40L);
  }

  /**
   * Gives items when entering the lobby, removes them when leaving.
   *
   * @param event the world-change event
   */
  @EventHandler
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    Player player = event.getPlayer();
    String lobbyWorld = plugin.getConfig().getString("lobby.world", "lobby");
    boolean enteredLobby = player.getWorld().getName().equals(lobbyWorld);
    boolean leftLobby = event.getFrom().getName().equals(lobbyWorld);

    if (enteredLobby && isPortalUnlocked(player)) {
      Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                if (player.isOnline()) {
                  giveItems(player);
                }
              },
              10L);
    } else if (leftLobby) {
      removeItems(player);
    }
  }

  /**
   * Prevents players from dropping the Guide Book or Threshold Key.
   *
   * @param event the drop event
   */
  @EventHandler
  public void onPlayerDrop(PlayerDropItemEvent event) {
    String type = getItemType(event.getItemDrop().getItemStack());
    if (VAL_GUIDE.equals(type) || VAL_KEY.equals(type)) {
      event.setCancelled(true);
    }
  }

  /**
   * Handles right-clicking the Guide Book (opens menu) and Threshold Key (returns to lobby).
   *
   * @param event the interact event
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
      return;
    }
    Action action = event.getAction();
    if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Player player = event.getPlayer();
    String type = getItemType(player.getInventory().getItemInMainHand());
    if (type == null) {
      return;
    }

    event.setCancelled(true);
    switch (type) {
      case VAL_GUIDE -> openGuideMenu(player);
      case VAL_KEY -> player.performCommand("spawn");
      default -> {
        // not a managed item
      }
    }
  }

  /**
   * Handles clicks inside the StoryTime guide menu GUI.
   *
   * @param event the click event
   */
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    if (!(event.getInventory().getHolder() instanceof GuideMenuHolder)) {
      return;
    }

    event.setCancelled(true);
    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || !clicked.hasItemMeta()) {
      return;
    }
    String cmd =
        clicked
            .getItemMeta()
            .getPersistentDataContainer()
            .get(menuActionKey, PersistentDataType.STRING);
    if (cmd == null) {
      return;
    }
    player.closeInventory();
    player.performCommand(cmd);
  }

  /**
   * Prevents dragging items inside the StoryTime guide menu GUI.
   *
   * @param event the drag event
   */
  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (event.getInventory().getHolder() instanceof GuideMenuHolder) {
      event.setCancelled(true);
    }
  }

  private void giveItems(Player player) {
    if (!hasItem(player, VAL_GUIDE)) {
      player.getInventory().setItem(SLOT_GUIDE, createGuideBook());
    }
    if (!hasItem(player, VAL_KEY)) {
      player.getInventory().setItem(SLOT_KEY, createThresholdKey());
    }
  }

  /**
   * Removes the Guide Book and Threshold Key from a player's inventory.
   *
   * @param player the player whose items should be removed
   */
  private void removeItems(Player player) {
    ItemStack[] contents = player.getInventory().getContents();
    for (int i = 0; i < contents.length; i++) {
      String type = getItemType(contents[i]);
      if (VAL_GUIDE.equals(type) || VAL_KEY.equals(type)) {
        player.getInventory().setItem(i, null);
      }
    }
  }

  private void openGuideMenu(Player player) {
    Inventory menu =
        Bukkit.createInventory(
            new GuideMenuHolder(),
            27,
            Component.text("StoryTime").color(NamedTextColor.DARK_PURPLE));

    ItemStack pane = pane();
    for (int i = 0; i < 27; i++) {
      menu.setItem(i, pane);
    }

    menu.setItem(
        10,
        menuButton(
            Material.EMERALD, "Quests", "Browse your daily and seasonal quests.", "stquests"));
    menu.setItem(
        12, menuButton(Material.BONE, "Pets", "View and manage your pet companions.", "stpets"));
    menu.setItem(
        13,
        menuButton(
            Material.COMPASS, "Biome Tracker", "Track your biome discoveries.", "biometracker"));
    menu.setItem(
        14,
        menuButton(Material.FEATHER, "Cosmetics", "Browse cosmetics and effects.", "stcosmetics"));
    menu.setItem(16, menuButton(Material.GOLD_INGOT, "Casino", "Test your luck.", "stcasino"));
    menu.setItem(
        22,
        menuButton(Material.PAPER, "Status", "View your playtime and server status.", "ststatus"));

    player.openInventory(menu);
  }

  private ItemStack menuButton(Material mat, String name, String lore, String cmd) {
    ItemStack stack = new ItemStack(mat);
    ItemMeta meta = stack.getItemMeta();
    meta.displayName(
        Component.text(name).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
    meta.lore(
        List.of(
            Component.text(lore)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, true)));
    meta.getPersistentDataContainer().set(menuActionKey, PersistentDataType.STRING, cmd);
    stack.setItemMeta(meta);
    return stack;
  }

  private ItemStack pane() {
    ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta meta = stack.getItemMeta();
    meta.displayName(Component.empty());
    stack.setItemMeta(meta);
    return stack;
  }

  private ItemStack createGuideBook() {
    ItemStack stack = new ItemStack(Material.BOOK);
    ItemMeta meta = stack.getItemMeta();
    meta.displayName(
        Component.text("StoryTime Guide")
            .color(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
    meta.lore(
        List.of(
            Component.text("Right-click to open the menu.")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, true)));
    meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, VAL_GUIDE);
    stack.setItemMeta(meta);
    return stack;
  }

  private ItemStack createThresholdKey() {
    ItemStack stack = new ItemStack(Material.CLOCK);
    ItemMeta meta = stack.getItemMeta();
    meta.displayName(
        Component.text("Threshold Key")
            .color(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
    meta.lore(
        List.of(
            Component.text("Right-click to teleport to spawn.")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, true)));
    meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, VAL_KEY);
    stack.setItemMeta(meta);
    return stack;
  }

  private boolean hasItem(Player player, String type) {
    for (ItemStack s : player.getInventory().getContents()) {
      if (type.equals(getItemType(s))) {
        return true;
      }
    }
    return false;
  }

  private String getItemType(ItemStack stack) {
    if (stack == null || !stack.hasItemMeta()) {
      return null;
    }
    return stack.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
  }

  private boolean isInLobbyWorld(Player player) {
    String lobbyWorld = plugin.getConfig().getString("lobby.world", "lobby");
    return player.getWorld().getName().equals(lobbyWorld);
  }

  private boolean isPortalUnlocked(Player player) {
    File edenData = new File(plugin.getDataFolder().getParentFile(), "EDEN");
    File playerFile = new File(edenData, "players/" + player.getUniqueId() + ".yml");
    if (!playerFile.exists()) {
      return false;
    }
    return YamlConfiguration.loadConfiguration(playerFile).getBoolean("portal-unlocked", false);
  }

  /** Internal InventoryHolder used to identify the guide menu inventory. */
  private static class GuideMenuHolder implements InventoryHolder {
    @Override
    public Inventory getInventory() {
      return null;
    }
  }
}
