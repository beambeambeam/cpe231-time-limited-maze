package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.components.ButtonComponent;
import cpe231.finalproject.timelimitedmaze.gui.components.DropdownComponent;
import cpe231.finalproject.timelimitedmaze.gui.state.GAAnimationState;
import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import com.raylib.Raylib;
import java.util.List;
import java.util.Map;

public final class GAInputHandler {
  private final ButtonComponent playStopButton;

  public GAInputHandler() {
    int panelX = GUIConstants.MAZE_PANEL_WIDTH;
    this.playStopButton = new ButtonComponent(panelX + 10,
        GUIConstants.WINDOW_HEIGHT - GUIConstants.BUTTON_HEIGHT - 20);
  }

  public InputResult handleInput(GAAnimationState state) {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    if (!Raylib.IsMouseButtonPressed(Raylib.MOUSE_BUTTON_LEFT)) {
      return new InputResult(InputType.NONE, null);
    }

    int panelX = GUIConstants.MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int mazeDropdownY = GUIConstants.PADDING + 30 + 10;

    int maxMazeOptionsHeight = calculateMaxMazeOptionsHeight(state.availableMazeFiles(), state.mazeValidityMap());
    int speedDropdownY = mazeDropdownY + GUIConstants.DROPDOWN_HEIGHT + maxMazeOptionsHeight + 10;

    DropdownComponent<String> mazeDropdown = new DropdownComponent<>(dropdownX, mazeDropdownY, fileName -> fileName);
    DropdownComponent<Integer> speedDropdown = new DropdownComponent<>(dropdownX, speedDropdownY,
        speed -> speed + " ms/gen");

    Integer mazeClicked = mazeDropdown.checkClickWithValidityMap(mousePos, state.availableMazeFiles(),
        state.mazeDropdownOpen(), state.mazeValidityMap());
    Integer speedClicked = speedDropdown.checkClick(mousePos, List.of(100, 500, 1000), state.speedDropdownOpen());
    boolean playStopClicked = playStopButton.checkClick(mousePos);

    if (mazeClicked != null) {
      if (mazeClicked == -1) {
        return new InputResult(InputType.TOGGLE_MAZE_DROPDOWN, null);
      } else {
        String selectedFileName = state.availableMazeFiles().get(mazeClicked);
        if (state.mazeValidityMap().getOrDefault(selectedFileName, false)) {
          return new InputResult(InputType.SELECT_MAZE, selectedFileName);
        }
      }
    } else if (speedClicked != null) {
      if (speedClicked == -1) {
        return new InputResult(InputType.TOGGLE_SPEED_DROPDOWN, null);
      } else {
        int[] speeds = { 100, 500, 1000 };
        if (speedClicked >= 0 && speedClicked < speeds.length) {
          return new InputResult(InputType.SELECT_SPEED, speeds[speedClicked]);
        }
      }
    } else if (playStopClicked) {
      if (state.maze() != null) {
        return new InputResult(InputType.TOGGLE_PLAY_STOP, null);
      }
    } else {
      if (state.mazeDropdownOpen() || state.speedDropdownOpen()) {
        return new InputResult(InputType.CLOSE_DROPDOWNS, null);
      }
    }

    return new InputResult(InputType.NONE, null);
  }

  private int calculateMaxMazeOptionsHeight(List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap) {
    int validCount = 0;
    for (String fileName : availableMazeFiles) {
      if (mazeValidityMap.getOrDefault(fileName, false)) {
        validCount++;
      }
    }
    return validCount * GUIConstants.DROPDOWN_OPTION_HEIGHT;
  }

  public enum InputType {
    NONE, TOGGLE_MAZE_DROPDOWN, SELECT_MAZE, TOGGLE_SPEED_DROPDOWN, SELECT_SPEED, TOGGLE_PLAY_STOP, CLOSE_DROPDOWNS
  }

  public record InputResult(InputType type, Object data) {
  }
}
