package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.gui.utils.WindowManager;

public final class GAAnimation extends WindowManager {
  private final GAAnimationController controller;

  public GAAnimation() {
    super(GUIConstants.WINDOW_WIDTH, GUIConstants.WINDOW_HEIGHT, "GA Animation");
    this.controller = new GAAnimationController();
  }

  public static void main(String[] args) {
    GAAnimation animation = new GAAnimation();
    animation.show();
  }

  @Override
  protected void handleInput() {
    controller.handleInput();
  }

  @Override
  protected void render() {
    controller.updateAnimation();
    controller.render();
  }
}
