package cpe231.finalproject.timelimitedmaze.gui.components;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import com.raylib.Raylib;
import com.raylib.Colors;
import com.raylib.Helpers;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class DropdownComponent<T> {
  private final int x;
  private final int y;
  private final Function<T, String> textExtractor;
  private final Predicate<T> optionFilter;

  public DropdownComponent(int x, int y, Function<T, String> textExtractor) {
    this(x, y, textExtractor, option -> true);
  }

  public DropdownComponent(int x, int y, Function<T, String> textExtractor, Predicate<T> optionFilter) {
    this.x = x;
    this.y = y;
    this.textExtractor = textExtractor;
    this.optionFilter = optionFilter;
  }

  public void render(List<T> options, T selectedOption, boolean isOpen, String placeholder) {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    String displayText = selectedOption != null
        ? textExtractor.apply(selectedOption)
        : placeholder;

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(x, y, GUIConstants.DROPDOWN_WIDTH, GUIConstants.DROPDOWN_HEIGHT);

    Raylib.DrawRectangle((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(),
        GUIConstants.DROPDOWN_BACKGROUND_COLOR);
    Raylib.DrawRectangleLines((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(),
        GUIConstants.DROPDOWN_BORDER_COLOR);

    Raylib.DrawText(displayText, x + 5, y + 6, 16, Colors.WHITE);

    int arrowX = x + GUIConstants.DROPDOWN_WIDTH - 20;
    int arrowY = y + GUIConstants.DROPDOWN_HEIGHT / 2;
    if (isOpen) {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY - 5),
          Helpers.newVector2(arrowX + 10, arrowY - 5),
          Helpers.newVector2(arrowX + 5, arrowY + 5),
          GUIConstants.DROPDOWN_BORDER_COLOR);
    } else {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY + 5),
          Helpers.newVector2(arrowX + 10, arrowY + 5),
          Helpers.newVector2(arrowX + 5, arrowY - 5),
          GUIConstants.DROPDOWN_BORDER_COLOR);
    }

    if (isOpen) {
      List<T> validOptions = options.stream()
          .filter(optionFilter)
          .toList();

      int totalOptionsHeight = validOptions.size() * GUIConstants.DROPDOWN_OPTION_HEIGHT;
      int optionsStartY = y + GUIConstants.DROPDOWN_HEIGHT;

      Raylib.DrawRectangle(x, optionsStartY, GUIConstants.DROPDOWN_WIDTH, totalOptionsHeight,
          GUIConstants.DROPDOWN_BACKGROUND_COLOR);
      Raylib.DrawRectangleLines(x, optionsStartY, GUIConstants.DROPDOWN_WIDTH, totalOptionsHeight,
          GUIConstants.DROPDOWN_BORDER_COLOR);

      for (int i = 0; i < validOptions.size(); i++) {
        T option = validOptions.get(i);
        int optionY = optionsStartY + i * GUIConstants.DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(x, optionY, GUIConstants.DROPDOWN_WIDTH, GUIConstants.DROPDOWN_OPTION_HEIGHT);

        boolean isHovered = Raylib.CheckCollisionPointRec(mousePos, optionRect);

        if (isHovered) {
          Raylib.DrawRectangle((int) optionRect.x(), (int) optionRect.y(),
              (int) optionRect.width(), (int) optionRect.height(),
              GUIConstants.DROPDOWN_HOVER_COLOR);
        }

        Raylib.DrawText(textExtractor.apply(option), x + 5, optionY + 6, 16, Colors.WHITE);
      }
    }
  }

  public void renderWithValidityMap(List<T> options, T selectedOption, boolean isOpen, String placeholder,
      Map<T, Boolean> validityMap) {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    String displayText = selectedOption != null
        ? textExtractor.apply(selectedOption)
        : placeholder;

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(x, y, GUIConstants.DROPDOWN_WIDTH, GUIConstants.DROPDOWN_HEIGHT);

    Raylib.DrawRectangle((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(),
        GUIConstants.DROPDOWN_BACKGROUND_COLOR);
    Raylib.DrawRectangleLines((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(),
        GUIConstants.DROPDOWN_BORDER_COLOR);

    Raylib.DrawText(displayText, x + 5, y + 6, 16, Colors.WHITE);

    int arrowX = x + GUIConstants.DROPDOWN_WIDTH - 20;
    int arrowY = y + GUIConstants.DROPDOWN_HEIGHT / 2;
    if (isOpen) {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY - 5),
          Helpers.newVector2(arrowX + 10, arrowY - 5),
          Helpers.newVector2(arrowX + 5, arrowY + 5),
          GUIConstants.DROPDOWN_BORDER_COLOR);
    } else {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY + 5),
          Helpers.newVector2(arrowX + 10, arrowY + 5),
          Helpers.newVector2(arrowX + 5, arrowY - 5),
          GUIConstants.DROPDOWN_BORDER_COLOR);
    }

    if (isOpen) {
      int totalOptionsHeight = options.size() * GUIConstants.DROPDOWN_OPTION_HEIGHT;
      int optionsStartY = y + GUIConstants.DROPDOWN_HEIGHT;

      Raylib.DrawRectangle(x, optionsStartY, GUIConstants.DROPDOWN_WIDTH, totalOptionsHeight,
          GUIConstants.DROPDOWN_BACKGROUND_COLOR);
      Raylib.DrawRectangleLines(x, optionsStartY, GUIConstants.DROPDOWN_WIDTH, totalOptionsHeight,
          GUIConstants.DROPDOWN_BORDER_COLOR);

      for (int i = 0; i < options.size(); i++) {
        T option = options.get(i);
        boolean isValid = validityMap.getOrDefault(option, false);
        int optionY = optionsStartY + i * GUIConstants.DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(x, optionY, GUIConstants.DROPDOWN_WIDTH, GUIConstants.DROPDOWN_OPTION_HEIGHT);

        if (isValid) {
          boolean isHovered = Raylib.CheckCollisionPointRec(mousePos, optionRect);

          if (isHovered) {
            Raylib.DrawRectangle((int) optionRect.x(), (int) optionRect.y(),
                (int) optionRect.width(), (int) optionRect.height(),
                GUIConstants.DROPDOWN_HOVER_COLOR);
          }

          Raylib.DrawText(textExtractor.apply(option), x + 5, optionY + 6, 16, Colors.WHITE);
        } else {
          Raylib.DrawRectangle((int) optionRect.x(), (int) optionRect.y(),
              (int) optionRect.width(), (int) optionRect.height(),
              GUIConstants.DROPDOWN_DISABLED_COLOR);
          Raylib.DrawText(textExtractor.apply(option), x + 5, optionY + 6, 16, GUIConstants.DROPDOWN_DISABLED_TEXT_COLOR);
        }
      }
    }
  }

  public Integer checkClick(Raylib.Vector2 mousePos, List<T> options, boolean isOpen) {
    Raylib.Rectangle dropdownRect = Helpers.newRectangle(x, y, GUIConstants.DROPDOWN_WIDTH, GUIConstants.DROPDOWN_HEIGHT);

    if (Raylib.CheckCollisionPointRec(mousePos, dropdownRect)) {
      return -1;
    }

    if (isOpen) {
      List<T> validOptions = options.stream()
          .filter(optionFilter)
          .toList();

      for (int i = 0; i < validOptions.size(); i++) {
        int optionY = y + GUIConstants.DROPDOWN_HEIGHT + i * GUIConstants.DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(x, optionY, GUIConstants.DROPDOWN_WIDTH, GUIConstants.DROPDOWN_OPTION_HEIGHT);

        if (Raylib.CheckCollisionPointRec(mousePos, optionRect)) {
          T clickedOption = validOptions.get(i);
          int originalIndex = options.indexOf(clickedOption);
          return originalIndex;
        }
      }
    }

    return null;
  }

  public Integer checkClickWithValidityMap(Raylib.Vector2 mousePos, List<T> options, boolean isOpen,
      Map<T, Boolean> validityMap) {
    Raylib.Rectangle dropdownRect = Helpers.newRectangle(x, y, GUIConstants.DROPDOWN_WIDTH, GUIConstants.DROPDOWN_HEIGHT);

    if (Raylib.CheckCollisionPointRec(mousePos, dropdownRect)) {
      return -1;
    }

    if (isOpen) {
      int validIndex = 0;
      for (int i = 0; i < options.size(); i++) {
        T option = options.get(i);
        if (!validityMap.getOrDefault(option, false)) {
          continue;
        }
        int optionY = y + GUIConstants.DROPDOWN_HEIGHT + validIndex * GUIConstants.DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(x, optionY, GUIConstants.DROPDOWN_WIDTH, GUIConstants.DROPDOWN_OPTION_HEIGHT);

        if (Raylib.CheckCollisionPointRec(mousePos, optionRect)) {
          return i;
        }
        validIndex++;
      }
    }

    return null;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }
}
