package com.storytimeproductions.stweaks.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.storytimeproductions.stweaks.Stweaks;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listener that handles applying and restoring the "lebron" skin based on whether a player is
 * wearing a full set of custom Lebron armor.
 */
public class LebronArmorListener implements Listener {

  private final Stweaks plugin;
  private final SkinsRestorer skinsRestorer = SkinsRestorerProvider.get();

  /** The custom skin property representing the Lebron skin. */
  private final SkinProperty lebronSkin =
      SkinProperty.of(
          "ewogICJ0aW1lc3RhbXAiIDogMTY0MTIwNjc4OTIwNywKICAicHJvZmlsZUlkIiA6ICJjZjgwY2E3NDFj"
              + "NWQ0N2E3YWFjNGNmYjI2MjI0NDJmYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJzb21lb25lX28i...",
          "P2+tca61qcDIdKmIUgENZ0bhGzq3Y7mlGrBNpqVTMXGem8A8dBv7JaUqJqdwdFDhQOn9VExiUb...");

  /** A map to track and restore each player's original skin using their UUID. */
  private final ConcurrentHashMap<UUID, String> originalSkins = new ConcurrentHashMap<>();

  /**
   * Constructs the listener with the given plugin reference.
   *
   * @param plugin The main plugin instance
   */
  public LebronArmorListener(Stweaks plugin) {
    this.plugin = plugin;
  }

  /**
   * Handles when a player changes armor. If they are wearing the full Lebron set, applies the
   * custom Lebron skin. If they remove part of the set, restores their original skin.
   *
   * @param event The armor change event
   * @throws DataRequestException If fetching or applying skin data fails
   */
  @EventHandler
  public void onArmorChange(PlayerArmorChangeEvent event) throws DataRequestException {
    Player player = event.getPlayer();
    boolean hasFullSet = isWearingFullLebronSet(player);
    SkinIdentifier skinId = SkinIdentifier.ofCustom("lebron");

    if (hasFullSet) {
      if (!originalSkins.containsKey(player.getUniqueId())) {
        try {
          Optional<SkinProperty> current =
              skinsRestorer
                  .getPlayerStorage()
                  .getSkinForPlayer(player.getUniqueId(), player.getName());
          current.ifPresent(prop -> originalSkins.put(player.getUniqueId(), prop.getValue()));
        } catch (DataRequestException e) {
          e.printStackTrace();
        }
      }

      Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                skinsRestorer.getSkinStorage().setCustomSkinData("lebron_custom", lebronSkin);
                skinsRestorer.getPlayerStorage().setSkinIdOfPlayer(player.getUniqueId(), skinId);
                try {
                  skinsRestorer.getSkinApplier(Player.class).applySkin(player);
                } catch (DataRequestException e) {
                  e.printStackTrace();
                }
              },
              1L);

    } else if (originalSkins.containsKey(player.getUniqueId())) {
      String original = originalSkins.remove(player.getUniqueId());
      String skinName = "orig_" + player.getUniqueId(); // Unique ID for temporary original skin

      skinsRestorer.getSkinStorage().setCustomSkinData(skinName, SkinProperty.of(original, ""));
      skinsRestorer
          .getPlayerStorage()
          .setSkinIdOfPlayer(player.getUniqueId(), SkinIdentifier.ofCustom(skinName));
      skinsRestorer.getSkinApplier(Player.class).applySkin(player);
    }
  }

  /**
   * Checks whether the player is wearing the full Lebron armor set.
   *
   * @param player The player to check
   * @return True if the player is wearing all four armor pieces, false otherwise
   */
  private boolean isWearingFullLebronSet(Player player) {
    return hasModel(player.getInventory().getHelmet(), "storytime:lebron_helmet")
        && hasModel(player.getInventory().getChestplate(), "storytime:lebron_chestplate")
        && hasModel(player.getInventory().getLeggings(), "storytime:lebron_leggings")
        && hasModel(player.getInventory().getBoots(), "storytime:lebron_boots");
  }

  /**
   * Checks whether the given item has a custom model matching the expected ID.
   *
   * @param item The armor item to check
   * @param expectedModel The expected custom model key
   * @return True if the item matches the expected model, false otherwise
   */
  private boolean hasModel(ItemStack item, String expectedModel) {
    if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    NamespacedKey key = meta.getItemModel();
    return key != null && key.toString().equals(expectedModel);
  }
}
