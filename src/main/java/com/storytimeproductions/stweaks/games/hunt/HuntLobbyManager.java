package com.storytimeproductions.stweaks.games.hunt;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Manages the Hunt game lobby system where players can select teams, classes, maps, and game modes.
 */
public class HuntLobbyManager {

  private final Map<UUID, HuntPlayerData> playerData;
  private final Map<UUID, Inventory> openMenus;

  /** Constructs a new HuntLobbyManager with the given plugin instance. */
  public HuntLobbyManager() {
    this.playerData = new HashMap<>();
    this.openMenus = new HashMap<>();
  }

  /** Opens the main Hunt lobby menu for a player. */
  public void openMainMenu(Player player) {
    HuntPlayerData data = getOrCreatePlayerData(player.getUniqueId());

    Inventory menu = Bukkit.createInventory(null, 27, Component.text("Hunt Game Lobby"));

    // Fill border with black stained glass panes
    fillBorder(menu, 27);

    // Team Selection (slot 10)
    String teamDescription;
    if (data.getPreferredGameMode() == HuntGameMode.NEXTBOT_HUNT) {
      teamDescription = "NextBot Hunt: All players are hiders";
    } else if (data.getSelectedTeam() != null) {
      teamDescription = "Current: " + data.getSelectedTeam().getDisplayName();
    } else {
      teamDescription = "Click to select your team";
    }

    ItemStack teamItem = createMenuItem(Material.WHITE_BANNER, "Select Team", teamDescription);
    menu.setItem(10, teamItem);

    // Class Selection (slot 11)
    ItemStack classItem =
        createMenuItem(Material.DIAMOND_CHESTPLATE, "Select Class", getClassDisplayText(data));
    menu.setItem(11, classItem);

    // Map Selection (slot 12)
    ItemStack mapItem =
        createMenuItem(
            Material.MAP,
            "Select Map",
            data.getPreferredMap() != null
                ? "Current: " + data.getPreferredMap().getDisplayName()
                : "Click to select a map");
    menu.setItem(12, mapItem);

    // Game Mode Selection (slot 13)
    ItemStack gameModeItem =
        createMenuItem(
            Material.COMPASS,
            "Select Game Mode",
            data.getPreferredGameMode() != null
                ? "Current: " + data.getPreferredGameMode().getDisplayName()
                : "Click to select game mode");
    menu.setItem(13, gameModeItem);

    // Ready Toggle (slot 16)
    ItemStack readyItem =
        createMenuItem(
            data.isReady() ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
            data.isReady() ? "Ready!" : "Not Ready",
            data.hasValidSelections()
                ? (data.isReady() ? "Click to unready" : "Click when ready to play")
                : "Complete all selections first");
    menu.setItem(16, readyItem);

    // Back button in bottom middle (slot 22 for 27-slot inventory)
    ItemStack backItem = createMenuItem(Material.ARROW, "Back", "Exit Hunt Lobby");
    menu.setItem(22, backItem);

    openMenus.put(player.getUniqueId(), menu);
    player.openInventory(menu);
  }

  /** Opens the team selection menu. */
  public void openTeamSelectionMenu(Player player) {
    HuntPlayerData data = getOrCreatePlayerData(player.getUniqueId());

    // Check if NextBots mode - only hiders allowed
    if (data.getPreferredGameMode() == HuntGameMode.NEXTBOT_HUNT) {
      // Automatically assign to hiders and return to main menu
      data.setSelectedTeam(HuntTeam.HIDERS);
      // Clear any hunter class selection since they're now a hider
      data.setSelectedHunterClass(null);
      player.sendMessage(
          Component.text("In NextBot Hunt, all players are hiders!", NamedTextColor.YELLOW));
      openMainMenu(player);
      return;
    }

    Inventory menu = Bukkit.createInventory(null, 9, Component.text("Select Team"));

    // Fill border with black stained glass panes
    fillBorder(menu, 9);

    // Hunters
    ItemStack huntersItem =
        createMenuItem(
            Material.IRON_SWORD,
            HuntTeam.HUNTERS.getDisplayName(),
            HuntTeam.HUNTERS.getDescription());
    menu.setItem(3, huntersItem);

    // Hiders
    ItemStack hidersItem =
        createMenuItem(
            Material.ENDER_PEARL,
            HuntTeam.HIDERS.getDisplayName(),
            HuntTeam.HIDERS.getDescription());
    menu.setItem(5, hidersItem);

    // Back button in bottom middle (since this is a 9-slot menu, bottom middle is
    // slot 4)
    ItemStack backItem = createMenuItem(Material.ARROW, "Back", "Return to main menu");
    menu.setItem(4, backItem);

    openMenus.put(player.getUniqueId(), menu);
    player.openInventory(menu);
  }

  /** Opens the class selection menu based on the player's selected team. */
  public void openClassSelectionMenu(Player player) {
    HuntPlayerData data = getOrCreatePlayerData(player.getUniqueId());

    if (data.getSelectedTeam() == null) {
      player.sendMessage(Component.text("Please select a team first!", NamedTextColor.RED));
      return;
    }

    if (data.getSelectedTeam() == HuntTeam.HUNTERS) {
      openHunterClassMenu(player);
    } else {
      openHiderClassMenu(player);
    }
  }

  private void openHunterClassMenu(Player player) {
    Inventory menu = Bukkit.createInventory(null, 18, Component.text("Select Hunter Class"));

    // Fill border with black stained glass panes
    fillBorder(menu, 18);

    int slot = 2;
    for (HunterClass hunterClass : HunterClass.values()) {
      ItemStack classItem =
          createMenuItem(
              hunterClass.getArmor().getType(),
              hunterClass.getDisplayName(),
              hunterClass.getDescription());
      menu.setItem(slot, classItem);
      slot += 3;
    }

    // Back button in bottom middle (slot 13 for 18-slot inventory)
    ItemStack backItem = createMenuItem(Material.ARROW, "Back", "Return to main menu");
    menu.setItem(13, backItem);

    openMenus.put(player.getUniqueId(), menu);
    player.openInventory(menu);
  }

  private void openHiderClassMenu(Player player) {
    Inventory menu = Bukkit.createInventory(null, 18, Component.text("Select Hider Class"));

    // Fill border with black stained glass panes
    fillBorder(menu, 18);

    int slot = 2;
    for (HiderClass hiderClass : HiderClass.values()) {
      ItemStack classItem =
          createMenuItem(
              hiderClass.getArmor().getType(),
              hiderClass.getDisplayName(),
              hiderClass.getDescription());
      menu.setItem(slot, classItem);
      slot += 3;
    }

    // Back button in bottom middle (slot 13 for 18-slot inventory)
    ItemStack backItem = createMenuItem(Material.ARROW, "Back", "Return to main menu");
    menu.setItem(13, backItem);

    openMenus.put(player.getUniqueId(), menu);
    player.openInventory(menu);
  }

  /** Opens the map selection menu. */
  public void openMapSelectionMenu(Player player) {
    Inventory menu = Bukkit.createInventory(null, 18, Component.text("Select Map"));

    // Fill border with black stained glass panes
    fillBorder(menu, 18);

    int slot = 1;
    for (HuntMap map : HuntMap.values()) {
      ItemStack mapItem =
          createMenuItem(Material.FILLED_MAP, map.getDisplayName(), map.getDescription());
      menu.setItem(slot, mapItem);
      slot += 2;
    }

    // Back button in bottom middle (slot 13 for 18-slot inventory)
    ItemStack backItem = createMenuItem(Material.ARROW, "Back", "Return to main menu");
    menu.setItem(13, backItem);

    openMenus.put(player.getUniqueId(), menu);
    player.openInventory(menu);
  }

  /** Opens the game mode selection menu. */
  public void openGameModeSelectionMenu(Player player) {
    Inventory menu = Bukkit.createInventory(null, 18, Component.text("Select Game Mode"));

    // Fill border with black stained glass panes
    fillBorder(menu, 18);

    int slot = 2;
    for (HuntGameMode gameMode : HuntGameMode.values()) {
      ItemStack gameModeItem =
          createMenuItem(
              Material.NETHER_STAR, gameMode.getDisplayName(), gameMode.getDescription());
      menu.setItem(slot, gameModeItem);
      slot += 3;
    }

    // Back button in bottom middle (slot 13 for 18-slot inventory)
    ItemStack backItem = createMenuItem(Material.ARROW, "Back", "Return to main menu");
    menu.setItem(13, backItem);

    openMenus.put(player.getUniqueId(), menu);
    player.openInventory(menu);
  }

  /**
   * Gets or creates the HuntPlayerData for the given player UUID.
   *
   * @param playerId The UUID of the player
   * @return The HuntPlayerData for the player
   */
  public HuntPlayerData getOrCreatePlayerData(UUID playerId) {
    return playerData.computeIfAbsent(playerId, HuntPlayerData::new);
  }

  private ItemStack createMenuItem(Material material, String name, String description) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.displayName(Component.text(name, NamedTextColor.YELLOW));
      meta.lore(java.util.List.of(Component.text(description, NamedTextColor.GRAY)));
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack createBorderPane() {
    ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta meta = pane.getItemMeta();
    if (meta != null) {
      meta.displayName(Component.text(" "));
      pane.setItemMeta(meta);
    }
    return pane;
  }

  private void fillBorder(Inventory menu, int size) {
    ItemStack borderPane = createBorderPane();

    // Fill top and bottom rows
    for (int i = 0; i < 9; i++) {
      menu.setItem(i, borderPane); // Top row
      if (size > 9) {
        menu.setItem(size - 9 + i, borderPane); // Bottom row
      }
    }

    // Fill left and right columns for multi-row inventories
    if (size > 9) {
      for (int row = 1; row < (size / 9) - 1; row++) {
        menu.setItem(row * 9, borderPane); // Left column
        menu.setItem(row * 9 + 8, borderPane); // Right column
      }
    }
  }

  private String getClassDisplayText(HuntPlayerData data) {
    if (data.getSelectedTeam() == null) {
      return "Select a team first";
    }

    if (data.getSelectedTeam() == HuntTeam.HUNTERS) {
      return data.getSelectedHunterClass() != null
          ? "Current: " + data.getSelectedHunterClass().getDisplayName()
          : "Click to select hunter class";
    } else {
      return data.getSelectedHiderClass() != null
          ? "Current: " + data.getSelectedHiderClass().getDisplayName()
          : "Click to select hider class";
    }
  }

  /**
   * Returns the player data for the given player UUID.
   *
   * @param playerId The UUID of the player
   * @return The HuntPlayerData for the player, or null if not found
   */
  public HuntPlayerData getPlayerData(UUID playerId) {
    return playerData.get(playerId);
  }

  /**
   * Returns a copy of all player data in the lobby.
   *
   * @return A map of player UUIDs to their HuntPlayerData
   */
  public Map<UUID, HuntPlayerData> getAllPlayerData() {
    return new HashMap<>(playerData);
  }

  /**
   * Removes a player from the lobby and closes their menu.
   *
   * @param playerId The UUID of the player to remove
   */
  public void removePlayer(UUID playerId) {
    playerData.remove(playerId);
    openMenus.remove(playerId);
  }

  /**
   * Sets a player's ready status and updates their data.
   *
   * @param playerId The UUID of the player
   * @param ready Whether the player is ready
   */
  public void setPlayerReady(UUID playerId, boolean ready) {
    HuntPlayerData data = getOrCreatePlayerData(playerId);
    data.setReady(ready);
  }

  /**
   * Checks if a player has an open menu in the lobby.
   *
   * @param playerId The UUID of the player
   * @return true if the player has an open menu, false otherwise
   */
  public boolean hasOpenMenu(UUID playerId) {
    return openMenus.containsKey(playerId);
  }

  /**
   * Gets the open menu inventory for a player.
   *
   * @param playerId The UUID of the player
   * @return The Inventory if open, or null
   */
  public Inventory getOpenMenu(UUID playerId) {
    return openMenus.get(playerId);
  }

  /**
   * Handles game mode selection and enforces NextBots rules. If NextBots is selected, all players
   * are automatically assigned to hiders.
   *
   * @param player The player selecting the game mode
   * @param gameMode The selected game mode
   */
  public void handleGameModeSelection(Player player, HuntGameMode gameMode) {
    HuntPlayerData data = getOrCreatePlayerData(player.getUniqueId());
    data.setPreferredGameMode(gameMode);

    if (gameMode == HuntGameMode.NEXTBOT_HUNT) {
      // Convert all hunters to hiders for NextBots mode
      reassignHuntersToHiders();
      player.sendMessage(
          Component.text(
              "NextBot Hunt selected! All players are now hiders.", NamedTextColor.YELLOW));
    }
  }

  /**
   * Reassigns all hunters to hiders and clears their hunter class selections. Used when switching
   * to NextBots game mode.
   */
  private void reassignHuntersToHiders() {
    for (HuntPlayerData data : playerData.values()) {
      if (data.getSelectedTeam() == HuntTeam.HUNTERS) {
        data.setSelectedTeam(HuntTeam.HIDERS);
        data.setSelectedHunterClass(null); // Clear hunter class since they're now hiders
      }
    }
  }
}
