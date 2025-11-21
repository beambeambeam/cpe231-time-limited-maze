package cpe231.finalproject.timelimitedmaze;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

final class AppTest {

  @Test
  void appExposesMainMethod() throws NoSuchMethodException {
    assertNotNull(App.class.getDeclaredMethod("main", String[].class));
  }
}
