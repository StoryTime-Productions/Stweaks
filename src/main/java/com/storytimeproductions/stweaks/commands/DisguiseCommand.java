package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.stweaks.util.EntityDisguiseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command for managing entity disguises in the lobby. Allows players to manually trigger disguise
 * operations.
 */
public class DisguiseCommand implements CommandExecutor {

  private final EntityDisguiseManager disguiseManager;

  /**
   * Constructor for DisguiseCommand.
   *
   * @param disguiseManager the EntityDisguiseManager instance used to manage disguises
   */
  public DisguiseCommand(EntityDisguiseManager disguiseManager) {
    this.disguiseManager = disguiseManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("stweaks.disguise")) {
      sender.sendMessage(
          Component.text("[Stweaks] ", NamedTextColor.YELLOW)
              .append(
                  Component.text(
                      "You don't have permission to use this command.", NamedTextColor.WHITE)));
      return true;
    }

    if (args.length == 0) {
      // Default action: apply disguises
      disguiseManager.disguiseSlimesInLobby();
      sender.sendMessage(
          Component.text("[Stweaks] ", NamedTextColor.YELLOW)
              .append(
                  Component.text(
                      "Applied disguises to slimes in the lobby world.", NamedTextColor.WHITE)));
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "apply":
        disguiseManager.disguiseSlimesInLobby();
        sender.sendMessage(
            Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                .append(
                    Component.text(
                        "Applied disguises to slimes in the lobby world.", NamedTextColor.WHITE)));
        break;
      case "remove":
        disguiseManager.removeAllDisguises();
        sender.sendMessage(
            Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                .append(
                    Component.text(
                        "Removed all disguises from slimes in the lobby world.",
                        NamedTextColor.WHITE)));
        break;
      case "reload":
        disguiseManager.removeAllDisguises();
        disguiseManager.disguiseSlimesInLobby();
        sender.sendMessage(
            Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                .append(
                    Component.text(
                        "Reloaded all disguises in the lobby world.", NamedTextColor.WHITE)));
        break;
      default:
        sender.sendMessage(
            Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                .append(
                    Component.text(
                        "Usage: /disguise [apply|remove|reload]", NamedTextColor.WHITE)));
        break;
    }
    return true;
  }
}
