package cpe231.finalproject.timelimitedmaze.gui.utils;

import com.raylib.Raylib;
import java.io.InputStream;

public final class TextureManager {
  private Raylib.Texture texture;
  private boolean textureLoaded = false;

  public TextureManager() {
  }

  public void initialize() {
    if (!textureLoaded) {
      texture = loadImage();
      textureLoaded = true;
    }
  }

  private void ensureTextureLoaded() {
    if (!textureLoaded && texture == null) {
      texture = loadImage();
      textureLoaded = true;
    }
  }

  private Raylib.Texture loadImage() {
    try {
      InputStream imageStream = getClass().getClassLoader().getResourceAsStream("ascii-art.png");
      if (imageStream == null) {
        System.err.println("Warning: Could not find ascii-art.png in resources");
        return null;
      }

      String tempPath = System.getProperty("java.io.tmpdir") + "/chiikawa_temp_" + System.currentTimeMillis() + ".jpg";
      java.nio.file.Files.copy(imageStream, java.nio.file.Paths.get(tempPath),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      imageStream.close();

      Raylib.Image image = Raylib.LoadImage(tempPath);
      if (image == null) {
        System.err.println("Warning: Failed to load image from " + tempPath);
        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempPath));
        return null;
      }

      Raylib.Texture loadedTexture = Raylib.LoadTextureFromImage(image);
      Raylib.UnloadImage(image);

      java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempPath));

      return loadedTexture;
    } catch (Exception e) {
      System.err.println("Error loading ascii-art.png: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  public void render(int panelX, int y, int maxWidth, int maxHeight) {
    ensureTextureLoaded();
    if (texture != null) {
      int textureWidth = texture.width();
      int textureHeight = texture.height();

      float scaleX = (float) maxWidth / textureWidth;
      float scaleY = (float) maxHeight / textureHeight;
      float scale = Math.min(scaleX, scaleY);

      int imageWidth = (int) (textureWidth * scale);
      int imageX = panelX + (GUIConstants.STATS_PANEL_WIDTH - imageWidth) / 2;

      Raylib.Vector2 position = com.raylib.Helpers.newVector2(imageX, y);
      Raylib.DrawTextureEx(texture, position, 0.0f, scale, com.raylib.Colors.WHITE);
    }
  }
}
