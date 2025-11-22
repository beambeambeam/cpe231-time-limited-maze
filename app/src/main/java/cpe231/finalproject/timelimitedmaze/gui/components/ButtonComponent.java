package cpe231.finalproject.timelimitedmaze.gui.components;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import com.raylib.Raylib;
import com.raylib.Colors;
import com.raylib.Helpers;

public final class ButtonComponent {
  private final int x;
  private final int y;
  private final int width;
  private final int height;

  public ButtonComponent(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public ButtonComponent(int x, int y) {
    this(x, y, GUIConstants.BUTTON_WIDTH, GUIConstants.BUTTON_HEIGHT);
  }

  public void render(String text, boolean isActive, Raylib.Color activeColor, Raylib.Color inactiveColor) {
    Raylib.Color buttonColor = isActive ? activeColor : inactiveColor;

    Raylib.DrawRectangle(x, y, width, height, buttonColor);
    Raylib.DrawRectangleLines(x, y, width, height, GUIConstants.BUTTON_BORDER_COLOR);

    int textWidth = Raylib.MeasureText(text, 18);
    int textX = x + (width - textWidth) / 2;
    int textY = y + (height - 18) / 2;
    Raylib.DrawText(text, textX, textY, 18, Colors.WHITE);
  }

  public boolean checkClick(Raylib.Vector2 mousePos) {
    Raylib.Rectangle buttonRect = Helpers.newRectangle(x, y, width, height);
    return Raylib.CheckCollisionPointRec(mousePos, buttonRect);
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
}
