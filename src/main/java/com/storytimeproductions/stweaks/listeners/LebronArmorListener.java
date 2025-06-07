package com.storytimeproductions.stweaks.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.Pair;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.storytimeproductions.stweaks.Stweaks;
import java.util.ArrayList;
import java.util.List;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener that handles applying and restoring the "Lebron" skin based on whether a player is
 * wearing a full set of custom Lebron armor.
 */
public class LebronArmorListener implements Listener {

  private final Stweaks plugin;
  private final SkinsRestorer skinsRestorer = SkinsRestorerProvider.get();

  /** The custom skin property representing the Lebron skin. */
  private final SkinProperty lebronSkin =
      SkinProperty.of(
          "ewogICJ0aW1lc3RhbXAiIDogMTc0NTYxOTEwMzkyNywKICAicHJvZmlsZUlkIiA6ICJiMjlj"
              + "NTE1MTFjY2U0NzQ1OTY0YTAxOWFlZDliOTM0MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJpaUx4"
              + "bnoiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAg"
              + "ICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQu"
              + "bmV0L3RleHR1cmUvNjJhZWFiMzAwYjIyNzVlYTlmODE0NjJiNGMzZTU3MmMzOWU5NzQ0MDc5"
              + "MDhiMzliMDE2OWYzMDNhMTE3YzkwMiIKICAgIH0sCiAgICAiQ0FQRSIgOiB7CiAgICAgICJ1"
              + "cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJjMjFlMjIy"
              + "NTI4ZTMwZGM4ODQ0NTMxNGY3YmU2ZmYxMmQzYWVlYmMzYzE5MjA1NGZiYTdlM2IzZjhjNzdi"
              + "MSIKICAgIH0KICB9Cn0=",
          "nwBFTurCe8y2tILGs1HpKf7ghsW7RL66FH5S1tPohpVJBFZkKVowN5pnO1XZ/HCII8ItNSlc"
              + "njiS2SxWQLl1o4kquLyBu9WHyT1UX7jPTHpgtCW3vOThaS5e33vLlYaRDyLxGYg+IShtr7Wiy"
              + "f9m6R4WuI4sIjKYvoOnMis1XBsbpnjkcGTpswXBf/aSVeveTGVAsdkl2won7vWg9XFvEcERV9"
              + "Bz8UMZ4oxCPp6CqHg1YpfolDso0YFWa4iwJyPIC2z4wDKWNwHu8r7cRMOhH2caEuH5mIR2lv9"
              + "f4U9tDyHC3IhMngzkyOM4KWgbEWj6JiYu/dnDdfiSeYfH86ZwCxbl7CYJNEMcZxEicYgj4Apf"
              + "ZcJ9694FP0yRTqHQB9/bAK7UzkDb81BkQ6VX8vVRxmk1gx6jO7MYk81E+WAFnjVp+K3GMntqq"
              + "l7+KuImbjV1WzmrtTN23lmBtOU2KWKMJP8dsOlX5IlhiWecfzXQ0K+hzS6NECR1VOkavPG8Gx"
              + "ptdqt1mbIyNdsAdDR3rZJQXhD6vwyDuV4xWHEm9WXmrGxYRM0MU/UWRG2oA1dH/DX5rO7yBWV"
              + "nm1TZ42iIlTPWQSKCA1BhexZ+GNI1j+VxiCdvlQghIR3brvkyp2Z5tABkRulHaH4b3nn2Ui3B"
              + "3jacTgrbFKY/lAtdip7SZkXobr8=");

  /** A map to track and restore each player's original skin using their UUID. */
  private final ConcurrentHashMap<UUID, String> originalSkins = new ConcurrentHashMap<>();

  /** A map to track whether a player has the full armor set equipped. */
  private final ConcurrentHashMap<UUID, Boolean> hasFullSetFlag = new ConcurrentHashMap<>();

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
    skinsRestorer.getPlayerStorage().removeSkinIdOfPlayer(player.getUniqueId());
    boolean hasFullSet = isWearingFullLebronSet(player);

    if (hasFullSet) {
      if (!originalSkins.containsKey(player.getUniqueId())) {
        try {
          Optional<SkinProperty> current =
              skinsRestorer
                  .getPlayerStorage()
                  .getSkinForPlayer(player.getUniqueId(), player.getName(), true);

          current.ifPresent(
              prop -> {
                originalSkins.put(player.getUniqueId(), prop.getValue());
              });

        } catch (DataRequestException e) {
          e.printStackTrace();
        }
      }

      // Apply custom skin only if this is the first time wearing the full set
      if (!hasFullSetFlag.getOrDefault(player.getUniqueId(), false)) {
        applySkin(player);
        givePermanentEffects(player);
        playSunshineSound(player);
        hasFullSetFlag.put(player.getUniqueId(), true);
      }

      new BukkitRunnable() {
        @Override
        public void run() {
          sendInvisibleArmor(player);
        }
      }.runTaskLater(plugin, 2L);

    } else {
      if (hasFullSetFlag.getOrDefault(player.getUniqueId(), false)) {
        hasFullSetFlag.put(player.getUniqueId(), false);
        removePermanentEffects(player);
        String muteCommand = String.format("stopsound %s", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), muteCommand);
        if (originalSkins.containsKey(player.getUniqueId())) {
          String original = originalSkins.remove(player.getUniqueId());
          restoreOriginalSkin(player, original);
        } else {
          try {
            Optional<SkinProperty> current =
                skinsRestorer
                    .getPlayerStorage()
                    .getSkinForPlayer(player.getUniqueId(), player.getName());
            current.ifPresent(prop -> restoreOriginalSkin(player, prop.getValue()));
          } catch (DataRequestException e) {
            e.printStackTrace();
          }
        }
      }

      new BukkitRunnable() {
        @Override
        public void run() {
          sendVisibleArmor(player);
        }
      }.runTaskLater(plugin, 2L);
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

  private void givePermanentEffects(Player player) {
    player.addPotionEffect(
        new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false));
    player.addPotionEffect(
        new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, true, false));
    player.addPotionEffect(
        new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, true, false));
  }

  private void playSunshineSound(Player player) {
    String muteCommand = String.format("stopsound %s", player.getName());
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), muteCommand);

    // Then, play the custom song
    String playCommand =
        String.format(
            "playsound minecraft:you_are_my_sunshine music %s ~ ~ ~ 1.0 1.0 1.0", player.getName());
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playCommand);
  }

  private void removePermanentEffects(Player player) {
    player.removePotionEffect(PotionEffectType.SPEED);
    player.removePotionEffect(PotionEffectType.STRENGTH);
    player.removePotionEffect(PotionEffectType.JUMP_BOOST);
  }

  /**
   * Applies the custom Lebron skin to the player.
   *
   * @param player The player to apply the skin to
   */
  private void applySkin(Player player) {
    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              skinsRestorer.getSkinStorage().setCustomSkinData("lebron_custom", lebronSkin);
              skinsRestorer
                  .getPlayerStorage()
                  .setSkinIdOfPlayer(
                      player.getUniqueId(), SkinIdentifier.ofCustom("lebron_custom"));
              try {
                skinsRestorer.getSkinApplier(Player.class).applySkin(player);
              } catch (DataRequestException e) {
                e.printStackTrace();
              }
            },
            1L);
  }

  /**
   * Restores the player's original skin.
   *
   * @param player The player to restore the skin for
   * @param skinValue The skin value to restore
   */
  private void restoreOriginalSkin(Player player, String skinValue) {
    String command = "skinsrestorer applySkin " + player.getName();
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
  }

  /**
   * Sends a packet to make the player's armor invisible.
   *
   * @param player The player whose armor will be made invisible
   */
  private void sendInvisibleArmor(Player player) {
    sendArmorPacket(player, true);
  }

  /**
   * Sends a packet to make the player's armor visible.
   *
   * @param player The player whose armor will be made visible
   */
  private void sendVisibleArmor(Player player) {
    sendArmorPacket(player, false);
  }

  /**
   * Sends a packet to change the visibility of the player's armor.
   *
   * @param player The player whose armor visibility is being changed
   * @param hide Whether to hide (true) or show (false) the armor
   */
  private void sendArmorPacket(Player player, boolean hide) {
    List<Pair<ItemSlot, ItemStack>> equipment = new ArrayList<>();
    equipment.add(
        new Pair<>(
            ItemSlot.HEAD, hide ? new ItemStack(Material.AIR) : player.getInventory().getHelmet()));
    equipment.add(
        new Pair<>(
            ItemSlot.CHEST,
            hide ? new ItemStack(Material.AIR) : player.getInventory().getChestplate()));
    equipment.add(
        new Pair<>(
            ItemSlot.LEGS,
            hide ? new ItemStack(Material.AIR) : player.getInventory().getLeggings()));
    equipment.add(
        new Pair<>(
            ItemSlot.FEET, hide ? new ItemStack(Material.AIR) : player.getInventory().getBoots()));

    // Creating packet for player model visibility change
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
    packet.getIntegers().write(0, player.getEntityId()); // Player's entity ID
    packet.getSlotStackPairLists().write(0, equipment);

    // Send packet to all online players to hide or show armor on the player model
    for (Player viewer : Bukkit.getOnlinePlayers()) {
      ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
    }
  }
}
