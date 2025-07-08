package com.storytimeproductions.stweaks.util;

import java.util.ArrayList;
import java.util.List;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MiscDisguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.ItemDisplayWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages disguising slime entities in the lobby world as either players or items with custom
 * models. This class handles finding slimes and applying disguises based on configured lists.
 */
public class EntityDisguiseManager {

  private final JavaPlugin plugin;
  private final List<String> playerUsernames;
  private final List<String> itemModels;

  /**
   * Constructs an EntityDisguiseManager with lists of usernames and item models. Automatically
   * schedules disguises to be applied 5 seconds after server fully starts.
   *
   * @param plugin The plugin instance
   */
  public EntityDisguiseManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.playerUsernames = new ArrayList<>();
    this.itemModels = new ArrayList<>();

    // Configure your disguise data here
    setupDisguiseData();

    // Schedule disguises to be applied after server fully starts
    scheduleDelayedDisguises();
  }

  /**
   * Sets up the lists of usernames and item models for disguises. Modify this method to add your
   * desired disguise data.
   */
  private void setupDisguiseData() {
    // Player usernames
    playerUsernames.add("ayolifeisdum");
    playerUsernames.add("earlgrayteabag");
    playerUsernames.add("elmosbussinbussy");
    playerUsernames.add("golimarr");
    playerUsernames.add("hyperfoxk");
    playerUsernames.add("invessive");
    playerUsernames.add("japachi");
    playerUsernames.add("jaymar2");
    playerUsernames.add("just1karam");
    playerUsernames.add("justnisha");
    playerUsernames.add("kamikaze121777");
    playerUsernames.add("kingtutan");
    playerUsernames.add("kseyie");
    playerUsernames.add("lavendermadder");
    playerUsernames.add("leafynemibih");
    playerUsernames.add("monkiegaming");
    playerUsernames.add("niravana");
    playerUsernames.add("niravanaaa");
    playerUsernames.add("notch");
    playerUsernames.add("polymorphism_");
    playerUsernames.add("prapt_x");
    playerUsernames.add("rubiflam");
    playerUsernames.add("senseivu17");
    playerUsernames.add("theycallmesu");
    playerUsernames.add("xllatina");

    itemModels.add("PAPER:hotbar_pets:chick:Chick Pet");
    itemModels.add("PAPER:hotbar_pets:chicken:Chicken Pet");
    itemModels.add("PAPER:hotbar_pets:gible:Gible Pet");
    itemModels.add("PAPER:hotbar_pets:husky:Husky Pet");
    itemModels.add("PAPER:hotbar_pets:iron_golem:Iron Golem Pet");
    itemModels.add("PAPER:hotbar_pets:kfc:KFC Pet");
    itemModels.add("PAPER:hotbar_pets:lavender:Lavender Pet");
    itemModels.add("PAPER:hotbar_pets:mcdonalds:McDonalds Pet");
    itemModels.add("PAPER:hotbar_pets:mooshroom:Mooshroom Pet");
    itemModels.add("PAPER:hotbar_pets:nautilus:Nautilus Pet");
    itemModels.add("PAPER:hotbar_pets:patrick:Patrick Pet");
    itemModels.add("PAPER:hotbar_pets:pig:Pig Pet");
    itemModels.add("PAPER:hotbar_pets:seal:Seal Pet");
    itemModels.add("PAPER:hotbar_pets:shark:Shark Pet");
    itemModels.add("PAPER:hotbar_pets:sheep:Sheep Pet");
    itemModels.add("PAPER:hotbar_pets:spider:Spider Pet");
    itemModels.add("PAPER:hotbar_pets:squid:Squid Pet");
    itemModels.add("PAPER:hotbar_pets:swablu:Swablu Pet");
    itemModels.add("PAPER:hotbar_pets:train:Train Pet");

    // Mini Sus items
    itemModels.add("PAPER:mini_sus:mini_sus_black:Mini Sus Black");
    itemModels.add("PAPER:mini_sus:mini_sus_blue:Mini Sus Blue");
    itemModels.add("PAPER:mini_sus:mini_sus_green:Mini Sus Green");
    itemModels.add("PAPER:mini_sus:mini_sus_purple:Mini Sus Purple");
    itemModels.add("PAPER:mini_sus:mini_sus_red:Mini Sus Red");
    itemModels.add("PAPER:mini_sus:mini_sus_white:Mini Sus White");
    itemModels.add("PAPER:mini_sus:mini_sus_yellow:Mini Sus Yellow");

    // Repo Hats
    itemModels.add("PAPER:repohats:repohatblackcrown:Repo Hat Black Crown");
    itemModels.add("PAPER:repohats:repohatbluecrown:Repo Hat Blue Crown");
    itemModels.add("PAPER:repohats:repohatgreencrown:Repo Hat Green Crown");
    itemModels.add("PAPER:repohats:repohatpinkcrown:Repo Hat Pink Crown");
    itemModels.add("PAPER:repohats:repohatpurplecrown:Repo Hat Purple Crown");
    itemModels.add("PAPER:repohats:repohatredcrown:Repo Hat Red Crown");
    itemModels.add("PAPER:repohats:repohatwhitecrown:Repo Hat White Crown");
    itemModels.add("PAPER:repohats:repohatyellowcrown:Repo Hat Yellow Crown");
  }

  /** Schedules the disguises to be applied 5 seconds after the server has fully started. */
  private void scheduleDelayedDisguises() {
    // Wait 5 seconds (100 ticks) after server startup to ensure everything is
    // loaded
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              disguiseSlimesInLobby();
            },
            100L); // 5 seconds = 100 ticks
  }

  /**
   * Disguises all slime entities in the lobby world alternating between players and items. Should
   * be called on server start or when you want to refresh disguises.
   */
  public void disguiseSlimesInLobby() {
    World lobbyWorld = Bukkit.getWorld("lobby");
    if (lobbyWorld == null) {
      plugin.getLogger().warning("Lobby world not found! Cannot disguise slimes.");
      return;
    }

    // First, kill all existing slimes
    for (Entity entity : lobbyWorld.getEntities()) {
      if (entity.getType() == EntityType.SLIME) {
        entity.remove();
      }
    }

    // Calculate total number of disguises needed
    int totalDisguises = playerUsernames.size() + itemModels.size();
    if (totalDisguises == 0) {
      plugin.getLogger().warning("No disguises configured!");
      return;
    }

    // Spawn new slimes at the specific location
    org.bukkit.Location spawnLocation = new org.bukkit.Location(lobbyWorld, -121, 225, 186);

    // Spawn slimes equal to the total number of disguises
    for (int i = 0; i < totalDisguises; i++) {
      Slime slime = (Slime) lobbyWorld.spawnEntity(spawnLocation, EntityType.SLIME);

      // Make it a baby slime
      slime.setSize(1); // Size 1 = baby slime

      // Alternate between player and item disguises
      if (i < playerUsernames.size()) {
        // Use player disguise
        String username = playerUsernames.get(i);
        disguiseSlimeAsPlayer(slime, username);
      } else {
        // Use item disguise
        int itemIndex = i - playerUsernames.size();
        String itemData = itemModels.get(itemIndex);
        disguiseSlimeAsItem(slime, itemData);
      }
    }

    plugin
        .getLogger()
        .info(
            "Spawned and disguised "
                + totalDisguises
                + " slimes in the lobby world at -121,225,186.");
  }

  /**
   * Disguises a single slime as a player.
   *
   * @param slime The slime entity to disguise
   * @param username The username to display for the disguised player
   */
  private void disguiseSlimeAsPlayer(Slime slime, String username) {
    try {
      // Create a player disguise with just the name
      PlayerDisguise disguise = new PlayerDisguise(username);
      PlayerWatcher watcher = (PlayerWatcher) disguise.getWatcher();

      // Set the player name
      watcher.setName(username);

      // Set visible name to false
      watcher.setNameVisible(false);

      // Set scale to 0.6
      watcher.setScale(0.6);

      // Set custom name visible
      slime.setCustomNameVisible(false);

      // Make the slime stationary and silent
      slime.setAI(true);
      slime.setSilent(true);
      slime.setInvulnerable(true);

      // Keep as baby slime (size already set to 1 during spawn)

      // Apply the disguise
      DisguiseAPI.disguiseEntity(slime, disguise);

    } catch (Exception e) {
      plugin.getLogger().severe("Failed to disguise slime as " + username + ": " + e.getMessage());
    }
  }

  /**
   * Disguises a single slime as an item display with custom model.
   *
   * @param slime The slime entity to disguise
   * @param itemModel The item model string (format: "MATERIAL:namespace:modelkey:DisplayName")
   */
  private void disguiseSlimeAsItem(Slime slime, String itemModel) {
    try {
      // Parse the item model string
      String[] parts = itemModel.split(":");
      if (parts.length < 4) {
        plugin.getLogger().warning("Invalid item model format: " + itemModel);
        return;
      }

      Material material = Material.valueOf(parts[0]);
      String namespace = parts[1];
      String modelKey = parts[2];
      String displayName = parts[3];

      // Create the item stack with custom model
      ItemStack itemStack = new ItemStack(material);
      ItemMeta meta = itemStack.getItemMeta();
      if (meta != null) {
        meta.setItemModel(new NamespacedKey(namespace, modelKey));
        meta.displayName(net.kyori.adventure.text.Component.text(displayName));
        itemStack.setItemMeta(meta);
      }

      // Set custom name visible
      slime.setCustomNameVisible(false);

      // Make the slime completely stationary
      slime.setAI(true);
      slime.setSilent(true);
      slime.setInvulnerable(true);
      slime.setGravity(false);

      // Keep as baby slime (size already set to 1 during spawn)

      // Create an item display disguise (without ItemStack constructor)
      final MiscDisguise disguise = new MiscDisguise(DisguiseType.ITEM_DISPLAY);
      ItemDisplayWatcher watcher = (ItemDisplayWatcher) disguise.getWatcher();

      // Set the item using the correct watcher method for item displays
      // ItemDisplayWatcher typically uses setItemStack or similar
      try {
        // Try different possible method names for setting the item
        if (hasMethod(watcher.getClass(), "setItemStack", ItemStack.class)) {
          java.lang.reflect.Method method =
              watcher.getClass().getMethod("setItemStack", ItemStack.class);
          method.invoke(watcher, itemStack);
        } else if (hasMethod(watcher.getClass(), "setDisplayItem", ItemStack.class)) {
          java.lang.reflect.Method method =
              watcher.getClass().getMethod("setDisplayItem", ItemStack.class);
          method.invoke(watcher, itemStack);
        } else {
          for (java.lang.reflect.Method method : watcher.getClass().getMethods()) {
            if (method.getName().toLowerCase().contains("item")
                || method.getName().toLowerCase().contains("display")) {
              plugin
                  .getLogger()
                  .warning(
                      "  "
                          + method.getName()
                          + "("
                          + java.util.Arrays.toString(method.getParameterTypes())
                          + ")");
            }
          }
          throw new NoSuchMethodException("No suitable method found for setting item");
        }
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning(
                "Could not set item for ITEM_DISPLAY: "
                    + e.getMessage()
                    + ". Falling back to DROPPED_ITEM.");
        // Fallback to DROPPED_ITEM
        final MiscDisguise fallbackDisguise =
            new MiscDisguise(DisguiseType.DROPPED_ITEM, itemStack);
        DisguiseAPI.disguiseEntity(slime, fallbackDisguise);
        return;
      }

      DisguiseAPI.disguiseEntity(slime, disguise);

      plugin
          .getLogger()
          .info(
              "Disguised slime as item: "
                  + displayName
                  + " with model: "
                  + namespace
                  + ":"
                  + modelKey);
    } catch (Exception e) {
      plugin
          .getLogger()
          .severe("Failed to disguise slime as item " + itemModel + ": " + e.getMessage());
    }
  }

  /** Removes all disguises from slimes in the lobby world. */
  public void removeAllDisguises() {
    World lobbyWorld = Bukkit.getWorld("lobby");
    if (lobbyWorld == null) {
      return;
    }

    int removedCount = 0;
    for (Entity entity : lobbyWorld.getEntities()) {
      if (entity.getType() == EntityType.SLIME && DisguiseAPI.isDisguised(entity)) {
        DisguiseAPI.undisguiseToAll(entity);
        removedCount++;
      }
    }

    plugin.getLogger().info("Removed disguises from " + removedCount + " slimes.");
  }

  /**
   * Adds a new player username to the disguise list.
   *
   * @param username The player username to add
   */
  public void addPlayerUsername(String username) {
    if (!playerUsernames.contains(username)) {
      playerUsernames.add(username);
    }
  }

  /**
   * Adds a new item model to the disguise list.
   *
   * @param material The base material
   * @param itemModel The custom item model identifier
   * @param displayName The display name for the item
   */
  public void addItemModel(Material material, String itemModel, String displayName) {
    itemModels.add(material.name() + ":" + itemModel + ":" + displayName);
  }

  /**
   * Adds a new item model string to the disguise list.
   *
   * @param itemModelString The item model string (format:
   *     "MATERIAL:namespace:modelkey:DisplayName")
   */
  public void addItemModel(String itemModelString) {
    itemModels.add(itemModelString);
  }

  /**
   * Removes a player username from the disguise list.
   *
   * @param username The username to remove
   */
  public void removePlayerUsername(String username) {
    playerUsernames.remove(username);
  }

  /**
   * Gets the current list of player usernames.
   *
   * @return A copy of the current player usernames
   */
  public List<String> getPlayerUsernames() {
    return new ArrayList<>(playerUsernames);
  }

  /**
   * Gets the current list of item models.
   *
   * @return A copy of the current item models
   */
  public List<String> getItemModels() {
    return new ArrayList<>(itemModels);
  }

  /** Helper method to check if a class has a method with given name and parameter types. */
  private boolean hasMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
    try {
      clazz.getMethod(methodName, parameterTypes);
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }
}
