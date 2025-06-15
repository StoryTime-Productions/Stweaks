package com.storytimeproductions.models.stgames;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a 3D cuboid defined by two opposite corners in a Minecraft world. This class provides
 * methods to check if a given location is within the cuboid.
 */
public class Cuboid {
  public final World world;
  private final int x1;
  private final int y1;
  private final int z1;
  private final int x2;
  private final int y2;
  private final int z2;

  /**
   * Constructs a Cuboid with the specified world and corner coordinates.
   *
   * @param world The world in which the cuboid exists.
   * @param x1 The x-coordinate of one corner.
   * @param y1 The y-coordinate of one corner.
   * @param z1 The z-coordinate of one corner.
   * @param x2 The x-coordinate of the opposite corner.
   * @param y2 The y-coordinate of the opposite corner.
   * @param z2 The z-coordinate of the opposite corner.
   */
  public Cuboid(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
    this.world = world;
    this.x1 = Math.min(x1, x2);
    this.y1 = Math.min(y1, y2);
    this.z1 = Math.min(z1, z2);
    this.x2 = Math.max(x1, x2);
    this.y2 = Math.max(y1, y2);
    this.z2 = Math.max(z1, z2);
  }

  /**
   * Checks if the specified location is within this cuboid.
   *
   * @param loc The location to check.
   * @return true if the location is within the cuboid, false otherwise.
   */
  public boolean contains(Location loc) {
    if (!loc.getWorld().equals(world)) {
      return false;
    }
    int lx = loc.getBlockX();
    int ly = loc.getBlockY();
    int lz = loc.getBlockZ();
    return lx >= x1 && lx <= x2 && ly >= y1 && ly <= y2 && lz >= z1 && lz <= z2;
  }

  /**
   * Returns a list of all block locations within this cuboid.
   *
   * @return List of Locations inside the cuboid.
   */
  public List<Location> getLocations() {
    List<Location> locations = new ArrayList<>();
    for (int x = x1; x <= x2; x++) {
      for (int y = y1; y <= y2; y++) {
        for (int z = z1; z <= z2; z++) {
          locations.add(new Location(world, x, y, z));
        }
      }
    }
    return locations;
  }
}
