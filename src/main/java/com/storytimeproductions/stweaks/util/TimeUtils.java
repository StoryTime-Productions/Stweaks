package com.storytimeproductions.stweaks.util;

import com.storytimeproductions.stweaks.config.SettingsManager;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * A utility class for time-related functions in the plugin.
 *
 * <p>This class provides methods for calculating playtime multipliers based on the current day of
 * the week. Specifically, it checks if today is a weekend (Saturday or Sunday) and applies a
 * weekend multiplier.
 */
public class TimeUtils {

  /**
   * Retrieves the multiplier for the current day.
   *
   * <p>This method checks if the current day is a weekend (Saturday or Sunday). If it is, it
   * returns the weekend multiplier configured in the plugin settings. Otherwise, it returns a
   * default multiplier of 1.0 (no multiplier).
   *
   * @return The multiplier for the current day based on the day of the week.
   */
  public static double getTodayMultiplier() {
    DayOfWeek today = LocalDate.now().getDayOfWeek();
    return (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY)
        ? SettingsManager.getWeekendMultiplier()
        : 1.0;
  }

  /**
   * Breaks down a total number of seconds into hours, minutes, and seconds.
   *
   * <p>This method takes a double representing total seconds and converts it into an array of three
   * integers: hours, minutes, and seconds.
   *
   * @param totalSeconds The total number of seconds to break down.
   * @return An array containing the breakdown of hours, minutes, and seconds.
   */
  public static int[] breakdownSeconds(double totalSeconds) {
    int total = (int) Math.floor(totalSeconds);
    int hours = total / 3600;
    int minutes = (total % 3600) / 60;
    int seconds = total % 60;
    return new int[] {hours, minutes, seconds};
  }
}
