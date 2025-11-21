package cpe231.finalproject.timelimitedmaze.utils;

import java.util.List;

public final class MazeFileLister {
  private MazeFileLister() {
  }

  public static List<String> listMazeFiles() {
    return List.of(
        "m100_100.txt",
        "m100_90.txt",
        "m15_15.txt",
        "m24_20.txt",
        "m30_30.txt",
        "m33_35.txt",
        "m40_40.txt",
        "m40_45.txt",
        "m45_45.txt",
        "m50_50.txt",
        "m60_60.txt",
        "m70_60.txt",
        "m80_50.txt"
    );
  }
}

