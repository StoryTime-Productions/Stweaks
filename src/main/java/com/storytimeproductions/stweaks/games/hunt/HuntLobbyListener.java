package com.storytimeproductions.stweaks.games.hunt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Handles inventory click events for the Hunt lobby system. */
public class HuntLobbyListener implements Listener {

  private final HuntLobbyManager lobbyManager;

  /**
   * Constructs a new HuntLobbyListener for the given lobby manager.
   *
   * @param lobbyManager The HuntLobbyManager instance
   */
  public HuntLobbyListener(HuntLobbyManager lobbyManager) {
    this.lobbyManager = lobbyManager;
  }

  /**
   * Handles inventory click events for the Hunt lobby menus.
   *
   * @param event The InventoryClickEvent
   */
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    if (!lobbyManager.hasOpenMenu(player.getUniqueId())) {
      return;
    }

    event.setCancelled(true);

    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem == null || !clickedItem.hasItemMeta()) {
      return;
    }

    ItemMeta meta = clickedItem.getItemMeta();
    Component displayName = meta.displayName();
    if (displayName == null) {
      return;
    }

    String itemName = ((net.kyori.adventure.text.TextComponent) displayName).content();
    String inventoryTitle =
        ((net.kyori.adventure.text.TextComponent) event.getView().title()).content();

    handleMenuClick(player, inventoryTitle, itemName, event.getSlot());
  }

  private void handleMenuClick(Player player, String inventoryTitle, String itemName, int slot) {
    HuntPlayerData data = lobbyManager.getPlayerData(player.getUniqueId());
    if (data == null) {
      return;
    }

    switch (inventoryTitle) {
      case "Hunt Game Lobby" -> handleMainMenuClick(player, data, itemName);
      case "Select Team" -> handleTeamSelectionClick(player, data, itemName);
      case "Select Hunter Class" -> handleHunterClassClick(player, data, slot);
      case "Select Hider Class" -> handleHiderClassClick(player, data, slot);
      case "Select Map" -> handleMapSelectionClick(player, data, slot);
      case "Select Game Mode" -> handleGameModeClick(player, data, slot);
      default -> {
        // No action
      }
    }
  }

  private void handleMainMenuClick(Player player, HuntPlayerData data, String itemName) {
    switch (itemName) {
      case "Select Team" -> lobbyManager.openTeamSelectionMenu(player);
      case "Select Class" -> lobbyManager.openClassSelectionMenu(player);
      case "Select Map" -> lobbyManager.openMapSelectionMenu(player);
      case "Select Game Mode" -> lobbyManager.openGameModeSelectionMenu(player);
      case "Ready!", "Not Ready" -> {
        if (data.hasValidSelections()) {
          data.setReady(!data.isReady());
          player.sendMessage(
              Component.text(
                  data.isReady() ? "You are now ready!" : "You are no longer ready.",
                  data.isReady() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
          lobbyManager.openMainMenu(player); // Refresh menu
        } else {
          player.sendMessage(
              Component.text(
                  "Please complete all selections before readying up!", NamedTextColor.RED));
        }
      }
      case "Back" -> {
        player.closeInventory();
        lobbyManager.removePlayer(player.getUniqueId());
        player.sendMessage(Component.text("Exited Hunt Lobby", NamedTextColor.YELLOW));
      }
      default -> {
        // No action
      }
    }
  }

  private void handleTeamSelectionClick(Player player, HuntPlayerData data, String itemName) {
    switch (itemName) {
      case "Hunters" -> {
        data.setSelectedTeam(HuntTeam.HUNTERS);
        data.setSelectedHiderClass(null); // Clear opposite team class
        player.sendMessage(Component.text("Selected team: Hunters", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case "Hiders" -> {
        data.setSelectedTeam(HuntTeam.HIDERS);
        data.setSelectedHunterClass(null); // Clear opposite team class
        player.sendMessage(Component.text("Selected team: Hiders", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case "Back" -> lobbyManager.openMainMenu(player);
      default -> {
        // No action
      }
    }
  }

  private void handleHunterClassClick(Player player, HuntPlayerData data, int slot) {
    switch (slot) {
      case 2 -> {
        data.setSelectedHunterClass(HunterClass.BRUTE);
        player.sendMessage(Component.text("Selected class: Brute", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case 5 -> {
        data.setSelectedHunterClass(HunterClass.NIMBLE);
        player.sendMessage(Component.text("Selected class: Nimble", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case 8 -> {
        data.setSelectedHunterClass(HunterClass.SABOTEUR);
        player.sendMessage(Component.text("Selected class: Saboteur", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case 13 -> lobbyManager.openMainMenu(player); // Back button
      default -> {
        // No action
      }
    }
  }

  private void handleHiderClassClick(Player player, HuntPlayerData data, int slot) {
    switch (slot) {
      case 2 -> {
        data.setSelectedHiderClass(HiderClass.TRICKSTER);
        player.sendMessage(Component.text("Selected class: Trickster", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case 5 -> {
        data.setSelectedHiderClass(HiderClass.PHASER);
        player.sendMessage(Component.text("Selected class: Phaser", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case 8 -> {
        data.setSelectedHiderClass(HiderClass.CLOAKER);
        player.sendMessage(Component.text("Selected class: Cloaker", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case 13 -> lobbyManager.openMainMenu(player); // Back button
      default -> {
        // No action
      }
    }
  }

  private void handleMapSelectionClick(Player player, HuntPlayerData data, int slot) {
    HuntMap[] maps = HuntMap.values();
    int mapIndex = (slot - 1) / 2; // Convert slot to map index

    if (mapIndex >= 0 && mapIndex < maps.length) {
      data.setPreferredMap(maps[mapIndex]);
      player.sendMessage(
          Component.text("Selected map: " + maps[mapIndex].getDisplayName(), NamedTextColor.GREEN));
      lobbyManager.openMainMenu(player);
    } else if (slot == 13) {
      lobbyManager.openMainMenu(player); // Back button
    }
  }

  private void handleGameModeClick(Player player, HuntPlayerData data, int slot) {
    switch (slot) {
      case 2 -> {
        lobbyManager.handleGameModeSelection(player, HuntGameMode.PROP_HUNT);
        player.sendMessage(Component.text("Selected mode: Prop Hunt", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case 5 -> {
        lobbyManager.handleGameModeSelection(player, HuntGameMode.IMPOSTER_HUNT);
        player.sendMessage(Component.text("Selected mode: Imposter Hunt", NamedTextColor.GREEN));
        lobbyManager.openMainMenu(player);
      }
      case 8 -> {
        lobbyManager.handleGameModeSelection(player, HuntGameMode.NEXTBOT_HUNT);
        // Message is handled in handleGameModeSelection for NextBots
        lobbyManager.openMainMenu(player);
      }
      case 13 -> lobbyManager.openMainMenu(player); // Back button
      default -> {
        // No action
      }
    }
  }
}
