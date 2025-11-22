package cpe231.finalproject.timelimitedmaze.gui.utils;

import com.raylib.Helpers;
import com.raylib.Raylib;

public final class GUIConstants {
  private GUIConstants() {
  }

  public static final int WINDOW_WIDTH = 1200;
  public static final int WINDOW_HEIGHT = 800;
  public static final int STATS_PANEL_WIDTH = 300;
  public static final int MAZE_PANEL_WIDTH = WINDOW_WIDTH - STATS_PANEL_WIDTH;
  public static final int MAZE_PANEL_HEIGHT = WINDOW_HEIGHT;
  public static final int PADDING = 20;
  public static final int DROPDOWN_WIDTH = 250;
  public static final int DROPDOWN_HEIGHT = 30;
  public static final int DROPDOWN_OPTION_HEIGHT = 30;
  public static final int BUTTON_WIDTH = 120;
  public static final int BUTTON_HEIGHT = 40;

  public static final int MAX_GENERATIONS = 200;
  public static final int POPULATION_SIZE = 500;
  public static final double ELITE_PERCENTAGE = 0.05;

  public static final Raylib.Color BACKGROUND_COLOR = Helpers.newColor((byte) 20, (byte) 20, (byte) 20, (byte) 255);
  public static final Raylib.Color STATS_PANEL_COLOR = Helpers.newColor((byte) 35, (byte) 35, (byte) 40, (byte) 255);
  public static final Raylib.Color DROPDOWN_BACKGROUND_COLOR = Helpers.newColor((byte) 50, (byte) 50, (byte) 55, (byte) 255);
  public static final Raylib.Color DROPDOWN_BORDER_COLOR = Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255);
  public static final Raylib.Color DROPDOWN_HOVER_COLOR = Helpers.newColor((byte) 60, (byte) 60, (byte) 65, (byte) 255);
  public static final Raylib.Color DROPDOWN_DISABLED_COLOR = Helpers.newColor((byte) 35, (byte) 35, (byte) 40, (byte) 255);
  public static final Raylib.Color DROPDOWN_DISABLED_TEXT_COLOR = Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255);
  public static final Raylib.Color BUTTON_BORDER_COLOR = Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255);
  public static final Raylib.Color PATH_LINE_COLOR = Helpers.newColor((byte) 0, (byte) 100, (byte) 255, (byte) 200);
}
